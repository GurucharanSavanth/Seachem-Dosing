package com.example.seachem_dosing.core.numerics

import java.math.BigDecimal

/** Sign policy a caller may require for a quantity type (ADR-011 §2). Enforced at build time. */
enum class StoredDecimalSign { ANY, NON_NEGATIVE, POSITIVE }

/**
 * Canonical, precision-safe decimal for persistence and audit identity (ADR-011 §2).
 *
 * Backed by one canonical string `== BigDecimal.toPlainString()` (no scientific notation, locale
 * separators, grouping, exponent, surrounding whitespace, or non-canonical forms like `01`/`1.`).
 *
 * **Scale is preserved** — `"1"`, `"1.0"`, `"1.00"` are DISTINCT canonical values carrying
 * user/source precision.
 *  - [equals] (value-class structural equality on [canonicalValue]) = **storage / audit identity**
 *    (scale-sensitive).
 *  - Numeric / safety comparisons MUST use [numericallyEquals] or `toBigDecimal().compareTo(...)`.
 *    Never use representation equality for dose limits, parameter thresholds, overdose checks, or
 *    unit conversion. Do not derive idempotency from the raw decimal string unless scale-sensitive
 *    identity is explicitly intended.
 *
 * Two acceptance envelopes (ADR-011 §2):
 *  - **New values** ([from], [parseNewValue]) — precision ≤ [NEW_VALUE_MAX_PRECISION],
 *    scale ≤ [NEW_VALUE_MAX_SCALE], length ≤ [NEW_VALUE_MAX_CANONICAL_LENGTH]. This is the
 *    persistence acceptance envelope, NOT the engine's intermediate precision (engines may use a
 *    larger/unlimited MathContext and round only at domain/display boundaries).
 *  - **Stored / legacy values** ([parseStoredCanonical], [fromLegacyBinary64]) — any canonical
 *    value, no envelope, so every value the persistence layer can write is guaranteed readable.
 *
 * There is no Double/Float path for new values. The single quarantined Double path,
 * [fromLegacyBinary64], exists only for the v1→v2 migration (ADR-011 §5).
 */
@JvmInline
value class StoredDecimal private constructor(val canonicalValue: String) {

    fun toBigDecimal(): BigDecimal = BigDecimal(canonicalValue)

    /** Numeric (scale-insensitive) equality. Use in logic/safety; never compare scale for safety. */
    fun numericallyEquals(other: StoredDecimal): Boolean =
        toBigDecimal().compareTo(other.toBigDecimal()) == 0

    override fun toString(): String = canonicalValue

    companion object {
        // NEW-value persistence acceptance envelope (ADR-011 §2). Tunable knobs. Not the engine's
        // intermediate precision. 553.4404496903-class inputs fit comfortably inside this.
        const val NEW_VALUE_MAX_PRECISION = 38
        const val NEW_VALUE_MAX_SCALE = 18
        const val NEW_VALUE_MAX_CANONICAL_LENGTH = 64

        private val CANONICAL = Regex("""^-?\d+(\.\d+)?$""")

        /** New value from a [BigDecimal]; canonicalizes via `toPlainString()`, new-value envelope + [sign]. */
        fun from(value: BigDecimal, sign: StoredDecimalSign = StoredDecimalSign.ANY): StoredDecimal =
            of(value.toPlainString(), sign, enforceNewValueEnvelope = true)

        /** Strict NEW-value parse of a canonical string (new-value envelope). The normal user-input path. */
        fun parseNewValue(raw: String, sign: StoredDecimalSign = StoredDecimalSign.ANY): StoredDecimal =
            of(raw, sign, enforceNewValueEnvelope = true)

        /**
         * MIGRATION ONLY (ADR-011 §5). Legacy binary64 [Double] → canonical decimal via
         * `BigDecimal.valueOf(d).toPlainString()`. Preserves the canonical decimal of the STORED
         * double (does NOT recover original precision). No envelope — never use for new records.
         */
        fun fromLegacyBinary64(
            value: Double,
            sign: StoredDecimalSign = StoredDecimalSign.ANY,
        ): StoredDecimal {
            require(!value.isNaN() && !value.isInfinite()) { "legacy value not finite: $value" }
            return of(BigDecimal.valueOf(value).toPlainString(), sign, enforceNewValueEnvelope = false)
        }

        /**
         * Reads any canonical value the persistence layer can hold — new OR legacy. Rejects
         * malformed/exponent/locale/whitespace/NaN/Infinity/non-canonical, but applies NO envelope,
         * so every value emitted by [from], [parseNewValue], or [fromLegacyBinary64] is guaranteed
         * readable (the legacy-read invariant). Used by the Room converter on read. NOT a user-input path.
         */
        fun parseStoredCanonical(raw: String): StoredDecimal =
            of(raw, StoredDecimalSign.ANY, enforceNewValueEnvelope = false)

        private fun of(
            raw: String,
            sign: StoredDecimalSign,
            enforceNewValueEnvelope: Boolean,
        ): StoredDecimal {
            require(raw.isNotBlank()) { "blank decimal" }
            require(raw == raw.trim()) { "surrounding whitespace not allowed: '$raw'" }
            require(!raw.contains('e', ignoreCase = true)) { "exponent notation not allowed: '$raw'" }
            require(!raw.contains(',')) { "locale/grouping separators not allowed: '$raw'" }
            require(CANONICAL.matches(raw)) { "not a plain decimal: '$raw'" }
            val bd = BigDecimal(raw)
            require(bd.toPlainString() == raw) { "non-canonical decimal '$raw' (canonical='${bd.toPlainString()}')" }
            if (enforceNewValueEnvelope) {
                require(bd.precision() <= NEW_VALUE_MAX_PRECISION) { "precision ${bd.precision()} > $NEW_VALUE_MAX_PRECISION: '$raw'" }
                require(bd.scale() in 0..NEW_VALUE_MAX_SCALE) { "scale ${bd.scale()} out of 0..$NEW_VALUE_MAX_SCALE: '$raw'" }
                require(raw.length <= NEW_VALUE_MAX_CANONICAL_LENGTH) { "length ${raw.length} > $NEW_VALUE_MAX_CANONICAL_LENGTH: '$raw'" }
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
