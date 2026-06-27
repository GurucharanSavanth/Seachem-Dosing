package com.example.seachem_dosing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row = one dose computed (and optionally administered).
 *
 * `volumeLitresAtDose` snapshots the tank volume at the moment of the dose so
 * that downstream analytics (cumulative product usage, dose density) remain
 * accurate even if the user later changes their tank's volume setting.
 *
 * `administered=false` means the dose was calculated but not yet applied — UI
 * can later flip it to true once the user confirms.
 */
@Entity(
    tableName = "dosing_log",
    indices = [
        Index(value = ["profile_id", "timestamp"]),
        Index(value = ["product"])
    ]
)
data class DosingLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "profile_id")
    val profileId: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "product")
    val product: String,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "unit")
    val unit: String,

    @ColumnInfo(name = "volume_litres_at_dose")
    val volumeLitresAtDose: Double,

    @ColumnInfo(name = "administered")
    val administered: Boolean = false,

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
