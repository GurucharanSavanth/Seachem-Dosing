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

    @Test fun parseNewValue_roundTrips_exactCanonicalString() {
        for (s in listOf("0", "42", "-3.5", "1.0", "0.50", "100.0", "0.000000000001", "553.4404496903")) {
            assertEquals(s, StoredDecimal.parseNewValue(s).canonicalValue)
            assertEquals(0, StoredDecimal.parseNewValue(s).toBigDecimal().compareTo(BigDecimal(s)))
        }
    }

    @Test fun value_553_4404496903_acceptedAndRoundTripsNoScaleLoss() {
        val v = StoredDecimal.parseNewValue("553.4404496903")
        assertEquals("553.4404496903", v.canonicalValue)
        assertEquals(BigDecimal("553.4404496903").scale(), v.toBigDecimal().scale())
    }

    @Test fun from_bigDecimal_canonicalizesViaToPlainString() {
        assertEquals("1.50", StoredDecimal.from(BigDecimal("1.50")).canonicalValue)
        assertEquals("100", StoredDecimal.from(BigDecimal("100")).canonicalValue)
    }

    @Test fun from_doesNotRound_preservesScale() {
        val v = StoredDecimal.from(BigDecimal("0.123456789012345678")) // scale 18
        assertEquals("0.123456789012345678", v.canonicalValue)
        assertEquals(18, v.toBigDecimal().scale())
    }

    // ---- scale identity vs numeric equality (tests 10 & 11) ----

    @Test fun scale_isPreserved_distinctCanonical_butNumericallyEqual() {
        val a = StoredDecimal.parseNewValue("1")
        val b = StoredDecimal.parseNewValue("1.0")
        val c = StoredDecimal.parseNewValue("1.00")
        assertFalse(a.canonicalValue == b.canonicalValue)
        assertFalse(b.canonicalValue == c.canonicalValue)
        assertFalse(a == b)                               // representation identity differs
        assertTrue(a.numericallyEquals(b))                // numeric equality holds
        assertTrue(b.numericallyEquals(c))
        assertEquals(StoredDecimal.parseNewValue("1.0"), b)
    }

    @Test fun safetyComparison_usesNumericNotRepresentationEquality() {
        val threshold = StoredDecimal.parseNewValue("1.0")
        val reading = StoredDecimal.parseNewValue("1.00")
        // A safety/threshold check must treat these as equal...
        assertEquals(0, reading.toBigDecimal().compareTo(threshold.toBigDecimal()))
        assertTrue(reading.numericallyEquals(threshold))
        // ...even though storage/audit identity differs.
        assertFalse(reading == threshold)
    }

    // ---- new-value envelope boundaries (tests 2,3,4,5,6) ----

    @Test fun precision38_accepted_precision39_rejected() {
        StoredDecimal.parseNewValue("9".repeat(StoredDecimal.NEW_VALUE_MAX_PRECISION))          // 38
        assertThrows(IllegalArgumentException::class.java) {
            StoredDecimal.parseNewValue("9".repeat(StoredDecimal.NEW_VALUE_MAX_PRECISION + 1))  // 39
        }
    }

    @Test fun scale18_accepted_scale19_rejected() {
        StoredDecimal.parseNewValue("0." + "1".repeat(StoredDecimal.NEW_VALUE_MAX_SCALE))         // scale 18
        assertThrows(IllegalArgumentException::class.java) {
            StoredDecimal.parseNewValue("0." + "1".repeat(StoredDecimal.NEW_VALUE_MAX_SCALE + 1)) // scale 19
        }
    }

    @Test fun newValue_over64Chars_rejected() {
        // 65 chars (precision also exceeds; the point is new values cannot exceed the length cap).
        assertThrows(IllegalArgumentException::class.java) {
            StoredDecimal.parseNewValue("9".repeat(StoredDecimal.NEW_VALUE_MAX_CANONICAL_LENGTH + 1))
        }
    }

    // ---- rejections ----

    @Test fun rejects_malformed_exponent_locale_whitespace_nonCanonical() {
        val bad = listOf(
            "", "   ", " 1.0 ", "\t1", "1\n",
            "1e3", "1E3", "1.5e-2",
            "1,5", "1,000.5", "1 000",
            "NaN", "Infinity", "-Infinity", "abc", "0x10",
            "1.2.3", "--1", "+1", "1.", ".5", "01", "00.5",
        )
        for (s in bad) {
            assertThrows("expected reject: '$s'", IllegalArgumentException::class.java) {
                StoredDecimal.parseNewValue(s)
            }
        }
    }

    @Test fun trimmedThenParse_succeeds() {
        assertEquals("1.0", StoredDecimal.parseNewValue(" 1.0 ".trim()).canonicalValue)
    }

    // ---- sign policy ----

    @Test fun sign_default_allowsNegative() {
        assertEquals("-2.5", StoredDecimal.parseNewValue("-2.5").canonicalValue)
    }

    @Test fun sign_nonNegative_rejectsNegative_allowsZero() {
        StoredDecimal.parseNewValue("0", StoredDecimalSign.NON_NEGATIVE)
        assertThrows(IllegalArgumentException::class.java) {
            StoredDecimal.parseNewValue("-0.1", StoredDecimalSign.NON_NEGATIVE)
        }
    }

    @Test fun sign_positive_rejectsZeroAndNegative() {
        StoredDecimal.parseNewValue("0.01", StoredDecimalSign.POSITIVE)
        for (s in listOf("0", "-1")) {
            assertThrows(IllegalArgumentException::class.java) {
                StoredDecimal.parseNewValue(s, StoredDecimalSign.POSITIVE)
            }
        }
    }

    // ---- legacy migration path + read invariant (tests 7,9) ----

    @Test fun fromLegacyBinary64_usesValueOf_notDoubleCtor() {
        // BigDecimal.valueOf(0.1) -> "0.1"; BigDecimal(0.1) would be 0.1000000000000000055...
        assertEquals("0.1", StoredDecimal.fromLegacyBinary64(0.1).canonicalValue)
        assertEquals("100.0", StoredDecimal.fromLegacyBinary64(100.0).canonicalValue)
    }

    @Test fun fromLegacyBinary64_highScale_exceedsNewEnvelope_butStoredCanonicalReadsIt() {
        val legacy = StoredDecimal.fromLegacyBinary64(1e-25)
        assertTrue("expected scale > new envelope", legacy.toBigDecimal().scale() > StoredDecimal.NEW_VALUE_MAX_SCALE)
        // The stricter new-value parser would reject this canonical value...
        assertThrows(IllegalArgumentException::class.java) {
            StoredDecimal.parseNewValue(legacy.canonicalValue)
        }
        // ...but the stored-canonical reader (used by the Room converter) accepts it: legacy-read invariant.
        assertEquals(legacy, StoredDecimal.parseStoredCanonical(legacy.canonicalValue))
    }

    @Test fun fromLegacyBinary64_rejectsNaNAndInfinity() {
        for (d in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { StoredDecimal.fromLegacyBinary64(d) }
        }
    }

    @Test fun parseStoredCanonical_stillRejectsMalformed() {
        for (s in listOf("1e3", "1,5", " 1.0 ", "abc", "1.", "")) {
            assertThrows("expected reject: '$s'", IllegalArgumentException::class.java) {
                StoredDecimal.parseStoredCanonical(s)
            }
        }
    }
}
