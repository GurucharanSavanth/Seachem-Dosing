package com.example.seachem_dosing.data.local.converter

import com.example.seachem_dosing.core.numerics.StoredDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal

/** ADR-011 §2 — Room converter is a lossless StoredDecimal <-> canonical String mapping. */
class StoredDecimalConverterTest {

    private val converter = StoredDecimalConverter()

    @Test fun toDb_returnsCanonicalString() {
        assertEquals("1.50", converter.toDb(StoredDecimal.from(BigDecimal("1.50"))))
    }

    @Test fun fromDb_parsesCanonicalString() {
        val v = converter.fromDb("0.000000000001")!!
        assertEquals("0.000000000001", v.canonicalValue)
    }

    @Test fun roundTrip_isLossless_andPreservesScale() {
        for (s in listOf("0", "1", "1.0", "1.00", "-3.5", "0.123456789012")) {
            val sd = StoredDecimal.parse(s)
            assertEquals(sd, converter.fromDb(converter.toDb(sd)))
            assertEquals(s, converter.toDb(converter.fromDb(s)))
        }
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
}
