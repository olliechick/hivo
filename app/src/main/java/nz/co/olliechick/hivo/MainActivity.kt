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
import nz.co.olliechick.hivo.util.*
import nz.co.olliechick.hivo.util.Constants.Companion.amplitudeKey
import nz.co.olliechick.hivo.util.Constants.Companion.audioFormat
import nz.co.olliechick.hivo.util.Constants.Companion.helpUrl
import nz.co.olliechick.hivo.util.Constants.Companion.newAmplitudeIntent
import nz.co.olliechick.hivo.util.Constants.Companion.recordingStartedIntent
import nz.co.olliechick.hivo.util.Constants.Companion.recordingStoppedIntent
import nz.co.olliechick.hivo.util.Constants.Companion.samplingRateHz
import nz.co.olliechick.hivo.util.Files.Companion.getRawFile
import nz.co.olliechick.hivo.util.Files.Companion.rawFileExists
import nz.co.olliechick.hivo.util.Files.Companion.saveWav
import nz.co.olliechick.hivo.util.Preferences.Companion.getStartTime
import nz.co.olliechick.hivo.util.Preferences.Companion.isOnboardingComplete
import nz.co.olliechick.hivo.util.Recordings.Companion.cancelCurrentStopRecordingIntent
import nz.co.olliechick.hivo.util.Recordings.Companion.startRecording
import nz.co.olliechick.hivo.util.Recordings.Companion.stopRecording
import nz.co.olliechick.hivo.util.StringProcessing.Companion.getNameForRecording
import nz.co.olliechick.hivo.util.StringProcessing.Companion.getTimeString
import nz.co.olliechick.hivo.util.StringProcessing.Companion.usesCustomFilename
import nz.co.olliechick.hivo.util.Ui.Companion.toast
import org.jetbrains.anko.*
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*


class MainActivity : AppCompatActivity() {

