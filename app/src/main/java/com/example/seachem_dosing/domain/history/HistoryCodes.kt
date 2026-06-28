package com.example.seachem_dosing.domain.history

/**
 * Closed persistence registries for the v2 history schema (ADR-011 §3/§4).
 *
 * **Storage rule:** entities persist the stable [StorageCoded.storageCode] **string** (never the
 * enum ordinal) in a `*_code` column. The enum is the domain interpretation of that string.
 *
 * **Unknown-code policy (forward-compatible):** [HistoryEventType.fromCode] etc. return `null` for an
 * unrecognized code — the raw string stays in the entity column, so a record written by a newer app
 * version is preserved, never silently mapped to an unrelated known value. Callers requiring a known
 * code (writing a NEW record) use `fromCodeOrThrow`, which raises a typed error.
 */
interface StorageCoded {
    val storageCode: String
}

/** Builds a code→entry map and fails fast at class-init on any duplicate storage code. */
private inline fun <reified T> codeMap(): Map<String, T> where T : Enum<T>, T : StorageCoded {
    val all = enumValues<T>()
    val map = all.associateBy { it.storageCode }
    require(map.size == all.size) { "duplicate storageCode in ${T::class.simpleName}" }
    return map
}

private inline fun <reified T> lookup(map: Map<String, T>, code: String): T =
    map[code] ?: throw IllegalArgumentException("unknown ${T::class.simpleName} code: '$code'")

/**
 * Kind of history event (ADR-011 §4). Calculation-only events are not persisted in the timeline.
 * Legacy dose rows split by the v1 `administered` flag (preserved structurally, not in notes);
 * neither legacy type is the modern [DOSE_ADMINISTERED].
 */
enum class HistoryEventType(override val storageCode: String) : StorageCoded {
    LEGACY_DOSE_CALCULATION("legacy_dose_calculation"),
    LEGACY_DOSE_ADMINISTERED("legacy_dose_administered"),
    LEGACY_PARAMETER_RECORD("legacy_parameter_record"),
    LEGACY_PARAMETER_SNAPSHOT_EMPTY("legacy_parameter_snapshot_empty"),
    DOSE_ADMINISTERED("dose_administered"),
    WATER_PARAMETER_RECORDED("water_parameter_recorded"),
    CORRECTION("correction"),
    VOID("void"),
    ;

    companion object {
        private val BY_CODE = codeMap<HistoryEventType>()
        fun fromCode(code: String): HistoryEventType? = BY_CODE[code]
        fun fromCodeOrThrow(code: String): HistoryEventType = lookup(BY_CODE, code)
    }
}

/** Precision provenance of a stored numeric value (ADR-011 §4/§6). */
enum class PrecisionStatus(override val storageCode: String) : StorageCoded {
    NEW_EXACT_RECORD("new_exact"),
    LEGACY_BINARY64_APPROXIMATION("legacy_binary64_approx"),
    UNKNOWN_PRECISION("unknown"),
    ;

    companion object {
        private val BY_CODE = codeMap<PrecisionStatus>()
        fun fromCode(code: String): PrecisionStatus? = BY_CODE[code]
        fun fromCodeOrThrow(code: String): PrecisionStatus = lookup(BY_CODE, code)
    }
}

/** Physical dimension a [UnitCode] measures — units are only interconvertible within a dimension. */
enum class UnitDimension { VOLUME, MASS, MASS_CONCENTRATION, ALKALINITY, SALINITY, TEMPERATURE, PH, HARDNESS, UNKNOWN }

/**
 * Dimensional unit codes (ADR-011 §3). Stored as [storageCode]; a unit is inferred from nothing.
 *
 * [requiresMeasureDefinition] units cannot stand for an exact quantity alone — they reference an
 * immutable [MeasureDefinition] (id stored in a `*_measure_definition_id` column):
 *  - `MANUFACTURER_SCOOP` / `MANUFACTURER_CAPFUL` need the product-specific definition;
 *  - `USER_CALIBRATED_SPOON` needs the user's calibration definition;
 *  - `LEGACY_ENGINE_*` reference the historical engine definitions ([LegacyMeasureDefinitions]).
 *
 * Future-use codes (`US_TEASPOON`, `METRIC_TEASPOON`, `MANUFACTURER_*`) are retained but are NOT
 * used for v1 migration (the v1 teaspoon/tablespoon were product-specific mass measures — ADR-011).
 */
