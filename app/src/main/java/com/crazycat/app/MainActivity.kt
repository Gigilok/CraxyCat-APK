package com.crazycat.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.crazycat.app.api.Esp32Client
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)

        // Botões do Menu Principal
        findViewById<Button>(R.id.btnSubGhz).setOnClickListener {
            val intent = Intent(this, ToolsActivity::class.java)
            intent.putExtra("CATEGORY", "SUBGHZ")
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnNrf24).setOnClickListener {
            val intent = Intent(this, ToolsActivity::class.java)
            intent.putExtra("CATEGORY", "NRF24")
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnBluetooth).setOnClickListener {
            val intent = Intent(this, ToolsActivity::class.java)
            intent.putExtra("CATEGORY", "BLUETOOTH")
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnWifi).setOnClickListener {
            val intent = Intent(this, ToolsActivity::class.java)
            intent.putExtra("CATEGORY", "WIFI")
            startActivity(intent)
        }

        checkConnection()
    }

    private fun checkConnection() {
        tvStatus.text = "Verificando ESP32..."
        lifecycleScope.launch {
            val connected = Esp32Client.checkConnection()
            runOnUiThread {
                if (connected) {
                    tvStatus.text = "✅ Crazy Cat Online"
                    tvStatus.setTextColor(getColor(android.R.color.holo_green_light))
                } else {
                    tvStatus.text = "❌ ESP32 Offline"
                    tvStatus.setTextColor(getColor(android.R.color.holo_red_light))
                }
            }
        }
    }
}
