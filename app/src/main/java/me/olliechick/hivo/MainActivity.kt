package me.olliechick.hivo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var recording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun recordClicked(v: View) {
        if (recording) {
            recordButton.setImageDrawable(getDrawable(R.drawable.rec_button))
            recording = false
        } else {
            recordButton.setImageDrawable(getDrawable(R.drawable.rec_button_recording))
            recording = true
        }
    }

}
