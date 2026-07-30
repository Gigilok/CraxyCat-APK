package com.crazycat.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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

        // Verifica conexão com o ESP32 ao abrir o app
        checkConnection()

        // Configura todos os botões
        setupButtons()
    }

    private fun checkConnection() {
        tvStatus.text = "Crazy Cat v3.1\nConectando ao ESP32..."
        lifecycleScope.launch {
            val connected = Esp32Client.checkConnection()
            runOnUiThread {
                if (connected) {
                    tvStatus.text = "Crazy Cat v3.1\nESP32 Conectado! IP: 192.168.4.1"
                    tvStatus.setTextColor(getColor(android.R.color.holo_green_light))
                } else {
                    tvStatus.text = "Crazy Cat v3.1\nESP32 Offline!"
                    tvStatus.setTextColor(getColor(android.R.color.holo_red_light))
                }
            }
        }
    }

    private fun setupButtons() {
        // Função auxiliar para não repetir código
        fun setupButton(id: Int, action: suspend () -> Boolean) {
            findViewById<Button>(id).setOnClickListener {
                lifecycleScope.launch {
                    val success = action()
                    runOnUiThread {
                        if (success) Toast.makeText(this@MainActivity, "Comando Enviado!", Toast.LENGTH_SHORT).show()
                        else Toast.makeText(this@MainActivity, "Falha! ESP32 offline.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // CC1101
        setupButton(R.id.btnCC1101Copy, Esp32Client::startCC1101Copy)
        setupButton(R.id.btnCC1101Replay, Esp32Client::startCC1101Replay)
        setupButton(R.id.btnCC1101Jammer, Esp32Client::startCC1101Jammer)
        setupButton(R.id.btnCC1101RollJam, Esp32Client::startCC1101RollJam)
        setupButton(R.id.btnCC1101Analyzer, Esp32Client::startCC1101Analyzer)

        // NRF24
        setupButton(R.id.btnNRF24Jammer, Esp32Client::startNRF24Jammer)
        setupButton(R.id.btnNRF24Scan, Esp32Client::startNRF24Scan)

        // BLE
        setupButton(R.id.btnBLESpam, Esp32Client::startBLESpam)
        setupButton(R.id.btnBLEScan, Esp32Client::startBLEScan)

        // WiFi
        setupButton(R.id.btnDeauth, Esp32Client::startDeauth)
        setupButton(R.id.btnEvilTwin, Esp32Client::startEvilTwin)
        setupButton(R.id.btnHandshake, Esp32Client::startHandshake)

        // Outros
        setupButton(R.id.btnDroneJammer, Esp32Client::startDroneJammer)
        setupButton(R.id.btnCameraFreeze, Esp32Client::startCameraFreeze)
        setupButton(R.id.btnBruteForce, Esp32Client::startBruteForceGate)
    }
}
