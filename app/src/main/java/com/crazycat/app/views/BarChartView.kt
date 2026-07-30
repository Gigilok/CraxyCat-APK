package com.crazycat.app.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bars = mutableListOf<Int>()
    private val animatedBars = mutableListOf<Float>()
    private var maxVal: Int = 100
    private var barColor: Int = Color.parseColor("#00FF41")
    private var barColorHigh: Int = Color.parseColor("#FF5252")
    private var gridColor: Int = Color.parseColor("#1A1A2E")
    private var labelColor: Int = Color.parseColor("#444444")

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = gridColor
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelColor
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        alpha = 40
    }

    private val barRect = RectF()

    fun setBars(values: List<Int>, maxValue: Int = 100) {
        maxVal = max(maxValue, 1)
        bars.clear()
        bars.addAll(values)

        // Initialize animated bars if needed
        while (animatedBars.size < bars.size) animatedBars.add(0f)
        while (animatedBars.size > bars.size) animatedBars.removeAt(animatedBars.size - 1)

        // Animate
        bars.forEachIndexed { i, targetVal ->
            val target = targetVal.toFloat() / maxVal
            val current = animatedBars[i]
            val anim = ValueAnimator.ofFloat(current, target)
            anim.duration = 200
            anim.addUpdateListener {
                animatedBars[i] = it.animatedValue as Float
                invalidate()
            }
            anim.start()
        }
        invalidate()
    }

    fun getBarCount(): Int = bars.size

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val barCount = max(animatedBars.size, 1)

        // Draw grid lines (4 horizontal)
        for (i in 1..4) {
            val y = h * i / 5f
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        // Draw bars
        val totalGap = (barCount + 1) * 2f
        val barW = (w - totalGap) / barCount
        val bottomY = h - 25f
        val topY = 10f
        val availableH = bottomY - topY

        for (i in 0 until animatedBars.size) {
            val fraction = animatedBars[i].coerceIn(0f, 1f)
            val barH = fraction * availableH
            val x = 2f + i * (barW + 2f)
            val y = bottomY - barH

            // Color based on height
            if (fraction > 0.8f) {
                barPaint.color = barColorHigh
                glowPaint.color = barColorHigh
            } else {
                barPaint.color = barColor
                glowPaint.color = barColor
            }

            // Glow effect for active bars
            if (fraction > 0.1f) {
                glowPaint.alpha = (fraction * 50).toInt()
                barRect.set(x - 1, y - 1, x + barW + 1, bottomY + 1)
                canvas.drawRoundRect(barRect, 2f, 2f, glowPaint)
            }

            // Bar
            if (barH > 0) {
                barRect.set(x, y, x + barW, bottomY)
                canvas.drawRoundRect(barRect, 2f, 2f, barPaint)
            }

            // Bottom label (every N bars)
            if (barCount <= 16 || i % 8 == 0) {
                textPaint.textSize = if (barCount <= 16) 18f else 14f
                canvas.drawText("$i", x + barW / 2f, h - 4f, textPaint)
            }
        }
    }
}
