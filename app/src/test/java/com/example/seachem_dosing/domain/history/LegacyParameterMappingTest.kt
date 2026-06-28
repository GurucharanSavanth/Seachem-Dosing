package com.example.seachem_dosing.domain.history

import org.junit.Assert.assertEquals
import org.junit.Test

/** ADR-011 17-field parameter mapping — every v1 parameter column has a unit; pH is PH_VALUE. */
class LegacyParameterMappingTest {

    @Test fun allSeventeenParameterTypes_resolveToAUnit() {
        assertEquals(17, ParameterType.entries.size)
        for (t in ParameterType.entries) {
            // unitFor is exhaustive; this asserts a stable mapping exists for every column.
            LegacyParameterMapping.unitFor(t)
        }
    }

    @Test fun keyMappings_areCorrect() {
        assertEquals(UnitCode.CELSIUS, LegacyParameterMapping.unitFor(ParameterType.TEMPERATURE))
        assertEquals(UnitCode.PH_VALUE, LegacyParameterMapping.unitFor(ParameterType.PH))
        assertEquals(UnitCode.DEGREE_GH, LegacyParameterMapping.unitFor(ParameterType.GH))
        assertEquals(UnitCode.DKH, LegacyParameterMapping.unitFor(ParameterType.KH))
        assertEquals(UnitCode.DKH, LegacyParameterMapping.unitFor(ParameterType.ALKALINITY))
        assertEquals(UnitCode.PPT, LegacyParameterMapping.unitFor(ParameterType.SALINITY))
        assertEquals(UnitCode.PPM_MG_PER_L, LegacyParameterMapping.unitFor(ParameterType.NITRATE))
        assertEquals(UnitCode.PPM_MG_PER_L, LegacyParameterMapping.unitFor(ParameterType.DISSOLVED_OXYGEN))
    }

    @Test fun ph_usesPhValueDimension_notDimensionlessPlaceholder() {
        assertEquals(UnitDimension.PH, LegacyParameterMapping.unitFor(ParameterType.PH).dimension)
    }
}
