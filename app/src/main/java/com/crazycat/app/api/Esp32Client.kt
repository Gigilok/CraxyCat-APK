package com.crazycat.app.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object Esp32Client {
    private const val BASE_URL = "http://192.168.4.1:8080"

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private suspend fun sendPost(endpoint: String, params: Map<String, String> = emptyMap()): Boolean = withContext(Dispatchers.IO) {
        try {
            val builder = FormBody.Builder()
            params.forEach { (k, v) -> builder.add(k, v) }
            val request = Request.Builder()
                .url("$BASE_URL$endpoint")
                .post(builder.build())
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) { false }
    }

    private suspend fun sendGet(endpoint: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$BASE_URL$endpoint").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) { null }
    }

    // === CONEXAO ===
    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$BASE_URL/api/status").build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) { false }
    }

    /**
     * GET /api/status — returns the full JSON status object
     * Used to check flags like cc1101_capturing, nrf24_scanning, etc.
     */
    suspend fun checkStatusJson(): String? = sendGet("/api/status")

    // === SUB-GHz (CC1101) ===
    suspend fun cc1101Copy(): Boolean = sendPost("/api/cc1101/copy")
    suspend fun cc1101Replay(id: Int): Boolean = sendPost("/api/cc1101/replay", mapOf("id" to id.toString()))
    suspend fun cc1101GetSignals(): String? = sendGet("/api/cc1101/signals")
    suspend fun cc1101GetRaw(id: Int): String? = sendGet("/api/cc1101/raw?id=$id")

    // === SUB-GHz (CC1101) - RollJam ===
    suspend fun cc1101RollJamStart(): Boolean = sendPost("/api/cc1101/rolljam/start")
    suspend fun cc1101RollJamStop(): Boolean = sendPost("/api/cc1101/rolljam/stop")

    // === SUB-GHz (CC1101) - Jammer ===
    suspend fun cc1101JammerStart(): Boolean = sendPost("/api/cc1101/jammer/start")
    suspend fun cc1101JammerStop(): Boolean = sendPost("/api/cc1101/jammer/stop")

    // === SUB-GHz (CC1101) - Spectrum Analyzer ===
    suspend fun cc1101AnalyzerStart(): Boolean = sendPost("/api/cc1101/analyzer/start")
    suspend fun cc1101AnalyzerStop(): Boolean = sendPost("/api/cc1101/analyzer/stop")
    suspend fun cc1101AnalyzerData(): String? = sendGet("/api/cc1101/analyzer/data")

    // === SUB-GHz (CC1101) - Clear ===
    suspend fun cc1101ClearSignals(): Boolean = sendPost("/api/cc1101/clear")

    // === NRF24 ===
    suspend fun nrf24JammerStart(): Boolean = sendPost("/api/nrf24/jammer/start")
    suspend fun nrf24JammerStop(): Boolean = sendPost("/api/nrf24/jammer/stop")
    suspend fun nrf24ScannerStart(): Boolean = sendPost("/api/nrf24/scanner/start")
    suspend fun nrf24ScannerStop(): Boolean = sendPost("/api/nrf24/scanner/stop")
    suspend fun nrf24ScanData(): String? = sendGet("/api/nrf24/scan")

    // === BLUETOOTH ===
    suspend fun btScan(): Boolean = sendPost("/api/attack/bt/scan")
    suspend fun btDevices(): String? = sendGet("/api/attack/bt/devices")
    suspend fun btJammerStart(id: Int): Boolean = sendPost("/api/attack/bt/jammer/start", mapOf("id" to id.toString()))
    suspend fun btJammerStop(): Boolean = sendPost("/api/attack/bt/jammer/stop")
    suspend fun btStatus(): String? = sendGet("/api/attack/bt/status")

    // === WIFI ===
    suspend fun wifiScanNetworks(): String? = sendGet("/api/networks")
    suspend fun wifiScanNetworksPost(): Boolean = sendPost("/api/networks/scan")
    suspend fun deauthStart(id: Int): Boolean = sendPost("/api/deauth/start", mapOf("id" to id.toString()))
    suspend fun deauthStop(): Boolean = sendPost("/api/deauth/stop")
    suspend fun eviltwinStart(id: Int): Boolean = sendPost("/api/eviltwin/start", mapOf("id" to id.toString()))
    suspend fun eviltwinStop(): Boolean = sendPost("/api/eviltwin/stop")
    suspend fun handshakeStatus(): String? = sendGet("/api/handshake")

    // === WIFI - DOWNLOAD HANDSHAKE PCAP ===
    suspend fun handshakeDownload(pcapFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$BASE_URL/api/handshake/download").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body ?: return@withContext false
                body.byteStream().use { input ->
                    pcapFile.outputStream().use { output -> input.copyTo(output) }
                }
                true
            }
        } catch (e: Exception) { false }
    }

    // === ATTACKS ===
    suspend fun droneJammerStart(): Boolean = sendPost("/api/attack/drone/jammer/start")
    suspend fun droneJammerStop(): Boolean = sendPost("/api/attack/drone/jammer/stop")
    suspend fun cameraFreezeStart(): Boolean = sendPost("/api/attack/camera/freeze/start")
    suspend fun cameraFreezeStop(): Boolean = sendPost("/api/attack/camera/freeze/stop")
    suspend fun bfGateStart(): Boolean = sendPost("/api/attack/bf/gate/start")
    suspend fun bfGateStop(): Boolean = sendPost("/api/attack/bf/gate/stop")
    suspend fun bfCarStart(brand: Int): Boolean = sendPost("/api/attack/bf/car/start", mapOf("brand" to brand.toString()))
    suspend fun bfCarStop(): Boolean = sendPost("/api/attack/bf/car/stop")
    suspend fun bfStatus(): String? = sendGet("/api/attack/bf/status")

    // === CC1101 TRANSMIT RAW (JSON body para Keeloq) ===
    suspend fun cc1101TransmitRaw(frequency: Long, timings: List<Int>): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            json.put("frequency", frequency)
            json.put("timings", JSONArray(timings))
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/api/cc1101/transmit_raw")
                .post(body)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) { false }
    }
}
