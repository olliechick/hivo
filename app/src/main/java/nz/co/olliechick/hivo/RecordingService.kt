package nz.co.olliechick.hivo

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import nz.co.olliechick.hivo.util.Constants
import nz.co.olliechick.hivo.util.Constants.Companion.amplitudeKey
import nz.co.olliechick.hivo.util.Constants.Companion.audioFormat
import nz.co.olliechick.hivo.util.Constants.Companion.bitsPerSample
import nz.co.olliechick.hivo.util.Constants.Companion.newAmplitudeIntent
import nz.co.olliechick.hivo.util.Constants.Companion.numChannels
import nz.co.olliechick.hivo.util.Constants.Companion.recordingStartedIntent
import nz.co.olliechick.hivo.util.Constants.Companion.recordingStoppedIntent
import nz.co.olliechick.hivo.util.Constants.Companion.samplingRateHz
import nz.co.olliechick.hivo.util.Constants.Companion.unsignedIntMaxValue
import nz.co.olliechick.hivo.util.Files
import nz.co.olliechick.hivo.util.Preferences
import nz.co.olliechick.hivo.util.Ui.Companion.toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.experimental.and
import kotlin.math.abs


class RecordingService : Service() {

    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val recordingInProgress = AtomicBoolean(false)
    val amplitudes = arrayListOf<Int>()
    private val myBinder = MyLocalBinder()

    override fun onBind(intent: Intent?): IBinder = myBinder

    override fun onCreate() {
        super.onCreate()
        startRecording()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.hivo_recording),
                NotificationManager.IMPORTANCE_DEFAULT
            )

            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(serviceChannel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.hivo_recording_in_bg))
            .setContentText(getString(R.string.tap_to_open))
            .setSmallIcon(R.drawable.icon)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon))
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startRecording() {
        toast("Recording started.")
        val intent = Intent(recordingStartedIntent)
        sendBroadcast(intent)

        Preferences.saveStartTime(PreferenceManager.getDefaultSharedPreferences(this))

        recorder = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT, samplingRateHz, CHANNEL_IN_CONFIG, audioFormat, BUFFER_SIZE
        )
        recorder?.startRecording()

        recordingInProgress.set(true)
        recordingThread = Thread(RecordingRunnable(), "Recording Thread")
        recordingThread!!.start()
    }

    private fun stopRecording() {
        toast("Recording stopped.")
        val intent = Intent(recordingStoppedIntent)
        sendBroadcast(intent)

        recordingInProgress.set(false)
        recorder?.stop()
        recorder?.release()
        recorder = null
    }

    inner class MyLocalBinder : Binder() {
        internal val service: RecordingService
            get() = this@RecordingService
    }

    private inner class RecordingRunnable : Runnable {

        override fun run() {
            val file = Files.getRawFile(this@RecordingService)
            val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
            var totalBytesWritten = 0L
            var outStream: FileOutputStream? = null

            try {
                outStream = FileOutputStream(file)
                writeHeaders(outStream)

                while (recordingInProgress.get()) {
                    val numBytesRead = recorder?.read(buffer, BUFFER_SIZE) // Read audio to buffer

                    if (numBytesRead != null) totalBytesWritten += numBytesRead
                    else throw IOException("recorder: AudioRecord is null")

                    // Save and send amplitude
                    val amplitude = generateAmplitude(buffer)
                    amplitudes.add(amplitude)
                    val intent = Intent(newAmplitudeIntent)
                    intent.putExtra(amplitudeKey, amplitude)
                    sendBroadcast(intent)

                    if (numBytesRead < 0) throw RuntimeException(
                        "Reading of audio buffer failed: " + getBufferReadFailureReason(numBytesRead)
                    )

                    if (totalBytesWritten + numBytesRead <= unsignedIntMaxValue) {
                        outStream.write(buffer.array(), 0, BUFFER_SIZE) // Write buffer to file
                    } else {
                        // Write out as much of the buffer as will "fit", byte by byte
                        var i = 0
                        while (totalBytesWritten + i < unsignedIntMaxValue) {
                            outStream.write(buffer[i].toInt())
                            i++
                        }
                        totalBytesWritten = unsignedIntMaxValue
                        recordingInProgress.set(false)
                    }

                    buffer.clear()
                }

            } catch (e: IOException) {
                throw RuntimeException("Writing of recorded audio failed", e)

            } finally {
                if (recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder?.stop()
                if (recorder?.state == AudioRecord.STATE_INITIALIZED) recorder?.release()
                outStream?.close()
            }

            updateHeaders(file)
        }

        private fun generateAmplitude(buffer: ByteBuffer) =
            abs((((buffer[0] and 0xff.toByte()).toInt() shl 8) or buffer[1].toInt()))

        private fun getBufferReadFailureReason(errorCode: Int): String {
            return when (errorCode) {
                AudioRecord.ERROR_INVALID_OPERATION -> "ERROR_INVALID_OPERATION"
                AudioRecord.ERROR_BAD_VALUE -> "ERROR_BAD_VALUE"
                AudioRecord.ERROR_DEAD_OBJECT -> "ERROR_DEAD_OBJECT"
                AudioRecord.ERROR -> "ERROR"
                else -> "Unknown ($errorCode)"
            }
        }
    }

    private fun writeHeaders(output: FileOutputStream) {
        // see https://web.archive.org/web/20141213140451/https://ccrma.stanford.edu/courses/422/projects/WaveFormat/
        Files.writeString(output, "RIFF") // chunk id
        Files.writeInt(output, 0) // chunk size - we need to update this later to rawData.size + 36
        Files.writeString(output, "WAVE") // format
        Files.writeString(output, "fmt ") // subchunk 1 id
        Files.writeInt(output, 16) // subchunk 1 size
        Files.writeShort(output, 1.toShort()) // audio format (1 = PCM)
        Files.writeShort(output, numChannels.toShort()) // number of channels
        Files.writeInt(output, samplingRateHz) // sample rate
        Files.writeInt(output, samplingRateHz * numChannels * bitsPerSample / 8) // byte rate
        Files.writeShort(output, (numChannels * bitsPerSample / 8).toShort()) // block align
        Files.writeShort(output, bitsPerSample.toShort()) // bits per sample
        Files.writeString(output, "data") // subchunk 2 id
        Files.writeInt(output, 0) // subchunk 2 size - we need to update this later to rawData.size
    }

    private fun updateHeaders(file: File) {
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

    companion object {
        private const val CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        private const val CHANNEL_ID = "RecordingServiceChannel"

        /**
         * Factor by that the minimum buffer size is multiplied. The bigger the factor is the less
         * likely it is that samples will be dropped, but more memory will be used. The minimum buffer
         * size is determined by [AudioRecord.getMinBufferSize] and depends on the
         * recording settings.
         */
        private const val BUFFER_SIZE_FACTOR = 2

        /**
         * Size of the buffer where the audio data is stored by Android
         */
        private val BUFFER_SIZE = AudioRecord.getMinBufferSize(
            samplingRateHz,
            CHANNEL_IN_CONFIG, audioFormat
        ) * BUFFER_SIZE_FACTOR

        /**
         * @return true if the service is currently running
         */
        fun isRunning(context: Context): Boolean {
            var serviceRunning = false
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            val i = activityManager.getRunningServices(Int.MAX_VALUE).iterator()
            while (i.hasNext()) {
                if (i.next().service.className == RecordingService::class.java.name) {
                    serviceRunning = true
                }
            }
            return serviceRunning

        }
    }

}