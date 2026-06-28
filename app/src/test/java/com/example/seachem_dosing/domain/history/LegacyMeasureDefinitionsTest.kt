package com.example.seachem_dosing.domain.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ADR-011 — only the two engine-proven measure definitions exist; no teaspoon; honest provenance. */
class LegacyMeasureDefinitionsTest {

    @Test fun capful_is5mL_volume_engineProvenance_notManufacturerVerified() {
        val d = LegacyMeasureDefinitions.get(LegacyMeasureDefinitions.CAPFUL_5ML)!!
        assertEquals(MeasureCategory.VOLUME, d.category)
        assertEquals("5", d.canonicalValue.canonicalValue)
        assertEquals(UnitCode.MILLILITER, d.canonicalUnit)
        assertEquals(MeasureProvenance.LEGACY_APPLICATION_ENGINE, d.provenance)
        assertFalse(d.manufacturerVerified)
    }

    @Test fun tablespoon_is16g_mass() {
        val d = LegacyMeasureDefinitions.get(LegacyMeasureDefinitions.TABLESPOON_16G)!!
        assertEquals(MeasureCategory.MASS, d.category)
        assertEquals("16", d.canonicalValue.canonicalValue)
        assertEquals(UnitCode.GRAM, d.canonicalUnit)
        assertFalse(d.manufacturerVerified)
    }

    @Test fun unknownDefinitionId_returnsNull() {
        assertNull(LegacyMeasureDefinitions.get("legacy-engine:teaspoon:7g"))
    }

    @Test fun onlyTwoDefinitions_noTeaspoonDefinition() {
        assertEquals(2, LegacyMeasureDefinitions.all().size)
        assertTrue(LegacyMeasureDefinitions.all().none { it.id.contains("teaspoon") })
    }
}
