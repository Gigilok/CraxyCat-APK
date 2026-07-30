package com.crazycat.app

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.crazycat.app.api.Esp32Client
import kotlinx.coroutines.launch

class ToolsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tools)

        val category = intent.getStringExtra("CATEGORY") ?: return

        // Atualiza o titulo da categoria no header
        val tvCategory = findViewById<TextView>(R.id.tvToolsCategory)
        tvCategory.text = when (category) {
            "SUBGHZ" -> "SUB-GHz · CC1101"
            "NRF24" -> "2.4 GHz · NRF24"
            "BLUETOOTH" -> "BLUETOOTH · BLE"
            "WIFI" -> "WI-FI · 802.11"
            else -> category
        }

        val btn1 = findViewById<View>(R.id.btnTool1)
        val btn2 = findViewById<View>(R.id.btnTool2)
        val btn3 = findViewById<View>(R.id.btnTool3)
        val btn4 = findViewById<View>(R.id.btnTool4)
        val btn5 = findViewById<View>(R.id.btnTool5)

        val tv1 = findViewById<TextView>(R.id.tvTool1)
        val tv2 = findViewById<TextView>(R.id.tvTool2)
        val tv3 = findViewById<TextView>(R.id.tvTool3)
        val tv4 = findViewById<TextView>(R.id.tvTool4)
        val tv5 = findViewById<TextView>(R.id.tvTool5)

        fun setupCard(card: View, textView: TextView, text: String, action: suspend () -> Boolean) {
            textView.text = text
            card.setOnClickListener {
                lifecycleScope.launch {
                    val success = action()
                    runOnUiThread {
                        Toast.makeText(this@ToolsActivity, if (success) "Enviado!" else "Falha!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            // Efeito de toque
            card.setOnTouchListener { view, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        view.alpha = 0.7f
                    }
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        view.alpha = 1.0f
                    }
                }
                false
            }
        }

        // Esconde botoes nao usados por padrao
        btn3.visibility = View.GONE
        btn4.visibility = View.GONE
        btn5.visibility = View.GONE

        when (category) {
            "SUBGHZ" -> {
                setupCard(btn1, tv1, "Copy", Esp32Client::startCC1101Copy)
                setupCard(btn2, tv2, "Replay", Esp32Client::startCC1101Replay)
                btn3.visibility = View.VISIBLE
                setupCard(btn3, tv3, "Jammer", Esp32Client::startCC1101Jammer)
                btn4.visibility = View.VISIBLE
                setupCard(btn4, tv4, "RollJam", Esp32Client::startCC1101RollJam)
                btn5.visibility = View.VISIBLE
                setupCard(btn5, tv5, "Analyzer", Esp32Client::startCC1101Analyzer)
            }
            "NRF24" -> {
                setupCard(btn1, tv1, "Jammer", Esp32Client::startNRF24Jammer)
                setupCard(btn2, tv2, "Scanner", Esp32Client::startNRF24Scan)
            }
            "BLUETOOTH" -> {
                setupCard(btn1, tv1, "BLE Spam", Esp32Client::startBLESpam)
                setupCard(btn2, tv2, "Scanner", Esp32Client::startBLEScan)
            }
            "WIFI" -> {
                setupCard(btn1, tv1, "Deauth", Esp32Client::startDeauth)
                setupCard(btn2, tv2, "Evil Twin", Esp32Client::startEvilTwin)
                btn3.visibility = View.VISIBLE
                setupCard(btn3, tv3, "Handshake", Esp32Client::startHandshake)
            }
        }
    }
}
