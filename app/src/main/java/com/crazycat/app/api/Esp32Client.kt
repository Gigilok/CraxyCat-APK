package com.crazycat.app.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object Esp32Client {
    private const val BASE_URL = "http://192.168.4.1"
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun getCapturedSignal(): List<Int>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$BASE_URL/api/cc1101/captured").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val json = JSONObject(response.body?.string() ?: "")
                val timingsArray = json.getJSONArray("timings")
                (0 until timingsArray.length()).map { timingsArray.getInt(it) }
            }
        } catch (e: Exception) { null }
    }

    suspend fun transmitSignal(frequency: Long, timings: List<Int>): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("frequency", frequency)
                val arr = JSONArray()
                timings.forEach { arr.put(it) }
                put("timings", arr)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url("$BASE_URL/api/cc1101/transmit").post(body).build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) { false }
    }

    suspend fun startDeauth(bssid: String, channel: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$BASE_URL/api/wifi/deauth?bssid=$bssid&ch=$channel").build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) { false }
    }
    
    suspend fun downloadPcap(): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$BASE_URL/api/wifi/handshake/download").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.bytes()
            }
        } catch (e: Exception) { null }
    }
}
