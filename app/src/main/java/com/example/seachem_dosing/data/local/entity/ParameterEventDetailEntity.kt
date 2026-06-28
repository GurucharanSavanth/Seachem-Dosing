package com.example.seachem_dosing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Water-parameter-reading detail (ADR-011 §1), 1:1 with its [HistoryEventEntity] via [eventId]
 * (PK + FK, `ON DELETE RESTRICT`). `measured_value_decimal` is a canonical
 * [com.example.seachem_dosing.core.numerics.StoredDecimal] string; `*_code` columns hold registry
 * storage codes (`ParameterType`, `UnitCode`, `ParameterValidationStatus`).
 */
@Entity(
    tableName = "parameter_event_detail",
    foreignKeys = [
        ForeignKey(
            entity = HistoryEventEntity::class,
            parentColumns = ["event_id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["parameter_type_code"])],
)
data class ParameterEventDetailEntity(
    @PrimaryKey @ColumnInfo(name = "event_id") val eventId: String,
    @ColumnInfo(name = "parameter_type_code") val parameterTypeCode: String,
    @ColumnInfo(name = "measured_value_decimal") val measuredValueDecimal: String,
    @ColumnInfo(name = "measured_unit_code") val measuredUnitCode: String,
    @ColumnInfo(name = "test_method") val testMethod: String? = null,
    @ColumnInfo(name = "source_device_or_kit") val sourceDeviceOrKit: String? = null,
    @ColumnInfo(name = "validation_status_code") val validationStatusCode: String,
)
