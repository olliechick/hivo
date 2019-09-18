package nz.co.olliechick.hivo.util

import android.content.Context
import android.os.Environment
import android.util.Log
import nz.co.olliechick.hivo.R
import nz.co.olliechick.hivo.util.Constants.Companion.bitsPerSample
import nz.co.olliechick.hivo.util.Constants.Companion.fileExt
import nz.co.olliechick.hivo.util.Constants.Companion.numChannels
import nz.co.olliechick.hivo.util.Constants.Companion.samplingRateHz
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Files {
    companion object {


        // adapted from https://stackoverflow.com/a/37436599/8355496
        @Throws(IOException::class)
        fun rawToWave(rawFile: File, waveFile: File, sampleRate: Int, numChannels: Int, bitsPerSample: Int) {

            val rawData = ByteArray(rawFile.length().toInt())
            var input: DataInputStream? = null
            try {
                input = DataInputStream(FileInputStream(rawFile))
                input.read(rawData)
            } finally {
                input?.close()
            }

            var output: DataOutputStream? = null
            try {
                output = DataOutputStream(FileOutputStream(waveFile))
                // WAVE header
                // see https://web.archive.org/web/20141213140451/https://ccrma.stanford.edu/courses/422/projects/WaveFormat/
                writeString(output, "RIFF") // chunk id
                writeInt(output, 36 + rawData.size) // chunk size
                writeString(output, "WAVE") // format
                writeString(output, "fmt ") // subchunk 1 id
                writeInt(output, 16) // subchunk 1 size
                writeShort(output, 1.toShort()) // audio format (1 = PCM)
                writeShort(output, numChannels.toShort()) // number of channels
                writeInt(output, sampleRate) // sample rate
                writeInt(output, sampleRate * numChannels * bitsPerSample / 8) // byte rate
                writeShort(output, (numChannels * bitsPerSample / 8).toShort()) // block align
                writeShort(output, bitsPerSample.toShort()) // bits per sample
                writeString(output, "data") // subchunk 2 id
                writeInt(output, rawData.size) // subchunk 2 size

                // Audio data (conversion big endian -> little endian)
                val shorts = ShortArray(rawData.size / 2)
                ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
                Log.i("hivo", "shorts.size = ${shorts.size}b ~= ${shorts.size / (1000 * 1000)}Mb")
                val bytes = ByteBuffer.allocate(shorts.size * 2)
                for (s in shorts) {
                    bytes.putShort(s)
                }

                output.write(fullyReadFileToBytes(rawFile))
            } finally {
                output?.close()
            }
        }

        @Throws(IOException::class)
        fun fullyReadFileToBytes(f: File): ByteArray {
            val size = f.length().toInt()
            val bytes = ByteArray(size)
            val tmpBuff = ByteArray(size)
            val fis = FileInputStream(f)
            try {

                var read = fis.read(bytes, 0, size)
                if (read < size) {
                    var remain = size - read
                    while (remain > 0) {
                        read = fis.read(tmpBuff, 0, remain)
                        System.arraycopy(tmpBuff, 0, bytes, size - remain, read)
                        remain -= read
                    }
                }
            } catch (e: IOException) {
                throw e
            } finally {
                fis.close()
            }

            return bytes
        }

        @Throws(IOException::class)
        private fun writeInt(output: DataOutputStream, value: Int) {
            output.write(value shr 0)
            output.write(value shr 8)
            output.write(value shr 16)
            output.write(value shr 24)
        }

        @Throws(IOException::class)
        private fun writeShort(output: DataOutputStream, value: Short) {
            output.write(value.toInt() shr 0)
            output.write(value.toInt() shr 8)
        }

        @Throws(IOException::class)
        private fun writeString(output: DataOutputStream, value: String) {
            for (element in value) {
                output.write(element.toInt())
            }
        }

        /**
         * Returns the directory (external storage)/HiVo recordings
         */
        private fun getPublicDirectory(context: Context): File? = File(
            Environment.getExternalStorageDirectory(),
            context.getString(R.string.hivo_recordings)
        ).apply { mkdirs() }

        private fun getPrivateDirectory(context: Context): File = context.filesDir

        /**
         * Saves a file to (external storage)/HiVo recordings/[filename].wav
         */
        fun saveWav(filename: String, context: Context) {
            val rawFile = getRawFile(context)
            val waveFile = File(getPublicDirectory(context), filename + fileExt)
            rawToWave(rawFile, waveFile, samplingRateHz, numChannels, bitsPerSample)
        }

        fun getRawFile(context: Context) = File(getPrivateDirectory(context), "recording.pcm")
    }
}