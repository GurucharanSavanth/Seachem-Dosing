package com.example.seachem_dosing.domain.history

/**
 * Closed persistence registries for the v2 history schema (ADR-011 §4).
 *
 * **Storage rule:** entities persist the stable [StorageCoded.storageCode] **string** (never the
 * enum ordinal) in a `*_code` column. The enum is the domain interpretation of that string.
 *
 * **Unknown-code policy (forward-compatible):** [fromCode] returns `null` for an unrecognized code
 * — the raw string stays in the entity column, so a record written by a newer app version is
 * preserved, never silently mapped to an unrelated known value. Callers that require a known code
 * (e.g. writing a NEW record) use [fromCodeOrThrow], which raises a typed error. Reads tolerate
 * unknowns; writes do not.
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

/** Kind of history event (ADR-011 §4). Calculation-only events are not persisted in the timeline. */
enum class HistoryEventType(override val storageCode: String) : StorageCoded {
    LEGACY_DOSE_RECORD("legacy_dose_record"),
    LEGACY_PARAMETER_RECORD("legacy_parameter_record"),
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
enum class UnitDimension { VOLUME, MASS, MASS_CONCENTRATION, ALKALINITY, SALINITY }

/**
 * Dimensional unit codes (ADR-011 §3). Stored as [storageCode]; a unit is inferred from nothing.
 *
 * [requiresCalibration] units cannot be used for an exact quantity by themselves:
 *  - `MANUFACTURER_SCOOP` needs the product-specific scoop-definition id (a companion column on the
 *    dose detail);
 *  - `USER_CALIBRATED_SPOON` needs the calibration volume + its unit. A label alone is insufficient.
 */
enum class UnitCode(
    override val storageCode: String,
    val dimension: UnitDimension,
    val requiresCalibration: Boolean = false,
) : StorageCoded {
    MILLILITER("ml", UnitDimension.VOLUME),
    LITER("l", UnitDimension.VOLUME),
    MICROLITER("ul", UnitDimension.VOLUME),
    US_GALLON("us_gal", UnitDimension.VOLUME),
    IMPERIAL_GALLON("imp_gal", UnitDimension.VOLUME),
    US_TEASPOON("tsp_us", UnitDimension.VOLUME),
    METRIC_TEASPOON("tsp_metric", UnitDimension.VOLUME),
    MANUFACTURER_SCOOP("scoop_mfr", UnitDimension.VOLUME, requiresCalibration = true),
    USER_CALIBRATED_SPOON("spoon_user", UnitDimension.VOLUME, requiresCalibration = true),
    GRAM("g", UnitDimension.MASS),
    MILLIGRAM("mg", UnitDimension.MASS),
    PPM_MG_PER_L("ppm_mg_per_l", UnitDimension.MASS_CONCENTRATION),
    DKH("dkh", UnitDimension.ALKALINITY),
    MEQ_PER_L("meq_per_l", UnitDimension.ALKALINITY),
    PPT("ppt", UnitDimension.SALINITY),
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
