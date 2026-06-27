package com.example.seachem_dosing.domain.model

/**
 * Immutable snapshot of water-quality parameters.
 *
 * Replaces the 18 individual MutableLiveData fields in [com.example.seachem_dosing.ui.MainViewModel]
 * during Phase 4.7 StateFlow migration. All fields nullable so partial readings
 * are first-class — users may record only ammonia + nitrite during cycling, or
 * only alkalinity for reef checks.
 */
data class WaterParameters(
    val ammonia: Double? = null,
    val nitrite: Double? = null,
    val nitrate: Double? = null,
    val gh: Double? = null,
    val kh: Double? = null,
    val ph: Double? = null,
    val temperature: Double? = null,
    val salinity: Double? = null,
    val alkalinity: Double? = null,
    val calcium: Double? = null,
    val magnesium: Double? = null,
    val phosphate: Double? = null,
    val dissolvedOxygen: Double? = null,
    val potassium: Double? = null,
    val iron: Double? = null,
    val strontium: Double? = null,
    val iodide: Double? = null,
    val ghUnit: HardnessUnit = HardnessUnit.DH,
    val khUnit: HardnessUnit = HardnessUnit.DH
) {
    enum class HardnessUnit(val code: String) {
        DH("dh"),
        PPM("ppm");

        companion object {
            fun fromCode(code: String): HardnessUnit =
                entries.firstOrNull { it.code == code } ?: DH
        }
    }

    companion object {
        val EMPTY = WaterParameters()
    }
}
