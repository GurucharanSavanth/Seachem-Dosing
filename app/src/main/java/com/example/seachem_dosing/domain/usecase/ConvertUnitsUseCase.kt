package com.example.seachem_dosing.domain.usecase

import com.example.seachem_dosing.domain.model.VolumeSettings
import com.example.seachem_dosing.domain.model.WaterParameters
import com.example.seachem_dosing.logic.Calculations

/**
 * Pure unit conversion logic — volume, hardness, dimensions.
 * Stateless. No repository dependency.
 *
 * Wraps [Calculations] static functions in typed enums so the rest of the
 * codebase doesn't pass raw `"L"` / `"US"` / `"cm"` strings around.
 */
class ConvertUnitsUseCase {

    fun toLitres(volume: Double, unit: VolumeSettings.VolumeUnit): Double =
        Calculations.toLitres(volume, unit.code)

    fun fromLitres(litres: Double, unit: VolumeSettings.VolumeUnit): Double =
        Calculations.fromLitres(litres, unit.code)

    fun dimensionsToLitres(
        length: Double,
        breadth: Double,
        height: Double,
        unit: VolumeSettings.DimensionUnit
    ): Double = Calculations.dimensionsToLitres(length, breadth, height, unit.code)

    fun ppmToDh(ppm: Double): Double = Calculations.ppmToDh(ppm)

    fun dhToPpm(dh: Double): Double = Calculations.dhToPpm(dh)

    fun convertHardness(
        value: Double,
        from: WaterParameters.HardnessUnit,
        to: WaterParameters.HardnessUnit
    ): Double = when {
        from == to -> value
        from == WaterParameters.HardnessUnit.PPM && to == WaterParameters.HardnessUnit.DH -> ppmToDh(value)
        from == WaterParameters.HardnessUnit.DH && to == WaterParameters.HardnessUnit.PPM -> dhToPpm(value)
        else -> value
    }

    fun effectiveVolumeLitres(settings: VolumeSettings): Double =
        when (settings.mode) {
            VolumeSettings.Mode.DIRECT ->
                toLitres(settings.volume, settings.volumeUnit)
            VolumeSettings.Mode.DIMENSIONS ->
                dimensionsToLitres(
                    settings.length,
                    settings.breadth,
                    settings.height,
                    settings.dimensionUnit
                )
        }
}
