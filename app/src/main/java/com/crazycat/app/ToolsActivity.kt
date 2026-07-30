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

    private var currentTool: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tools)

        val category = intent.getStringExtra("CATEGORY") ?: return

        val tvCategory = findViewById<TextView>(R.id.tvToolsCategory)
        tvCategory.text = when (category) {
            "SUBGHZ" -> "SUB-GHz \u00b7 CC1101"
            "NRF24" -> "2.4 GHz \u00b7 NRF24"
            "BLUETOOTH" -> "BLUETOOTH \u00b7 BLE"
            "WIFI" -> "WI-FI \u00b7 802.11"
            "ATTACKS" -> "ATAQUES"
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

        fun setupCard(card: View, textView: TextView, text: String, toolName: String, action: suspend () -> Boolean) {
            textView.text = text
            card.setOnClickListener {
                currentTool = toolName
                lifecycleScope.launch {
                    val success = action()
                    runOnUiThread {
                        Toast.makeText(this@ToolsActivity, if (success) "Ativado!" else "Falha!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            card.setOnTouchListener { view, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> view.alpha = 0.7f
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> view.alpha = 1.0f
                }
                false
            }
        }

        btn3.visibility = View.GONE
        btn4.visibility = View.GONE
        btn5.visibility = View.GONE

        when (category) {
            "SUBGHZ" -> {
                setupCard(btn1, tv1, "Copy", "cc1101_copy", Esp32Client::cc1101Copy)
                setupCard(btn2, tv2, "Replay (id=0)", "cc1101_replay") {
                    Esp32Client.cc1101Replay(0)
                }
                btn3.visibility = View.VISIBLE
                setupCard(btn3, tv3, "Ver Sinais", "cc1101_signals") {
                    Esp32Client.cc1101GetSignals() != null
                }
            }
            "NRF24" -> {
                setupCard(btn1, tv1, "Jammer", "nrf24_jammer", Esp32Client::nrf24JammerStart)
                setupCard(btn2, tv2, "Scanner", "nrf24_scanner", Esp32Client::nrf24ScannerStart)
                btn3.visibility = View.VISIBLE
                setupCard(btn3, tv3, "Parar NRF24", "nrf24_stop") {
                    Esp32Client.nrf24JammerStop()
                    Esp32Client.nrf24ScannerStop()
                    true
                }
            }
            "BLUETOOTH" -> {
                setupCard(btn1, tv1, "BLE Scan", "bt_scan", Esp32Client::btScan)
                setupCard(btn2, tv2, "BLE Jammer", "bt_jammer") {
                    Esp32Client.btJammerStart(0)
                }
                btn3.visibility = View.VISIBLE
                setupCard(btn3, tv3, "Parar BT", "bt_stop", Esp32Client::btJammerStop)
            }
            "WIFI" -> {
                setupCard(btn1, tv1, "Scan Redes", "wifi_scan", Esp32Client::wifiScanNetworksPost)
                setupCard(btn2, tv2, "Deauth (id=0)", "deauth") {
                    Esp32Client.deauthStart(0)
                }
                btn3.visibility = View.VISIBLE
                setupCard(btn3, tv3, "Parar Deauth", "deauth_stop", Esp32Client::deauthStop)
                btn4.visibility = View.VISIBLE
                setupCard(btn4, tv4, "Evil Twin (id=0)", "eviltwin") {
                    Esp32Client.eviltwinStart(0)
                }
                btn5.visibility = View.VISIBLE
                setupCard(btn5, tv5, "Parar Evil Twin", "eviltwin_stop", Esp32Client::eviltwinStop)
            }
            "ATTACKS" -> {
                setupCard(btn1, tv1, "Drone Jammer", "drone_jammer", Esp32Client::droneJammerStart)
                setupCard(btn2, tv2, "Camera Freeze", "camera_freeze", Esp32Client::cameraFreezeStart)
                btn3.visibility = View.VISIBLE
                setupCard(btn3, tv3, "BruteForce Portao", "bf_gate", Esp32Client::bfGateStart)
                btn4.visibility = View.VISIBLE
                setupCard(btn4, tv4, "BruteForce Carro", "bf_car") {
                    Esp32Client.bfCarStart(0)
                }
                btn5.visibility = View.VISIBLE
                setupCard(btn5, tv5, "Parar Tudo", "stop_all") {
                    Esp32Client.droneJammerStop()
                    Esp32Client.cameraFreezeStop()
                    Esp32Client.bfGateStop()
                    Esp32Client.bfCarStop()
                    true
                }
            }
        }
    }

    override fun onBackPressed() {
        stopCurrentTool()
        super.onBackPressed()
    }

    override fun onStop() {
        stopCurrentTool()
        super.onStop()
    }

    private fun stopCurrentTool() {
        if (currentTool == null) return
        lifecycleScope.launch {
            when (currentTool) {
                "nrf24_jammer" -> Esp32Client.nrf24JammerStop()
                "nrf24_scanner" -> Esp32Client.nrf24ScannerStop()
                "bt_jammer" -> Esp32Client.btJammerStop()
                "deauth" -> Esp32Client.deauthStop()
                "eviltwin" -> Esp32Client.eviltwinStop()
                "drone_jammer" -> Esp32Client.droneJammerStop()
                "camera_freeze" -> Esp32Client.cameraFreezeStop()
                "bf_gate" -> Esp32Client.bfGateStop()
                "bf_car" -> Esp32Client.bfCarStop()
                "stop_all" -> {
                    Esp32Client.droneJammerStop()
                    Esp32Client.cameraFreezeStop()
                    Esp32Client.bfGateStop()
                    Esp32Client.bfCarStop()
                }
            }
            currentTool = null
        }
    }
}
