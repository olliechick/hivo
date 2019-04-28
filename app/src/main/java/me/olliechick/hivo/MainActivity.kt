package me.olliechick.hivo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import android.animation.AnimatorInflater
import android.animation.ValueAnimator
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.GradientDrawable
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private var isRecording = false
    private lateinit var valueAnimator: ValueAnimator
    private lateinit var timeFormat: SimpleDateFormat
    private lateinit var timeFormatWithAmPm: SimpleDateFormat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val layerDrawable = getDrawable(R.drawable.rec_button) as LayerDrawable
        val gradientDrawable = layerDrawable.findDrawableByLayerId(R.id.redCircle) as GradientDrawable

        valueAnimator = AnimatorInflater.loadAnimator(
            this, R.animator.rec_button_transition
        ) as ValueAnimator

        valueAnimator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Float
            gradientDrawable.cornerRadius = progress
            recordButton.setImageDrawable(layerDrawable)
        }

        val locale: Locale = resources.configuration.locales[0]
        timeFormat = SimpleDateFormat("h:mm", locale)
        timeFormatWithAmPm = SimpleDateFormat("HH:mm z", locale)
    }

    fun recordClicked(v: View) {
        if (isRecording) {
            isRecording = false

            valueAnimator.reverse()

            bufferLinearLayout.visibility = View.VISIBLE
            saveButton.visibility = View.GONE
            timeLinearLayout.visibility = View.INVISIBLE
        } else {
            isRecording = true

            valueAnimator.start()

            bufferLinearLayout.visibility = View.GONE
            saveButton.visibility = View.VISIBLE
            timeLinearLayout.visibility = View.VISIBLE

            val currentTime = timeFormat.format(Date())
            startTimeText.text = currentTime
            currentTimeText.format12Hour = "K:m"
        }
    }


}
