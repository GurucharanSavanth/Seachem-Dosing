package com.example.seachem_dosing.data.local.converter

import androidx.room.TypeConverter
import com.example.seachem_dosing.core.numerics.StoredDecimal

/**
 * Lossless Room converter [StoredDecimal] ↔ canonical String (ADR-011 §2). No rounding, no
 * Double path. Reading a malformed / non-canonical stored value throws (via
 * [StoredDecimal.parse]) rather than silently coercing — the v1→v2 migration guarantees only
 * canonical strings are ever written, so a throw here signals real DB corruption.
 *
 * Registered on [com.example.seachem_dosing.data.local.database.AppDatabase] at v2 (Commit C/E).
 */
class StoredDecimalConverter {

    @TypeConverter
    fun toDb(value: StoredDecimal?): String? = value?.canonicalValue

    @TypeConverter
    fun fromDb(stored: String?): StoredDecimal? = stored?.let { StoredDecimal.parse(it) }
}
