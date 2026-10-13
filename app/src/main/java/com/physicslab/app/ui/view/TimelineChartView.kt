package com.physicslab.app.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.physicslab.app.R
import com.physicslab.app.data.SensorReading
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Native Canvas Timeline Chart View for the Statistics section.
 *
 * Requirements satisfied:
 * - 0 readings: Displays an attractive empty baseline state.
 * - 1 reading: Renders a distinct single point without a broken line.
 * - 2+ readings: Smooth continuous line with area fill and points.
 * - Guard against NaN/Infinity and flat range (min == max) via [ChartScaleHelper].
 * - Interactive touch: scrubbing or tapping near a point displays a tooltip with
 *   name, value, unit, and date, and emits [onPointSelectedListener].
 * - Disallows parent scroll during touch so scrubbing is seamless in scroll views.
 */
class TimelineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density

    // Theme Colors
    private val colorPrimary = ContextCompat.getColor(context, R.color.lab_primary)
    private val colorPrimaryLight = ContextCompat.getColor(context, R.color.lab_primary_light)
    private val colorBorder = ContextCompat.getColor(context, R.color.lab_card_border)
    private val colorTextPrimary = ContextCompat.getColor(context, R.color.lab_text_primary)
    private val colorTextSecondary = ContextCompat.getColor(context, R.color.lab_text_secondary)
    private val colorTextTertiary = ContextCompat.getColor(context, R.color.lab_text_tertiary)
    private val colorSurface = ContextCompat.getColor(context, R.color.lab_surface)

    // Paints
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = colorPrimary
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val pointFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorPrimary
    }

    private val pointStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = Color.WHITE
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = colorBorder
        pathEffect = DashPathEffect(floatArrayOf(6f * density, 6f * density), 0f)
    }

    private val axisLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = colorBorder
    }

    private val yLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTextTertiary
        textSize = 10f * density
        textAlign = Paint.Align.RIGHT
    }

    private val xLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTextTertiary
        textSize = 10f * density
        textAlign = Paint.Align.CENTER
    }

    private val emptyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTextTertiary
        textSize = 13f * density
        textAlign = Paint.Align.CENTER
    }

    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorPrimary
        alpha = 50
    }

    private val guideLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = colorPrimary
        alpha = 100
        pathEffect = DashPathEffect(floatArrayOf(4f * density, 4f * density), 0f)
    }

    private val tooltipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorSurface
    }

    private val tooltipBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = colorBorder
    }

    private val tooltipTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTextPrimary
        textSize = 11f * density
        typeface = Typeface.DEFAULT_BOLD
    }

    private val tooltipValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorPrimary
        textSize = 12f * density
        typeface = Typeface.DEFAULT_BOLD
    }

    private val tooltipDatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTextSecondary
        textSize = 10f * density
    }

    // Paths
    private val linePath = Path()
    private val fillPath = Path()
    private val tooltipRect = RectF()

    // Formatter
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val fullDateTimeFormat = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())

    // Plotted Point Screen Coordinates
    private data class PlottedPoint(
        val x: Float,
        val y: Float,
        val reading: SensorReading
    )
    private val plottedPoints = mutableListOf<PlottedPoint>()

    // State
    private var readings: List<SensorReading> = emptyList()
    private var unit: String = ""
    private var selectedIndex: Int = -1

    var onPointSelectedListener: ((SensorReading?) -> Unit)? = null

    /**
     * Sets the readings to display.
     * Expects readings in chronological order (oldest to newest, left to right).
     */
    fun setReadings(items: List<SensorReading>, physicalUnit: String) {
        readings = items.filter { it.value.isFinite() }
        unit = physicalUnit
        // If selection is out of range, clamp or reset to the newest point
        if (readings.isNotEmpty()) {
            if (selectedIndex < 0 || selectedIndex >= readings.size) {
                selectedIndex = readings.lastIndex
            }
            onPointSelectedListener?.invoke(readings[selectedIndex])
        } else {
            selectedIndex = -1
            onPointSelectedListener?.invoke(null)
        }
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (readings.isEmpty()) return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val touchX = event.x
                val touchY = event.y

                var closestIndex = -1
                var minDistance = Float.MAX_VALUE
                val touchTolerance = 48f * density

                for (i in plottedPoints.indices) {
                    val pt = plottedPoints[i]
                    val dist = hypot(touchX - pt.x, touchY - pt.y)
                    val xDist = abs(touchX - pt.x)
                    // Combine total distance with horizontal proximity
                    val effectiveDist = min(dist, xDist)
                    if (effectiveDist < touchTolerance && dist < minDistance) {
                        minDistance = dist
                        closestIndex = i
                    }
                }

                if (closestIndex != -1 && closestIndex != selectedIndex) {
                    selectedIndex = closestIndex
                    onPointSelectedListener?.invoke(readings[closestIndex])
                    invalidate()
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        plottedPoints.clear()

        val padLeft = 46f * density
        val padRight = 20f * density
        val padTop = 32f * density
        val padBottom = 32f * density
        val chartW = w - padLeft - padRight
        val chartH = h - padTop - padBottom
        val baselineY = padTop + chartH

        // Draw baseline axis
        canvas.drawLine(padLeft, baselineY, padLeft + chartW, baselineY, axisLinePaint)

        // 1. Edge case: 0 readings -> empty state
        if (readings.isEmpty()) {
            canvas.drawText("No saved readings recorded yet", w / 2f, h / 2f, emptyTextPaint)
            return
        }

        // 2. Edge case: 1 reading -> single point, no broken line
        if (readings.size == 1) {
            val reading = readings[0]
            val scale = ChartScaleHelper.computeScale(listOf(reading.value))

            // Draw 3 horizontal grid lines (top, value, bottom)
            val yValues = listOf(scale.yMax, reading.value, scale.yMin)
            for (v in yValues) {
                val gridY = padTop + chartH - ((v - scale.yMin) / scale.yRange * chartH).toFloat()
                canvas.drawLine(padLeft, gridY, padLeft + chartW, gridY, gridPaint)
                canvas.drawText(
                    "${ChartScaleHelper.formatAxisValue(v)} $unit",
                    padLeft - 6f * density,
                    gridY + 3.5f * density,
                    yLabelPaint
                )
            }

            val cx = padLeft + chartW / 2f
            val cy = padTop + chartH / 2f
            plottedPoints.add(PlottedPoint(cx, cy, reading))

            // Draw single point with halo
            canvas.drawCircle(cx, cy, 10f * density, haloPaint)
            canvas.drawCircle(cx, cy, 6f * density, pointFillPaint)
            canvas.drawCircle(cx, cy, 6f * density, pointStrokePaint)
            canvas.drawCircle(cx, cy, 2.5f * density, tooltipBgPaint)

            // Draw bottom date label
            canvas.drawText(timeFormat.format(Date(reading.timestamp)), cx, baselineY + 18f * density, xLabelPaint)

            // Draw tooltip bubble above point
            drawTooltip(canvas, cx, cy, reading, w)
            return
        }

        // 3. 2+ readings: compute scale and line
        val values = readings.map { it.value }
        val scale = ChartScaleHelper.computeScale(values)

        // Draw 3 horizontal grid lines (Max, Mid, Min)
        val gridSteps = 3
        for (step in 0..gridSteps) {
            val ratio = step.toFloat() / gridSteps
            val v = scale.yMin + ratio * scale.yRange
            val gridY = padTop + chartH - (ratio * chartH)
            canvas.drawLine(padLeft, gridY, padLeft + chartW, gridY, gridPaint)
            val labelStr = if (step == gridSteps) {
                "${ChartScaleHelper.formatAxisValue(v)} $unit"
            } else {
                ChartScaleHelper.formatAxisValue(v)
            }
            canvas.drawText(labelStr, padLeft - 6f * density, gridY + 3.5f * density, yLabelPaint)
        }

        // Compute point positions
        val count = readings.size
        for (i in 0 until count) {
            val r = readings[i]
            val x = padLeft + (i.toFloat() / (count - 1)) * chartW
            val yRatio = ((r.value - scale.yMin) / scale.yRange).coerceIn(0.0, 1.0).toFloat()
            val y = padTop + chartH - (yRatio * chartH)
            plottedPoints.add(PlottedPoint(x, y, r))
        }

        // Build continuous line path and gradient fill path
        linePath.reset()
        fillPath.reset()

        fillPath.moveTo(plottedPoints.first().x, baselineY)

        for (i in plottedPoints.indices) {
            val pt = plottedPoints[i]
            if (i == 0) {
                linePath.moveTo(pt.x, pt.y)
                fillPath.lineTo(pt.x, pt.y)
            } else {
                linePath.lineTo(pt.x, pt.y)
                fillPath.lineTo(pt.x, pt.y)
            }
        }

        fillPath.lineTo(plottedPoints.last().x, baselineY)
        fillPath.close()

        // Draw gradient fill
        fillPaint.shader = LinearGradient(
            0f, padTop, 0f, baselineY,
            colorPrimaryLight, Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillPath, fillPaint)

        // Draw line
        canvas.drawPath(linePath, linePaint)

        // Draw X-axis date labels: First, Middle, Last
        val firstDate = timeFormat.format(Date(readings.first().timestamp))
        val lastDate = timeFormat.format(Date(readings.last().timestamp))
        canvas.drawText(firstDate, plottedPoints.first().x, baselineY + 18f * density, xLabelPaint)
        canvas.drawText(lastDate, plottedPoints.last().x, baselineY + 18f * density, xLabelPaint)
        if (count >= 4) {
            val midIdx = count / 2
            val midDate = timeFormat.format(Date(readings[midIdx].timestamp))
            canvas.drawText(midDate, plottedPoints[midIdx].x, baselineY + 18f * density, xLabelPaint)
        }

        // Draw point dots
        val dotRadius = if (count > 30) 2.5f * density else 3.5f * density
        for (i in plottedPoints.indices) {
            val pt = plottedPoints[i]
            if (i != selectedIndex) {
                canvas.drawCircle(pt.x, pt.y, dotRadius, pointFillPaint)
                canvas.drawCircle(pt.x, pt.y, dotRadius, pointStrokePaint)
            }
        }

        // Draw selected point and callout tooltip
        if (selectedIndex in plottedPoints.indices) {
            val sel = plottedPoints[selectedIndex]

            // Vertical guide line
            canvas.drawLine(sel.x, padTop, sel.x, baselineY, guideLinePaint)

            // Glowing halo & highlight ring
            canvas.drawCircle(sel.x, sel.y, 10f * density, haloPaint)
            canvas.drawCircle(sel.x, sel.y, 6f * density, pointFillPaint)
            canvas.drawCircle(sel.x, sel.y, 6f * density, pointStrokePaint)
            canvas.drawCircle(sel.x, sel.y, 2.5f * density, tooltipBgPaint)

            // Draw floating tooltip
            drawTooltip(canvas, sel.x, sel.y, sel.reading, w)
        }
    }

    private fun drawTooltip(canvas: Canvas, ptX: Float, ptY: Float, reading: SensorReading, totalWidth: Float) {
        val boxW = 160f * density
        val boxH = 54f * density
        val corner = 8f * density

        // Clamp box horizontally so it never clips left or right edge
        val left = (ptX - boxW / 2f).coerceIn(12f * density, totalWidth - boxW - 12f * density)
        val right = left + boxW

        // Position above point; if too close to top, position below
        val top = if (ptY - boxH - 12f * density > 8f * density) {
            ptY - boxH - 10f * density
        } else {
            ptY + 12f * density
        }
        val bottom = top + boxH

        tooltipRect.set(left, top, right, bottom)
        canvas.drawRoundRect(tooltipRect, corner, corner, tooltipBgPaint)
        canvas.drawRoundRect(tooltipRect, corner, corner, tooltipBorderPaint)

        // Text lines
        val padding = 8f * density
        val textLeft = left + padding

        // Truncate name if too long
        val displayName = if (reading.name.length > 20) {
            reading.name.take(19) + "…"
        } else {
            reading.name
        }

        canvas.drawText(displayName, textLeft, top + 15f * density, tooltipTitlePaint)

        val formattedVal = String.format(Locale.US, "%.2f %s", reading.value, unit)
        canvas.drawText(formattedVal, textLeft, top + 31f * density, tooltipValuePaint)

        val dateStr = fullDateTimeFormat.format(Date(reading.timestamp))
        canvas.drawText(dateStr, textLeft, top + 46f * density, tooltipDatePaint)
    }
}
