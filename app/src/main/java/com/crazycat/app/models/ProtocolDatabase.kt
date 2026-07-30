package com.crazycat.app.models

data class SubGhzProtocol(
    val name: String,
    val frequency: Long,
    val baseTiming: Int,
    val manufacturerKey: Long,
    val prefixMasks: List<Int>
)

object ProtocolDatabase {
    val protocols = listOf(
        SubGhzProtocol("CAME TOP-432", 433920000, 320, 0x0000000000000000, listOf(0x0100, 0x0200, 0x0400, 0x0800)),
        SubGhzProtocol("NICE FLO-R", 433920000, 320, 0x5252525252525252, listOf(0x0001, 0x0002, 0x0004, 0x0008)),
        SubGhzProtocol("FAAC SLH", 868350000, 250, 0x4343434343434343, listOf(0x0010, 0x0020, 0x0040, 0x0080)),
        SubGhzProtocol("BFT MITTO", 433920000, 400, 0x6262626262626262, listOf(0x0100, 0x0200, 0x0400, 0x0800)),
        SubGhzProtocol("Gate TX (Fixed)", 433920000, 320, 0, listOf(0x0000))
    )

    fun findProtocolBySignal(timings: List<Int>): SubGhzProtocol? {
        if (timings.isEmpty()) return null
        val avgTiming = timings.average().toInt()
        return protocols.minByOrNull { kotlin.math.abs(it.baseTiming - avgTiming) }
    }
}
