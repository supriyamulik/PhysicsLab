package com.physicslab.app.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.physicslab.app.R
import java.util.Locale
import kotlin.math.max

/**
 * Live rolling trend chart view for the Live tab of each quantity screen.
 * Displays grid lines, Y-axis value bounds, and a rolling polyline with subtle area fill.
 */
class LiveTrendView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val lineColor: Int = ContextCompat.getColor(context, R.color.lab_primary)
    private val fillColor: Int = ContextCompat.getColor(context, R.color.lab_primary_light)
    private val gridColor: Int = ContextCompat.getColor(context, R.color.lab_card_border)
    private val textColor: Int = ContextCompat.getColor(context, R.color.lab_text_tertiary)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = lineColor
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = gridColor
        pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = 24f
    }

    private val linePath = Path()
    private val fillPath = Path()

    private var points: List<Double> = emptyList()

    fun setPoints(newPoints: List<Double>) {
        points = newPoints.takeLast(40)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val padLeft = 60f
        val padRight = 20f
        val padTop = 20f
        val padBottom = 40f
        val chartW = w - padLeft - padRight
        val chartH = h - padTop - padBottom

        // Draw 3 horizontal grid lines & labels
        val minVal = points.minOrNull() ?: 0.0
        val maxVal = points.maxOrNull() ?: (minVal + 10.0)
        val safeRange = max(0.01, maxVal - minVal)
        val midVal = minVal + safeRange / 2.0

        val yTop = padTop
        val yMid = padTop + chartH / 2f
        val yBot = padTop + chartH

        canvas.drawLine(padLeft, yTop, padLeft + chartW, yTop, gridPaint)
        canvas.drawLine(padLeft, yMid, padLeft + chartW, yMid, gridPaint)
        canvas.drawLine(padLeft, yBot, padLeft + chartW, yBot, gridPaint)

        // Labels
        canvas.drawText(String.format(Locale.US, "%.1f", maxVal), 8f, yTop + 8f, textPaint)
        canvas.drawText(String.format(Locale.US, "%.1f", midVal), 8f, yMid + 8f, textPaint)
        canvas.drawText(String.format(Locale.US, "%.1f", minVal), 8f, yBot + 8f, textPaint)

        // Time labels at bottom
        canvas.drawText("-10s", padLeft, h - 8f, textPaint)
        canvas.drawText("-5s", padLeft + chartW / 2f - 16f, h - 8f, textPaint)
        canvas.drawText("now", padLeft + chartW - 32f, h - 8f, textPaint)

        if (points.size < 2) return

        val stepX = chartW / (points.size - 1)
        linePath.reset()
        fillPath.reset()

        val startX = padLeft
        val startY = padTop + chartH - ((points[0] - minVal) / safeRange * chartH).toFloat()
        linePath.moveTo(startX, startY)
        fillPath.moveTo(startX, padTop + chartH)
        fillPath.lineTo(startX, startY)

        for (i in 1 until points.size) {
            val x = padLeft + stepX * i
            val y = padTop + chartH - ((points[i] - minVal) / safeRange * chartH).toFloat()
            linePath.lineTo(x, y)
            fillPath.lineTo(x, y)
        }

        fillPath.lineTo(padLeft + chartW, padTop + chartH)
        fillPath.close()

        fillPaint.shader = LinearGradient(
            0f, padTop, 0f, padTop + chartH,
            fillColor,
            0x00FFFFFF,
            Shader.TileMode.CLAMP
        )

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)
    }
}
