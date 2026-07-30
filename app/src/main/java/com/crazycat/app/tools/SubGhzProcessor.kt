package com.crazycat.app.tools

import com.crazycat.app.crypto.KeeloqEngine
import com.crazycat.app.models.ProtocolDatabase
import com.crazycat.app.models.SubGhzProtocol

object SubGhzProcessor {

    fun timingsToBits(timings: List<Int>, baseTiming: Int): Int {
        var code = 0
        for (i in timings.indices step 2) {
            code = code shl 1
            if (timings[i] > baseTiming) code = code or 1
        }
        return code
    }

    fun bitsToTimings(code: Int, baseTiming: Int): List<Int> {
        val timings = mutableListOf<Int>()
        for (i in 31 downTo 0) {
            val bit = (code shr i) and 1
            if (bit == 1) {
                timings.add(baseTiming * 2); timings.add(baseTiming)
            } else {
                timings.add(baseTiming); timings.add(baseTiming * 2)
            }
        }
        return timings
    }

    // Fluxo de clonagem avançada
    fun processRollingCode(timings: List<Int>): Pair<SubGhzProtocol, List<Int>>? {
        val protocol = ProtocolDatabase.findProtocolBySignal(timings) ?: return null
        val capturedCode = timingsToBits(timings, protocol.baseTiming)

        if (protocol.manufacturerKey != 0L) {
            val decrypted = KeeloqEngine.decryptRollingCode(capturedCode, protocol.manufacturerKey)
            val nextCode = KeeloqEngine.generateNextValidCode(decrypted, protocol.manufacturerKey)
            val newTimings = bitsToTimings(nextCode, protocol.baseTiming)
            return Pair(protocol, newTimings)
        } else {
            // Protocolo de código fixo, só repete
            return Pair(protocol, timings)
        }
    }
}
