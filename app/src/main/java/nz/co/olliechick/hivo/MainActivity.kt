package nz.co.olliechick.hivo

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.*
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Sample that demonstrates how to record a device's microphone using [AudioRecord].
 */
class MainActivity : AppCompatActivity() {

    /**
     * Signals whether a recording is in progress (true) or not (false).
     */
    private val recordingInProgress = AtomicBoolean(false)

    private var playbackInProgress = false

    private var recorder: AudioRecord? = null

    private var recordingThread: Thread? = null

    private var startButton: Button? = null

    private var stopButton: Button? = null

    private lateinit var audio: AudioTrack
    private var inputStream: FileInputStream? = null
    private var isPaused = false
    private var bytesread = 0

    override fun onCreate(savedInstanceState: Bundle?) {
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

        btnPlayPause.setOnClickListener {
            when {
                playbackInProgress -> pause()
                isPaused -> resume()
                else -> {
                    playFile()
                    btnPlayPause.text = getString(R.string.pause_file)
                    playbackInProgress = true
                }
            }
        }

        btnSave.setOnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //todo re-engineer so it works back to api 16
                toast("Saving...")
                val timeRecordingStarted = LocalDateTime.now() //todo store recording start time when it happens
                val filename = timeRecordingStarted.format(DateTimeFormatter.ofPattern("HH.mm.ss, dd MM yyyy")) + ".wav"
                val rawFile = getRawFile()
                val waveFile = File(getPublicDirectory(), filename)
                Util.rawToWave(rawFile, waveFile, SAMPLING_RATE_IN_HZ)
                toast("Saved.")
            } else {
                toast("Error: API level too low")
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        val inflater = menuInflater
        inflater.inflate(R.menu.mainmenu, menu)
        return true
    }

    private fun getPublicDirectory(): File? {
        val file = File(Environment.getExternalStorageDirectory(), "HiVo recordings")
        file.mkdirs()
        return file
    }

    private fun getPrivateDirectory() = filesDir
    private fun getRawFile() = File(getPrivateDirectory(), "recording.pcm")


    override fun onResume() {
        super.onResume()

        startButton!!.isEnabled = true
        stopButton!!.isEnabled = false
    }

    override fun onPause() {
        super.onPause()

        stopRecording()
    }

    private fun startRecording() {
        recorder = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT, SAMPLING_RATE_IN_HZ,
            CHANNEL_IN_CONFIG, AUDIO_FORMAT, BUFFER_SIZE
        )

        recorder!!.startRecording()

        recordingInProgress.set(true)

        recordingThread = Thread(RecordingRunnable(), "Recording Thread")
        recordingThread!!.start()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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

    /**
     * Adapted from https://jongladwin.blogspot.com/2010/03/android-play-pcmwav-audio-buffer-using.html
     */
    private fun playFile() {
        doAsync {

            // read file
            val file = getRawFile()
            val count = 512 * 1024 // 512 kb
            val byteData = ByteArray(count)

            if (inputStream == null) {
                try {
                    inputStream = FileInputStream(file)
                } catch (e: FileNotFoundException) {
                    uiThread {
                        toast(getString(R.string.no_file_found))
                    }
                    return@doAsync
                }

                val bufferSize = AudioTrack.getMinBufferSize(SAMPLING_RATE_IN_HZ, CHANNEL_OUT_CONFIG, AUDIO_FORMAT)

                @Suppress("DEPRECATION")
                audio = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLING_RATE_IN_HZ,
                    CHANNEL_OUT_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
                bytesread = 0
            } else isPaused = false

            val size = file.length().toInt()
            audio.play()
            while (bytesread < size) {
                if (isPaused) {
                    audio.pause()
                } else {
                    audio.play()
                    if (inputStream != null && audio.state == AudioTrack.STATE_INITIALIZED) {
                        val ret = inputStream?.read(byteData, 0, count)
                        if (ret != null && ret != -1) {
                            // Write the byte array to the track
                            audio.write(byteData, 0, ret)
                            bytesread += ret
                        } else break
                    } else break
                }
            }

            uiThread {
                stopFile()
            }
        }
    }

    private fun resume() {
        toast("Resuming.")
        isPaused = false
        playbackInProgress = true

        btnPlayPause.text = getString(R.string.pause_file)
    }

    private fun pause() {
        toast("Paused.")
        isPaused = true
        playbackInProgress = false

        btnPlayPause.text = getString(R.string.play_file)
    }

    private fun stopFile() {
        toast("Stopped.")
        inputStream?.close()
        inputStream = null
        audio.stop()
        audio.release()
        playbackInProgress = false

        btnPlayPause.text = getString(R.string.play_file)
    }


    private inner class RecordingRunnable : Runnable {

        override fun run() {
            val file = getRawFile()
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

        private val CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        private val CHANNEL_OUT_CONFIG = AudioFormat.CHANNEL_OUT_STEREO

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
            CHANNEL_IN_CONFIG, AUDIO_FORMAT
        ) * BUFFER_SIZE_FACTOR
    }
}