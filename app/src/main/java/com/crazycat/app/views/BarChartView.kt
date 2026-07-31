package com.crazycat.app.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Spectrum analyzer chart for the Crazy Cat APK.
 *
 * Two operating modes (auto-detected by bar count):
 *  - 16 bars  → legacy NRF24 scanner (RSSI -100..-40)
 *  - 64 bars  → NRF24 Flipper-style scanner OR CC1101 analyzer
 *
 * Visual: solid filled bars with vertical gradient (cyan→blue→purple),
 * external peak-hold markers (amber), smooth waterfall below baseline.
 *
 * Performance: single Choreographer callback drives all animation.
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val targetBars = mutableListOf<Float>()
    private val animBars   = mutableListOf<Float>()
    private val peakFrac   = mutableListOf<Float>()  // internal auto-peak (decays)
    private val externalPeaks = mutableListOf<Int>() // external peak values from firmware
    private var maxVal: Int = 100

    private val WATERFALL_MAX = 24
    private val waterfallHistory = mutableListOf<FloatArray>()
    private var waterfallEnabled = false

    // ---- palette (car multimedia) ----
    private val colorLow      = Color.parseColor("#00D4FF")
    private val colorMid      = Color.parseColor("#3B82F6")
    private val colorHigh     = Color.parseColor("#A855F7")
    private val colorPeak     = Color.parseColor("#F59E0B")
    private val colorGrid     = Color.parseColor("#1E232D")
    private val colorBaseline = Color.parseColor("#2A3140")
    private val colorLabel    = Color.parseColor("#5A6473")

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val peakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorPeak
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorGrid
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorBaseline
        strokeWidth = 1.5f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorLabel
        textAlign = Paint.Align.CENTER
    }
    private val waterfallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val shaderCache = HashMap<Int, LinearGradient>()
    private val barRect = RectF()

    private var choreographerCallback: Choreographer.FrameCallback? = null
    private var lastFrameNanos = 0L
    private val ANIM_SPEED = 6.0f
    private val PEAK_DECAY = 0.45f

    /**
     * Set new bar values and animate towards them.
     * @param values   raw values (0..maxValue)
     * @param maxValue the value that maps to 100% height
     */
    fun setBars(values: List<Int>, maxValue: Int = 100) {
        maxVal = max(maxValue, 1)
        targetBars.clear()
        targetBars.addAll(values.map { (it.toFloat() / maxVal).coerceIn(0f, 1f) })

        while (animBars.size < targetBars.size) { animBars.add(0f); peakFrac.add(0f) }
        while (animBars.size > targetBars.size) {
            animBars.removeAt(animBars.size - 1)
            peakFrac.removeAt(peakFrac.size - 1)
        }

        // Auto peak-hold (internal): rises with target, decays in animation loop
        for (i in targetBars.indices) {
            if (targetBars[i] > peakFrac[i]) peakFrac[i] = targetBars[i]
        }

        // Waterfall for any bar count (was only ≤16 before — now also for 64-bar NRF24 scanner)
        waterfallEnabled = true

        if (waterfallEnabled) {
            val snapshot = FloatArray(animBars.size) { idx -> animBars[idx] }
            waterfallHistory.add(snapshot)
            if (waterfallHistory.size > WATERFALL_MAX) waterfallHistory.removeAt(0)
        }

        ensureAnimationRunning()
        invalidate()
    }

    /**
     * Set external peak values from firmware (Flipper-style scanner).
     * When set, these override the internal auto-peak markers.
     * Pass null to clear external peaks.
     */
    fun setExternalPeaks(peaks: List<Int>?, maxValue: Int = 40) {
        externalPeaks.clear()
        if (peaks != null) {
            val mv = max(maxValue, 1)
            externalPeaks.addAll(peaks.map { (it.toFloat() / mv).coerceIn(0f, 1f) })
        }
        invalidate()
    }

    fun getBarCount(): Int = targetBars.size

    fun reset() {
        for (i in animBars.indices) animBars[i] = 0f
        for (i in peakFrac.indices) peakFrac[i] = 0f
        externalPeaks.clear()
        waterfallHistory.clear()
        shaderCache.clear()
        invalidate()
    }

    private fun ensureAnimationRunning() {
        if (choreographerCallback != null) return
        val cb = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastFrameNanos == 0L) lastFrameNanos = frameTimeNanos
                val dtSec = ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
                lastFrameNanos = frameTimeNanos

                var needsAnother = false
                for (i in animBars.indices) {
                    val target = if (i < targetBars.size) targetBars[i] else 0f
                    val current = animBars[i]
                    if (current != target) {
                        val diff = target - current
                        val step = diff * (ANIM_SPEED * dtSec)
                        val newVal = if (kotlin.math.abs(step) >= kotlin.math.abs(diff)) target
                                     else current + step
                        animBars[i] = newVal
                        if (newVal != target) needsAnother = true
                    }
                    // internal peak decay (only if no external peaks provided)
                    if (externalPeaks.isEmpty() && peakFrac[i] > 0f) {
                        peakFrac[i] = (peakFrac[i] - PEAK_DECAY * dtSec).coerceAtLeast(0f)
                        if (peakFrac[i] > 0f) needsAnother = true
                    }
                }

                invalidate()
                if (needsAnother || animBars.any { it > 0f } || peakFrac.any { it > 0f } || externalPeaks.isNotEmpty()) {
                    choreographerCallback = this
                    Choreographer.getInstance().postFrameCallback(this)
                } else {
                    choreographerCallback = null
                    lastFrameNanos = 0L
                }
            }
        }
        choreographerCallback = cb
        lastFrameNanos = 0L
        Choreographer.getInstance().postFrameCallback(cb)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        choreographerCallback?.let { Choreographer.getInstance().removeFrameCallback(it) }
        choreographerCallback = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val barCount = max(animBars.size, 1)

        val hasWaterfall = waterfallEnabled && waterfallHistory.size > 1
        val labelH    = if (barCount <= 16) 26f else 16f
        val waterfallH = if (hasWaterfall) h * 0.22f else 0f
        val baseY     = h - labelH - waterfallH
        val topPad    = 10f
        val barMaxH   = (baseY - topPad).coerceAtLeast(20f)

        // ---- grid lines (5 horizontal, subtle) ----
        for (i in 1..4) {
            val y = topPad + barMaxH * i / 5f
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        // ---- calculate bar geometry ----
        val gap: Float = if (barCount <= 16) 6f else 1.5f
        val barW: Float = ((w - (barCount + 1) * gap) / barCount).coerceAtLeast(
            if (barCount <= 16) 8f else 2f
        )

        // ---- draw bars (solid fill with vertical gradient) ----
        for (i in 0 until animBars.size) {
            val fraction = animBars[i].coerceIn(0f, 1f)
            if (fraction < 0.005f) continue

            val barH = fraction * barMaxH
            val cx = gap + i * (barW + gap) + barW / 2f
            val left = cx - barW / 2f
            val right = cx + barW / 2f
            val top = baseY - barH
            val bottom = baseY

            val (lowColor, highColor) = when {
                fraction > 0.85f -> Pair(colorHigh, Color.parseColor("#F472B6"))
                fraction > 0.55f -> Pair(colorMid, colorHigh)
                else              -> Pair(colorLow, colorMid)
            }

            val heightBucket = (barH / 4f).toInt()
            val cacheKey = i * 100_000 + heightBucket
            var shader = shaderCache[cacheKey]
            if (shader == null) {
                shader = LinearGradient(0f, top, 0f, bottom, highColor, lowColor, Shader.TileMode.CLAMP)
                if (shaderCache.size < 512) shaderCache[cacheKey] = shader
            }
            barPaint.shader = shader

            if (barCount <= 16 && barW > 6f) {
                barRect.set(left, top, right, bottom)
                val r = min(barW / 2f, 4f)
                canvas.drawRoundRect(barRect, r, r, barPaint)
            } else {
                canvas.drawRect(left, top, right, bottom, barPaint)
            }
        }
        barPaint.shader = null

        // ---- peak-hold markers (amber dot above bar) ----
        // External peaks (from firmware) take priority; fall back to internal auto-peak
        if (externalPeaks.isNotEmpty()) {
            for (i in externalPeaks.indices) {
                if (externalPeaks[i] < 0.02f) continue
                val peakH = externalPeaks[i] * barMaxH
                val cx = gap + i * (barW + gap) + barW / 2f
                val yPeak = baseY - peakH
                val dotW = max(barW / 2.5f, if (barCount <= 16) 3f else 1.5f)
                canvas.drawRect(cx - dotW, yPeak - 2f, cx + dotW, yPeak + 1f, peakPaint)
            }
        } else if (barCount <= 16) {
            for (i in peakFrac.indices) {
                if (peakFrac[i] < 0.02f) continue
                val peakH = peakFrac[i] * barMaxH
                val cx = gap + i * (barW + gap) + barW / 2f
                val yPeak = baseY - peakH
                val dotW = max(barW / 2.5f, 3f)
                canvas.drawRect(cx - dotW, yPeak - 2f, cx + dotW, yPeak + 1f, peakPaint)
            }
        }

        // ---- baseline ----
        canvas.drawLine(0f, baseY, w, baseY, basePaint)

        // ---- waterfall (below baseline) ----
        if (hasWaterfall) {
            val rows = waterfallHistory.size
            val rowH = waterfallH / WATERFALL_MAX
            for (r in 0 until rows) {
                val row = waterfallHistory[r]
                val wy = baseY + 2f + r * rowH
                val alpha = ((r + 1).toFloat() / rows * 200).toInt()
                waterfallPaint.color = colorLow
                waterfallPaint.alpha = alpha
                for (c in row.indices) {
                    if (row[c] < 0.05f) continue
                    val cx = gap + c * (barW + gap) + barW / 2f
                    val dotW = max(barW / 1.8f, 2.5f)
                    canvas.drawRect(cx - dotW / 2f, wy, cx + dotW / 2f, wy + max(rowH - 0.5f, 1f), waterfallPaint)
                }
            }
            waterfallPaint.alpha = 255
        }

        // ---- bottom labels ----
        textPaint.textSize = if (barCount <= 16) 16f else 11f
        textPaint.color = colorLabel
        val step = if (barCount <= 16) 2 else if (barCount <= 32) 4 else 8
        for (i in 0 until barCount step step) {
            val cx = gap + i * (barW + gap) + barW / 2f
            canvas.drawText("$i", cx, h - 4f, textPaint)
        }
    }
}
