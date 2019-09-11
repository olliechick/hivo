package nz.co.olliechick.hivo

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.save_filename_dialog.view.*
import nz.co.olliechick.hivo.util.Constants.Companion.amplitudeKey
import nz.co.olliechick.hivo.util.Constants.Companion.audioFormat
import nz.co.olliechick.hivo.util.Constants.Companion.debugToast
import nz.co.olliechick.hivo.util.Constants.Companion.fileExt
import nz.co.olliechick.hivo.util.Constants.Companion.helpUrl
import nz.co.olliechick.hivo.util.Constants.Companion.newAmplitudeIntent
import nz.co.olliechick.hivo.util.Constants.Companion.samplingRateHz
import nz.co.olliechick.hivo.util.Files.Companion.getRawFile
import nz.co.olliechick.hivo.util.Files.Companion.saveWav
import nz.co.olliechick.hivo.util.StringProcessing.Companion.getNameForCurrentRecording
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.image
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.FileInputStream
import java.io.FileNotFoundException


class MainActivity : AppCompatActivity() {

    private var playbackInProgress = false
        set(value) {
            field = value
            playPauseButton.image = ContextCompat.getDrawable(
                this,
                if (value) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
        }

    private var audio: AudioTrack? = null
    private var inputStream: FileInputStream? = null
    private var isPaused = false
    private var bytesread = 0

    private val amplitudeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val amplitude = intent.getIntExtra(amplitudeKey, 0)
            seekBar.addAmplitude(amplitude)
        }
    }

    // Lifecycle methods

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )

        registerReceiver(amplitudeReceiver, IntentFilter(newAmplitudeIntent))

        if (RecordingService.isRunning(this)) {

            var recordingService: RecordingService

            val connection = object : ServiceConnection {
                override fun onServiceConnected(className: ComponentName, service: IBinder) {
                    val binder = service as RecordingService.MyLocalBinder
                    recordingService = binder.service
                    seekBar.setAmplitudes(recordingService.amplitudes)
                    unbindService(this)
                }

                override fun onServiceDisconnected(name: ComponentName?) {}
            }

            recordingSwitch.isChecked = true
            intent = Intent(this, RecordingService::class.java)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        recordingSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startRecording()
            } else {
                stopRecording()
                stopPlayback()
            }
        }

        playPauseButton.visibility = View.GONE
        playPauseButton.setOnClickListener {
            when {
                playbackInProgress -> pausePlayback()
                isPaused -> resumePlayback()
                else -> {
                    startPlayback()
                    playbackInProgress = true
                }
            }
        }

        saveButton.setOnClickListener {
            val dateString = getNameForCurrentRecording(this)
            if (dateString == null) promptForFilenameAndSave()
            else saveWav(dateString)
        }

        seekBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Pressed down
                    toast("action down")
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Released
                    if (event.eventTime - event.downTime > 8000) togglePlayPauseButton()
                    saveButton.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL ->
                    // Released - Dragged finger outside
                    true
                else -> false
            }
        }
    }

    private fun togglePlayPauseButton() {
        playPauseButton.visibility = when (playPauseButton.visibility) {
            View.GONE -> View.VISIBLE
            else -> View.GONE
        }
    }

    override fun onDestroy() {
        unregisterReceiver(amplitudeReceiver)
        super.onDestroy()
    }

    // Options menu

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        val inflater = menuInflater
        inflater.inflate(R.menu.mainmenu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.scheduled_recordings -> {
            val intent = Intent(this, SchedRecordingsActivity::class.java)
            startActivity(intent)
            true
        }

        R.id.past_recordings -> {
            val intent = Intent(this, TestVisualisationActivity::class.java)
            startActivity(intent)
//            debugToast(this, "Not yet implemented")todo
            true
        }

        R.id.help -> {
            val uri = Uri.parse(helpUrl)
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

    // Permissions

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 402) {
            if (PackageManager.PERMISSION_DENIED in grantResults) {
                toast(getString(R.string.you_have_to_grant_permissions))
                finishAffinity()
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
            if (ActivityCompat.checkSelfPermission(
                    this, permission
                ) != PackageManager.PERMISSION_GRANTED
            ) permissionsToGet.add(permission)
        }

        return if (permissionsToGet.isEmpty()) true
        else {
            ActivityCompat.requestPermissions(this, permissionsToGet.toTypedArray(), 402)
            false
        }
    }

    // Recording audio

    private fun startRecording() {
        intent = Intent(this, RecordingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
    }

    private fun stopRecording() {
        intent = Intent(this, RecordingService::class.java)
        stopService(intent)
    }

    // Playing audio

    /**
     * Adapted from https://jongladwin.blogspot.com/2010/03/android-play-pcmwav-audio-buffer-using.html
     */
    private fun startPlayback() {
        doAsync {

            // read file
            val file = getRawFile(this@MainActivity)
            val count = 1024 // 1 kb
            val byteData = ByteArray(count)

            if (inputStream == null) {
                try {
                    inputStream = FileInputStream(file)
                } catch (e: FileNotFoundException) {
                    uiThread {
                        toast(getString(R.string.no_recording_found))
                        // Note that the user should never get here - if they click play, they are already recording
                    }
                    return@doAsync
                }

                val bufferSize = AudioTrack.getMinBufferSize(
                    samplingRateHz,
                    CHANNEL_OUT_CONFIG,
                    audioFormat
                )

                @Suppress("DEPRECATION")
                audio = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    samplingRateHz,
                    CHANNEL_OUT_CONFIG,
                    audioFormat,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
                bytesread = 0
            } else isPaused = false

            val size = file.length().toInt()
            audio!!.play()
            while (bytesread < size) {
                if (isPaused) {
                    audio!!.pause()
                } else {
                    audio!!.play()
                    if (inputStream != null && audio!!.state == AudioTrack.STATE_INITIALIZED) {
                        val ret = inputStream?.read(byteData, 0, count)
                        if (ret != null && ret != -1) {
                            // Write the byte array to the track
                            audio!!.write(byteData, 0, ret)
                            bytesread += ret
                        } else break
                    } else break
                }
            }

            uiThread {
                stopPlayback()
            }
        }
    }

    private fun resumePlayback() {
        debugToast(this, "Resuming.")
        isPaused = false
        playbackInProgress = true
    }

    private fun pausePlayback() {
        debugToast(this, "Paused.")
        isPaused = true
        playbackInProgress = false
    }

    private fun stopPlayback() {
        inputStream?.close()
        inputStream = null
        if (audio != null && playbackInProgress) debugToast(this, "Stopped.")
        audio?.takeIf { playbackInProgress }?.run {
            stop()
            release()
        }
        playbackInProgress = false
    }

    // Saving audio

    private fun saveWav(name: String) {
        toast(getString(R.string.saving))
        saveWav(name + fileExt, this, samplingRateHz)
        toast(getString(R.string.saved))
    }

    @SuppressLint("InflateParams")
    private fun promptForFilenameAndSave() {
        val view = layoutInflater.inflate(R.layout.save_filename_dialog, null)
        AlertDialog.Builder(this).apply {
            setView(view)
            setTitle(getString(R.string.save_recording))
            setPositiveButton(getString(R.string.save)) { _, _ -> saveWav(view.input?.text.toString()) }
            setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }
            create()
            show()
        }
    }

    companion object {
        private const val CHANNEL_OUT_CONFIG = AudioFormat.CHANNEL_OUT_STEREO
    }
}