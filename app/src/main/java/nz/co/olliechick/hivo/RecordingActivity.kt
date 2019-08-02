package nz.co.olliechick.hivo

import android.app.Activity
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.view.View
import java.io.IOException


class RecordingActivity : Activity() {
    private var visualizerView: VisualizerView? = null
    private val recorder = MediaRecorder()
    private val handler = Handler()
    internal val updater: Runnable = object : Runnable {
        override fun run() {
            handler.postDelayed(this, 1)
            val maxAmplitude = recorder.maxAmplitude
            if (maxAmplitude != 0) {
                visualizerView!!.addAmplitude(maxAmplitude)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recording)
        visualizerView = findViewById<View>(R.id.visualizer) as VisualizerView
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            recorder.setOutputFile("/dev/null")
            recorder.prepare()
            recorder.start()
        } catch (ignored: IllegalStateException) {
        } catch (ignored: IOException) {
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updater)
        recorder.stop()
        recorder.reset()
        recorder.release()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        handler.post(updater)
    }
}