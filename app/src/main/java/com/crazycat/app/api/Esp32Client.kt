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
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    @Volatile
    var lastError: String? = null
        private set

    private fun recordError(e: Throwable) {
        lastError = e.message ?: e.javaClass.simpleName
    }

    private fun clearError() { lastError = null }

        private suspend fun sendPost(endpoint: String, params: Map<String, String> = emptyMap()): Boolean = withContext(Dispatchers.IO) {
        try {
            val builder = FormBody.Builder()
            params.forEach { (k, v) -> builder.add(k, v) }
            val request = Request.Builder()
                .url("$BASE_URL$endpoint")
                .post(builder.build())
                .build()
            var httpCode = 200
            val ok = client.newCall(request).execute().use { response ->
                httpCode = response.code
                response.isSuccessful
            }
            if (ok) clearError() else recordError(java.io.IOException("HTTP $httpCode"))
            ok
        } catch (e: Exception) {
            recordError(e)
            false
        }
        }

        private suspend fun sendGet(endpoint: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$BASE_URL$endpoint").build()
            var httpCode = 200
            val body = client.newCall(request).execute().use { response ->
                httpCode = response.code
                if (response.isSuccessful) response.body?.string() else null
            }
            if (body != null) clearError() else recordError(java.io.IOException("HTTP $httpCode"))
            body
        } catch (e: Exception) {
            recordError(e)
            null
        }
        }

        suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$BASE_URL/api/status").build()
            var httpCode = 200
            val ok = client.newCall(request).execute().use { response ->
                httpCode = response.code
                response.isSuccessful
            }
            if (ok) clearError() else recordError(java.io.IOException("HTTP $httpCode"))
            ok
        } catch (e: Exception) {
            recordError(e)
            false
        }
        }

    suspend fun checkStatusJson(): String? = sendGet("/api/status")

    suspend fun cc1101Copy(): Boolean = sendPost("/api/cc1101/copy")
    suspend fun cc1101Replay(id: Int): Boolean = sendPost("/api/cc1101/replay", mapOf("id" to id.toString()))
    suspend fun cc1101GetSignals(): String? = sendGet("/api/cc1101/signals")
    suspend fun cc1101GetRaw(id: Int): String? = sendGet("/api/cc1101/raw?id=$id")

    suspend fun cc1101RollJamStart(): Boolean = sendPost("/api/cc1101/rolljam/start")
    suspend fun cc1101RollJamStop(): Boolean = sendPost("/api/cc1101/rolljam/stop")

    suspend fun cc1101JammerStart(): Boolean = sendPost("/api/cc1101/jammer/start")
    suspend fun cc1101JammerStop(): Boolean = sendPost("/api/cc1101/jammer/stop")

    suspend fun cc1101AnalyzerStart(): Boolean = sendPost("/api/cc1101/analyzer/start")
    suspend fun cc1101AnalyzerStop(): Boolean = sendPost("/api/cc1101/analyzer/stop")
    suspend fun cc1101AnalyzerData(): String? = sendGet("/api/cc1101/analyzer/data")

    suspend fun cc1101ClearSignals(): Boolean = sendPost("/api/cc1101/clear")

    suspend fun nrf24JammerStart(): Boolean = sendPost("/api/nrf24/jammer/start")
    suspend fun nrf24JammerStop(): Boolean = sendPost("/api/nrf24/jammer/stop")
    suspend fun nrf24ScannerStart(): Boolean = sendPost("/api/nrf24/scanner/start")
    suspend fun nrf24ScannerStop(): Boolean = sendPost("/api/nrf24/scanner/stop")
    suspend fun nrf24ScanData(): String? = sendGet("/api/nrf24/scan")

    suspend fun btScan(): Boolean = sendPost("/api/attack/bt/scan")
    suspend fun btDevices(): String? = sendGet("/api/attack/bt/devices")
    suspend fun btJammerStart(id: Int): Boolean = sendPost("/api/attack/bt/jammer/start", mapOf("id" to id.toString()))
    suspend fun btJammerStop(): Boolean = sendPost("/api/attack/bt/jammer/stop")
    suspend fun btStatus(): String? = sendGet("/api/attack/bt/status")

    suspend fun wifiScanNetworks(): String? = sendGet("/api/networks")
    suspend fun wifiScanNetworksPost(): Boolean = sendPost("/api/networks/scan")
    suspend fun deauthStart(id: Int): Boolean = sendPost("/api/deauth/start", mapOf("id" to id.toString()))
    suspend fun deauthStop(): Boolean = sendPost("/api/deauth/stop")
    suspend fun eviltwinStart(id: Int): Boolean = sendPost("/api/eviltwin/start", mapOf("id" to id.toString()))
    suspend fun eviltwinStop(): Boolean = sendPost("/api/eviltwin/stop")
    suspend fun handshakeStatus(): String? = sendGet("/api/handshake")

    suspend fun handshakeDownload(pcapFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$BASE_URL/api/handshake/download").build()
            val ok = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val body = response.body ?: return@use false
                body.byteStream().use { input ->
                    pcapFile.outputStream().use { output -> input.copyTo(output) }
                }
                true
            }
            if (ok) clearError() else recordError(java.io.IOException("HTTP non-2xx"))
            ok
        } catch (e: Exception) {
            recordError(e)
            false
        }
    }

    suspend fun droneJammerStart(): Boolean = sendPost("/api/attack/drone/jammer/start")
    suspend fun droneJammerStop(): Boolean = sendPost("/api/attack/drone/jammer/stop")
    suspend fun cameraFreezeStart(): Boolean = sendPost("/api/attack/camera/freeze/start")
    suspend fun cameraFreezeStop(): Boolean = sendPost("/api/attack/camera/freeze/stop")
    suspend fun bfGateStart(): Boolean = sendPost("/api/attack/bf/gate/start")
    suspend fun bfGateStop(): Boolean = sendPost("/api/attack/bf/gate/stop")
    suspend fun bfCarStart(brand: Int): Boolean = sendPost("/api/attack/bf/car/start", mapOf("brand" to brand.toString()))
    suspend fun bfCarStop(): Boolean = sendPost("/api/attack/bf/car/stop")
    suspend fun bfStatus(): String? = sendGet("/api/attack/bf/status")

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
            var httpCode = 200
            val ok = client.newCall(request).execute().use { response ->
                httpCode = response.code
                response.isSuccessful
            }
            if (ok) clearError() else recordError(java.io.IOException("HTTP $httpCode"))
            ok
        } catch (e: Exception) {
            recordError(e)
            false
        }
        }
