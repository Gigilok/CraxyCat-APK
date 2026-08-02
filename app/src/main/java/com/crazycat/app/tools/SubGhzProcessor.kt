package com.crazycat.app.tools

import com.crazycat.app.crypto.KeeloqEngine
import com.crazycat.app.models.ProtocolDatabase
import com.crazycat.app.models.SubGhzProtocol

object SubGhzProcessor {

    /**
     * Converte lista de timings em c贸digo de bits.
     * Cada PAR de timings (high,low) = 1 bit.
     * Se high > baseTiming: bit=1 (PWM: longo-curto)
     * Se high <= baseTiming: bit=0 (PWM: curto-longo)
     */
    fun timingsToBits(timings: List<Int>, baseTiming: Int): Long {
        var code: Long = 0
        var bitCount = 0
        for (i in timings.indices step 2) {
            if (i + 1 >= timings.size) break
            code = code shl 1
            // PWM: bit 1 = (longo, curto), bit 0 = (curto, longo)
            if (timings[i] > baseTiming) code = code or 1
            bitCount++
            if (bitCount >= 64) break  // Limite seguro
        }
        return code
    }

    /**
     * Converte c贸digo de bits em lista de timings.
     * Gera exatamente `bitCount` bits usando PWM encoding.
     * bit=1: (base*2, base)  bit=0: (base, base*2)
     */
    fun bitsToTimings(code: Long, baseTiming: Int, bitCount: Int): List<Int> {
        val timings = mutableListOf<Int>()
        // Adiciona preamble (sync pulse): base*10 + base
        timings.add(baseTiming * 10)
        timings.add(baseTiming)
        // Bits do c贸digo (MSB first)
        for (i in bitCount - 1 downTo 0) {
            val bit = ((code shr i) and 1).toInt()
            if (bit == 1) {
                timings.add(baseTiming * 2); timings.add(baseTiming)
            } else {
                timings.add(baseTiming); timings.add(baseTiming * 2)
            }
        }
        // Adiciona footer (pausa entre repeti莽玫es)
        timings.add(baseTiming * 4)
        return timings
    }

    /**
     * Fluxo de clonagem avan莽ada com rolling code.
     * Retorna (protocolo, novos_timings) ou null se c贸digo fixo.
     */
    fun processRollingCode(timings: List<Int>): Pair<SubGhzProtocol, List<Int>>? {
        if (timings.size < 4) return null

        val protocol = ProtocolDatabase.findProtocolBySignal(timings) ?: return null

        // Calcula n煤mero de bits do sinal capturado
        val capturedBitCount = timings.size / 2
        val code = timingsToBits(timings, protocol.baseTiming)

        if (protocol.manufacturerKey != 0L) {
            // Rolling code (Keeloq) - precisa de 32 bits
            if (capturedBitCount < 32) {
                // Sinal muito curto para rolling code, faz replay simples
                return Pair(protocol, timings)
            }
            // Pega s贸 os 32 bits do c贸digo (ignora prefixo/sync se houver)
            val code32 = (code and 0xFFFFFFFFL).toInt()
            val decrypted = KeeloqEngine.decryptRollingCode(code32, protocol.manufacturerKey)
            val nextCode = KeeloqEngine.generateNextValidCode(decrypted, protocol.manufacturerKey)
            // Gera timings com 32 bits (tamanho padr茫o Keeloq)
            val newTimings = bitsToTimings(nextCode.toLong() and 0xFFFFFFFFL, protocol.baseTiming, 32)
            return Pair(protocol, newTimings)
        } else {
            // Protocolo de c贸digo fixo, s贸 repete os timings originais
            return Pair(protocol, timings)
        }
    }
}
