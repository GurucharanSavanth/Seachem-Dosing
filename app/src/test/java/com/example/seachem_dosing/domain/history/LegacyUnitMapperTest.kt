package com.example.seachem_dosing.domain.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** ADR-011 legacy unit mapping — proven engine measures, raw preservation, no volume-spoon guessing. */
class LegacyUnitMapperTest {

    @Test fun g_mapsToGram() {
        val m = LegacyUnitMapper.map("g")
        assertEquals(UnitCode.GRAM, m.unitCode); assertNull(m.measureDefinitionId)
    }

    @Test fun mL_mapsToMilliliter() {
        val m = LegacyUnitMapper.map("mL")
        assertEquals(UnitCode.MILLILITER, m.unitCode); assertNull(m.measureDefinitionId)
    }

    @Test fun caps_mapsToEngineVolume_withCapfulDefinition() {
        val m = LegacyUnitMapper.map("caps")
        assertEquals(UnitCode.LEGACY_ENGINE_VOLUME_MEASURE, m.unitCode)
        assertEquals(LegacyMeasureDefinitions.CAPFUL_5ML, m.measureDefinitionId)
    }

    @Test fun tbsp_mapsToEngineMass_withTablespoonDefinition() {
        val m = LegacyUnitMapper.map("tbsp")
        assertEquals(UnitCode.LEGACY_ENGINE_MASS_MEASURE, m.unitCode)
        assertEquals(LegacyMeasureDefinitions.TABLESPOON_16G, m.measureDefinitionId)
    }

    @Test fun tsp_mapsToLegacyUnspecified_rawPreserved_noConversion() {
        val m = LegacyUnitMapper.map("tsp")
        assertEquals(UnitCode.LEGACY_UNSPECIFIED, m.unitCode)
        assertNull(m.measureDefinitionId)
        assertEquals("tsp", m.originalText)
    }

    @Test fun unknownUnit_mapsToLegacyUnspecified_rawPreserved() {
        val m = LegacyUnitMapper.map("globules")
        assertEquals(UnitCode.LEGACY_UNSPECIFIED, m.unitCode)
        assertEquals("globules", m.originalText)
    }

    @Test fun tsp_and_tbsp_neverMapToVolumeSpoonUnits() {
        for (u in listOf("tsp", "tbsp")) {
            val c = LegacyUnitMapper.map(u).unitCode
            assertNotEquals(UnitCode.US_TEASPOON, c)
            assertNotEquals(UnitCode.METRIC_TEASPOON, c)
        }
    }
}
