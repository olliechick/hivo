package nz.co.olliechick.hivo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat

class VisualizerView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var vectors: FloatArray? = null
    private var insertIdx = 0
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

    /**
     * modifies draw arrays. adds each one to x = size of screen - 1, decrementing the x value of each other one
     */
    fun addAmplitude(amplitude: Int) {
        invalidate()
        val scaledHeight = amplitude.toFloat() / MAX_AMPLITUDE * (screenHeight - 1)
        var vectorIdx = 0

        if (insertIdx < screenWidth) {
            // Initial phase, first lines on canvas

            while (vectorIdx < insertIdx * 4) {
                // Decrement x value of each other line
                val newX = vectors?.get(vectorIdx)!! - 1
                if (newX < 0) {
                    vectors?.set(vectorIdx++, 0f) // x
                    vectors?.set(vectorIdx++, 0f) // y
                } else {
                    vectors?.set(vectorIdx, newX) // x
                    vectorIdx += 2
                }
            }

            vectors?.set(vectorIdx++, screenWidth.toFloat() - 1) // x0
            vectors?.set(vectorIdx++, (screenHeight.toFloat() - scaledHeight) / 2)   // y0
            vectors?.set(vectorIdx++, screenWidth.toFloat() - 1)   // x1
            vectors?.set(vectorIdx, (screenHeight.toFloat() + scaledHeight) / 2)  // y1

            insertIdx++

        } else {
            // After canvas is filled up

            while (vectorIdx < (insertIdx - 1) * 4) {
                // Decrement x value of each other line
                val newX = vectors?.get(vectorIdx)!! - 1
                if (newX < 0) {
                    vectors?.set(vectorIdx++, screenWidth.toFloat() - 1) // x0
                    vectors?.set(vectorIdx++, (screenHeight.toFloat() - scaledHeight) / 2)   // y0
                    vectors?.set(vectorIdx++, screenWidth.toFloat() - 1)   // x1
                    vectors?.set(vectorIdx++, (screenHeight.toFloat() + scaledHeight) / 2)  // y1
                } else {
                    vectors?.set(vectorIdx, newX) // x
                    vectorIdx += 2
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        vectors?.run { canvas.drawLines(this, linePaint) }
    }

    companion object {
        private val MAX_AMPLITUDE = 32767
    }
}