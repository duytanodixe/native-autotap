package com.example.autotap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CrosshairView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        strokeWidth = 4f
    }

    private val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (width.coerceAtMost(height) / 2f) - 6f

        // Circle
        canvas.drawCircle(centerX, centerY, radius, circlePaint)

        // Crosshair lines
        canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, linePaint)
        canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, linePaint)

        // Center dot
        canvas.drawCircle(centerX, centerY, 6f, centerDotPaint)
    }
}


