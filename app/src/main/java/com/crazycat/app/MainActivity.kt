package com.crazycat.app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.crazycat.app.api.Esp32Client
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var statusDot: View
    private lateinit var statusCard: LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private var retryRunnable: Runnable? = null
    private var isConnected = false
    private var retryCount = 0
    private val MAX_RETRIES = 0
    private val RETRY_INTERVAL_MS = 4000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        statusDot = findViewById(R.id.statusDot)
        statusCard = findViewById(R.id.statusCard)

        setupCard(R.id.cardSubGhz, "SUBGHZ")
        setupCard(R.id.cardNrf24, "NRF24")
        setupCard(R.id.cardBluetooth, "BLUETOOTH")
        setupCard(R.id.cardWifi, "WIFI")
        setupCard(R.id.cardAtaques, "ATTACKS")
    }

    override fun onResume() {
        super.onResume()
        startConnectionCheck()
    }

    override fun onPause() {
        super.onPause()
        stopConnectionCheck()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopConnectionCheck()
    }

    private fun setupCard(cardId: Int, category: String) {
        val card = findViewById<LinearLayout>(cardId)

        card.setOnClickListener { view ->
            if (!isConnected) {
                view.alpha = 0.5f
                handler.postDelayed({ view.alpha = 1.0f }, 200)
                return@setOnClickListener
            }
            val intent = Intent(this, ToolsActivity::class.java)
            intent.putExtra("CATEGORY", category)
            startActivity(intent)
        }

        card.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    view.scaleX = 0.96f
                    view.scaleY = 0.96f
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    view.scaleX = 1.0f
                    view.scaleY = 1.0f
                }
            }
            false
        }
    }

    private fun startConnectionCheck() {
        stopConnectionCheck()
        updateStatusUI(state = ConnState.CONNECTING)

        retryRunnable = object : Runnable {
            override fun run() {
                lifecycleScope.launch {
                    val connected = try { Esp32Client.checkConnection() } catch (_: Exception) { false }
                    isConnected = connected

                    if (connected) {
                        updateStatusUI(state = ConnState.ONLINE)
                        retryCount = 0
                        stopConnectionCheck()
                    } else {
                        retryCount++
                        updateStatusUI(state = ConnState.OFFLINE, retry = retryCount)
                        if (MAX_RETRIES == 0 || retryCount < MAX_RETRIES) {
                            retryRunnable?.let { handler.postDelayed(it, RETRY_INTERVAL_MS) }
                        }
                    }
                }
            }
        }

        handler.postDelayed(retryRunnable!!, 1500)
    }

    private fun stopConnectionCheck() {
        retryRunnable?.let { handler.removeCallbacks(it) }
        retryRunnable = null
    }

    private enum class ConnState { CONNECTING, ONLINE, OFFLINE }

    private fun updateStatusUI(state: ConnState, retry: Int = 0) {
        runOnUiThread {
            val dotBg: GradientDrawable
            val cardBg: GradientDrawable
            val text: String
            val textColor: Int
            val dotColor: Int

            when (state) {
                ConnState.CONNECTING -> {
                    dotColor = Color.parseColor("#F59E0B")
                    text = getString(R.string.status_connecting)
                    textColor = Color.parseColor("#E6EAF2")
                    dotBg = roundedDot(dotColor)
                    cardBg = roundedCard("#1A2030", "#2A3140")
                }
                ConnState.ONLINE -> {
                    dotColor = Color.parseColor("#22C55E")
                    text = getString(R.string.status_online)
                    textColor = Color.parseColor("#E6EAF2")
                    dotBg = roundedDot(dotColor)
                    cardBg = roundedCard("#0E2A1A", "#22C55E")
                }
                ConnState.OFFLINE -> {
                    dotColor = Color.parseColor("#EF4444")
                    text = "${getString(R.string.status_offline)} · ${getString(R.string.status_retry, retry)}"
                    textColor = Color.parseColor("#8A93A2")
                    dotBg = roundedDot(dotColor)
                    cardBg = roundedCard("#2A0E0E", "#3A1A1A")
                }
            }

            tvStatus.text = text
            tvStatus.setTextColor(textColor)
            statusDot.background = dotBg
            statusCard.background = cardBg
        }
    }

    private fun roundedDot(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    private fun roundedCard(bgColor: String, borderColor: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(bgColor))
            cornerRadius = 28f
            setStroke(1, Color.parseColor(borderColor))
        }
    }
}
