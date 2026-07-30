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
    private val RETRY_INTERVAL_MS = 5000L

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
                    view.scaleX = 0.95f
                    view.scaleY = 0.95f
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
        updateStatusUI(false, "Verificando ESP32...")

        retryRunnable = object : Runnable {
            override fun run() {
                lifecycleScope.launch {
                    val connected = Esp32Client.checkConnection()
                    isConnected = connected
                    if (connected) {
                        updateStatusUI(true, "Crazy Cat Online")
                        retryCount = 0
                        stopConnectionCheck()
                    } else {
                        retryCount++
                        updateStatusUI(false, "ESP32 Offline · Tentativa $retryCount")
                        if (MAX_RETRIES == 0 || retryCount < MAX_RETRIES) {
                            retryRunnable?.let { handler.postDelayed(it, RETRY_INTERVAL_MS) }
                        }
                    }
                }
            }
        }
        handler.postDelayed(retryRunnable!!, 2000)
    }

    private fun stopConnectionCheck() {
        retryRunnable?.let { handler.removeCallbacks(it) }
        retryRunnable = null
    }

    private fun updateStatusUI(connected: Boolean, text: String) {
        runOnUiThread {
            tvStatus.text = text
            if (connected) {
                statusDot.setBackgroundColor(Color.parseColor("#00FF41"))
                tvStatus.setTextColor(Color.parseColor("#00FF41"))
                statusCard.background = createRoundedBg("#0D2818")
            } else {
                statusDot.setBackgroundColor(Color.parseColor("#FF3333"))
                tvStatus.setTextColor(Color.parseColor("#AAAAAA"))
                statusCard.background = createRoundedBg("#1A1A2E")
            }
        }
    }

    private fun createRoundedBg(colorHex: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(colorHex))
            cornerRadius = 24f
        }
    }
}