    private var playbackInProgress = false
        set(value) {
            field = value
            playPauseButton.image = ContextCompat.getDrawable(
                this,
                if (value) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
        }
    private var recordingInProgress = false

    private var audio: AudioTrack? = null
    private var inputStream: FileInputStream? = null
    private var isPaused = false
    private var bytesread = 0

    private var view: View? = null
    private lateinit var db: RecordingDatabase

    private val amplitudeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val amplitude = intent.getIntExtra(amplitudeKey, 0)
            seekBar.addAmplitude(amplitude)
        }
    }

    private val recordingToggledReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == recordingStartedIntent) {
                seekBar.clear()
                recordingInProgress = true
            } else if (intent?.action == recordingStoppedIntent) recordingInProgress = false
            recordingSwitch.isChecked = recordingInProgress
        }
    }

    // Lifecycle methods

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!isOnboardingComplete(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            finish()
            return
        }

        checkPermissions(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )

        registerReceiver(amplitudeReceiver, IntentFilter(newAmplitudeIntent))
        registerReceiver(recordingToggledReceiver, IntentFilter(recordingStartedIntent))
        registerReceiver(recordingToggledReceiver, IntentFilter(recordingStoppedIntent))

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

        recordingSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                if (isChecked) {
                    if (rawFileExists(this)) {
                        AlertDialog.Builder(this).apply {
                            setMessage("You are about to overwrite what was last listened to (unless it was saved). Are you sure you want to do this?")
                            setPositiveButton(getString(R.string.yes)) { subDialog, _ ->
                                subDialog.dismiss()
                                seekBar.clear()
                                startRecording(this@MainActivity)
                                recordingInProgress = true
                            }
                            setNegativeButton(getString(R.string.cancel)) { subDialog, _ ->
                                recordingSwitch.isChecked = false // turn if back off
                                subDialog.dismiss()
                            }
                            create()
                            show().setCanceledOnTouchOutside(false)
                        }
                    } else { // First ever time turning this on
                        seekBar.clear()
                        startRecording(this)
                        recordingInProgress = true
                    }
                } else { // Switch was turned off
                    stopRecording(this)
                    recordingInProgress = false
                    stopPlayback()
                    cancelCurrentStopRecordingIntent(applicationContext)
                }
            }
            // if it's not pressed, then a scheduled recording starting or stopping triggered it to flip, so we don't
            // need to start or stop the recording - this was already done by the thing that triggered it!
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

        saveButton.setOnClickListener { save() }

        // Make the play button appear/disappear when you hold down on the visualisation for 8 seconds
        seekBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Pressed down
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Released
                    if (event.eventTime - event.downTime > 8000) togglePlayPauseButton()
                    seekBar.performClick()
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
        try {
            unregisterReceiver(amplitudeReceiver)
        } catch (e: IllegalArgumentException) {
        }
        try {
            unregisterReceiver(recordingToggledReceiver)
        } catch (e: IllegalArgumentException) {
        }
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
            val intent = Intent(this, PastRecordingActivity::class.java)
            startActivity(intent)
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

    // Playing audio (hidden behind feature flag)

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
        isPaused = false
        playbackInProgress = true
    }

    private fun pausePlayback() {
        isPaused = true
        playbackInProgress = false
    }

    private fun stopPlayback() {
        inputStream?.close()
        inputStream = null
        audio?.takeIf { playbackInProgress }?.run {
            stop()
            release()
        }
        playbackInProgress = false
    }

    // Saving audio

    /**
     * Saves WAV file of audio from [startClip] to [endClip], and adds entry to database.
     */
    private fun saveWav(name: String, startClip: Calendar, endClip: Calendar) {
        toast(getString(R.string.saving))

        val start = Calendar.getInstance().apply { time = getStartTime(this@MainActivity) }
        val end = Calendar.getInstance().apply { time = getEndTime(this@MainActivity) }

        val millisBeforeStart: Long = startClip.timeInMillis - start.timeInMillis
        val durationInMillis: Long = endClip.timeInMillis - startClip.timeInMillis

        val saveSuccessful = if (start == startClip && end == endClip) saveWav(name, this)
        else saveWav(name, millisBeforeStart, durationInMillis, this)

        if (saveSuccessful) {
            doAsync {
                val recording = Recording(name, startClip, endClip)
                db = Database.initialiseDb(applicationContext)
                recording.id = db.recordingDao().insert(recording)
                db.close()
                uiThread { toast(getString(R.string.saved)) } //todo include name
            }
        } else toast(getString(R.string.saving_failed))
    }

    /**
     * Opens save dialog, with (possible) filename input and start/stop time selection.
     */
    @SuppressLint("InflateParams")
    private fun save() {
        view = layoutInflater.inflate(R.layout.save_filename_dialog, null)

        val start = Calendar.getInstance().apply { time = getStartTime(this@MainActivity) }
        var end = Calendar.getInstance().apply { time = getEndTime(this@MainActivity) }

        /** True if the recording stopped in the same minute it started. */
        var recordingAllInOneMinute = start.get(Calendar.MINUTE) == end.get(Calendar.MINUTE)

        val startClip = start.clone() as Calendar
        val endClip = end.clone() as Calendar

        val dialog = AlertDialog.Builder(this).run {
            setView(view)

            setTitle(getString(R.string.save_recording))
            setPositiveButton(getString(R.string.save), null) // will be overridden
            setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }

            view?.startTime?.run {
                text = getTimeString(startClip)

                fun update() {
                    text = getTimeString(startClip)
                }
                onClick {
                    Ui.showTimePicker(this@MainActivity, startClip, ::update)
                }

                onLongClick {
                    toast(getString(R.string.start_time))
                    true
                }
            }
            view?.endTime?.run {
                text = getTimeString(endClip)

                fun update() {
                    text = getTimeString(endClip)
                }
                onClick {
                    Ui.showTimePicker(this@MainActivity, endClip, ::update)
                }

                onLongClick {
                    toast(getString(R.string.end_time))
                    true
                }
            }

            if (recordingAllInOneMinute && !recordingInProgress) {
                // Recording cannot extend beyond the minute it was in, so editing times is disabled.
                view?.startTime?.enabled = false
                view?.endTime?.enabled = false
            }

            if (!usesCustomFilename(this@MainActivity)) {
                view?.input?.visibility = View.GONE
            }

            create()
        }

        // Override positive button, so that it only dismisses if validation passes
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val validationDialogBuilder = AlertDialog.Builder(this)
                doAsync {
                    // Update end time
                    end = Calendar.getInstance().apply { time = getEndTime(this@MainActivity) }
                    recordingAllInOneMinute = start.get(Calendar.MINUTE) == end.get(Calendar.MINUTE)

                    // Fix up seconds and millis
                    if (startClip.getTimeInMinutes() == start.getTimeInMinutes()) startClip.setSecondsAndMillis(start)
                    else startClip.zeroSecondsAndMillis()
                    if (endClip.getTimeInMinutes() == end.getTimeInMinutes()) endClip.setSecondsAndMillis(end)
                    else endClip.zeroSecondsAndMillis()

                    val name = getRecordingNameFromViewOrDefault(startClip)

                    db = Database.initialiseDb(applicationContext)
                    val nameExists = db.recordingDao().nameExists(name)
                    val replacementName = if (nameExists) generateUniqueName(name) else ""
                    db.close()

                    uiThread {
                        val startMins = start.getTimeInMinutes()
                        val startClipMins = startClip.getTimeInMinutes()
                        val endClipMins = endClip.getTimeInMinutes()
                        val endMins = end.getTimeInMinutes()

                        if ((recordingAllInOneMinute && !(startMins == startClipMins && startClipMins == endClipMins))
                            || (!recordingAllInOneMinute && !(startClipMins in startMins until endClipMins && endClipMins <= endMins))
                        ) {
                            // Passes (i.e. we don't get here) if we didn't record it all in the same minute, or we did and chose that minute,
                            // OR we did record all in one minute, or start <= start of clip < end of clip <= end

                            validationDialogBuilder.apply {
                                setMessage(
                                    getString(
                                        R.string.start_and_end_must_be_valid,
                                        getTimeString(start), getTimeString(end)
                                    )
                                )
                                setPositiveButton(getString(R.string.ok)) { subDialog, _ ->
                                    subDialog.dismiss()
                                }
                                create()
                                show()
                            }
                        } else if (endClip.getTimeInMinutes() - startClip.getTimeInMinutes() > 6 * 60) {
                            // Passes if clip is shorter than 6 hours
                            validationDialogBuilder.apply {
                                setMessage(getString(R.string.recordings_must_be_less_than_6_hrs))
                                setPositiveButton(getString(R.string.ok)) { subDialog, _ ->
                                    subDialog.dismiss()
                                }
                                create()
                                show()
                            }
                        } else if (nameExists) {
                            // Passes if name is unique (doesn't exist)
                            if (usesCustomFilename(this@MainActivity)) {
                                AlertDialog.Builder(this@MainActivity).apply {
                                    setTitle(getString(R.string.already_recording_with_name))
                                    setMessage(getString(R.string.save_as_instead, replacementName))
                                    setPositiveButton(getString(R.string.yes)) { subDialog, _ ->
                                        dialog.dismiss()
                                        subDialog.dismiss()
                                        saveWav(name, startClip, endClip)
                                    }
                                    setNegativeButton(getString(R.string.no)) { subDialog, _ -> subDialog.dismiss() }

                                    create()
                                    show()
                                }
                            } else { // user doesn't specify name, so just use the replacement name
                                dialog.dismiss()
                                saveWav(replacementName, startClip, endClip)
                            }
                        } else {
                            // All valid :)
                            dialog.dismiss()
                            saveWav(name, startClip, endClip)
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun getRecordingNameFromViewOrDefault(start: Calendar): String {
        return if (usesCustomFilename(this)) {
            val inputName = view?.input?.text?.toString()
            if (inputName == null || inputName == "") getString(R.string.no_title)
            else inputName
        } else getNameForRecording(this, start.time)!!
    }

    private fun generateUniqueName(name: String): String =
        StringProcessing.generateUniqueName(name) { altName -> db.recordingDao().nameExists(altName) }

    /**
     * Returns the recording end time (this will be now if a recording is in progress).
     */
    private fun getEndTime(context: Context): Date =
        if (recordingInProgress) Date() else Preferences.getEndTime(context)

    companion object {
        private const val CHANNEL_OUT_CONFIG = AudioFormat.CHANNEL_OUT_STEREO
    }
}