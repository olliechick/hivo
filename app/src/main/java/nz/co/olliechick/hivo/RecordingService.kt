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
import android.util.Log
import androidx.core.app.NotificationCompat

import nz.co.olliechick.hivo.util.Constants.Companion.amplitudeKey
import nz.co.olliechick.hivo.util.Constants.Companion.audioFormat
import nz.co.olliechick.hivo.util.Constants.Companion.newAmplitudeIntent
import nz.co.olliechick.hivo.util.Constants.Companion.recordingStartedIntent
import nz.co.olliechick.hivo.util.Constants.Companion.recordingStoppedIntent
import nz.co.olliechick.hivo.util.Constants.Companion.samplingRateHz
import nz.co.olliechick.hivo.util.Constants.Companion.unsignedIntMaxValue
import nz.co.olliechick.hivo.util.Files
import nz.co.olliechick.hivo.util.Files.Companion.updateHeaders
import nz.co.olliechick.hivo.util.Files.Companion.writeHeaders
import nz.co.olliechick.hivo.util.Preferences.Companion.saveEndTime
import nz.co.olliechick.hivo.util.Preferences.Companion.saveStartTime
import nz.co.olliechick.hivo.util.Ui.Companion.toast
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
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
        toast(getString(R.string.recording_started))
        val intent = Intent(recordingStartedIntent)
        sendBroadcast(intent)

        saveStartTime(this)

        recorder = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT, samplingRateHz, CHANNEL_IN_CONFIG, audioFormat, BUFFER_SIZE
        )
        recorder?.startRecording()

        recordingInProgress.set(true)
        recordingThread = Thread(RecordingRunnable(), "Recording Thread")
        recordingThread!!.start()
    }

    private fun stopRecording() {
        toast(getString(R.string.recording_stopped))
        val intent = Intent(recordingStoppedIntent)
        sendBroadcast(intent)

        recordingInProgress.set(false)
        recorder?.stop()
        saveEndTime(this)
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