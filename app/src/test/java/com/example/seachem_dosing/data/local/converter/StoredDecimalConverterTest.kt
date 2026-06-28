package com.example.seachem_dosing.data.local.converter

import com.example.seachem_dosing.core.numerics.StoredDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/** ADR-011 §2 — Room converter is a lossless StoredDecimal <-> canonical String mapping. */
class StoredDecimalConverterTest {

    private val converter = StoredDecimalConverter()

    @Test fun toDb_returnsCanonicalString() {
        assertEquals("1.50", converter.toDb(StoredDecimal.from(BigDecimal("1.50"))))
    }

    @Test fun fromDb_parsesCanonicalString() {
        assertEquals("0.000000000001", converter.fromDb("0.000000000001")!!.canonicalValue)
    }

    @Test fun roundTrip_isLossless_andPreservesScale() {
        for (s in listOf("0", "1", "1.0", "1.00", "-3.5", "0.123456789012345678", "553.4404496903")) {
            val sd = StoredDecimal.parseNewValue(s)
            assertEquals(sd, converter.fromDb(converter.toDb(sd)))
            assertEquals(s, converter.toDb(converter.fromDb(s)))
        }
    }

    /** Test 7 — legacy-read invariant: high-scale legacy double survives write+read, numerically preserved. */
    @Test fun legacy_highScale_writtenAndReadByConverter_numericallyPreserved() {
        val legacy = StoredDecimal.fromLegacyBinary64(1e-25)
        assertTrue(legacy.toBigDecimal().scale() > StoredDecimal.NEW_VALUE_MAX_SCALE)
        val db = converter.toDb(legacy)
        val back = converter.fromDb(db)!!
        assertEquals(legacy, back)                                              // exact canonical round-trip
        assertEquals(0, back.toBigDecimal().compareTo(BigDecimal.valueOf(1e-25))) // numerically preserved
    }

    /** Test 8 — converter reads every value emitted by both new-value and legacy factories. */
    @Test fun converter_readsEveryValueFromBothFactories() {
        val values = listOf(
            StoredDecimal.from(BigDecimal("1.50")),
            StoredDecimal.parseNewValue("553.4404496903"),
            StoredDecimal.parseNewValue("0"),
            StoredDecimal.fromLegacyBinary64(0.1),
            StoredDecimal.fromLegacyBinary64(1e-25),
            StoredDecimal.fromLegacyBinary64(-9999.999),
        )
        for (v in values) assertEquals(v, converter.fromDb(converter.toDb(v)))
    }

    @Test fun null_roundTrips() {
        assertNull(converter.toDb(null))
        assertNull(converter.fromDb(null))
    }

    @Test fun fromDb_malformedStoredValue_throws() {
        for (s in listOf("1,5", "1e3", " 1.0 ", "abc", "1.")) {
            assertThrows("expected reject: '$s'", IllegalArgumentException::class.java) {
                converter.fromDb(s)
            }
        }
    }

    /** Test 12 — no rounding in either direction, even for a long-scale value. */
    @Test fun noRounding_eitherDirection() {
        val longScale = "0.123456789012345678"
        assertEquals(longScale, converter.toDb(StoredDecimal.parseNewValue(longScale)))
        assertEquals(18, converter.fromDb(longScale)!!.toBigDecimal().scale())
    }
}
