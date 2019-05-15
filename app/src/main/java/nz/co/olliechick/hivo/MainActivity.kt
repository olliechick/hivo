package nz.co.olliechick.hivo

import android.media.MediaRecorder
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Telephony.Mms.Part.FILENAME
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.Manifest.permission.RECORD_AUDIO
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.view.View
import org.jetbrains.anko.toast
import android.widget.Toast




class MainActivity : AppCompatActivity() {

    val filename = Environment.getExternalStorageDirectory().absolutePath + "/AudioRecording.3gp"
    val REQUEST_AUDIO_PERMISSION_CODE = 402
    var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private val recorder = MediaRecorder()

    fun record(v: View) {
        if (isRecording) {
            toast("Stopping recording")
            stopRecording()
        } else {
            toast("Starting recording")
            if (checkPermissions()) startRecordingToFile(filename)
            else requestPermissions()
        }
    }

    private fun startRecordingToFile(filename: String) {
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS) // "useful for streaming" according to docs
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(filename)
            prepare()
            start()
        }
        isRecording = true
    }

    private fun stopRecording() {
        recorder.apply {
            stop()
//            reset()  // You can reuse the object by going back to setAudioSource() step
            release() // Now the object cannot be reused
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_AUDIO_PERMISSION_CODE -> if (grantResults.isNotEmpty()) {
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


    private fun checkPermissions(): Boolean {
        val result = ContextCompat.checkSelfPermission(applicationContext, WRITE_EXTERNAL_STORAGE)
        val result1 = ContextCompat.checkSelfPermission(applicationContext, RECORD_AUDIO)
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() = ActivityCompat.requestPermissions(
        this@MainActivity,
        arrayOf(RECORD_AUDIO, WRITE_EXTERNAL_STORAGE),
        REQUEST_AUDIO_PERMISSION_CODE
    )

}