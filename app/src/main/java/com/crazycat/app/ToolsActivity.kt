package com.crazycat.app

import android.content.Intent
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

        val tvCategory = findViewById<TextView>(R.id.tvToolsCategory)
        tvCategory.text = when (category) {
            "SUBGHZ" -> "SUB-GHz \u00b7 CC1101"
            "NRF24" -> "2.4 GHz \u00b7 NRF24"
            "BLUETOOTH" -> "BLUETOOTH \u00b7 BLE"
            "WIFI" -> "WI-FI \u00b7 802.11"
            "ATTACKS" -> "ATAQUES"
            else -> category
        }

        val btns = listOf(
            findViewById<View>(R.id.btnTool1),
            findViewById<View>(R.id.btnTool2),
            findViewById<View>(R.id.btnTool3),
            findViewById<View>(R.id.btnTool4),
            findViewById<View>(R.id.btnTool5),
            findViewById<View>(R.id.btnTool6),
            findViewById<View>(R.id.btnTool7),
            findViewById<View>(R.id.btnTool8)
        )
        val tvs = listOf(
            findViewById<TextView>(R.id.tvTool1),
            findViewById<TextView>(R.id.tvTool2),
            findViewById<TextView>(R.id.tvTool3),
            findViewById<TextView>(R.id.tvTool4),
            findViewById<TextView>(R.id.tvTool5),
            findViewById<TextView>(R.id.tvTool6),
            findViewById<TextView>(R.id.tvTool7),
            findViewById<TextView>(R.id.tvTool8)
        )

        btns.forEach { it.visibility = View.GONE }

        fun tool(btn: View, tv: TextView, label: String, tool: String, mode: String, param: Int = 0) {
            tv.text = label
            btn.visibility = View.VISIBLE
            btn.setOnClickListener {
                val intent = Intent(this, ToolViewerActivity::class.java)
                intent.putExtra(ToolViewerActivity.EXTRA_TOOL, tool)
                intent.putExtra(ToolViewerActivity.EXTRA_LABEL, label)
                intent.putExtra(ToolViewerActivity.EXTRA_MODE, mode)
                intent.putExtra(ToolViewerActivity.EXTRA_PARAM, param)
                startActivity(intent)
            }
        }

        fun stopTool(btn: View, tv: TextView, label: String, vararg actions: suspend () -> Boolean) {
            tv.text = label
            btn.visibility = View.VISIBLE
            btn.setOnClickListener {
                lifecycleScope.launch {
                    actions.forEach { it() }
                    Toast.makeText(this@ToolsActivity, "Parado!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        when (category) {
            "SUBGHZ" -> {
                tool(btns[0], tvs[0], "Copy", "cc1101_copy", "status")
                tool(btns[1], tvs[1], "Replay", "cc1101_replay", "pick_signal")
                tool(btns[2], tvs[2], "Rolling Code", "rolling_code", "keeloq")
                tool(btns[3], tvs[3], "Ver Sinais", "cc1101_signals", "list_signals")
                tool(btns[4], tvs[4], "RollJam", "cc1101_rolljam", "status")
                tool(btns[5], tvs[5], "Jammer 433MHz", "cc1101_jammer", "status")
                tool(btns[6], tvs[6], "Analyzer", "cc1101_analyzer", "graph64")
                tool(btns[7], tvs[7], "Clear Sinais", "cc1101_clear", "confirm")
            }
            "NRF24" -> {
                tool(btns[0], tvs[0], "Jammer", "nrf24_jammer", "status")
                tool(btns[1], tvs[1], "Scanner", "nrf24_scanner", "graph16")
                stopTool(btns[2], tvs[2], "Parar NRF24",
                    { Esp32Client.nrf24JammerStop() },
                    { Esp32Client.nrf24ScannerStop() }
                )
            }
            "BLUETOOTH" -> {
                tool(btns[0], tvs[0], "BLE Scan", "bt_scan", "list_devices")
                tool(btns[1], tvs[1], "BLE Jammer", "bt_jammer", "pick_device")
                stopTool(btns[2], tvs[2], "Parar BT",
                    { Esp32Client.btJammerStop() }
                )
            }
            "WIFI" -> {
                tool(btns[0], tvs[0], "Scan Redes", "wifi_scan", "list_networks")
                tool(btns[1], tvs[1], "Deauth", "deauth", "pick_network")
                tool(btns[2], tvs[2], "Evil Twin", "eviltwin", "pick_network")
                tool(btns[3], tvs[3], "Crack Senha", "crack", "crack")
                stopTool(btns[4], tvs[4], "Parar WiFi",
                    { Esp32Client.deauthStop() },
                    { Esp32Client.eviltwinStop() }
                )
            }
            "ATTACKS" -> {
                tool(btns[0], tvs[0], "Drone Jammer", "drone_jammer", "status")
                tool(btns[1], tvs[1], "Camera Freeze", "camera_freeze", "status")
                tool(btns[2], tvs[2], "BruteForce Portao", "bf_gate", "progress")
                tool(btns[3], tvs[3], "BruteForce Carro", "bf_car", "progress", 0)
                stopTool(btns[4], tvs[4], "Parar Tudo",
                    { Esp32Client.droneJammerStop() },
                    { Esp32Client.cameraFreezeStop() },
                    { Esp32Client.bfGateStop() },
                    { Esp32Client.bfCarStop() }
                )
            }
        }
    }

    override fun onBackPressed() { super.onBackPressed() }
}
