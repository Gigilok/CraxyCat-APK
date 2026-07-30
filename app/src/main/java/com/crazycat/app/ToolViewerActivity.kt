package com.crazycat.app

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import org.json.JSONObject
import java.io.File


class ToolViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TOOL = "tool"
        const val EXTRA_LABEL = "label"
        const val EXTRA_MODE = "mode"
        const val EXTRA_PARAM = "param"
    }

    // UI
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
    private var isRunning = false

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

    // ============================================================
    // PANEL MANAGEMENT
    // ============================================================
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

    // ============================================================
    // TOOL STARTER
    // ============================================================
    private fun startTool(tool: String, mode: String, param: Int) {
        isRunning = true
        startTime = System.currentTimeMillis()
        setStatusRunning(true)
        startTimer()

        when (tool) {
            // === NRF24 ===
            "nrf24_scanner" -> {
                lifecycleScope.launch {
                    Esp32Client.nrf24ScannerStart()
                    pollNRF24Scanner()
                }
            }
            "nrf24_jammer" -> {
                lifecycleScope.launch {
                    Esp32Client.nrf24JammerStart()
                    tvStatusTitle.text = "NRF24 JAMMER"
                    tvStatusDetail.text = "2.4 GHz - Bloqueando"
                    startPulse(Color.parseColor("#FF9100"))
                    pollGenericStatus()
                }
            }

            // === CC1101 ===
            "cc1101_copy" -> {
                lifecycleScope.launch {
                    tvStatusTitle.text = "CAPTURANDO"
                    tvStatusDetail.text = "Varrendo 315/433/868/915 MHz"
                    startPulse(Color.parseColor("#00FF41"))
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
                    tvStatusDetail.text = "Catch and Jam - 433 MHz"
                    startPulse(Color.parseColor("#FF5252"))
                    Esp32Client.cc1101RollJamStart()
                    pollGenericStatus()
                }
            }
            "cc1101_jammer" -> {
                lifecycleScope.launch {
                    tvStatusTitle.text = "JAMMER SUB-GHz"
                    tvStatusDetail.text = "433.92 MHz - Bloqueando"
                    startPulse(Color.parseColor("#FF5252"))
                    Esp32Client.cc1101JammerStart()
                    pollGenericStatus()
                }
            }
            "cc1101_analyzer" -> {
                lifecycleScope.launch {
                    Esp32Client.cc1101AnalyzerStart()
                    pollCC1101Analyzer()
                }
            }
            "cc1101_signals" -> {
                lifecycleScope.launch { loadSignalListReadOnly() }
            }
            "cc1101_clear" -> {
                lifecycleScope.launch {
                    tvStatusTitle.text = "LIMPAR SINAIS"
                    tvStatusDetail.text = "Apagando todos os sinais salvos..."
                    Esp32Client.cc1101ClearSignals()
                    delay(500)
                    tvStatusTitle.text = "LIMPO!"
                    tvStatusDetail.text = "Todos os sinais foram apagados"
                    setStatusRunning(false)
                }
            }

            // === BLUETOOTH ===
            "bt_scan" -> {
                lifecycleScope.launch {
                    Esp32Client.btScan()
                    tvStatusTitle.text = "ESCANEANDO BLE"
                    tvStatusDetail.text = "Aguardando 15 segundos..."
                    startPulse(Color.parseColor("#42A5F5"))
                    delay(3000)
                    pollBTDevices()
                }
            }
            "bt_jammer" -> {
                lifecycleScope.launch { loadBTDeviceList() }
            }

            // === WIFI ===
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

            // === ATTACKS ===
            "drone_jammer" -> {
                lifecycleScope.launch {
                    tvStatusTitle.text = "DRONE JAMMER"
                    tvStatusDetail.text = "868/915 MHz - Bloqueando"
                    startPulse(Color.parseColor("#FF5252"))
                    Esp32Client.droneJammerStart()
                    pollGenericStatus()
                }
            }
            "camera_freeze" -> {
                lifecycleScope.launch {
                    tvStatusTitle.text = "CAMERA FREEZE"
                    tvStatusDetail.text = "Bloqueando transmissao de video"
                    startPulse(Color.parseColor("#FF9100"))
                    Esp32Client.cameraFreezeStart()
                    pollGenericStatus()
                }
            }
            "bf_gate" -> {
                lifecycleScope.launch {
                    tvProgressLabel.text = "BRUTE FORCE PORTAO"
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

    // ============================================================
    // STOP
    // ============================================================
    private fun stopAndFinish() {
        pollJob?.cancel()
        timerJob?.cancel()
        pulseJob?.cancel()

        lifecycleScope.launch {
            val tool = intent.getStringExtra(EXTRA_TOOL) ?: ""
            when (tool) {
                "nrf24_scanner" -> Esp32Client.nrf24ScannerStop()
                "nrf24_jammer" -> Esp32Client.nrf24JammerStop()
                "cc1101_rolljam" -> Esp32Client.cc1101RollJamStop()
                "cc1101_jammer" -> Esp32Client.cc1101JammerStop()
                "cc1101_analyzer" -> Esp32Client.cc1101AnalyzerStop()
                "bt_jammer" -> Esp32Client.btJammerStop()
                "deauth" -> Esp32Client.deauthStop()
                "eviltwin" -> Esp32Client.eviltwinStop()
                "drone_jammer" -> Esp32Client.droneJammerStop()
                "camera_freeze" -> Esp32Client.cameraFreezeStop()
                "bf_gate" -> Esp32Client.bfGateStop()
                "bf_car" -> Esp32Client.bfCarStop()
            }
        }

        finish()
    }

    override fun onBackPressed() { stopAndFinish() }

    // ============================================================
    // STATUS HELPERS
    // ============================================================
    private fun setStatusRunning(running: Boolean) {
        isRunning = running
        runOnUiThread {
            if (running) {
                tvStatusIndicator.text = "ATIVO"
                tvStatusIndicator.setTextColor(Color.parseColor("#00FF41"))
            } else {
                tvStatusIndicator.text = "PARADO"
                tvStatusIndicator.setTextColor(Color.parseColor("#FF5252"))
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
        pulseJob = lifecycleScope.launch {
            val gd = pulseCircle.background as GradientDrawable
            while (true) {
                for (alpha in 100..255 step 5) {
                    gd.setColor(Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)))
                    delay(20)
                }
                for (alpha in 255 downTo 100 step 5) {
                    gd.setColor(Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)))
                    delay(20)
                }
            }
        }
    }

    // ============================================================
    // POLLING: NRF24 SCANNER (16 bars graph)
    // ============================================================
    private suspend fun pollNRF24Scanner() {
        pollJob = lifecycleScope.launch {
            while (true) {
                try {
                    val json = Esp32Client.nrf24ScanData() ?: continue
                    val obj = JSONObject(json)
                    val barsArr = obj.getJSONArray("bars")
                    val packets = obj.optLong("packets", 0)
                    val values = mutableListOf<Int>()
                    for (i in 0 until barsArr.length()) {
                        values.add(barsArr.getInt(i) + 128) // shift from int8
                    }
                    runOnUiThread {
                        barChart.setBars(values, 256)
                        tvInfoText.text = "Canais: 2.4 GHz"
                        tvInfoText2.text = "Pacotes: $packets"
                    }
                } catch (_: Exception) {}
                delay(400)
            }
        }
    }

    // ============================================================
    // POLLING: CC1101 ANALYZER (64 bars graph)
    // ============================================================
    private suspend fun pollCC1101Analyzer() {
        pollJob = lifecycleScope.launch {
            while (true) {
                try {
                    val json = Esp32Client.cc1101AnalyzerData() ?: continue
                    val obj = JSONObject(json)
                    val barsArr = obj.getJSONArray("bars")
                    val freqsArr = obj.getJSONArray("freqs")
                    val values = mutableListOf<Int>()
                    for (i in 0 until barsArr.length()) {
                        values.add(barsArr.getInt(i))
                    }
                    runOnUiThread {
                        barChart.setBars(values, 40)
                        tvInfoText.text = "Espectro Sub-GHz (64 pontos)"
                        if (freqsArr.length() > 0) {
                            tvInfoText2.text = "${freqsArr.getInt(0)}-${freqsArr.getInt(freqsArr.length()-1)} MHz"
                        }
                    }
                } catch (_: Exception) {}
                delay(300)
            }
        }
    }

    // ============================================================
    // POLLING: GENERIC STATUS
    // ============================================================
    private suspend fun pollGenericStatus() {
        pollJob = lifecycleScope.launch {
            while (true) {
                try {
                    val json = Esp32Client.checkConnection()
                    if (json == null) {
                        setStatusRunning(false)
                        break
                    }
                } catch (_: Exception) {}
                delay(2000)
            }
        }
    }

    // ============================================================
    // POLLING: CAPTURE STATUS
    // ============================================================
    private suspend fun pollCaptureStatus() {
        pollJob = lifecycleScope.launch {
            while (true) {
                try {
                    val json = Esp32Client.checkConnection()
                    if (json != null) {
                        val obj = JSONObject(json)
                        val capturing = obj.optBoolean("cc1101_capturing", false)
                        val signals = obj.optInt("cc1101_signals", 0)
                        runOnUiThread {
                            tvStatusDetail.text = if (capturing) "Varrendo frequencias..." else "Captura completa!"
                            if (!capturing && signals > 0) {
                                tvStatusTitle.text = "SINAL CAPTURADO"
                                tvStatusDetail.text = "$signals sinal(is) salvo(s)"
                                setStatusRunning(false)
                                return@launch
                            }
                        }
                    }
                } catch (_: Exception) {}
                delay(1000)
            }
        }
    }

    // ============================================================
    // POLLING: BRUTE FORCE (progress)
    // ============================================================
    private suspend fun pollBruteForce() {
        pollJob = lifecycleScope.launch {
            while (true) {
                try {
                    val json = Esp32Client.bfStatus() ?: continue
                    val obj = JSONObject(json)
                    val running = obj.optBoolean("running", false)
                    val current = obj.optInt("current_index", 0)
                    val total = obj.optInt("gate_total", 1)
                    val percent = if (total > 0) (current * 100 / total) else 0

                    runOnUiThread {
                        if (!running) {
                            tvProgressText.text = "Concluido"
                            tvProgressPercent.text = "100%"
                            progressBar.progress = 100
                            setStatusRunning(false)
                            return@launch
                        }
                        tvProgressText.text = "$current / $total"
                        tvProgressPercent.text = "$percent%"
                        progressBar.progress = percent
                    }
                } catch (_: Exception) {}
                delay(500)
            }
        }
    }

    // ============================================================
    // POLLING: BT DEVICES
    // ============================================================
    private suspend fun pollBTDevices() {
        // First wait for scan
        pollJob = lifecycleScope.launch {
            var attempts = 0
            while (attempts < 30) {
                try {
                    val json = Esp32Client.btStatus() ?: continue
                    val obj = JSONObject(json)
                    val scanning = obj.optBoolean("scanning", false)
                    if (!scanning) {
                        loadBTDeviceListReadOnly()
                        return@launch
                    }
                } catch (_: Exception) {}
                attempts++
                delay(1000)
            }
            loadBTDeviceListReadOnly()
        }
    }

    // ============================================================
    // LIST BUILDERS
    // ============================================================
    private fun createListItem(title: String, subtitle: String, accentColor: Int, clickable: Boolean = false, onClick: (() -> Unit)? = null): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#162447"))
            setPadding(24, 20, 24, 20)
            if (clickable && onClick != null) {
                setOnClickListener { onClick() }
                isClickable = true
                isFocusable = true
            }
        }

        // Accent bar
        val accent = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(4, LinearLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(accentColor)
        }
        item.addView(accent)

        // Text column
        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(16, 0, 0, 0)
        }

        val tvTitle = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        textCol.addView(tvTitle)

        if (subtitle.isNotEmpty()) {
            val tvSub = TextView(this).apply {
                text = subtitle
                setTextColor(Color.parseColor("#888888"))
                textSize = 12sp
            }
            textCol.addView(tvSub)
        }

        item.addView(textCol)

        // Chevron
        if (clickable) {
            val tvChevron = TextView(this).apply {
                text = "▶"
                setTextColor(Color.parseColor("#333333"))
                textSize = 14f
                gravity = Gravity.CENTER_VERTICAL
            }
            item.addView(tvChevron)
        }

        // Bottom margin
        val params = item.layoutParams as LinearLayout.MarginLayoutParams
        params.bottomMargin = 8
        item.layoutParams = params

        return item
    }

    private fun clearList() {
        runOnUiThread { listContent.removeAllViews() }
    }

    // ============================================================
    // LOAD: SIGNALS (read-only)
    // ============================================================
    private suspend fun loadSignalListReadOnly() {
        try {
            val json = Esp32Client.cc1101GetSignals() ?: return
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("signals")
            clearList()
            if (arr.length() == 0) {
                runOnUiThread {
                    listContent.addView(createListItem(
                        "Nenhum sinal", "Use Copy para capturar", Color.parseColor("#FF5252")
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
                        name, "${freq} MHz  |  ID: $id", Color.parseColor("#00FF41")
                    ))
                }
            }
            setStatusRunning(false)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    // ============================================================
    // LOAD: SIGNALS (pick to replay)
    // ============================================================
    private suspend fun loadSignalList() {
        try {
            val json = Esp32Client.cc1101GetSignals() ?: return
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("signals")
            clearList()
            if (arr.length() == 0) {
                runOnUiThread {
                    listContent.addView(createListItem(
                        "Nenhum sinal", "Use Copy para capturar", Color.parseColor("#FF5252")
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
                        name, "${freq} MHz  |  ID: $id", Color.parseColor("#00FF41"), true
                    ) {
                        lifecycleScope.launch {
                            Toast.makeText(this@ToolViewerActivity, "Reproduzindo $name...", Toast.LENGTH_SHORT).show()
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

    // ============================================================
    // LOAD: SIGNALS (pick for Keeloq)
    // ============================================================
    private suspend fun loadSignalListForKeeloq() {
        try {
            // Show keeloq panel initially with signal list below
            keeloqPanel.visibility = View.GONE
            listContainer.visibility = View.VISIBLE

            val json = Esp32Client.cc1101GetSignals() ?: return
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("signals")
            clearList()

            // Add header
            runOnUiThread {
                val header = TextView(this).apply {
                    text = "Selecione um sinal para Rolling Code:"
                    setTextColor(Color.parseColor("#00FF41"))
                    textSize = 14sp
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 0, 0, 16)
                }
                listContent.addView(header)
            }

            if (arr.length() == 0) {
                runOnUiThread {
                    listContent.addView(createListItem(
                        "Nenhum sinal", "Use Copy para capturar primeiro", Color.parseColor("#FF5252")
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
                        name, "${freq} MHz  |  ID: $id", Color.parseColor("#00FF41"), true
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
            // Switch to keeloq panel
            runOnUiThread {
                listContainer.visibility = View.GONE
                keeloqPanel.visibility = View.VISIBLE
                tvKeeloqStatus.text = "Lendo sinal bruto..."
                startPulse(Color.parseColor("#00FF41"))
            }

            val rawJson = Esp32Client.cc1101GetRaw(signalId)
            if (rawJson == null) {
                runOnUiThread { tvKeeloqStatus.text = "Erro ao ler sinal" }
                return
            }

            runOnUiThread { tvKeeloqStatus.text = "Processando Keeloq..." }

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
                    tvKeeloqStatus.text = "Protocolo fixo - Fazendo replay..."
                }
                Esp32Client.cc1101Replay(signalId)
                runOnUiThread { tvKeeloqStatus.text = "Replay enviado!" }
                return
            }

            val (protocol, newTimings) = result
            runOnUiThread {
                tvKeeloqStatus.text = "${protocol.name}\nTransmitindo proximo codigo..."
            }

            val success = Esp32Client.cc1101TransmitRaw(protocol.frequency, newTimings)
            runOnUiThread {
                tvKeeloqStatus.text = if (success) "${protocol.name}\nCodigo transmitido com sucesso!" else "Falha na transmissao"
            }
        } catch (e: Exception) {
            runOnUiThread { tvKeeloqStatus.text = "Erro: ${e.message}" }
        }
    }

    // ============================================================
    // LOAD: NETWORKS (read-only)
    // ============================================================
    private suspend fun loadNetworkList() {
        try {
            val json = Esp32Client.wifiScanNetworks() ?: return
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("networks")
            clearList()
            if (arr.length() == 0) {
                runOnUiThread {
                    listContent.addView(createListItem(
                        "Nenhuma rede", "Nenhuma rede encontrada", Color.parseColor("#FF5252")
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
                val id = net.optInt("id", i)
                val lockIcon = if (encrypted) " [WPA]" else " [OPEN]"
                val captureId = id
                runOnUiThread {
                    listContent.addView(createListItem(
                        ssid, "CH:$channel  RSSI:$rssi$lockIcon  $bssid",
                        Color.parseColor("#FF5252")
                    ))
                }
            }
            setStatusRunning(false)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    // ============================================================
    // LOAD: NETWORKS (pick for Deauth)
    // ============================================================
    private suspend fun loadNetworkListForDeauth() {
        try {
            val json = Esp32Client.wifiScanNetworks() ?: return
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("networks")
            clearList()

            runOnUiThread {
                val header = TextView(this).apply {
                    text = "Selecione uma rede para Deauth:"
                    setTextColor(Color.parseColor("#FF5252"))
                    textSize = 14sp
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 0, 0, 16)
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
                        ssid, "CH:$channel  RSSI:$rssi", Color.parseColor("#FF5252"), true
                    ) {
                        lifecycleScope.launch {
                            listContainer.visibility = View.GONE
                            statusPanel.visibility = View.VISIBLE
                            tvStatusTitle.text = "DEAUTH"
                            tvStatusDetail.text = "Desautenticando $ssid"
                            startPulse(Color.parseColor("#FF5252"))
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

    // ============================================================
    // LOAD: NETWORKS (pick for Evil Twin)
    // ============================================================
    private suspend fun loadNetworkListForEvilTwin() {
        try {
            val json = Esp32Client.wifiScanNetworks() ?: return
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("networks")
            clearList()

            runOnUiThread {
                val header = TextView(this).apply {
                    text = "Selecione uma rede para Evil Twin:"
                    setTextColor(Color.parseColor("#FF9100"))
                    textSize = 14sp
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 0, 0, 16)
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
                        ssid, "CH:$channel  RSSI:$rssi", Color.parseColor("#FF9100"), true
                    ) {
                        lifecycleScope.launch {
                            listContainer.visibility = View.GONE
                            statusPanel.visibility = View.VISIBLE
                            tvStatusTitle.text = "EVIL TWIN"
                            tvStatusDetail.text = "Clonando $ssid"
                            startPulse(Color.parseColor("#FF9100"))
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

    // ============================================================
    // LOAD: BT DEVICES (pick for jammer)
    // ============================================================
    private suspend fun loadBTDeviceList() {
        try {
            val json = Esp32Client.btDevices() ?: return
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("devices")
            clearList()

            runOnUiThread {
                val header = TextView(this).apply {
                    text = "Selecione um dispositivo BLE:"
                    setTextColor(Color.parseColor("#42A5F5"))
                    textSize = 14sp
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(0, 0, 0, 16)
                }
                listContent.addView(header)
            }

            if (arr.length() == 0) {
                runOnUiThread {
                    listContent.addView(createListItem(
                        "Nenhum dispositivo", "Escaneie primeiro", Color.parseColor("#FF5252")
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
                        name, "RSSI: $rssi", Color.parseColor("#42A5F5"), true
                    ) {
                        lifecycleScope.launch {
                            listContainer.visibility = View.GONE
                            statusPanel.visibility = View.VISIBLE
                            tvStatusTitle.text = "BLE JAMMER"
                            tvStatusDetail.text = "Jamming $name"
                            startPulse(Color.parseColor("#42A5F5"))
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

    // ============================================================
    // LOAD: BT DEVICES (read-only)
    // ============================================================
    private suspend fun loadBTDeviceListReadOnly() {
        try {
            val json = Esp32Client.btDevices() ?: return
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("devices")

            runOnUiThread {
                listContainer.visibility = View.VISIBLE
                statusPanel.visibility = View.GONE
                pulseJob?.cancel()
            }
            clearList()

            if (arr.length() == 0) {
                runOnUiThread {
                    listContent.addView(createListItem(
                        "Nenhum dispositivo", "Nenhum BLE encontrado", Color.parseColor("#FF5252")
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
                        name, "RSSI: $rssi", Color.parseColor("#42A5F5")
                    ))
                }
            }
            setStatusRunning(false)
        } catch (e: Exception) {
            runOnUiThread { Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    // ============================================================
    // CRACK FLOW (Aircrack)
    // ============================================================
    private suspend fun runCrackFlow() {
        try {
            runOnUiThread {
                crackProgress.visibility = View.VISIBLE
                tvCrackStatus.text = "Verificando handshake..."
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

            runOnUiThread { tvCrackStatus.text = "Baixando PCAP ($frames frames)..." }

            val pcapFile = File(filesDir, "handshake.pcap")
            val downloaded = Esp32Client.handshakeDownload(pcapFile)
            if (!downloaded || !pcapFile.exists()) {
                runOnUiThread { tvCrackStatus.text = "Falha ao baixar PCAP" }
                return
            }

            runOnUiThread {
                tvCrackStatus.text = "Rodando aircrack...\nAguarde, pode demorar minutos"
                crackProgress.isIndeterminate = true
            }

            AircrackRunner.crackHandshake(this, pcapFile) { foundKey ->
                runOnUiThread {
                    crackProgress.isIndeterminate = false
                    crackProgress.visibility = View.GONE
                    if (foundKey != null) {
                        tvCrackStatus.text = "SENHA ENCONTRADA!\n$foundKey"
                        tvCrackStatus.setTextColor(Color.parseColor("#00FF41"))
                    } else {
                        tvCrackStatus.text = "Senha nao encontrada\nna wordlist"
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
    }
}
