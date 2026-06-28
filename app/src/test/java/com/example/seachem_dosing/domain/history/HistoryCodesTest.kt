package com.example.seachem_dosing.domain.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** ADR-011 §3/§4 — closed persistence registries: round-trip, uniqueness, unknown handling. */
class HistoryCodesTest {

    private fun <T> roundTrips(entries: Array<T>, code: (T) -> String, from: (String) -> T?) where T : Enum<T> {
        for (e in entries) assertEquals(e, from(code(e)))
        val codes = entries.map(code)
        assertEquals("storage codes must be unique", codes.size, codes.toSet().size)
        // ordinal independence: storage code is never just the ordinal
        for (e in entries) assertNotEquals(e.ordinal.toString(), code(e))
    }

    @Test fun historyEventType_roundTrips_unique_ordinalIndependent() {
        roundTrips(HistoryEventType.entries.toTypedArray(), { it.storageCode }, HistoryEventType::fromCode)
    }

    @Test fun precisionStatus_roundTrips() {
        roundTrips(PrecisionStatus.entries.toTypedArray(), { it.storageCode }, PrecisionStatus::fromCode)
    }

    @Test fun unitCode_roundTrips() {
        roundTrips(UnitCode.entries.toTypedArray(), { it.storageCode }, UnitCode::fromCode)
    }

    @Test fun administrationRoute_roundTrips() {
        roundTrips(AdministrationRoute.entries.toTypedArray(), { it.storageCode }, AdministrationRoute::fromCode)
    }

    @Test fun parameterValidationStatus_roundTrips() {
        roundTrips(ParameterValidationStatus.entries.toTypedArray(), { it.storageCode }, ParameterValidationStatus::fromCode)
    }

    @Test fun fromCode_unknown_returnsNull_preservingRawCode() {
        assertNull(HistoryEventType.fromCode("does_not_exist"))
        assertNull(UnitCode.fromCode("furlongs"))
        assertNull(PrecisionStatus.fromCode(""))
    }

    @Test fun fromCodeOrThrow_unknown_throws() {
        assertThrows(IllegalArgumentException::class.java) { HistoryEventType.fromCodeOrThrow("nope") }
        assertThrows(IllegalArgumentException::class.java) { UnitCode.fromCodeOrThrow("nope") }
        assertThrows(IllegalArgumentException::class.java) { AdministrationRoute.fromCodeOrThrow("nope") }
    }

    @Test fun codes_areCaseSensitive() {
        // codes are lowercase; an upper-cased name must NOT resolve (no silent mapping)
        assertNull(HistoryEventType.fromCode("DOSE_ADMINISTERED"))
        assertNull(UnitCode.fromCode("ML"))
    }

    @Test fun unitCode_dimensionsAndCalibration() {
        assertEquals(UnitDimension.VOLUME, UnitCode.MILLILITER.dimension)
        assertEquals(UnitDimension.MASS, UnitCode.GRAM.dimension)
        assertEquals(UnitDimension.MASS_CONCENTRATION, UnitCode.PPM_MG_PER_L.dimension)
        assertEquals(UnitDimension.ALKALINITY, UnitCode.DKH.dimension)
        assertEquals(UnitDimension.SALINITY, UnitCode.PPT.dimension)
        // only scoop/calibrated spoon require calibration
        val needCal = UnitCode.entries.filter { it.requiresCalibration }.toSet()
        assertEquals(setOf(UnitCode.MANUFACTURER_SCOOP, UnitCode.USER_CALIBRATED_SPOON), needCal)
    }
}
