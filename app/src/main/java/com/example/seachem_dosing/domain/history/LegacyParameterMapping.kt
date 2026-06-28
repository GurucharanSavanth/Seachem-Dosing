package com.example.seachem_dosing.domain.history

/**
 * v1 `parameter_log` column → v2 unit (ADR-011 17-field mapping). The v1 column name **is** the
 * deterministic source identity: it equals [ParameterType.storageCode], so fan-out is reversible.
 *
 * Defensive: the v1 writer was orphaned, so values migrate under the app's default unit convention
 * (`MainViewModel` defaults) — nutrients as ppm (mg/L), GH as dGH, KH/alkalinity as dKH, temperature
 * as Celsius, pH as pH-value, salinity as ppt. The `when` is exhaustive over all 17 [ParameterType]s.
 */
object LegacyParameterMapping {
    fun unitFor(type: ParameterType): UnitCode = when (type) {
        ParameterType.PH -> UnitCode.PH_VALUE
        ParameterType.TEMPERATURE -> UnitCode.CELSIUS
        ParameterType.GH -> UnitCode.DEGREE_GH
        ParameterType.KH, ParameterType.ALKALINITY -> UnitCode.DKH
        ParameterType.SALINITY -> UnitCode.PPT
        ParameterType.AMMONIA, ParameterType.NITRITE, ParameterType.NITRATE, ParameterType.PHOSPHATE,
        ParameterType.CALCIUM, ParameterType.MAGNESIUM, ParameterType.POTASSIUM, ParameterType.IRON,
        ParameterType.STRONTIUM, ParameterType.IODIDE, ParameterType.DISSOLVED_OXYGEN,
        -> UnitCode.PPM_MG_PER_L
    }
}
