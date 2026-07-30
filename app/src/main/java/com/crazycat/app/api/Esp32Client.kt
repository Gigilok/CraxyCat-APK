package com.crazycat.app.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object Esp32Client {
    private const val BASE_URL = "http://192.168.4.1"
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private suspend fun sendCommand(endpoint: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$BASE_URL$endpoint").build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) { false }
    }

    suspend fun checkConnection(): Boolean = sendCommand("/api/status")

    // CC1101
    suspend fun startCC1101Copy() = sendCommand("/api/cc1101/capture")
    suspend fun stopCC1101Copy() = sendCommand("/api/cc1101/stop")
    suspend fun startCC1101Replay() = sendCommand("/api/cc1101/replay")
    suspend fun startCC1101Jammer() = sendCommand("/api/cc1101/jam?action=start")
    suspend fun stopCC1101Jammer() = sendCommand("/api/cc1101/jam?action=stop")
    suspend fun startCC1101RollJam() = sendCommand("/api/cc1101/rolljam")
    suspend fun startCC1101Analyzer() = sendCommand("/api/cc1101/analyzer")

    // NRF24
    suspend fun startNRF24Jammer() = sendCommand("/api/nrf24/jam?action=start")
    suspend fun stopNRF24Jammer() = sendCommand("/api/nrf24/jam?action=stop")
    suspend fun startNRF24Scan() = sendCommand("/api/nrf24/scan")

    // BLE
    suspend fun startBLESpam() = sendCommand("/api/ble/spam")
    suspend fun startBLEScan() = sendCommand("/api/ble/scan")

    // WiFi
    suspend fun startDeauth() = sendCommand("/api/wifi/deauth")
    suspend fun startEvilTwin() = sendCommand("/api/wifi/portal")
    suspend fun startHandshake() = sendCommand("/api/wifi/handshake")

    // Outros
    suspend fun startDroneJammer() = sendCommand("/api/drone/jam")
    suspend fun startCameraFreeze() = sendCommand("/api/camera/freeze")
    suspend fun startBruteForceGate() = sendCommand("/api/bruteforce/gate")
}
