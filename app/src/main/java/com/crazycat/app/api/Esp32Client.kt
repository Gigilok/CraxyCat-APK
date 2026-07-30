package com.crazycat.app.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object Esp32Client {
    // CORRECAO: Porta 8080 (firmware usa WebServer na 8080)
    private const val BASE_URL = "http://192.168.4.1:8080"

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

    // Verifica conexao via endpoint de status
    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$BASE_URL/api/status").build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) { false }
    }

    // === SUB-GHz (CC1101) ===
    suspend fun startCC1101Copy(): Boolean = sendCommand("/api/cc1101/copy")
    suspend fun startCC1101Replay(): Boolean = sendCommand("/api/cc1101/replay")
    suspend fun startCC1101Jammer(): Boolean = sendCommand("/api/cc1101/jammer/start")
    suspend fun startCC1101RollJam(): Boolean = sendCommand("/api/cc1101/rolljam/start")
    suspend fun startCC1101Analyzer(): Boolean = sendCommand("/api/cc1101/analyzer/start")

    // === NRF24 ===
    suspend fun startNRF24Jammer(): Boolean = sendCommand("/api/nrf24/jammer/start")
    suspend fun startNRF24Scan(): Boolean = sendCommand("/api/nrf24/scanner/start")

    // === BLUETOOTH ===
    suspend fun startBLESpam(): Boolean = sendCommand("/api/attack/bt/jammer/start")
    suspend fun startBLEScan(): Boolean = sendCommand("/api/attack/bt/scan")

    // === WIFI ===
    suspend fun startDeauth(): Boolean = sendCommand("/api/deauth/start")
    suspend fun startEvilTwin(): Boolean = sendCommand("/api/eviltwin/start")
    suspend fun startHandshake(): Boolean = sendCommand("/api/handshake/start")
}
