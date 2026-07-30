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
import kotlin.math.min

/**
 * Spectrum analyzer bar chart — replicates the CrazyCat firmware OLED style
 * (Flipper Zero style: dual-line bars, peak-hold dots, waterfall, baseline).
 *
 * 16 bars → NRF24 scanner  (with waterfall + peaks)
 * 64 bars → CC1101 analyzer (no waterfall, no peaks, denser)
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ---- data ----
    private val targetBars = mutableListOf<Int>()   // target values from setBars()
    private val animBars   = mutableListOf<Float>() // current animated fractions [0..1]
    private val peakFrac   = mutableListOf<Float>() // peak-hold fractions [0..1]
    private var maxVal: Int = 100

    // waterfall history (oldest row = index 0, newest = last)
    private val WATERFALL_MAX = 30
    private val waterfallHistory = mutableListOf<List<Float>>() // each entry = per-bar fraction snapshot
    private var waterfallEnabled = false

    // ---- colors (firmware SSD1306 white-on-black → green-on-dark for APK) ----
    private var barColor:    Int = Color.parseColor("#00FF41")
    private var peakColor:   Int = Color.parseColor("#AAFFAA")
    private var gridColor:   Int = Color.parseColor("#1A1A2E")
    private var labelColor:  Int = Color.parseColor("#555555")
    private var baseColor:   Int = Color.parseColor("#333333")
    private var waterfallAlpha = 80

    // ---- paints ----
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val peakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = gridColor
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = baseColor
        strokeWidth = 1f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelColor
        textAlign = Paint.Align.CENTER
    }
    private val waterfallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val barRect = RectF()

    /**
     * Set new bar values and animate towards them.
     * @param values   raw values (e.g. RSSI -100..-40 for NRF24, or 0..40 for CC1101)
     * @param maxValue the value that maps to 100 % height
     */
    fun setBars(values: List<Int>, maxValue: Int = 100) {
        maxVal = max(maxValue, 1)
        targetBars.clear()
        targetBars.addAll(values)

        // grow / shrink animated & peak arrays to match
        while (animBars.size < targetBars.size) { animBars.add(0f); peakFrac.add(0f) }
        while (animBars.size > targetBars.size) { animBars.removeAt(animBars.size - 1); peakFrac.removeAt(peakFrac.size - 1) }

        // Enable waterfall only for 16-bar mode (NRF24)
        waterfallEnabled = (targetBars.size <= 16)

        // Animate each bar toward its target fraction
        targetBars.forEachIndexed { i, targetVal ->
            val target = targetVal.toFloat() / maxVal
            val current = animBars[i]
            if (current != target) {
                val anim = ValueAnimator.ofFloat(current, target)
                anim.duration = 150
                anim.addUpdateListener {
                    animBars[i] = it.animatedValue as Float
                    invalidate()
                }
                anim.start()
            }
            // Peak hold: only rise, never fall in this frame
            if (target > peakFrac[i]) peakFrac[i] = target
        }

        // Snapshot current state for waterfall (use post-values so animation completes first)
        post {
            val snapshot = animBars.map { it }
            waterfallHistory.add(snapshot)
            if (waterfallHistory.size > WATERFALL_MAX) waterfallHistory.removeAt(0)
        }

        invalidate()
    }

    fun getBarCount(): Int = targetBars.size

    // ------------------------------------------------------------------
    //  DRAW
    // ------------------------------------------------------------------
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val barCount = max(animBars.size, 1)

        // Layout proportions (mimics firmware: bars take ~65 %, waterfall ~30 %, labels ~5 %)
        val hasWaterfall = waterfallEnabled && waterfallHistory.size > 1
        val labelH    = if (barCount <= 16) 28f else 18f
        val waterfallH = if (hasWaterfall) h * 0.25f else 0f
        val baseY     = h - labelH - waterfallH          // baseline y position
        val topPad    = 8f
        val barMaxH   = baseY - topPad                    // max bar height in px

        // ---- grid lines (4 horizontal, like firmware) ----
        for (i in 1..4) {
            val y = topPad + barMaxH * i / 5f
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        // ---- calculate bar geometry ----
        // Firmware: each bar = 2 px wide on 128 px OLED  →  64 bars at 2 px each
        // APK: scale proportionally, but enforce minimum bar width for readability
        val gap: Float = if (barCount <= 16) 4f else 1f
        val barW: Float = (w - (barCount + 1) * gap) / barCount
        val clampedBarW = max(barW, if (barCount <= 16) 8f else 3f)

        // ---- draw bars (firmware style: two vertical lines per bar) ----
        barPaint.color = barColor
        barPaint.strokeWidth = max(clampedBarW / 3f, 1.5f)

        for (i in 0 until animBars.size) {
            val fraction = animBars[i].coerceIn(0f, 1f)
            if (fraction < 0.005f) continue

            val barH = fraction * barMaxH
            val cx = gap + i * (clampedBarW + gap) + clampedBarW / 2f   // center x of bar
            val x1 = cx - clampedBarW / 4f
            val x2 = cx + clampedBarW / 4f
            val yTop = baseY - barH

            // Color intensity based on height (low = dim green, high = bright green/red)
            if (fraction > 0.85f) {
                barPaint.color = Color.parseColor("#FF5252") // red for very strong signals
            } else if (fraction > 0.6f) {
                barPaint.color = Color.parseColor("#76FF03") // lime for medium
            } else {
                barPaint.color = barColor // standard green
            }

            // Two vertical lines (firmware drawLine × 2)
            canvas.drawLine(x1, baseY, x1, yTop, barPaint)
            canvas.drawLine(x2, baseY, x2, yTop, barPaint)
        }

        // ---- peak-hold dots (only for NRF24 / 16-bar mode) ----
        if (barCount <= 16) {
            peakPaint.color = peakColor
            // Slowly decay peaks
            for (i in 0 until peakFrac.size) {
                if (peakFrac[i] > animBars.getOrElse(i) { 0f }) {
                    peakFrac[i] -= 0.003f // slow decay
                }
                if (peakFrac[i] < 0f) peakFrac[i] = 0f
            }
            for (i in 0 until peakFrac.size) {
                if (peakFrac[i] < 0.01f) continue
                val peakH = peakFrac[i] * barMaxH
                val cx = gap + i * (clampedBarW + gap) + clampedBarW / 2f
                val yPeak = baseY - peakH
                // Draw small rectangle as peak indicator (firmware uses 2 pixels)
                val dotR = max(clampedBarW / 3f, 2f)
                canvas.drawRect(cx - dotR, yPeak - 1f, cx + dotR, yPeak + 1f, peakPaint)
            }
        }

        // ---- baseline (firmware: drawLine across full width) ----
        basePaint.color = baseColor
        basePaint.strokeWidth = 1.5f
        canvas.drawLine(0f, baseY, w, baseY, basePaint)

        // ---- waterfall (NRF24 mode only, below baseline) ----
        if (hasWaterfall) {
            waterfallPaint.color = barColor
            val rows = waterfallHistory.size
            val rowH = waterfallH / WATERFALL_MAX

            for (r in 0 until rows) {
                val row = waterfallHistory[r]
                val wy = baseY + 2f + r * rowH
                // Older rows are more transparent
                val alpha = ((r + 1).toFloat() / rows * waterfallAlpha).toInt()
                waterfallPaint.alpha = alpha
                for (c in row.indices) {
                    if (row[c] < 0.05f) continue
                    val cx = gap + c * (clampedBarW + gap) + clampedBarW / 2f
                    val dotW = max(clampedBarW / 2f, 2f)
                    canvas.drawRect(cx - dotW / 2f, wy, cx + dotW / 2f, wy + max(rowH - 0.5f, 1f), waterfallPaint)
                }
            }
            waterfallPaint.alpha = 255
        }

        // ---- bottom labels (channel / frequency numbers) ----
        textPaint.textSize = if (barCount <= 16) 18f else 12f
        textPaint.color = labelColor
        val step = if (barCount <= 16) 1 else if (barCount <= 32) 4 else 8
        for (i in 0 until barCount step step) {
            val cx = gap + i * (clampedBarW + gap) + clampedBarW / 2f
            canvas.drawText("$i", cx, h - 4f, textPaint)
        }
    }
}
