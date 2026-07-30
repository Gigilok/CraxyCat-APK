package com.crazycat.app

import android.os.Bundle
import android.widget.Button
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

        val btn1 = findViewById<Button>(R.id.btnTool1)
        val btn2 = findViewById<Button>(R.id.btnTool2)
        val btn3 = findViewById<Button>(R.id.btnTool3)
        val btn4 = findViewById<Button>(R.id.btnTool4)
        val btn5 = findViewById<Button>(R.id.btnTool5)

        fun setupButton(btn: Button, text: String, action: suspend () -> Boolean) {
            btn.text = text
            btn.setOnClickListener {
                lifecycleScope.launch {
                    val success = action()
                    runOnUiThread {
                        Toast.makeText(this@ToolsActivity, if (success) "✅ Enviado!" else "❌ Falha!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Esconde o botão 5 por padrão
        btn5.visibility = Button.GONE

        when (category) {
            "SUBGHZ" -> {
                title = "📡 Sub-GHz"
                setupButton(btn1, "📋 Copy", Esp32Client::startCC1101Copy)
                setupButton(btn2, "▶️ Replay", Esp32Client::startCC1101Replay)
                setupButton(btn3, "🚫 Jammer", Esp32Client::startCC1101Jammer)
                setupButton(btn4, "🛑 RollJam", Esp32Client::startCC1101RollJam)
                btn5.visibility = Button.VISIBLE
                setupButton(btn5, "📊 Analyzer", Esp32Client::startCC1101Analyzer)
            }
            "NRF24" -> {
                title = "📶 2.4GHz"
                setupButton(btn1, "🚫 Jammer", Esp32Client::startNRF24Jammer)
                setupButton(btn2, "🔍 Scanner", Esp32Client::startNRF24Scan)
                btn3.visibility = Button.GONE
                btn4.visibility = Button.GONE
            }
            "BLUETOOTH" -> {
                title = "🦷 Bluetooth"
                setupButton(btn1, "💣 BLE Spam", Esp32Client::startBLESpam)
                setupButton(btn2, "🔍 Scanner", Esp32Client::startBLEScan)
                btn3.visibility = Button.GONE
                btn4.visibility = Button.GONE
            }
            "WIFI" -> {
                title = "📶 WiFi"
                setupButton(btn1, "💣 Deauth", Esp32Client::startDeauth)
                setupButton(btn2, "🧙 Evil Twin", Esp32Client::startEvilTwin)
                setupButton(btn3, "🤝 Handshake", Esp32Client::startHandshake)
                btn4.visibility = Button.GONE
            }
        }
    }
}
