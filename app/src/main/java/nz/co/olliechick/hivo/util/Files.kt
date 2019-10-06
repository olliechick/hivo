package nz.co.olliechick.hivo.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import nz.co.olliechick.hivo.R
import nz.co.olliechick.hivo.util.Constants.Companion.bitsPerSample
import nz.co.olliechick.hivo.util.Constants.Companion.blockAlign
import nz.co.olliechick.hivo.util.Constants.Companion.fileExt
import nz.co.olliechick.hivo.util.Constants.Companion.numChannels
import nz.co.olliechick.hivo.util.Constants.Companion.providerPath
import nz.co.olliechick.hivo.util.Constants.Companion.samplingRateHz
import nz.co.olliechick.hivo.util.Constants.Companion.sizeOfWavHeader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Files {
    companion object {

        // Getters

        /**
         * Returns the directory (external storage)/HiVo recordings
         * If necessary, creates it.
         */
        private fun getPublicDirectory(context: Context): File? = File(
            Environment.getExternalStorageDirectory(),
            context.getString(R.string.hivo_recordings)
        ).apply { mkdirs() }

        private fun getPrivateDirectory(context: Context): File = context.filesDir

        fun getRawFile(context: Context) = File(getPrivateDirectory(context), "recording.wav")

        private fun getLocationForCroppedFile(context: Context) = File(getPrivateDirectory(context), "cropped.wav")

        fun rawFileExists(context: Context) = getRawFile(context).exists()

        // Saving WAVs

        /**
         * Crops audio from the raw file to start at [millisBeforeStart] ms into the recording,
         * and end [durationInMillis] ms later.
         * Saves this to (external storage)/HiVo recordings/[filename].wav
         *
         * @return  <code>true</code> if and only if the saving succeeded;
         *          <code>false</code> otherwise
         */
        fun saveWav(filename: String, millisBeforeStart: Long, durationInMillis: Long, context: Context): Boolean {
            return try {

                // Create the cropped version in the same directory as the raw file

                val rawFile = getRawFile(context)
                val croppedFile = getLocationForCroppedFile(context)
                val rawFileRandomAccessChannel = RandomAccessFile(rawFile, "r").channel
                val startByte = sizeOfWavHeader + blockAlign * millisBeforeStart * (samplingRateHz / 1000)
                val byteSize = blockAlign * durationInMillis * (samplingRateHz / 1000)

                // Write headers
                FileOutputStream(croppedFile).run {
                    writeHeaders(this)
                    close()
                }

                // Copy over the data
                RandomAccessFile(croppedFile, "rw").use { toFile ->
                    toFile.channel.use { toChannel ->
                        rawFileRandomAccessChannel.position(startByte)
                        toChannel.transferFrom(rawFileRandomAccessChannel, sizeOfWavHeader.toLong(), byteSize)
                    }
                }

                // Update headers
                updateHeaders(croppedFile)

                // Move the cropped version to external storage

                val waveFile = File(getPublicDirectory(context), filename + fileExt)
                val successfulRename = croppedFile.renameTo(waveFile)

                // If renaming the raw file didn't work (e.g. because it is on a different drive), then move it.
                if (!successfulRename) {
                    croppedFile.copyTo(waveFile, overwrite = true)
                    //todo delete origial
                }

                true
            } catch (e: IOException) {
                false
            }
        }

        /**
         * Copies the entire raw file to (external storage)/HiVo recordings/[filename].wav
         *
         * @return  <code>true</code> if and only if the renaming succeeded;
         *          <code>false</code> otherwise
         */
        fun saveWav(filename: String, context: Context): Boolean {
            val rawFile = getRawFile(context)
            val waveFile = File(getPublicDirectory(context), filename + fileExt)
            return try {
                rawFile.copyTo(waveFile, overwrite = true)
                true
            } catch (e: IOException) {
                false
            }
        }

        // Open files

        /**
         * Launches an implicit intent to open the audio file at (external storage)/HiVo recordings/[recordingName].wav
         */
        fun launchImplicitAudioIntent(context: Context, recordingName: String) {
            val filepath = getPublicDirectory(context).toString() + "/" + recordingName + fileExt
            val fileUri: Uri? = try {
                FileProvider.getUriForFile(context, providerPath, File(filepath))
            } catch (e: IllegalArgumentException) {
                null
            }

            if (fileUri != null) {
                Intent(Intent.ACTION_VIEW).apply {
                    flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    setDataAndType(fileUri, "audio/*")
                    context.startActivity(this)
                }
            }
        }

        // Headers

        fun writeHeaders(output: FileOutputStream) {
            // see https://web.archive.org/web/20141213140451/https://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF") // chunk id
            writeInt(output, 0) // chunk size - we need to update this later to rawData.size + 36
            writeString(output, "WAVE") // format
            writeString(output, "fmt ") // subchunk 1 id
            writeInt(output, 16) // subchunk 1 size
            writeShort(output, 1.toShort()) // audio format (1 = PCM)
            writeShort(output, numChannels.toShort()) // number of channels
            writeInt(output, samplingRateHz) // sample rate
            writeInt(
                output,
                samplingRateHz * numChannels * bitsPerSample / 8
            ) // byte rate
            writeShort(output, blockAlign.toShort())
            writeShort(output, bitsPerSample.toShort()) // bits per sample
            writeString(output, "data") // subchunk 2 id
            writeInt(output, 0) // subchunk 2 size - we need to update this later to rawData.size
        }

        /**
         * Updates Chunk Size header and Subchunk2 Size header to match size of file (minus 8 and minus 44
         * respectively).
         */
        fun updateHeaders(file: File) {
            // Create a byte buffer [chunksize1 chunksize2 ... chunksize8 subchunk2size1 subchunk2size2 ...  subchunk2size8]
            // Note that we're only interested in the first four bytes of each size (as that is how many bytes the RIFF
            // can hold) - these are stored as longs rather than ints so that we have more space (as they are signed,
            // int can only hold up to 2^31 - 1, but with four bytes you can store up to 2^32 - 1).
            val sizes = ByteBuffer
                .allocate(16)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(file.length() - 8) // chunk size
                .putLong(file.length() - 44) // subchunk 2 size
                .array()

            var accessWave: RandomAccessFile? = null

            try {
                accessWave = RandomAccessFile(file, "rw")
                // chunk size
                accessWave.seek(4)
                accessWave.write(sizes, 0, 4)

                // subchunk 2 size
                accessWave.seek(40)
                accessWave.write(sizes, 8, 4)
            } catch (e: IOException) {
                throw e
            } finally {
                accessWave?.close()
            }
        }

        // Writing primitives to streams

        @Throws(IOException::class)
        fun writeInt(output: FileOutputStream, value: Int) {
            output.write(value shr 0)
            output.write(value shr 8)
            output.write(value shr 16)
            output.write(value shr 24)
        }

        @Throws(IOException::class)
        fun writeShort(output: FileOutputStream, value: Short) {
            output.write(value.toInt() shr 0)
            output.write(value.toInt() shr 8)
        }

        @Throws(IOException::class)
        fun writeString(output: FileOutputStream, value: String) {
            for (element in value) {
                output.write(element.toInt())
            }
        }
    }
}