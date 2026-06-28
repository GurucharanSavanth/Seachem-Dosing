package com.example.seachem_dosing.core.numerics

import java.math.BigDecimal

/** Sign policy a caller may require for a quantity type (ADR-011 §2). Enforced at parse/build time. */
enum class StoredDecimalSign { ANY, NON_NEGATIVE, POSITIVE }

/**
 * Canonical, precision-safe decimal for persistence and audit identity (ADR-011 §2).
 *
 * Backed by one canonical string `== BigDecimal.toPlainString()`:
 *  - no scientific notation, no locale separators, no grouping, no exponent, no surrounding
 *    whitespace, no non-canonical forms (`01`, `1.`, `.5`, `+1`);
 *  - **scale is preserved** — `"1"`, `"1.0"`, `"1.00"` are DISTINCT canonical values that carry
 *    user/source precision. Numeric (scale-insensitive) comparison is [numericallyEquals];
 *    storage/audit identity is value-class structural equality on [canonicalValue]. Scale is
 *    NEVER a safety signal (ADR-011 §2).
 *
 * New values may only be built from a [BigDecimal] ([from]) or a canonical [String] ([parse]).
 * There is deliberately **no Double/Float path for new values** — that would reintroduce the
 * binary64 error the engines avoid. The single quarantined Double path, [fromLegacyBinary64],
 * exists ONLY for the v1→v2 migration (ADR-011 §5) and is exempt from the NEW-value limits.
 */
@JvmInline
value class StoredDecimal private constructor(val canonicalValue: String) {

    fun toBigDecimal(): BigDecimal = BigDecimal(canonicalValue)

    /** Numeric (scale-insensitive) equality. Use in logic; never compare scale for a safety decision. */
    fun numericallyEquals(other: StoredDecimal): Boolean =
        toBigDecimal().compareTo(other.toBigDecimal()) == 0

    override fun toString(): String = canonicalValue

    companion object {
        // Tunable limits for NEW values (ADR-011 §2). Calibration knobs — widen if a real quantity needs it.
        const val MAX_PRECISION = 30   // significant digits
        const val MAX_SCALE = 12       // fractional digits
        const val MAX_LENGTH = 40      // canonical string length

        private val CANONICAL = Regex("""^-?\d+(\.\d+)?$""")

        /** Build from a [BigDecimal]; canonicalizes via `toPlainString()`, enforces limits + [sign]. */
        fun from(value: BigDecimal, sign: StoredDecimalSign = StoredDecimalSign.ANY): StoredDecimal =
            of(value.toPlainString(), sign, enforceLimits = true)

        /**
         * Strict parse of an already-canonical decimal string. Rejects exponent notation, locale
         * commas, grouping, surrounding whitespace, blank, NaN/Infinity (any letters), malformed
         * signs, non-canonical form, and over-precision/scale/length.
         */
        fun parse(raw: String, sign: StoredDecimalSign = StoredDecimalSign.ANY): StoredDecimal =
            of(raw, sign, enforceLimits = true)

        /**
         * MIGRATION ONLY (ADR-011 §5). Converts a legacy binary64 [Double] to its canonical decimal
         * via `BigDecimal.valueOf(d).toPlainString()`. Preserves the canonical decimal of the STORED
         * double; does NOT recover the user's original precision. Exempt from the NEW-value
         * precision/scale/length limits (legacy values are an explicit approximation class). Never
         * use for a NEW_EXACT_RECORD value.
         */
        fun fromLegacyBinary64(
            legacy: Double,
            sign: StoredDecimalSign = StoredDecimalSign.ANY,
        ): StoredDecimal {
            require(!legacy.isNaN() && !legacy.isInfinite()) { "legacy value not finite: $legacy" }
            return of(BigDecimal.valueOf(legacy).toPlainString(), sign, enforceLimits = false)
        }

        private fun of(raw: String, sign: StoredDecimalSign, enforceLimits: Boolean): StoredDecimal {
            require(raw.isNotBlank()) { "blank decimal" }
            require(raw == raw.trim()) { "surrounding whitespace not allowed: '$raw'" }
            require(!raw.contains('e', ignoreCase = true)) { "exponent notation not allowed: '$raw'" }
            require(!raw.contains(',')) { "locale/grouping separators not allowed: '$raw'" }
            require(CANONICAL.matches(raw)) { "not a plain decimal: '$raw'" }
            val bd = BigDecimal(raw)
            // Canonical-form guard: BigDecimal must round-trip to the exact same string (rejects "01", "1.0" stays).
            require(bd.toPlainString() == raw) { "non-canonical decimal '$raw' (canonical='${bd.toPlainString()}')" }
            if (enforceLimits) {
                require(bd.precision() <= MAX_PRECISION) { "precision ${bd.precision()} > $MAX_PRECISION: '$raw'" }
                require(bd.scale() in 0..MAX_SCALE) { "scale ${bd.scale()} out of 0..$MAX_SCALE: '$raw'" }
                require(raw.length <= MAX_LENGTH) { "length ${raw.length} > $MAX_LENGTH: '$raw'" }
            }
            when (sign) {
                StoredDecimalSign.ANY -> {}
                StoredDecimalSign.NON_NEGATIVE -> require(bd.signum() >= 0) { "negative not allowed: '$raw'" }
                StoredDecimalSign.POSITIVE -> require(bd.signum() > 0) { "must be > 0: '$raw'" }
            }
            return StoredDecimal(raw)
        }
    }
}
