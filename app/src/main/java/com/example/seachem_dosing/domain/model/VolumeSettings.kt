package com.example.seachem_dosing.domain.model

/**
 * Tank volume configuration.
 *
 * Two modes:
 * - [Mode.DIRECT] — user enters volume + unit (L, US, UK).
 * - [Mode.DIMENSIONS] — user enters length × breadth × height + unit (cm, in, ft);
 *   volume is derived.
 *
 * Defaults match v1.0 [com.example.seachem_dosing.ui.MainViewModel] initial state.
 */
data class VolumeSettings(
    val mode: Mode = Mode.DIRECT,
    val volume: Double = 10.0,
    val volumeUnit: VolumeUnit = VolumeUnit.US_GAL,
    val length: Double = 60.0,
    val breadth: Double = 30.0,
    val height: Double = 40.0,
    val dimensionUnit: DimensionUnit = DimensionUnit.CM
) {
    enum class Mode(val code: String) {
        DIRECT("direct"),
        DIMENSIONS("lbh");

        companion object {
            fun fromCode(code: String): Mode =
                entries.firstOrNull { it.code == code } ?: DIRECT
        }
    }

    enum class VolumeUnit(val code: String) {
        LITRE("L"),
        US_GAL("US"),
        UK_GAL("UK");

        companion object {
            fun fromCode(code: String): VolumeUnit =
                entries.firstOrNull { it.code == code } ?: US_GAL
        }
    }

    enum class DimensionUnit(val code: String) {
        CM("cm"),
        IN("in"),
        FT("ft");

        companion object {
            fun fromCode(code: String): DimensionUnit =
                entries.firstOrNull { it.code == code } ?: CM
        }
    }

    companion object {
        val DEFAULT = VolumeSettings()
    }
}
