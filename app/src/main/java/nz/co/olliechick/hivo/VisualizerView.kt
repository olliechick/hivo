package nz.co.olliechick.hivo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.res.ResourcesCompat
import java.util.ArrayList

class VisualizerView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var vectors: FloatArray? = null
    private var amplitudeIndex = 0
    private val linePaint: Paint = Paint()
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    init {
        linePaint.color = ResourcesCompat.getColor(resources, R.color.colorPrimary, null)
        linePaint.strokeWidth = 1F
    }

    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        screenWidth = width
        screenHeight = height
        vectors = FloatArray(this.screenWidth * 4) // xxyy for each line across the screenWidth
    }

    private fun amplitudeToHeightOnScreen(amplitude: Int) =
        amplitude.toFloat() / MAX_AMPLITUDE * (screenHeight - 1)

    /**
     * modifies draw arrays. adds each one to x = size of screen - 1,
     * decrementing the x value of each other one
     */
    fun addAmplitude(amplitude: Int) {
        invalidate()
        val scaledHeight = amplitudeToHeightOnScreen(amplitude)
        var vectorIndex = 0

        if (amplitudeIndex < screenWidth) {
            // Initial phase, first lines on canvas

            // Decrement x value of each other line, so they all move left one
            // vectorIndex = 0,1, 2,3,
            while (vectorIndex < amplitudeIndex * 4) {
                val newX = vectors?.get(vectorIndex)!! - 1
                vectors?.set(vectorIndex, newX) // x
                vectorIndex += 2

            }

            // Add the new line  to the canvas by appending the points to [vectors]
            val x = screenWidth.toFloat() - 1
            vectors?.set(vectorIndex++, x) // x
            vectors?.set(vectorIndex++, (screenHeight.toFloat() - scaledHeight) / 2) // y0
            vectors?.set(vectorIndex++, x) // x
            vectors?.set(vectorIndex, (screenHeight.toFloat() + scaledHeight) / 2) // y1

            amplitudeIndex++

        } else {
            // After canvas is filled up

            while (vectorIndex < (amplitudeIndex - 1) * 4) {
                // Decrement x value of each other line, so they all move left one
                val newX = vectors?.get(vectorIndex)!! - 1
                if (newX < 0) {
                    vectors?.set(vectorIndex++, screenWidth.toFloat() - 1) // x
                    vectors?.set(vectorIndex++, (screenHeight.toFloat() - scaledHeight) / 2)   // y0
                    vectors?.set(vectorIndex++, screenWidth.toFloat() - 1)   // x
                    vectors?.set(vectorIndex++, (screenHeight.toFloat() + scaledHeight) / 2)  // y1
                } else {
                    vectors?.set(vectorIndex, newX) // x
                    vectorIndex += 2
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        vectors?.run { canvas.drawLines(this, linePaint) }
    }

    fun setAmplitudes(amplitudes: ArrayList<Int>) {
        Log.i("FOO", "Adding ${amplitudes.size} amps")
        //todo actually add them amplitudes.forEach { addAmplitude(it) } doesn't work - ConcurrentModificationException
    }

    companion object {
        private const val MAX_AMPLITUDE = Short.MAX_VALUE
    }
}