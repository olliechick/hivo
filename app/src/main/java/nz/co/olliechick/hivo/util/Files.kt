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

        /**
         * Returns the directory (external storage)/HiVo recordings
         */
        private fun getPublicDirectory(context: Context): File? = File(
            Environment.getExternalStorageDirectory(),
            context.getString(R.string.hivo_recordings)
        ).apply { mkdirs() }

        private fun getPrivateDirectory(context: Context): File = context.filesDir

        /**
         * Saves the raw file to (external storage)/HiVo recordings/[filename].wav
         *
         * @return  <code>true</code> if and only if the renaming succeeded;
         *          <code>false</code> otherwise
         */
        fun saveWav(filename: String, context: Context): Boolean {
            val rawFile = getRawFile(context)
            val waveFile = File(getPublicDirectory(context), filename + fileExt)
            return rawFile.renameTo(waveFile)
        }

        fun getRawFile(context: Context) = File(getPrivateDirectory(context), "recording.wav")
    }
}