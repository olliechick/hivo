package nz.co.olliechick.hivo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import nz.co.olliechick.hivo.Util.Companion.getPublicDirectory
import nz.co.olliechick.hivo.Util.Companion.getRawFile
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.*
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean


class MainActivity : AppCompatActivity() {

    /**
     * Signals whether a recording is in progress (true) or not (false).
     */
    private val recordingInProgress = AtomicBoolean(false)

    private var playbackInProgress = false

    private var recorder: AudioRecord? = null

    private var recordingThread: Thread? = null

    private lateinit var audio: AudioTrack
    private var inputStream: FileInputStream? = null
    private var isPaused = false
    private var bytesread = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE))

        startRecordingButton!!.setOnClickListener {
            startRecording()
            startRecordingButton!!.isEnabled = false
            stopRecordingButton!!.isEnabled = true
        }

        stopRecordingButton.setOnClickListener {
            stopRecording()
            startRecordingButton!!.isEnabled = true
            stopRecordingButton!!.isEnabled = false
        }

        playPauseButton.setOnClickListener {
            when {
                playbackInProgress -> pause()
                isPaused -> resume()
                else -> {
                    playFile()
                    playPauseButton.text = getString(R.string.pause_file)
                    playbackInProgress = true
                }
            }
        }

        saveButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //todo re-engineer so it works back to api 16
                toast("Saving...")
                val timeRecordingStarted = LocalDateTime.now() //todo store recording start time when it happens
                val filename = timeRecordingStarted.format(DateTimeFormatter.ofPattern("HH.mm.ss, dd MM yyyy")) + ".wav"
                val rawFile = getRawFile(this)
                val waveFile = File(getPublicDirectory(), filename)
                Util.rawToWave(rawFile, waveFile, SAMPLING_RATE_IN_HZ)
                toast("Saved.")
            } else {
                toast("Error: API level too low")
            }
        }
    }

    override fun onResume() {
        super.onResume()

        startRecordingButton!!.isEnabled = true
        stopRecordingButton!!.isEnabled = false
    }

    override fun onPause() {
        super.onPause()

        stopRecording()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        val inflater = menuInflater
        inflater.inflate(R.menu.mainmenu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.scheduled_recordings -> {
            toast("Not yet implemented")
            true
        }

        R.id.past_recordings -> {
            toast("Not yet implemented")
            true
        }

        R.id.help -> {
            val uri = Uri.parse(Util.helpUrl)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
            true
        }

        R.id.settings -> {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            true
        }

        else -> super.onOptionsItemSelected(item)
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
            val file = getRawFile(this@MainActivity)
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

        playPauseButton.text = getString(R.string.pause_file)
    }

    private fun pause() {
        toast("Paused.")
        isPaused = true
        playbackInProgress = false

        playPauseButton.text = getString(R.string.play_file)
    }

    private fun stopFile() {
        toast("Stopped.")
        inputStream?.close()
        inputStream = null
        audio.stop()
        audio.release()
        playbackInProgress = false

        playPauseButton.text = getString(R.string.play_file)
    }


    private inner class RecordingRunnable : Runnable {

        override fun run() {
            val file = getRawFile(this@MainActivity)
            val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)

            try {
                FileOutputStream(file).use { outStream ->
                    while (recordingInProgress.get()) {
                        val result = recorder!!.read(buffer, BUFFER_SIZE)
                        if (result < 0) {
                            throw RuntimeException(
                                "Reading of audio buffer failed: " + getBufferReadFailureReason(result)
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

        private const val SAMPLING_RATE_IN_HZ = 44100
        private const val CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        private const val CHANNEL_OUT_CONFIG = AudioFormat.CHANNEL_OUT_STEREO
        private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

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
            SAMPLING_RATE_IN_HZ,
            CHANNEL_IN_CONFIG, AUDIO_FORMAT
        ) * BUFFER_SIZE_FACTOR
    }
}