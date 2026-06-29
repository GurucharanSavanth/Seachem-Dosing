package com.example.seachem_dosing.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.seachem_dosing.data.local.dao.HistoryDao
import com.example.seachem_dosing.data.local.entity.DoseEventDetailEntity
import com.example.seachem_dosing.data.local.entity.HistoryEventEntity
import com.example.seachem_dosing.data.local.entity.ParameterEventDetailEntity

/**
 * Application Room database (v2 — ADR-011). The v1 wide tables (`dosing_log`, `parameter_log`) are
 * replaced by the append-only event model; [MIGRATION_1_2] converts any v1 rows non-destructively.
 *
 * Schema is exported to `app/schemas/<version>.json` (ksp `room.schemaLocation`) for migration tests.
 */
@Database(
    entities = [
        HistoryEventEntity::class,
        DoseEventDetailEntity::class,
        ParameterEventDetailEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao

    companion object {
        const val DB_NAME = "seachem_dosing.db"
    }
}
