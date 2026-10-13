package com.physicslab.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

/**
 * Transparent overlay drawn on top of the CameraX preview. Lets the user tap
 * up to two points; draws markers + a connecting line, and reports the pixel
 * distance between them so RulerActivity can convert it to a real-world
 * distance using the stored calibration ratio.
 */
class RulerOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var onPointsChanged: ((pixelDistance: Float?) -> Unit)? = null

    private val points = mutableListOf<Pair<Float, Float>>()

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#38BDF8"); style = Paint.Style.FILL }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#34D399"); strokeWidth = 4f }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (points.size >= 2) points.clear()
            points.add(event.x to event.y)
            invalidate()
            notifyChange()
            return true
        }
        return super.onTouchEvent(event)
    }

    fun reset() {
        points.clear()
        invalidate()
        notifyChange()
    }

    private fun notifyChange() {
        val distance = if (points.size == 2) {
            val (x1, y1) = points[0]; val (x2, y2) = points[1]
            hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
        } else null
        onPointsChanged?.invoke(distance)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.size == 2) {
            val (x1, y1) = points[0]; val (x2, y2) = points[1]
            canvas.drawLine(x1, y1, x2, y2, linePaint)
        }
        for ((x, y) in points) {
            canvas.drawCircle(x, y, 14f, dotPaint)
        }
    }
}
