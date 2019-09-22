package nz.co.olliechick.hivo.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import nz.co.olliechick.hivo.R
import nz.co.olliechick.hivo.util.Constants.Companion.fileExt
import nz.co.olliechick.hivo.util.Constants.Companion.providerPath
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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
         * If necessary, creates it.
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
    }
}