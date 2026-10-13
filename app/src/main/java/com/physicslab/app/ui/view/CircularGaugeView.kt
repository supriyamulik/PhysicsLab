package com.physicslab.app.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.physicslab.app.R
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Custom circular gauge view matching the modern light UI design.
 * Renders a 240-degree arc with a background track and active primary-blue progress,
 * displaying the live measurement value, unit, and subtitle inside the arc.
 */
class CircularGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val primaryColor: Int = ContextCompat.getColor(context, R.color.lab_primary)
    private val trackColor: Int = ContextCompat.getColor(context, R.color.lab_card_border)
    private val textPrimaryColor: Int = ContextCompat.getColor(context, R.color.lab_text_primary)
    private val textSecondaryColor: Int = ContextCompat.getColor(context, R.color.lab_text_secondary)

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 24f
        strokeCap = Paint.Cap.ROUND
        color = trackColor
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 24f
        strokeCap = Paint.Cap.ROUND
        color = primaryColor
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textPrimaryColor
        textAlign = Paint.Align.CENTER
        textSize = 72f
        isFakeBoldText = true
    }

    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryColor
        textAlign = Paint.Align.CENTER
        textSize = 36f
        isFakeBoldText = true
    }

    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textSecondaryColor
        textAlign = Paint.Align.CENTER
        textSize = 28f
    }

    private val arcBounds = RectF()
    private val startAngle = 150f
    private val sweepTotal = 240f

    private var currentProgress = 0f // 0.0 to 1.0
    private var valueString = "--"
    private var unitString = ""
    private var subtitleString = ""

    fun setMeasurement(
        value: Double?,
        unit: String,
        subtitle: String,
        expectedMin: Double = 0.0,
        expectedMax: Double = 100.0,
        customValueFormatted: String? = null
    ) {
        unitString = unit
        subtitleString = subtitle

        if (value == null || value.isNaN() || value.isInfinite()) {
            valueString = "--"
            currentProgress = 0f
            contentDescription = "$subtitle: No data"
        } else {
            valueString = customValueFormatted ?: String.format(Locale.US, "%.2f", value)
            val range = max(0.001, expectedMax - expectedMin)
            val fraction = ((value - expectedMin) / range).toFloat()
            currentProgress = min(1f, max(0f, fraction))
            contentDescription = "$subtitle: $valueString $unitString"
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val stroke = progressPaint.strokeWidth
        val padding = stroke + 16f
        val diameter = min(w - padding * 2, h - padding * 2)
        val left = (w - diameter) / 2f
        val top = (h - diameter) / 2f
        arcBounds.set(left, top, left + diameter, top + diameter)

        // Scale text size proportional to view width
        val baseScale = w / 320f
        valuePaint.textSize = 64f * baseScale
        unitPaint.textSize = 32f * baseScale
        subtitlePaint.textSize = 26f * baseScale
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (arcBounds.isEmpty) return

        // 1. Draw track
        canvas.drawArc(arcBounds, startAngle, sweepTotal, false, trackPaint)

        // 2. Draw active progress
        val activeSweep = sweepTotal * currentProgress
        if (activeSweep > 0f) {
            canvas.drawArc(arcBounds, startAngle, activeSweep, false, progressPaint)
        }

        // 3. Draw text in center
        val centerX = arcBounds.centerX()
        val centerY = arcBounds.centerY()

        val lengthFactor = if (valueString.length > 7) 0.7f else if (valueString.length > 5) 0.85f else 1.0f
        val baseScale = width / 320f
        valuePaint.textSize = 64f * baseScale * lengthFactor

        // Numeric value
        val valueBaseline = centerY - (valuePaint.descent() + valuePaint.ascent()) / 2f - 20f
        canvas.drawText(valueString, centerX, valueBaseline, valuePaint)

        // Unit right below value
        val unitBaseline = valueBaseline + 42f
        canvas.drawText(unitString, centerX, unitBaseline, unitPaint)

        // Subtitle below unit
        if (subtitleString.isNotEmpty()) {
            val subBaseline = unitBaseline + 36f
            canvas.drawText(subtitleString, centerX, subBaseline, subtitlePaint)
        }
    }
}
