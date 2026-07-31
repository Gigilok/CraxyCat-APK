package com.crazycat.app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.crazycat.app.api.Esp32Client
import com.crazycat.app.tools.AircrackRunner
import com.crazycat.app.tools.SubGhzProcessor
import com.crazycat.app.views.BarChartView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File


class ToolViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TOOL = "tool"
        const val EXTRA_LABEL = "label"
        const val EXTRA_MODE = "mode"
        const val EXTRA_PARAM = "param"
    }

    private lateinit var tvTitle: TextView
    private lateinit var tvStatusIndicator: TextView
    private lateinit var infoBar: LinearLayout
    private lateinit var tvInfoText: TextView
    private lateinit var tvInfoText2: TextView
    private lateinit var barChart: BarChartView
    private lateinit var listContainer: ScrollView
    private lateinit var listContent: LinearLayout
    private lateinit var statusPanel: LinearLayout
    private lateinit var pulseCircle: View
    private lateinit var tvStatusTitle: TextView
    private lateinit var tvStatusDetail: TextView
    private lateinit var tvStatusTimer: TextView
    private lateinit var progressPanel: LinearLayout
    private lateinit var tvProgressLabel: TextView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var tvProgressText: TextView
    private lateinit var tvProgressPercent: TextView
    private lateinit var keeloqPanel: LinearLayout
    private lateinit var tvKeeloqStatus: TextView
    private lateinit var crackPanel: LinearLayout
    private lateinit var tvCrackStatus: TextView
    private lateinit var crackProgress: android.widget.ProgressBar
    private lateinit var btnStop: Button

    private var pollJob: Job? = null
    private var startTime: Long = 0
    private var timerJob: Job? = null
    private var pulseJob: Job? = null
    @Volatile private var isRunning: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tool_viewer)

        bindViews()
        val tool = intent.getStringExtra(EXTRA_TOOL) ?: return
        val label = intent.getStringExtra(EXTRA_LABEL) ?: tool
        val mode = intent.getStringExtra(EXTRA_MODE) ?: "status"
        val param = intent.getIntExtra(EXTRA_PARAM, 0)

        tvTitle.text = label
        btnStop.setOnClickListener { stopAndFinish() }
        findViewById<View>(R.id.btnBack).setOnClickListener { stopAndFinish() }

        showPanel(mode)
        startTool(tool, mode, param)
    }

    private fun bindViews() {
        tvTitle = findViewById(R.id.tvToolTitle)
        tvStatusIndicator = findViewById(R.id.tvStatusIndicator)
        infoBar = findViewById(R.id.infoBar)
        tvInfoText = findViewById(R.id.tvInfoText)
        tvInfoText2 = findViewById(R.id.tvInfoText2)
        barChart = findViewById(R.id.barChart)
        listContainer = findViewById(R.id.listContainer)
        listContent = findViewById(R.id.listContent)
        statusPanel = findViewById(R.id.statusPanel)
        pulseCircle = findViewById(R.id.pulseCircle)
        tvStatusTitle = findViewById(R.id.tvStatusTitle)
        tvStatusDetail = findViewById(R.id.tvStatusDetail)
        tvStatusTimer = findViewById(R.id.tvStatusTimer)
        progressPanel = findViewById(R.id.progressPanel)
        tvProgressLabel = findViewById(R.id.tvProgressLabel)
        progressBar = findViewById(R.id.progressBar)
        tvProgressText = findViewById(R.id.tvProgressText)
        tvProgressPercent = findViewById(R.id.tvProgressPercent)
        keeloqPanel = findViewById(R.id.keeloqPanel)
        tvKeeloqStatus = findViewById(R.id.tvKeeloqStatus)
        crackPanel = findViewById(R.id.crackPanel)
        tvCrackStatus = findViewById(R.id.tvCrackStatus)
        crackProgress = findViewById(R.id.crackProgress)
        btnStop = findViewById(R.id.btnStop)
    }

    private fun showPanel(mode: String) {
        barChart.visibility = View.GONE
        listContainer.visibility = View.GONE
        statusPanel.visibility = View.GONE
        progressPanel.visibility = View.GONE
        keeloqPanel.visibility = View.GONE
        crackPanel.visibility = View.GONE
        infoBar.visibility = View.GONE

        when (mode) {
            "graph16", "graph64" -> {
                barChart.visibility = View.VISIBLE
                infoBar.visibility = View.VISIBLE
            }
            "list_signals", "list_networks", "list_devices" -> {
                listContainer.visibility = View.VISIBLE
            }
            "pick_signal", "pick_network", "pick_device" -> {
                listContainer.visibility = View.VISIBLE
            }
            "status", "confirm" -> {
                statusPanel.visibility = View.VISIBLE
            }
            "progress" -> {
                progressPanel.visibility = View.VISIBLE
            }
            "keeloq" -> {
                keeloqPanel.visibility = View.VISIBLE
            }
            "crack" -> {
                crackPanel.visibility = View.VISIBLE
            }
        }
    }

    private fun switchToPanel(panel: String) {
        runOnUiThread {
            barChart.visibility = View.GONE
            listContainer.visibility = View.GONE
            statusPanel.visibility = View.GONE
            progressPanel.visibility = View.GONE
            keeloqPanel.visibility = View.GONE
            crackPanel.visibility = View.GONE
            pulseJob?.cancel()
            when (panel) {
                "graph" -> { barChart.visibility = View.VISIBLE; infoBar.visibility = View.VISIBLE }
                "list"  -> { listContainer.visibility = View.VISIBLE }
                "status"-> { statusPanel.visibility = View.VISIBLE }
            }
        }
    }

    private fun startTool(tool: String, mode: String, param: Int) {
        isRunning = true
        startTime = System.currentTimeMillis()
        setStatusRunning(true)
        startTimer()

        when (tool) {
            "nrf24_scanner" -> {
                lifecycleScope.launch {
                    barChart.setBars(List(16) { 0 }, 60)
                    tvInfoText.text = "SCAN 2.4GHz"
                    tvInfoText2.text = "F:0"
                    val ok = Esp32Client.nrf24ScannerStart()
                    if (!ok) {
                        tvInfoText.text = "Falha ao iniciar scanner"
                        tvInfoText2.text = "Verifique o firmware"
                    }
                    pollNRF24Scanner()
                }
            }
            "nrf24_jammer" -> {
                lifecycleScope.launch {
                    tvStatusTitle.text = "NRF24 JAMMER"
                    tvStatusDetail.text = "2.4 GHz · Bloqueando"
                    startPulse(Color.parseColor("#F59E0B"))
                    Esp32Client.nrf24JammerStart()
                    pollGenericStatus()
                }
            }
            "cc1101_copy" -> {
                lifecycleScope.launch {
                    tvStatusTitle.text = "CAPTURANDO"
                    tvStatusDetail.text = "Varrendo 315 / 433 / 868 / 915 MHz"
                    startPulse(Color.parseColor("#00D4FF"))
                    Esp32Client.cc1101Copy()
                    pollCaptureStatus()
                }
            }
            "cc1101_replay" -> {
                lifecycleScope.launch { loadSignalList() }
            }
            "rolling_code" -> {
                lifecycleScope.launch { loadSignalListForKeeloq() }
            }
            "cc1101_rolljam" -> {
                lifecycleScope.launch {
                    tvStatusTitle.text = "ROLLJAM"
                    tvStatusDetail.text = "Catch & Jam · 433 MHz"
                    startPulse(Color.parseColor("#A855F7"))
                    val ok = Esp32Client.cc1101RollJamStart()
                    if (!ok) {
                        tvStatusDetail.text = "Erro ao iniciar RollJam"
                    }
                    pollGenericStatus()
                }
            }
            "cc1101_jammer" -> {
                lifecycleScope.launch {
                    tvStatusTitle.text = "JAMMER SUB-GHz"
                    tvStatusDetail.text = "433.92 MHz · Bloqueando"
                    startPulse(Color.parseColor("#EF4444"))
                    val ok = Esp32Client.cc1101JammerStart()
                    if (!ok) {
                        tvStatusDetail.text = "Erro ao iniciar Jammer"
                    }
                    pollGenericStatus()
                }
            }
            "cc1101_analyzer" -> {
                lifecycleScope.launch {
                    barChart.setBars(List(64) { 0 }, 40)
                    tvInfoText.text = "Analisador Sub-GHz"
                    tvInfoText2.text = ""
                    val ok = Esp32Client.cc1101AnalyzerStart()
                    if (!ok) {
                        tvInfoText.text = "Analyzer indisponível"
                        tvInfoText2.text = "Atualize o firmware"
                    }
                    pollCC1101Analyzer()
                }
            }
            "cc1101_signals" -> {
                lifecycleScope.launch { loadSignalListReadOnly() }
            }
            "cc1101_clear" -> {
                lifecycleScope.launch {
                    tvStatusTitle.text = "LIMPAR SINAIS"
                    tvStatusDetail.text = "Apagando todos os sinais salvos…"
                    val ok = Esp32Client.cc1101ClearSignals()
                    delay(500)
                    tvStatusTitle.text = if (ok) "CONCLUÍDO" else "ERRO"
                    tvStatusDetail.text = if (ok) "Todos os sinais foram apagados"
                        else "Falha ao apagar sinais"
                    setStatusRunning(false)
                }
            }
            "bt_scan" -> {
                lifecycleScope.launch {
                    switchToPanel("status")
                    tvStatusTitle.text = "ESCANEANDO BLE"
                    tvStatusDetail.text = "Aguardando 15 segundos…"
                    startPulse(Color.parseColor("#3B82F6"))
                    val ok = Esp32Client.btScan()
                    if (!ok) {
                        tvStatusDetail.text = "Erro ao iniciar scan BLE"
                    }
                    delay(3000)
                    pollBTDevices()
                }
            }
            "bt_jammer" -> {
                lifecycleScope.launch { loadBTDeviceList() }
            }
            "wifi_scan" -> {
                lifecycleScope.launch {
                    Esp32Client.wifiScanNetworksPost()
                    delay(2000)
                    loadNetworkList()
                }
            }
            "deauth" -> {
                lifecycleScope.launch { loadNetworkListForDeauth() }
            }
            "eviltwin" -> {
                lifecycleScope.launch { loadNetworkListForEvilTwin() }
            }
            "crack" -> {
                lifecycleScope.launch { runCrackFlow() }
            }
            "drone_jammer" -> {
                lifecycleScope.launch {
                    tvStatusTitle.text = "DRONE JAMMER"
                    tvStatusDetail.text = "868 / 915 MHz · Bloqueando"
                    startPulse(Color.parseColor("#EF4444"))
                    Esp32Client.droneJammerStart()
                    pollGenericStatus()
                }
            }
            "camera_freeze" -> {
                lifecycleScope.launch {
                    tvStatusTitle.text = "CAMERA FREEZE"
                    tvStatusDetail.text = "Bloqueando transmissão de vídeo"
                    startPulse(Color.parseColor("#F59E0B"))
                    Esp32Client.cameraFreezeStart()
                    pollGenericStatus()
                }
            }
            "bf_gate" -> {
                lifecycleScope.launch {
                    tvProgressLabel.text = "BRUTE FORCE PORTÃO"
                    Esp32Client.bfGateStart()
                    pollBruteForce()
                }
            }
            "bf_car" -> {
                lifecycleScope.launch {
                    tvProgressLabel.text = "BRUTE FORCE CARRO"
                    Esp32Client.bfCarStart(param)
                    pollBruteForce()
                }
            }
        }
    }

    private fun stopAndFinish() {
        pollJob?.cancel()
        timerJob?.cancel()
        pulseJob?.cancel()
        isRunning = false

        val tool = intent.getStringExtra(EXTRA_TOOL) ?: ""
                Thread {
            try {
                runBlocking {
                    when (tool) {
                        "nrf24_scanner" -> Esp32Client.nrf24ScannerStop()
                        "nrf24_jammer"  -> Esp32Client.nrf24JammerStop()
                        "cc1101_rolljam"-> Esp32Client.cc1101RollJamStop()
                        "cc1101_jammer" -> Esp32Client.cc1101JammerStop()
                        "cc1101_analyzer" -> Esp32Client.cc1101AnalyzerStop()
                        "bt_jammer"     -> Esp32Client.btJammerStop()
                        "deauth"        -> Esp32Client.deauthStop()
                        "eviltwin"      -> Esp32Client.eviltwinStop()
                        "drone_jammer"  -> Esp32Client.droneJammerStop()
                        "camera_freeze" -> Esp32Client.cameraFreezeStop()
                        "bf_gate"       -> Esp32Client.bfGateStop()
                        "bf_car"        -> Esp32Client.bfCarStop()
                        else -> {}
                    }
                }
            } catch (_: Exception) { /* swallow — we're leaving anyway */ }
            runOnUiThread {
                try { barChart.reset() } catch (_: Exception) {}
                finish()
            }
        }.start()
    }

    override fun onBackPressed() { stopAndFinish() }

    private fun setStatusRunning(running: Boolean) {
        isRunning = running
        runOnUiThread {
            if (running) {
                tvStatusIndicator.text = "ATIVO"
                tvStatusIndicator.setTextColor(Color.parseColor("#22C55E"))
            } else {
                tvStatusIndicator.text = "PARADO"
                tvStatusIndicator.setTextColor(Color.parseColor("#EF4444"))
            }
        }
    }

    private fun startTimer() {
        timerJob = lifecycleScope.launch {
            while (true) {
                val elapsed = ((System.currentTimeMillis() - startTime) / 1000)
                val min = String.format("%02d", elapsed / 60)
                val sec = String.format("%02d", elapsed % 60)
                runOnUiThread { tvStatusTimer.text = "$min:$sec" }
                delay(1000)
            }
        }
    }

    private fun startPulse(color: Int) {
        pulseJob?.cancel()
        pulseJob = lifecycleScope.launch {
            val gd = GradientDrawable()
            gd.shape = GradientDrawable.OVAL
            val baseColor = Color.argb(80, Color.red(color), Color.green(color), Color.blue(color))
            gd.setColor(baseColor)
            gd.setStroke(2, color)
            runOnUiThread { pulseCircle.background = gd }

            while (true) {
                for (alpha in 80..220 step 8) {
                    if (!isRunning) break
                    val c = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
                    runOnUiThread { gd.setColor(c) }
                    delay(24)
                }
                for (alpha in 220 downTo 80 step 8) {
                    if (!isRunning) break
                    val c = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
                    runOnUiThread { gd.setColor(c) }
                    delay(24)
                }
            }
        }
    }

    private suspend fun pollNRF24Scanner() {
        pollJob = lifecycleScope.launch {
            var errorCount = 0
            while (true) {
                if (!isRunning) break
                try {
                    val json = Esp32Client.nrf24ScanData()
                    if (json == null) {
                        errorCount++
                        if (errorCount > 5) {
                            runOnUiThread {
                                tvInfoText.text = "Sem resposta do ESP32"
                            }
                            delay(2000)
                            continue
                        }
                        delay(400)
                        continue
                    }
                    errorCount = 0
                    val obj = JSONObject(json)

                    if (obj.has("status") && obj.optString("status") == "error") {
                        runOnUiThread {
                            tvInfoText.text = "Erro: ${obj.optString("message", "?")}"
                        }
                        delay(2000)
                        continue
                    }

                    val barsArr = obj.getJSONArray("bars")
                    val packets = obj.optLong("packets", 0)

                    val values = mutableListOf<Int>()
                    for (i in 0 until barsArr.length()) {
                        val rssi = jsonArrEntryToInt(barsArr, i, default = -100)
                        val displayVal = rssi + 100
                        values.add(displayVal.coerceIn(0, 60))
                    }

                    runOnUiThread {
                        barChart.setBars(values, 60)
                        tvInfoText.text = "SCAN 2.4GHz"
                        tvInfoText2.text = "F:$packets"
                    }
                } catch (_: Exception) {
                }
                delay(400)
            }
        }
    }

    private suspend fun pollCC1101Analyzer() {
        pollJob = lifecycleScope.launch {
            var errorCount = 0
            while (true) {
                if (!isRunning) break
                try {
                    val data = Esp32Client.cc1101AnalyzerData()
                    if (data == null) {
                        errorCount++
                        if (errorCount <= 3) {
                            delay(1000)
                            continue
                        }
                        runOnUiThread {
                            tvInfoText.text = "Endpoint indisponível"
                            tvInfoText2.text = "Atualize o firmware"
                        }
                        delay(5000)
                        continue
                    }
                    errorCount = 0
                    val obj = JSONObject(data)

                    if (obj.has("status") && obj.optString("status") == "error") {
                        runOnUiThread {
                            tvInfoText.text = "Erro: ${obj.optString("message", "?")}"
                        }
                        delay(3000)
                        continue
                    }

                    val barsArr = obj.getJSONArray("bars")
                    val values = mutableListOf<Int>()
                    for (i in 0 until barsArr.length()) {
                        values.add(jsonArrEntryToInt(barsArr, i, default = 0).coerceIn(0, 40))
                    }

                    runOnUiThread {
                        barChart.setBars(values, 40)
                        tvInfoText.text = "Espectro Sub-GHz · 64 pontos"
                        if (obj.has("freqs")) {
                            val freqsArr = obj.getJSONArray("freqs")
                            if (freqsArr.length() > 0) {
                                tvInfoText2.text = "${freqsArr.getInt(0)}–${freqsArr.getInt(freqsArr.length()-1)} MHz"
                            }
                        }
                    }
                } catch (_: Exception) {
                }
                delay(300)
            }
        }
    }

    private fun jsonArrEntryToInt(arr: org.json.JSONArray, idx: Int, default: Int): Int {
        return try {
            when (val v = arr.get(idx)) {
                is Number    -> v.toInt()
                is String    -> {
                    if (v.isNotEmpty()) v[0].code.toByte().toInt() else default
                }
                is Boolean   -> if (v) 1 else 0
                else         -> default
            }
        } catch (_: Exception) { default }
    }

    private suspend fun pollGenericStatus() {
        pollJob = lifecycleScope.launch {
            var failCount = 0
            while (true) {
                if (!isRunning) break
                try {
                    val connected = Esp32Client.checkConnection()
                    if (!connected) {
                        failCount++
                        if (failCount >= 3) {
                            setStatusRunning(false)
                            runOnUiThread { tvStatusDetail.text = "ESP32 desconectado!" }
                            break
                        }
                    } else {
                        failCount = 0
                    }
                } catch (_: Exception) { failCount++ }
                delay(2000)
            }
        }
    }

    private suspend fun pollCaptureStatus() {
        pollJob = lifecycleScope.launch {
            var waited = 0
            while (true) {
                if (!isRunning) break
                try {
                    val json = Esp32Client.checkStatusJson()
                    if (json != null) {
                        val obj = JSONObject(json)
                        val capturing = obj.optBoolean("cc1101_capturing", false)
                        val signalCount = obj.optInt("cc1101_signals", 0)
                        runOnUiThread {
                            tvStatusDetail.text = "Varrendo… Sinais: $signalCount"
                        }
                        if (!capturing) {
                            runOnUiThread {
                                tvStatusTitle.text = "CAPTURADO!"
                                tvStatusDetail.text = "Sinal capturado com sucesso"
                            }
                            setStatusRunning(false)
                            break
                        }
                    }
                } catch (_: Exception) {}
                delay(1000)
                waited++
                if (waited > 120) {
                    runOnUiThread { tvStatusDetail.text = "Timeout — tente novamente" }
                    setStatusRunning(false)
                    break
                }
            }
        }
    }

    private suspend fun pollBruteForce() {
        pollJob = lifecycleScope.launch {
            while (true) {
                if (!isRunning) break
                try {
                    var json: String? = null
                    try { json = Esp32Client.bfStatus() } catch (_: Exception) {}
                    if (json == null) { delay(500); continue }
                    val obj = JSONObject(json)
                    val running = obj.optBoolean("running", false)
                    val current = obj.optInt("current_index", 0)
                    val total = obj.optInt("gate_total", 1).coerceAtLeast(1)
                    val percent = (current * 100 / total).coerceIn(0, 100)

                    val finished = !running
                    runOnUiThread {
                        if (finished) {
                            tvProgressText.text = "Concluído"
                            tvProgressPercent.text = "100%"
                            progressBar.progress = 100
                        } else {
                            tvProgressText.text = "$current / $total"
                            tvProgressPercent.text = "$percent%"
                            progressBar.progress = percent
                        }
                    }
                    if (finished) {
                        setStatusRunning(false)
                        break
                    }
                } catch (_: Exception) {}
                delay(500)
            }
        }
    }

    private suspend fun pollBTDevices() {
        pollJob = lifecycleScope.launch {
            var attempts = 0
            while (attempts < 20 && isRunning) {
                try {
                    val json = Esp32Client.btStatus()
                    if (json == null) {
                        attempts++
                        delay(1000)
                        continue
                    }
                    val obj = JSONObject(json)
                    val scanning = obj.optBoolean("scanning", false)
                    if (!scanning) {
                        loadBTDeviceListReadOnly()
                        break
                    }
                    runOnUiThread {
                        tvStatusDetail.text = "Escaneando… ${attempts}s"
                    }
                } catch (_: Exception) {}
                attempts++
                delay(1000)
            }
            loadBTDeviceListReadOnly()
        }
    }

    private fun createListItem(title: String, subtitle: String, accentColor: Int,
                               clickable: Boolean = false, onClick: (() -> Unit)? = null): View {
        val rippleForeground = if (clickable) {
            val attrs = intArrayOf(android.R.attr.selectableItemBackground)
            val ta = obtainStyledAttributes(attrs)
            val d = ta.getDrawable(0)
            ta.recycle()
            d
        } else null

        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = android.content.resources.getDrawable(R.drawable.list_item_bg, theme)
            setPadding(18.dp(), 16.dp(), 18.dp(), 16.dp())
            gravity = Gravity.CENTER_VERTICAL
            if (clickable && onClick != null) {
                setOnClickListener { onClick() }
                isClickable = true
                isFocusable = true
                if (rippleForeground != null) foreground = rippleForeground
            }
        }

        val accent = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(4.dp(), 36.dp())
            setBackgroundColor(accentColor)
        }
        item.addView(accent)

        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(16.dp(), 0, 0, 0)
        }

        val tvT = TextView(this).apply {
            text = title
            setTextColor(Color.parseColor("#E6EAF2"))
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            letterSpacing = 0.02f
        }
        textCol.addView(tvT)

        if (subtitle.isNotEmpty()) {
            val tvS = TextView(this).apply {
                text = subtitle
                setTextColor(Color.parseColor("#8A93A2"))
                textSize = 12f
                letterSpacing = 0.02f
            }
            textCol.addView(tvS)
        }

        item.addView(textCol)

        if (clickable) {
            val tvChevron = TextView(this).apply {
                text = "\u25B6"
                setTextColor(Color.parseColor("#5A6473"))
                textSize = 12f
                gravity = Gravity.CENTER_VERTICAL
            }
            item.addView(tvChevron)
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.bottomMargin = 10.dp()
        item.layoutParams = params

        return item
    }

        /** Quick dp → px helper */
    private fun Int.dp(): Int = (this * this@ToolViewerActivity.resources.displayMetrics.density).toInt()

    private fun clearList() {
        runOnUiThread { listContent.removeAllViews() }
    }

    private suspend fun loadSignalListReadOnly() {
        try {
            val json = Esp32Client.cc1101GetSignals() ?: return
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("signals")
            clearList()
            if (arr.length() == 0) {
                runOnUiThread {
                    listContent.addView(createListItem(
                        "Nenhum sinal", "Use Copy para capturar", Color.parseColor("#EF4444")
                    ))
                }
                return
            }
            for (i in 0 until arr.length()) {
                val sig = arr.getJSONObject(i)
                val name = sig.optString("name", "Sinal $i")
                val freq = sig.optLong("frequency", 0) / 1000000
                val id = sig.optInt("id", i)
                runOnUiThread {
                    listContent.addView(createListItem(
                        name, "${freq} MHz  ·  ID: $id", Color.parseColor("#00D4FF")
                    ))
                }
            }
            setStatusRunning(false)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private suspend fun loadSignalList() {
        try {
            val json = Esp32Client.cc1101GetSignals() ?: return
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("signals")
            clearList()
            if (arr.length() == 0) {
                runOnUiThread {
                    listContent.addView(createListItem(
                        "Nenhum sinal", "Use Copy para capturar", Color.parseColor("#EF4444")
                    ))
                }
                return
            }
            for (i in 0 until arr.length()) {
                val sig = arr.getJSONObject(i)
                val name = sig.optString("name", "Sinal $i")
                val freq = sig.optLong("frequency", 0) / 1000000
                val id = sig.optInt("id", i)
                val captureId = id
                runOnUiThread {
                    listContent.addView(createListItem(
                        name, "${freq} MHz  ·  ID: $id", Color.parseColor("#00D4FF"), true
                    ) {
                        lifecycleScope.launch {
                            Toast.makeText(this@ToolViewerActivity, "Reproduzindo $name…", Toast.LENGTH_SHORT).show()
                            Esp32Client.cc1101Replay(captureId)
                            Toast.makeText(this@ToolViewerActivity, "Replay OK!", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
            setStatusRunning(false)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private suspend fun loadSignalListForKeeloq() {
        try {
            keeloqPanel.visibility = View.GONE
            listContainer.visibility = View.VISIBLE

            val json = Esp32Client.cc1101GetSignals() ?: return
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("signals")
            clearList()

            runOnUiThread {
                val header = TextView(this).apply {
                    text = "Selecione um sinal para Rolling Code:"
                    setTextColor(Color.parseColor("#22C55E"))
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(4.dp(), 0, 0, 16.dp())
                    letterSpacing = 0.02f
                }
                listContent.addView(header)
            }

            if (arr.length() == 0) {
                runOnUiThread {
                    listContent.addView(createListItem(
                        "Nenhum sinal", "Use Copy para capturar primeiro", Color.parseColor("#EF4444")
                    ))
                }
                return
            }

            for (i in 0 until arr.length()) {
                val sig = arr.getJSONObject(i)
                val name = sig.optString("name", "Sinal $i")
                val freq = sig.optLong("frequency", 0) / 1000000
                val id = sig.optInt("id", i)
                val captureId = id
                runOnUiThread {
                    listContent.addView(createListItem(
                        name, "${freq} MHz  ·  ID: $id", Color.parseColor("#22C55E"), true
                    ) {
                        lifecycleScope.launch { processKeeloq(captureId) }
                    })
                }
            }
            setStatusRunning(false)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private suspend fun processKeeloq(signalId: Int) {
        try {
            runOnUiThread {
                listContainer.visibility = View.GONE
                keeloqPanel.visibility = View.VISIBLE
                tvKeeloqStatus.text = "Lendo sinal bruto…"
                startPulse(Color.parseColor("#22C55E"))
            }

            val rawJson = Esp32Client.cc1101GetRaw(signalId)
            if (rawJson == null) {
                runOnUiThread { tvKeeloqStatus.text = "Erro ao ler sinal" }
                return
            }

            runOnUiThread { tvKeeloqStatus.text = "Processando Keeloq…" }

            val json = JSONObject(rawJson)
            val frequency = json.getLong("frequency")
            val length = json.getInt("length")
            val timingsArray = json.getJSONArray("timings")
            val timings = mutableListOf<Int>()
            for (i in 0 until length) timings.add(timingsArray.getInt(i))

            if (timings.size < 4) {
                runOnUiThread { tvKeeloqStatus.text = "Sinal muito curto" }
                return
            }

            val result = SubGhzProcessor.processRollingCode(timings)
            if (result == null) {
                runOnUiThread {
                    tvKeeloqStatus.text = "Protocolo fixo · Fazendo replay…"
                }
                Esp32Client.cc1101Replay(signalId)
                runOnUiThread { tvKeeloqStatus.text = "Replay enviado!" }
                return
            }

            val (protocol, newTimings) = result
            runOnUiThread {
                tvKeeloqStatus.text = "${protocol.name}\nTransmitindo próximo código…"
            }

            val success = Esp32Client.cc1101TransmitRaw(protocol.frequency, newTimings)
            runOnUiThread {
                tvKeeloqStatus.text = if (success) "${protocol.name}\nCódigo transmitido com sucesso!"
                                       else "Falha na transmissão"
            }
        } catch (e: Exception) {
            runOnUiThread { tvKeeloqStatus.text = "Erro: ${e.message}" }
        }
    }

    private suspend fun loadNetworkList() {
        try {
            val json = Esp32Client.wifiScanNetworks() ?: return
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("networks")
            clearList()
            if (arr.length() == 0) {
                runOnUiThread {
                    listContent.addView(createListItem(
                        "Nenhuma rede", "Nenhuma rede encontrada", Color.parseColor("#EF4444")
                    ))
                }
                return
            }
            for (i in 0 until arr.length()) {
                val net = arr.getJSONObject(i)
                val ssid = net.optString("ssid", "?")
                val channel = net.optInt("channel", 0)
                val rssi = net.optInt("rssi", 0)
                val encrypted = net.optBoolean("encrypted", false)
                val bssid = net.optString("bssid", "")
                val lockIcon = if (encrypted) "  [WPA]" else "  [OPEN]"
                runOnUiThread {
                    listContent.addView(createListItem(
                        ssid, "CH:$channel  ·  RSSI:$rssi$lockIcon  ·  $bssid",
                        Color.parseColor("#EF4444")
                    ))
                }
            }
            setStatusRunning(false)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private suspend fun loadNetworkListForDeauth() {
        try {
            val json = Esp32Client.wifiScanNetworks() ?: return
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("networks")
            clearList()

            runOnUiThread {
                val header = TextView(this).apply {
                    text = "Selecione uma rede para Deauth:"
                    setTextColor(Color.parseColor("#EF4444"))
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(4.dp(), 0, 0, 16.dp())
                    letterSpacing = 0.02f
                }
                listContent.addView(header)
            }

            for (i in 0 until arr.length()) {
                val net = arr.getJSONObject(i)
                val ssid = net.optString("ssid", "?")
                val channel = net.optInt("channel", 0)
                val rssi = net.optInt("rssi", 0)
                val id = net.optInt("id", i)
                val captureId = id
                runOnUiThread {
                    listContent.addView(createListItem(
                        ssid, "CH:$channel  ·  RSSI:$rssi", Color.parseColor("#EF4444"), true
                    ) {
                        lifecycleScope.launch {
                            switchToPanel("status")
                            tvStatusTitle.text = "DEAUTH"
                            tvStatusDetail.text = "Desautenticando $ssid"
                            startPulse(Color.parseColor("#EF4444"))
                            Esp32Client.deauthStart(captureId)
                        }
                    })
                }
            }
            setStatusRunning(false)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private suspend fun loadNetworkListForEvilTwin() {
        try {
            val json = Esp32Client.wifiScanNetworks() ?: return
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("networks")
            clearList()

            runOnUiThread {
                val header = TextView(this).apply {
                    text = "Selecione uma rede para Evil Twin:"
                    setTextColor(Color.parseColor("#F59E0B"))
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(4.dp(), 0, 0, 16.dp())
                    letterSpacing = 0.02f
                }
                listContent.addView(header)
            }

            for (i in 0 until arr.length()) {
                val net = arr.getJSONObject(i)
                val ssid = net.optString("ssid", "?")
                val channel = net.optInt("channel", 0)
                val rssi = net.optInt("rssi", 0)
                val id = net.optInt("id", i)
                val captureId = id
                runOnUiThread {
                    listContent.addView(createListItem(
                        ssid, "CH:$channel  ·  RSSI:$rssi", Color.parseColor("#F59E0B"), true
                    ) {
                        lifecycleScope.launch {
                            switchToPanel("status")
                            tvStatusTitle.text = "EVIL TWIN"
                            tvStatusDetail.text = "Clonando $ssid"
                            startPulse(Color.parseColor("#F59E0B"))
                            Esp32Client.eviltwinStart(captureId)
                        }
                    })
                }
            }
            setStatusRunning(false)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private suspend fun loadBTDeviceList() {
        try {
            val json = Esp32Client.btDevices() ?: return
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("devices")
            clearList()

            runOnUiThread {
                val header = TextView(this).apply {
                    text = "Selecione um dispositivo BLE:"
                    setTextColor(Color.parseColor("#3B82F6"))
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(4.dp(), 0, 0, 16.dp())
                    letterSpacing = 0.02f
                }
                listContent.addView(header)
            }

            if (arr.length() == 0) {
                runOnUiThread {
                    listContent.addView(createListItem(
                        "Nenhum dispositivo", "Escaneie primeiro", Color.parseColor("#EF4444")
                    ))
                }
                return
            }

            for (i in 0 until arr.length()) {
                val dev = arr.getJSONObject(i)
                val name = dev.optString("name", "BLE $i")
                val rssi = dev.optInt("rssi", 0)
                val id = dev.optInt("id", i)
                val captureId = id
                runOnUiThread {
                    listContent.addView(createListItem(
                        name, "RSSI: $rssi", Color.parseColor("#3B82F6"), true
                    ) {
                        lifecycleScope.launch {
                            switchToPanel("status")
                            tvStatusTitle.text = "BLE JAMMER"
                            tvStatusDetail.text = "Jamming $name"
                            startPulse(Color.parseColor("#3B82F6"))
                            Esp32Client.btJammerStart(captureId)
                        }
                    })
                }
            }
            setStatusRunning(false)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private suspend fun loadBTDeviceListReadOnly() {
        try {
            val json = Esp32Client.btDevices() ?: return
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("devices")

            switchToPanel("list")
            clearList()

            if (arr.length() == 0) {
                runOnUiThread {
                    listContent.addView(createListItem(
                        "Nenhum dispositivo", "Nenhum BLE encontrado", Color.parseColor("#EF4444")
                    ))
                }
                return
            }

            for (i in 0 until arr.length()) {
                val dev = arr.getJSONObject(i)
                val name = dev.optString("name", "BLE $i")
                val rssi = dev.optInt("rssi", 0)
                runOnUiThread {
                    listContent.addView(createListItem(
                        name, "RSSI: $rssi", Color.parseColor("#3B82F6")
                    ))
                }
            }
            setStatusRunning(false)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private suspend fun runCrackFlow() {
        try {
            runOnUiThread {
                crackProgress.visibility = View.VISIBLE
                tvCrackStatus.text = "Verificando handshake…"
            }

            val statusJson = Esp32Client.handshakeStatus()
            if (statusJson == null) {
                runOnUiThread { tvCrackStatus.text = "ESP32 fora do alcance" }
                return
            }

            val status = JSONObject(statusJson)
            val complete = status.optBoolean("complete", false)
            val frames = status.optInt("frames", 0)

            if (!complete || frames == 0) {
                runOnUiThread { tvCrackStatus.text = "Nenhum handshake capturado\nUse Evil Twin primeiro!" }
                return
            }

            runOnUiThread { tvCrackStatus.text = "Baixando PCAP ($frames frames)…" }

            val pcapFile = File(filesDir, "handshake.pcap")
            val downloaded = Esp32Client.handshakeDownload(pcapFile)
            if (!downloaded || !pcapFile.exists()) {
                runOnUiThread { tvCrackStatus.text = "Falha ao baixar PCAP" }
                return
            }

            runOnUiThread {
                tvCrackStatus.text = "Rodando aircrack…\nAguarde, pode demorar minutos"
                crackProgress.isIndeterminate = true
            }

            AircrackRunner.crackHandshake(this, pcapFile) { foundKey ->
                runOnUiThread {
                    crackProgress.isIndeterminate = false
                    crackProgress.visibility = View.GONE
                    if (foundKey != null) {
                        tvCrackStatus.text = "SENHA ENCONTRADA!\n$foundKey"
                        tvCrackStatus.setTextColor(Color.parseColor("#22C55E"))
                    } else {
                        tvCrackStatus.text = "Senha não encontrada\nna wordlist"
                    }
                }
            }
            setStatusRunning(false)
        } catch (e: Exception) {
            runOnUiThread { tvCrackStatus.text = "Erro: ${e.message}" }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollJob?.cancel()
        timerJob?.cancel()
        pulseJob?.cancel()
        try { barChart.reset() } catch (_: Exception) {}
    }
}
