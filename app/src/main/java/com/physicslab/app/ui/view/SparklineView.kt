package com.physicslab.app.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.physicslab.app.R
import kotlin.math.max

/**
 * Compact sparkline view for Dashboard cards showing recent trend without axis clutter.
 */
class SparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val strokeColor: Int = ContextCompat.getColor(context, R.color.lab_chart_line)
    private val fillColor: Int = ContextCompat.getColor(context, R.color.lab_primary_light)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = strokeColor
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val linePath = Path()
    private val fillPath = Path()

    private var points: List<Double> = emptyList()

    fun setPoints(newPoints: List<Double>) {
        points = newPoints.takeLast(20)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.size < 2 || width <= 0 || height <= 0) return

        val minVal = points.minOrNull() ?: 0.0
        val maxVal = points.maxOrNull() ?: 1.0
        val range = max(0.001, maxVal - minVal)

        val padX = 8f
        val padY = 8f
        val usableW = width - padX * 2
        val usableH = height - padY * 2
        val stepX = usableW / (points.size - 1)

        linePath.reset()
        fillPath.reset()

        val firstX = padX
        val firstY = padY + usableH - ((points[0] - minVal) / range * usableH).toFloat()
        linePath.moveTo(firstX, firstY)
        fillPath.moveTo(firstX, height.toFloat())
        fillPath.lineTo(firstX, firstY)

        for (i in 1 until points.size) {
            val x = padX + stepX * i
            val y = padY + usableH - ((points[i] - minVal) / range * usableH).toFloat()
            linePath.lineTo(x, y)
            fillPath.lineTo(x, y)
        }

        fillPath.lineTo(padX + usableW, height.toFloat())
        fillPath.close()

        fillPaint.shader = LinearGradient(
            0f, padY, 0f, height.toFloat(),
            fillColor,
            0x00FFFFFF,
            Shader.TileMode.CLAMP
        )

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)
    }
}
