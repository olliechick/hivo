package nz.co.olliechick.hivo

import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import java.io.IOException
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var startbtn: Button? = null
    private var stopbtn: Button? = null
    private var playbtn: Button? = null
    private var stopplay: Button? = null
    private var mRecorder: MediaRecorder? = null
    private var mPlayer: MediaPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startbtn = findViewById<View>(R.id.btnRecord) as Button
        stopbtn = findViewById<View>(R.id.btnStop) as Button
        playbtn = findViewById<View>(R.id.btnPlay) as Button
        stopplay = findViewById<View>(R.id.btnStopPlay) as Button
        stopbtn!!.isEnabled = false
        playbtn!!.isEnabled = false
        stopplay!!.isEnabled = false
        mFileName = Environment.getExternalStorageDirectory().absolutePath
        mFileName += "/AudioRecording.3gp"

        fun stopRec() {

            stopbtn!!.isEnabled = false
            startbtn!!.isEnabled = true
            playbtn!!.isEnabled = true
            stopplay!!.isEnabled = true
            mRecorder!!.stop()
            mRecorder!!.release()
            mRecorder = null
        }

        fun startRec(filename: String) {
            stopbtn!!.isEnabled = true
            startbtn!!.isEnabled = false
            playbtn!!.isEnabled = false
            stopplay!!.isEnabled = false
            mRecorder = MediaRecorder()
            mRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mRecorder!!.setOutputFile(filename)
            try {
                mRecorder!!.prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }

            mRecorder!!.start()

        }

        startbtn!!.setOnClickListener {
            if (CheckPermissions()) {
                startRec(mFileName!!)
                Toast.makeText(applicationContext, "Recording Started", Toast.LENGTH_LONG).show()
            } else {
                RequestPermissions()
            }
        }
        stopbtn!!.setOnClickListener {
            stopRec()
            Toast.makeText(applicationContext, "Recording Stopped", Toast.LENGTH_LONG).show()
        }
        btnStopStart!!.setOnClickListener {
            stopRec()
            startRec( Environment.getExternalStorageDirectory().absolutePath + "/AudioRecording2.3gp")
            Toast.makeText(applicationContext, "Recording Stopped and started", Toast.LENGTH_LONG).show()
        }
        playbtn!!.setOnClickListener {
            stopbtn!!.isEnabled = false
            startbtn!!.isEnabled = true
            playbtn!!.isEnabled = false
            stopplay!!.isEnabled = true
            mPlayer = MediaPlayer()
            try {
                mPlayer!!.setDataSource(mFileName)
                mPlayer!!.prepare()
                mPlayer!!.start()
                Toast.makeText(applicationContext, "Recording Started Playing", Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }
        }
        stopplay!!.setOnClickListener {
            mPlayer!!.release()
            mPlayer = null
            stopbtn!!.isEnabled = false
            startbtn!!.isEnabled = true
            playbtn!!.isEnabled = true
            stopplay!!.isEnabled = false
            Toast.makeText(applicationContext, "Playing Audio Stopped", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_AUDIO_PERMISSION_CODE -> if (grantResults.size > 0) {
                val permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val permissionToStore = grantResults[1] == PackageManager.PERMISSION_GRANTED
                if (permissionToRecord && permissionToStore) {
                    Toast.makeText(applicationContext, "Permission Granted", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(applicationContext, "Permission Denied", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun CheckPermissions(): Boolean {
        val result = ContextCompat.checkSelfPermission(applicationContext, WRITE_EXTERNAL_STORAGE)
        val result1 = ContextCompat.checkSelfPermission(applicationContext, RECORD_AUDIO)
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
    }

    private fun RequestPermissions() {
        ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(RECORD_AUDIO, WRITE_EXTERNAL_STORAGE),
            REQUEST_AUDIO_PERMISSION_CODE
        )
    }

    companion object {
        private val LOG_TAG = "AudioRecording"
        private var mFileName: String? = null
        val REQUEST_AUDIO_PERMISSION_CODE = 1
    }
}