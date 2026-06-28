package com.example.seachem_dosing.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Read projection for the history timeline: an event plus its optional type-specific detail
 * (exactly one of [dose] / [parameter] is non-null for a well-formed event; both null only for a
 * bare CORRECTION/VOID header). Used by `HistoryDao` queries (Commit D); not a table.
 */
data class HistoryTimelineRow(
    @Embedded val event: HistoryEventEntity,
    @Relation(parentColumn = "event_id", entityColumn = "event_id")
    val dose: DoseEventDetailEntity? = null,
    @Relation(parentColumn = "event_id", entityColumn = "event_id")
    val parameter: ParameterEventDetailEntity? = null,
)
