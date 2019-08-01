package nz.co.olliechick.hivo

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.view.Gravity
import android.widget.Toast
import androidx.preference.PreferenceManager
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*


class Util {
    companion object {
        const val prefsFile = "me.olliechick.instagramunfollowers.prefs"
        const val startTimeKey = "start_time"
        const val filenameKey = "filename" //also defined in root_preferences.xml

        // adapted from https://stackoverflow.com/a/37436599/8355496
        @Throws(IOException::class)
        fun rawToWave(rawFile: File, waveFile: File, sampleRate: Int) {

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
                // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
                writeString(output, "RIFF") // chunk id
                writeInt(output, 36 + rawData.size) // chunk size
                writeString(output, "WAVE") // format
                writeString(output, "fmt ") // subchunk 1 id
                writeInt(output, 16) // subchunk 1 size
                writeShort(output, 1.toShort()) // audio format (1 = PCM)
                writeShort(output, 1.toShort()) // number of channels
                writeInt(output, sampleRate * 2) // sample rate
                writeInt(output, sampleRate) // byte rate
                writeShort(output, 2.toShort()) // block align
                writeShort(output, 16.toShort()) // bits per sample
                writeString(output, "data") // subchunk 2 id
                writeInt(output, rawData.size) // subchunk 2 size
                // Audio data (conversion big endian -> little endian)
                val shorts = ShortArray(rawData.size / 2)
                ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
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
            for (i in 0 until value.length) {
                output.write(value[i].toInt())
            }
        }

        fun getPublicDirectory(context: Context): File? {
            val file = File(Environment.getExternalStorageDirectory(), context.getString(R.string.hivo_recordings))
            file.mkdirs()
            return file
        }

        private fun getPrivateDirectory(context: Context): File = context.filesDir
        fun getRawFile(context: Context) =
            File(getPrivateDirectory(context), context.getString(R.string.raw_recording_filename))

        fun getDateString(context: Context, format: FilenameFormat, now: Date): String? {
            val pattern = when (format) {
                FilenameFormat.READABLE -> "h:mm:ss a, d MMM yyyy"
                FilenameFormat.SORTABLE -> "yyyy-MM-dd-HH-mm-ss"
                else -> null
            }
            return if (pattern == null) null
            else SimpleDateFormat(pattern, Locale.ENGLISH).format(now) + context.getString(R.string.wav_ext)
        }

        fun getDateString(context: Context): String? {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val name = sharedPreferences.getString(filenameKey, null)
            val now = Date().apply { time = sharedPreferences.getLong(startTimeKey, 0) }//Date().time)}

            return if (name == null) null // shouldn't happen
            else getDateString(context, FilenameFormat.valueOf(name), now)
        }

        fun saveStartTime(prefs: SharedPreferences) {
            val prefsEditor = prefs.edit()
            prefsEditor.putLong(startTimeKey, Date().time)
            prefsEditor.apply()
        }

        /**
         * This is just for debug purposes, and should be removed for the delivered product.
         */
        fun debugToast(context: Context, text: String) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).run {
                setGravity(Gravity.CENTER, 0, 300)
                show()
            }
        }
    }
}