package com.example.seachem_dosing.data.local.converter

import androidx.room.TypeConverter
import com.example.seachem_dosing.core.numerics.StoredDecimal

/**
 * Lossless Room converter [StoredDecimal] ↔ canonical String (ADR-011 §2). No rounding, no
 * Double path. Reads via [StoredDecimal.parseStoredCanonical] (the no-envelope canonical reader),
 * NOT the stricter new-value parser, so every value the persistence layer can write — including
 * high-scale legacy values from [StoredDecimal.fromLegacyBinary64] — is guaranteed readable.
 * A malformed / non-canonical stored value still throws, signalling real DB corruption.
 *
 * Registered on [com.example.seachem_dosing.data.local.database.AppDatabase] at v2 (Commit C/E).
 */
class StoredDecimalConverter {

    @TypeConverter
    fun toDb(value: StoredDecimal?): String? = value?.canonicalValue

    @TypeConverter
    fun fromDb(stored: String?): StoredDecimal? = stored?.let { StoredDecimal.parseStoredCanonical(it) }
}
