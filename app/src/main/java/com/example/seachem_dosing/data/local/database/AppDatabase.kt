package com.example.seachem_dosing.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.seachem_dosing.data.local.dao.DosingLogDao
import com.example.seachem_dosing.data.local.dao.ParameterLogDao
import com.example.seachem_dosing.data.local.entity.DosingLogEntity
import com.example.seachem_dosing.data.local.entity.ParameterLogEntity

/**
 * Application Room database.
 *
 * Schema is exported to app/schemas/<version>.json (configured via
 * ksp.arg("room.schemaLocation", ...) in app/build.gradle.kts) so future
 * migrations can be auto-generated and reviewed in PRs.
 */
@Database(
    entities = [
        ParameterLogEntity::class,
        DosingLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun parameterLogDao(): ParameterLogDao
    abstract fun dosingLogDao(): DosingLogDao

    companion object {
        const val DB_NAME = "seachem_dosing.db"
    }
}
