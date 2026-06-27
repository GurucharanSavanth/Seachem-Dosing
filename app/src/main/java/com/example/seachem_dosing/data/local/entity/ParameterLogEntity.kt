package com.example.seachem_dosing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row = one snapshot of water parameters at a point in time.
 * Nutrient columns are nullable — users may record only a subset (e.g.
 * ammonia + nitrite during cycling, or just alkalinity for reef checks).
 *
 * Volume is captured per-row so historical readings remain interpretable
 * even if the user later changes the configured tank volume.
 */
@Entity(
    tableName = "parameter_log",
    indices = [
        Index(value = ["profile_id", "timestamp"]),
        Index(value = ["timestamp"])
    ]
)
data class ParameterLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "profile_id")
    val profileId: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "ammonia") val ammonia: Double? = null,
    @ColumnInfo(name = "nitrite") val nitrite: Double? = null,
    @ColumnInfo(name = "nitrate") val nitrate: Double? = null,
    @ColumnInfo(name = "gh") val gh: Double? = null,
    @ColumnInfo(name = "kh") val kh: Double? = null,
    @ColumnInfo(name = "ph") val ph: Double? = null,
    @ColumnInfo(name = "temperature") val temperature: Double? = null,
    @ColumnInfo(name = "salinity") val salinity: Double? = null,
    @ColumnInfo(name = "alkalinity") val alkalinity: Double? = null,
    @ColumnInfo(name = "calcium") val calcium: Double? = null,
    @ColumnInfo(name = "magnesium") val magnesium: Double? = null,
    @ColumnInfo(name = "phosphate") val phosphate: Double? = null,
    @ColumnInfo(name = "dissolved_oxygen") val dissolvedOxygen: Double? = null,
    @ColumnInfo(name = "potassium") val potassium: Double? = null,
    @ColumnInfo(name = "iron") val iron: Double? = null,
    @ColumnInfo(name = "strontium") val strontium: Double? = null,
    @ColumnInfo(name = "iodide") val iodide: Double? = null,

    @ColumnInfo(name = "volume_litres")
    val volumeLitres: Double,

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
