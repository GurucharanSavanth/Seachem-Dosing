package com.example.seachem_dosing.core.numerics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/** ADR-011 §2 / Gate F — canonical precision-safe decimal contract. */
class StoredDecimalTest {

    // ---- canonicalization & round-trip ----

    @Test fun parse_roundTrips_exactCanonicalString() {
        for (s in listOf("0", "42", "-3.5", "1.0", "0.50", "100.0", "0.000000000001")) {
            assertEquals(s, StoredDecimal.parse(s).canonicalValue)
            assertEquals(0, StoredDecimal.parse(s).toBigDecimal().compareTo(BigDecimal(s)))
        }
    }

    @Test fun from_bigDecimal_canonicalizesViaToPlainString() {
        assertEquals("1.50", StoredDecimal.from(BigDecimal("1.50")).canonicalValue)
        // valueOf path is NOT used by from(); from() takes the BigDecimal exactly.
        assertEquals("100", StoredDecimal.from(BigDecimal("100")).canonicalValue)
    }

    @Test fun from_doesNotRound_preservesScale() {
        val v = StoredDecimal.from(BigDecimal("0.123456789012")) // scale 12
        assertEquals("0.123456789012", v.canonicalValue)
        assertEquals(12, v.toBigDecimal().scale())
    }

    // ---- scale identity vs numeric equality ----

    @Test fun scale_isPreserved_distinctCanonical_butNumericallyEqual() {
        val a = StoredDecimal.parse("1")
        val b = StoredDecimal.parse("1.0")
        val c = StoredDecimal.parse("1.00")
        assertFalse(a.canonicalValue == b.canonicalValue)
        assertFalse(b.canonicalValue == c.canonicalValue)
        assertTrue(a.numericallyEquals(b))
        assertTrue(b.numericallyEquals(c))
        assertTrue(a.numericallyEquals(c))
        // value-class structural equality == canonical (storage/audit identity)
        assertFalse(a == b)
        assertEquals(StoredDecimal.parse("1.0"), b)
    }

    // ---- limit boundaries ----

    @Test fun maxPrecision_boundary() {
        StoredDecimal.parse("9".repeat(StoredDecimal.MAX_PRECISION))              // 30 digits ok
        assertThrows(IllegalArgumentException::class.java) {
            StoredDecimal.parse("9".repeat(StoredDecimal.MAX_PRECISION + 1))      // 31 digits
        }
    }

    @Test fun maxScale_boundary() {
        StoredDecimal.parse("0." + "1".repeat(StoredDecimal.MAX_SCALE))           // scale 12 ok
        assertThrows(IllegalArgumentException::class.java) {
            StoredDecimal.parse("0." + "1".repeat(StoredDecimal.MAX_SCALE + 1))   // scale 13
        }
    }

    @Test fun overLength_rejected() {
        assertThrows(IllegalArgumentException::class.java) {
            StoredDecimal.parse("1".repeat(StoredDecimal.MAX_LENGTH + 1))
        }
    }

    // ---- rejections ----

    @Test fun rejects_malformed_exponent_locale_whitespace_nonCanonical() {
        val bad = listOf(
            "", "   ", " 1.0 ", "\t1", "1\n",            // blank / whitespace-padded
            "1e3", "1E3", "1.5e-2",                       // exponent
            "1,5", "1,000.5", "1 000",                    // locale / grouping / space
            "NaN", "Infinity", "-Infinity", "abc", "0x10",// non-numeric
            "1.2.3", "--1", "+1", "1.", ".5", "01", "00.5"// malformed / non-canonical
        )
        for (s in bad) {
            assertThrows("expected reject: '$s'", IllegalArgumentException::class.java) {
                StoredDecimal.parse(s)
            }
        }
    }

    @Test fun trimmedThenParse_succeeds() {
        assertEquals("1.0", StoredDecimal.parse(" 1.0 ".trim()).canonicalValue)
    }

    // ---- sign policy ----

    @Test fun sign_default_allowsNegative() {
        assertEquals("-2.5", StoredDecimal.parse("-2.5").canonicalValue)
    }

    @Test fun sign_nonNegative_rejectsNegative_allowsZero() {
        StoredDecimal.parse("0", StoredDecimalSign.NON_NEGATIVE)
        assertThrows(IllegalArgumentException::class.java) {
            StoredDecimal.parse("-0.1", StoredDecimalSign.NON_NEGATIVE)
        }
    }

    @Test fun sign_positive_rejectsZeroAndNegative() {
        StoredDecimal.parse("0.01", StoredDecimalSign.POSITIVE)
        for (s in listOf("0", "-1")) {
            assertThrows(IllegalArgumentException::class.java) {
                StoredDecimal.parse(s, StoredDecimalSign.POSITIVE)
            }
        }
    }

    // ---- legacy migration path (ADR-011 §5) ----

    @Test fun fromLegacyBinary64_usesValueOf_notDoubleCtor() {
        // BigDecimal.valueOf(0.1) -> "0.1"; BigDecimal(0.1) would be 0.1000000000000000055...
        assertEquals("0.1", StoredDecimal.fromLegacyBinary64(0.1).canonicalValue)
        assertEquals("100.0", StoredDecimal.fromLegacyBinary64(100.0).canonicalValue)
    }

    @Test fun fromLegacyBinary64_allowsHighScale_exemptFromNewLimits() {
        // 1.0/3.0 -> scale 16 (> MAX_SCALE) — accepted as a legacy approximation, not rejected.
        val v = StoredDecimal.fromLegacyBinary64(1.0 / 3.0)
        assertTrue(v.toBigDecimal().scale() > StoredDecimal.MAX_SCALE)
    }

    @Test fun fromLegacyBinary64_rejectsNaNAndInfinity() {
        for (d in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { StoredDecimal.fromLegacyBinary64(d) }
        }
    }
}
