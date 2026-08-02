package com.crazycat.app.models

data class SubGhzProtocol(
    val name: String,
    val frequency: Long,
    val baseTiming: Int,
    val manufacturerKey: Long,
    val bitCount: Int,           // Número de bits do código (12, 32, 64, etc.)
    val prefixMasks: List<Int>   // Padrões de identificação (não usado atualmente)
)

object ProtocolDatabase {
    val protocols = listOf(
        // CAME TOP-432: 12 bits fixos, 433.92MHz, base 320μs
        SubGhzProtocol("CAME TOP-432", 433920000, 320, 0L, 12, listOf(0x0100, 0x0200)),
        // NICE FLO-R: 12 bits com rolling, 433.92MHz, base 320μs
        SubGhzProtocol("NICE FLO-R", 433920000, 320, 0x5252525252525252, 12, listOf(0x0001, 0x0002)),
        // FAAC SLH: 64 bits com rolling, 868.35MHz, base 250μs
        SubGhzProtocol("FAAC SLH", 868350000, 250, 0x4343434343434343, 64, listOf(0x0010, 0x0020)),
        // BFT MITTO: 64 bits com rolling, 433.92MHz, base 400μs
        SubGhzProtocol("BFT MITTO", 433920000, 400, 0x6262626262626262, 64, listOf(0x0100, 0x0200)),
        // Gate TX (Fixed): 24 bits fixos, 433.92MHz, base 320μs
        SubGhzProtocol("Gate TX (Fixed)", 433920000, 320, 0L, 24, listOf(0x0000))
    )

    /**
     * Encontra o protocolo mais provável baseado em:
     * 1. Número de bits do sinal (conta pares de timings)
     * 2. Timing médio (base timing aproximado)
     *
     * Retorna null se timings for vazio ou muito curto.
     */
    fun findProtocolBySignal(timings: List<Int>): SubGhzProtocol? {
        if (timings.size < 4) return null

        // Conta bits (cada par de timings = 1 bit)
        val bitCount = timings.size / 2

        // Calcula timing médio dos pulsos (ignora outliers)
        val sortedTimings = timings.sorted()
        val median = sortedTimings[sortedTimings.size / 2]

        // Primeiro tenta match exato de bitCount + timing aproximado
        val exactMatch = protocols.find { proto ->
            kotlin.math.abs(proto.bitCount - bitCount) <= 2 &&
            kotlin.math.abs(proto.baseTiming - median) < 100
        }
        if (exactMatch != null) return exactMatch

        // Fallback: protocolo com timing mais próximo (código fixo)
        return protocols.filter { it.manufacturerKey == 0L }
            .minByOrNull { kotlin.math.abs(it.baseTiming - median) }
    }
}
