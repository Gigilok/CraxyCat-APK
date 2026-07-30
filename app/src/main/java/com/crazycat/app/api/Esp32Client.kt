package com.crazycat.app.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object Esp32Client {
    private const val BASE_URL = "http://192.168.4.1:8080"

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private suspend fun post(endpoint: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = "".toRequestBody("text/plain".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL$endpoint")
                .post(body)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) { false }
    }

    private suspend fun postWithParams(endpoint: String, params: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = "".toRequestBody("text/plain".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL$endpoint?$params")
                .post(body)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) { false }
    }

    private suspend fun get(endpoint: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$BASE_URL$endpoint").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) { null }
    }

    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$BASE_URL/api/status").build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) { false }
    }

    // === SUB-GHz (CC1101) - endpoints que EXISTEM no firmware ===
    suspend fun startCC1101Copy(): Boolean = post("/api/cc1101/copy")
    suspend fun getCC1101Signals(): String? = get("/api/cc1101/signals")
    suspend fun startCC1101Replay(signalId: Int): Boolean = postWithParams("/api/cc1101/replay", "id=$signalId")

    // === NRF24 ===
    suspend fun startNRF24Jammer(): Boolean = post("/api/nrf24/jammer/start")
    suspend fun stopNRF24Jammer(): Boolean = post("/api/nrf24/jammer/stop")
    suspend fun startNRF24Scan(): Boolean = post("/api/nrf24/scanner/start")
    suspend fun stopNRF24Scan(): Boolean = post("/api/nrf24/scanner/stop")
    suspend fun getNRF24ScanData(): String? = get("/api/nrf24/scan")

    // === BLUETOOTH (via /api/attack/bt/) ===
    suspend fun startBTScan(): Boolean = post("/api/attack/bt/scan")
    suspend fun getBTDevices(): String? = get("/api/attack/bt/devices")
    suspend fun getBTScanStatus(): String? = get("/api/attack/bt/status")
    suspend fun startBTJammer(): Boolean = post("/api/attack/bt/jammer/start")
    suspend fun stopBTJammer(): Boolean = post("/api/attack/bt/jammer/stop")

    // === WIFI ===
    suspend fun startDeauth(netId: Int): Boolean = postWithParams("/api/deauth/start", "id=$netId")
    suspend fun stopDeauth(): Boolean = post("/api/deauth/stop")
    suspend fun startEvilTwin(): Boolean = post("/api/eviltwin/start")
    suspend fun stopEvilTwin(): Boolean = post("/api/eviltwin/stop")
    suspend fun getNetworks(): String? = get("/api/networks")
    suspend fun scanNetworks(): Boolean = post("/api/networks/scan")
    suspend fun getHandshakeStatus(): String? = get("/api/handshake")

    // === ATAQUES ===
    // Drone
    suspend fun startDroneJammer(): Boolean = post("/api/attack/drone/jammer/start")
    suspend fun stopDroneJammer(): Boolean = post("/api/attack/drone/jammer/stop")

    // Camera
    suspend fun startCameraFreeze(): Boolean = post("/api/attack/camera/freeze/start")
    suspend fun stopCameraFreeze(): Boolean = post("/api/attack/camera/freeze/stop")

    // BruteForce
    suspend fun startBFGate(): Boolean = post("/api/attack/bf/gate/start")
    suspend fun stopBFGate(): Boolean = post("/api/attack/bf/gate/stop")
    suspend fun startBFCar(): Boolean = post("/api/attack/bf/car/start")
    suspend fun stopBFCar(): Boolean = post("/api/attack/bf/car/stop")
    suspend fun getBFStatus(): String? = get("/api/attack/bf/status")
}
