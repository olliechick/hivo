package me.olliechick.hivo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import android.animation.AnimatorInflater
import android.animation.ValueAnimator
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.GradientDrawable


class MainActivity : AppCompatActivity() {

    private var isRecording = false
    private lateinit var valueAnimator: ValueAnimator

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
    }

    fun recordClicked(v: View) {
        if (isRecording) {
            valueAnimator.reverse()
            isRecording = false
        } else {
            valueAnimator.start()
            isRecording = true
        }
    }


}
