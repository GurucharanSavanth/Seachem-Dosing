package com.example.seachem_dosing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.seachem_dosing.data.local.entity.DoseEventDetailEntity
import com.example.seachem_dosing.data.local.entity.HistoryEventEntity
import com.example.seachem_dosing.data.local.entity.HistoryTimelineRow
import com.example.seachem_dosing.data.local.entity.ParameterEventDetailEntity
import kotlinx.coroutines.flow.Flow

/**
 * Transactional persistence for the v2 history audit log (ADR-011 §1, Gate D).
 *
 * Abstract class (not interface) so the `@Transaction` methods have bodies that compose the
 * abstract `@Insert`s — the event header and its detail are inserted atomically (both or neither).
 * No physical `DELETE` is exposed (append-only; corrections/voids are new events — Gate D).
 * Wired into `AppDatabase` at Commit E; SQL is Room/KSP-validated then.
 */
@Dao
abstract class HistoryDao {

    @Insert protected abstract suspend fun insertEvent(event: HistoryEventEntity)
    @Insert protected abstract suspend fun insertDoseDetail(detail: DoseEventDetailEntity)
    @Insert protected abstract suspend fun insertParameterDetail(detail: ParameterEventDetailEntity)

    @Transaction
    open suspend fun insertDoseEvent(event: HistoryEventEntity, detail: DoseEventDetailEntity) {
        insertEvent(event)
        insertDoseDetail(detail)
    }

    @Transaction
    open suspend fun insertParameterEvent(event: HistoryEventEntity, detail: ParameterEventDetailEntity) {
        insertEvent(event)
        insertParameterDetail(detail)
    }

    @Transaction
    open suspend fun insertCorrectionEvent(event: HistoryEventEntity, replacementDose: DoseEventDetailEntity?) {
        insertEvent(event)
        if (replacementDose != null) insertDoseDetail(replacementDose)
    }

    @Transaction
    open suspend fun insertVoidEvent(event: HistoryEventEntity) {
        insertEvent(event)
    }

    @Query("SELECT event_id FROM history_event WHERE idempotency_key = :key LIMIT 1")
    abstract suspend fun findEventIdByIdempotencyKey(key: String): String?

    @Query("SELECT * FROM history_event WHERE event_id = :id")
    abstract suspend fun getEventById(id: String): HistoryEventEntity?

    @Transaction
    @Query("SELECT * FROM history_event WHERE aquarium_profile_id = :profileId ORDER BY occurred_at_epoch_millis DESC")
    abstract fun observeTimeline(profileId: String): Flow<List<HistoryTimelineRow>>

    @Transaction
    @Query(
        "SELECT * FROM history_event WHERE aquarium_profile_id = :profileId AND event_type_code = :typeCode " +
            "ORDER BY occurred_at_epoch_millis DESC",
    )
    abstract fun observeByType(profileId: String, typeCode: String): Flow<List<HistoryTimelineRow>>

    @Transaction
    @Query(
        "SELECT * FROM history_event WHERE aquarium_profile_id = :profileId " +
            "AND occurred_at_epoch_millis BETWEEN :fromInclusive AND :toInclusive ORDER BY occurred_at_epoch_millis DESC",
    )
    abstract fun observeByDateRange(profileId: String, fromInclusive: Long, toInclusive: Long): Flow<List<HistoryTimelineRow>>

    /** Event ids that have been voided (a VOID event points at them), so the UI can hide/flag them. */
    @Query("SELECT voids_event_id FROM history_event WHERE aquarium_profile_id = :profileId AND voids_event_id IS NOT NULL")
    abstract fun observeVoidedEventIds(profileId: String): Flow<List<String>>
}