enum class UnitCode(
    override val storageCode: String,
    val dimension: UnitDimension,
    val requiresMeasureDefinition: Boolean = false,
) : StorageCoded {
    MILLILITER("ml", UnitDimension.VOLUME),
    LITER("l", UnitDimension.VOLUME),
    MICROLITER("ul", UnitDimension.VOLUME),
    US_GALLON("us_gal", UnitDimension.VOLUME),
    IMPERIAL_GALLON("imp_gal", UnitDimension.VOLUME),
    US_TEASPOON("tsp_us", UnitDimension.VOLUME),
    METRIC_TEASPOON("tsp_metric", UnitDimension.VOLUME),
    MANUFACTURER_SCOOP("scoop_mfr", UnitDimension.VOLUME, requiresMeasureDefinition = true),
    MANUFACTURER_CAPFUL("capful_mfr", UnitDimension.VOLUME, requiresMeasureDefinition = true),
    USER_CALIBRATED_SPOON("spoon_user", UnitDimension.VOLUME, requiresMeasureDefinition = true),
    GRAM("g", UnitDimension.MASS),
    MILLIGRAM("mg", UnitDimension.MASS),
    PPM_MG_PER_L("ppm_mg_per_l", UnitDimension.MASS_CONCENTRATION),
    DKH("dkh", UnitDimension.ALKALINITY),
    MEQ_PER_L("meq_per_l", UnitDimension.ALKALINITY),
    PPT("ppt", UnitDimension.SALINITY),
    CELSIUS("celsius", UnitDimension.TEMPERATURE),
    PH_VALUE("ph_value", UnitDimension.PH),
    DEGREE_GH("dgh", UnitDimension.HARDNESS),
    // legacy engine-defined measures (migration only) — reference LegacyMeasureDefinitions
    LEGACY_ENGINE_MASS_MEASURE("legacy_engine_mass", UnitDimension.MASS, requiresMeasureDefinition = true),
    LEGACY_ENGINE_VOLUME_MEASURE("legacy_engine_volume", UnitDimension.VOLUME, requiresMeasureDefinition = true),
    LEGACY_UNSPECIFIED("legacy_unspecified", UnitDimension.UNKNOWN),
    ;

    companion object {
        private val BY_CODE = codeMap<UnitCode>()
        fun fromCode(code: String): UnitCode? = BY_CODE[code]
        fun fromCodeOrThrow(code: String): UnitCode = lookup(BY_CODE, code)
    }
}

/** Route of administration for a dose event (decision-support metadata, not a clinical claim). */
enum class AdministrationRoute(override val storageCode: String) : StorageCoded {
    WATER_COLUMN("water_column"),
    MEDICATED_FOOD("medicated_food"),
    BATH_DIP("bath_dip"),
    QUARANTINE_ONLY("quarantine_only"),
    EXTERNAL_ONLY("external_only"),
    UNKNOWN("unknown"),
    ;

    companion object {
        private val BY_CODE = codeMap<AdministrationRoute>()
        fun fromCode(code: String): AdministrationRoute? = BY_CODE[code]
        fun fromCodeOrThrow(code: String): AdministrationRoute = lookup(BY_CODE, code)
    }
}

/** Validation provenance of a recorded water-parameter reading. */
enum class ParameterValidationStatus(override val storageCode: String) : StorageCoded {
    VALIDATED("validated"),
    OUT_OF_TYPICAL_RANGE("out_of_typical_range"),
    UNVALIDATED("unvalidated"),
    ;

    companion object {
        private val BY_CODE = codeMap<ParameterValidationStatus>()
        fun fromCode(code: String): ParameterValidationStatus? = BY_CODE[code]
        fun fromCodeOrThrow(code: String): ParameterValidationStatus = lookup(BY_CODE, code)
    }
}

/** Water-parameter kind recorded by a parameter event (mirrors the v1 parameter_log columns). */
enum class ParameterType(override val storageCode: String) : StorageCoded {
    AMMONIA("ammonia"),
    NITRITE("nitrite"),
    NITRATE("nitrate"),
    GH("gh"),
    KH("kh"),
    PH("ph"),
    TEMPERATURE("temperature"),
    SALINITY("salinity"),
    ALKALINITY("alkalinity"),
    CALCIUM("calcium"),
    MAGNESIUM("magnesium"),
    PHOSPHATE("phosphate"),
    DISSOLVED_OXYGEN("dissolved_oxygen"),
    POTASSIUM("potassium"),
    IRON("iron"),
    STRONTIUM("strontium"),
    IODIDE("iodide"),
    ;

    companion object {
        private val BY_CODE = codeMap<ParameterType>()
        fun fromCode(code: String): ParameterType? = BY_CODE[code]
        fun fromCodeOrThrow(code: String): ParameterType = lookup(BY_CODE, code)
    }
}
