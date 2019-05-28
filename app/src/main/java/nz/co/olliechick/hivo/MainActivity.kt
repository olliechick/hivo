package nz.co.olliechick.hivo

import android.Manifest
import android.media.*
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import android.media.AudioTrack
import java.io.*
import android.media.AudioRecord
import android.media.MediaRecorder
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import android.media.AudioManager




/**
 * Sample that demonstrates how to record a device's microphone using [AudioRecord].
 */
class MainActivity : AppCompatActivity() {

    /**
     * Signals whether a recording is in progress (true) or not (false).
     */
    private val recordingInProgress = AtomicBoolean(false)

    private var recorder: AudioRecord? = null

    private var recordingThread: Thread? = null

    private var startButton: Button? = null

    private var stopButton: Button? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE))

        startButton = findViewById<View>(R.id.btnStart) as Button
        startButton!!.setOnClickListener {
            startRecording()
            startButton!!.isEnabled = false
            stopButton!!.isEnabled = true
        }

        stopButton = findViewById<View>(R.id.btnStop) as Button
        stopButton!!.setOnClickListener {
            stopRecording()
            startButton!!.isEnabled = true
            stopButton!!.isEnabled = false
        }

        btnStream.setOnClickListener {
            streamFile()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i("hivo", "onResume")

        startButton!!.isEnabled = true
        stopButton!!.isEnabled = false
    }

    override fun onPause() {
        super.onPause()

        stopRecording()
    }

    private fun startRecording() {
        Log.i("hivo", "triggered start recording")
        Log.i("hivo", "we have permission")
        recorder = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE
        )

        recorder!!.startRecording()

        recordingInProgress.set(true)

        recordingThread = Thread(RecordingRunnable(), "Recording Thread")
        recordingThread!!.start()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 402) {
            if (PackageManager.PERMISSION_DENIED in grantResults) {
                Toast.makeText(this, "You will have to grant permissions to be able to record.", Toast.LENGTH_SHORT)
                    .show()
                finishAffinity()
            } else {
                startRecording()
            }
        }
    }

    /*
     * If we don't have the permissions we need, gets them asynchronously and returns false.
     * If we do, returns true.
     */
    private fun checkPermissions(permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true

        val permissionsToGet = arrayListOf<String>()

        permissions.forEach { permission ->
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                permissionsToGet.add(permission)
        }

        Log.i("hivo", "we have to request ${permissionsToGet.size} permissions")

        return if (permissionsToGet.isEmpty()) true
        else {
            ActivityCompat.requestPermissions(this, permissionsToGet.toTypedArray(), 402)
            false
        }
    }

    private fun stopRecording() {
        if (null == recorder) {
            return
        }

        recordingInProgress.set(false)

        recorder!!.stop()

        recorder!!.release()

        recorder = null

        recordingThread = null
    }

    private fun streamFile() {
        // read file
        var byteData: ByteArray? = null
        val file = File(Environment.getExternalStorageDirectory(), "recording.pcm")
        byteData = ByteArray(file.length().toInt())
        val inputStream: FileInputStream?
        try {
            inputStream = FileInputStream(file)
            inputStream.read(byteData)
            inputStream.close()
        } catch (e: FileNotFoundException) {
            Toast.makeText(this, "No file found...", Toast.LENGTH_SHORT).show()
        }

        val audioRate = 44100
        val bufsize =
            AudioTrack.getMinBufferSize(audioRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)

        val audio = AudioTrack(
            AudioManager.STREAM_MUSIC,
            audioRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufsize,
            AudioTrack.MODE_STREAM
        )

        audio.play()
        audio.write(byteData, 0, byteData.size)
        audio.stop()
        audio.release()
    }

    private inner class RecordingRunnable : Runnable {

        override fun run() {
            val file = File(Environment.getExternalStorageDirectory(), "recording.pcm")
            val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)

            try {
                FileOutputStream(file).use { outStream ->
                    while (recordingInProgress.get()) {
                        val result = recorder!!.read(buffer, BUFFER_SIZE)
                        if (result < 0) {
                            throw RuntimeException(
                                "Reading of audio buffer failed: " + getBufferReadFailureReason(
                                    result
                                )
                            )
                        }
                        outStream.write(buffer.array(), 0, BUFFER_SIZE)
                        buffer.clear()
                    }
                }
            } catch (e: IOException) {
                throw RuntimeException("Writing of recorded audio failed", e)
            }

        }

        private fun getBufferReadFailureReason(errorCode: Int): String {
            when (errorCode) {
                AudioRecord.ERROR_INVALID_OPERATION -> return "ERROR_INVALID_OPERATION"
                AudioRecord.ERROR_BAD_VALUE -> return "ERROR_BAD_VALUE"
                AudioRecord.ERROR_DEAD_OBJECT -> return "ERROR_DEAD_OBJECT"
                AudioRecord.ERROR -> return "ERROR"
                else -> return "Unknown ($errorCode)"
            }
        }
    }

    companion object {

        private val SAMPLING_RATE_IN_HZ = 44100

        private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO

        private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        /**
         * Factor by that the minimum buffer size is multiplied. The bigger the factor is the less
         * likely it is that samples will be dropped, but more memory will be used. The minimum buffer
         * size is determined by [AudioRecord.getMinBufferSize] and depends on the
         * recording settings.
         */
        private val BUFFER_SIZE_FACTOR = 2

        /**
         * Size of the buffer where the audio data is stored by Android
         */
        private val BUFFER_SIZE = AudioRecord.getMinBufferSize(
            SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT
        ) * BUFFER_SIZE_FACTOR
    }
}