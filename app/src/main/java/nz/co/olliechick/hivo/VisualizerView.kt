package nz.co.olliechick.hivo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class VisualizerView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var amplitudes: FloatArray? = null
    private var vectors: FloatArray? = null
    private var insertIdx = 0
    private val pointPaint: Paint
    private val linePaint: Paint = Paint()
    private var width1: Int = 0
    private var height1: Int = 0

    init {
        linePaint.color = Color.GREEN
        linePaint.strokeWidth = 1F
        pointPaint = Paint()
        pointPaint.color = Color.BLUE
        pointPaint.strokeWidth = 1F
    }

    override fun onSizeChanged(width: Int, h: Int, oldw: Int, oldh: Int) {
        this.width1 = width
        height1 = h
        amplitudes = FloatArray(this.width1 * 2) // xy for each point across the width1
        vectors = FloatArray(this.width1 * 4) // xxyy for each line across the width1
    }

    /**
     * modifies draw arrays. cycles back to zero when amplitude samples reach max screen size
     */
    fun addAmplitude(amplitude: Int) {
        invalidate()
        val scaledHeight = amplitude.toFloat() / MAX_AMPLITUDE * (height1 - 1)
        var ampIdx = insertIdx * 2
        amplitudes?.set(ampIdx++, insertIdx.toFloat())   // x
        amplitudes?.set(ampIdx, scaledHeight)  // y
        var vectorIdx = insertIdx * 4
        vectors?.set(vectorIdx++, insertIdx.toFloat())   // x0
        vectors?.set(vectorIdx++, 0f)           // y0
        vectors?.set(vectorIdx++, insertIdx.toFloat())   // x1
        vectors?.set(vectorIdx, scaledHeight)  // y1
        // insert index must be shorter than screen width1
        insertIdx = if (++insertIdx >= width1) 0 else insertIdx
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawLines(vectors, linePaint)
        canvas.drawPoints(amplitudes, pointPaint)
    }

    companion object {
        private val MAX_AMPLITUDE = 32767
    }
}