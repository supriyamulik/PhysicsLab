package com.physicslab.app.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.physicslab.app.R
import java.util.Locale
import kotlin.math.max

/**
 * Bar chart view for the Statistics tab matching the visual design.
 * Renders vertical bars with rounded top corners for recent reading aggregates.
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class BarData(val label: String, val value: Double)

    private val primaryColor: Int = ContextCompat.getColor(context, R.color.lab_primary)
    private val secondaryColor: Int = ContextCompat.getColor(context, R.color.lab_primary_light)
    private val textColor: Int = ContextCompat.getColor(context, R.color.lab_text_secondary)
    private val gridColor: Int = ContextCompat.getColor(context, R.color.lab_card_border)

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = primaryColor
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = gridColor
    }

    private var items: List<BarData> = emptyList()
    private val barRect = RectF()

    fun setData(data: List<BarData>) {
        items = data.takeLast(8)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val padBottom = 48f
        val padTop = 24f
        val padX = 24f
        val chartH = h - padBottom - padTop
        val chartW = w - padX * 2

        // Draw baseline
        val baselineY = h - padBottom
        canvas.drawLine(padX, baselineY, padX + chartW, baselineY, baselinePaint)

        if (items.isEmpty()) {
            labelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("No reading data recorded yet", w / 2f, h / 2f, labelPaint)
            return
        }

        val maxVal = max(0.01, items.maxOfOrNull { it.value } ?: 1.0)
        val count = items.size
        val slotW = chartW / count
        val barW = slotW * 0.55f

        for (i in 0 until count) {
            val item = items[i]
            val left = padX + slotW * i + (slotW - barW) / 2f
            val right = left + barW
            val barH = (item.value / maxVal * chartH).toFloat().coerceAtLeast(8f)
            val top = baselineY - barH

            barRect.set(left, top, right, baselineY)
            canvas.drawRoundRect(barRect, 8f, 8f, barPaint)

            // Label under the bar
            val labelX = left + barW / 2f
            canvas.drawText(item.label, labelX, baselineY + 32f, labelPaint)
        }
    }
}
