package com.physicslab.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Lightweight custom trend chart (no external charting library). Draws a
 * simple polyline connecting reading values in chronological order, scaled
 * to fit the view. Good enough to visualize a trend without pulling in a
 * heavyweight dependency.
 */
class TrendGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var values: List<Double> = emptyList()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38BDF8"); strokeWidth = 4f; style = Paint.Style.STROKE
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#34D399") }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748B"); textSize = 32f; textAlign = Paint.Align.CENTER
    }

    fun setValues(newValues: List<Double>) {
        values = newValues
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.size < 2) {
            canvas.drawText("Not enough data for a trend yet", width / 2f, height / 2f, emptyPaint)
            return
        }
        val min = values.min()
        val max = values.max()
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        val padding = 20f
        val usableW = width - padding * 2
        val usableH = height - padding * 2
        val stepX = usableW / (values.size - 1)

        var prevX = padding
        var prevY = padding + usableH - ((values[0] - min) / range * usableH).toFloat()
        for (i in 1 until values.size) {
            val x = padding + stepX * i
            val y = padding + usableH - ((values[i] - min) / range * usableH).toFloat()
            canvas.drawLine(prevX, prevY, x, y, linePaint)
            prevX = x; prevY = y
        }
        for (i in values.indices) {
            val x = padding + stepX * i
            val y = padding + usableH - ((values[i] - min) / range * usableH).toFloat()
            canvas.drawCircle(x, y, 5f, dotPaint)
        }
    }
}
