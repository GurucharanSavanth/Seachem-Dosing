package com.example.seachem_dosing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Append-only history event header (ADR-011 §1). One row per timeline event; type-specific data
 * lives in [DoseEventDetailEntity] / [ParameterEventDetailEntity] (1:1 by [eventId]).
 *
 * `*_code` columns hold stable registry storage codes (`com.example.seachem_dosing.domain.history`),
 * never enum ordinals. Self-referential [supersedesEventId]/[voidsEventId] support append-only
 * CORRECTION/VOID without mutating the original (ADR-011 §7); `ON DELETE RESTRICT` keeps the audit
 * chain intact (physical deletion is not offered — Gate D).
 */
@Entity(
    tableName = "history_event",
    indices = [
        Index(value = ["idempotency_key"], unique = true),
        Index(value = ["aquarium_profile_id", "occurred_at_epoch_millis"]),
        Index(value = ["event_type_code"]),
        Index(value = ["supersedes_event_id"]),
        Index(value = ["voids_event_id"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = HistoryEventEntity::class,
            parentColumns = ["event_id"],
            childColumns = ["supersedes_event_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = HistoryEventEntity::class,
            parentColumns = ["event_id"],
            childColumns = ["voids_event_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class HistoryEventEntity(
    @PrimaryKey @ColumnInfo(name = "event_id") val eventId: String,
    @ColumnInfo(name = "event_type_code") val eventTypeCode: String,
    @ColumnInfo(name = "aquarium_profile_id") val aquariumProfileId: String,
    @ColumnInfo(name = "occurred_at_epoch_millis") val occurredAtEpochMillis: Long,
    @ColumnInfo(name = "created_at_epoch_millis") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "source_module_code") val sourceModuleCode: String,
    @ColumnInfo(name = "app_version") val appVersion: String? = null,
    @ColumnInfo(name = "engine_version") val engineVersion: String? = null,
    @ColumnInfo(name = "idempotency_key") val idempotencyKey: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int,
    @ColumnInfo(name = "precision_status_code") val precisionStatusCode: String,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "supersedes_event_id") val supersedesEventId: String? = null,
    @ColumnInfo(name = "voids_event_id") val voidsEventId: String? = null,
    @ColumnInfo(name = "correction_reason") val correctionReason: String? = null,
)
