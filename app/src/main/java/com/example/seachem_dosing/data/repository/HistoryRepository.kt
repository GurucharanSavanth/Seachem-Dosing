package com.example.seachem_dosing.data.repository

import com.example.seachem_dosing.data.local.entity.DosingLogEntity
import com.example.seachem_dosing.data.local.entity.ParameterLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * Time-series persistence for water-parameter readings and dosing events.
 *
 * Wraps the Room DAOs ([com.example.seachem_dosing.data.local.dao.ParameterLogDao],
 * [com.example.seachem_dosing.data.local.dao.DosingLogDao]) so callers (UseCase,
 * ViewModel) depend on this contract instead of the DAOs directly.
 */
interface HistoryRepository {

    suspend fun recordParameters(snapshot: ParameterLogEntity): Long

    fun observeParametersSince(profileId: String, sinceMillis: Long): Flow<List<ParameterLogEntity>>

    fun observeRecentParameters(profileId: String, limit: Int = 30): Flow<List<ParameterLogEntity>>

    suspend fun pruneParameters(profileId: String, olderThanMillis: Long): Int

    suspend fun recordDose(dose: DosingLogEntity): Long

    fun observeRecentDoses(profileId: String, limit: Int = 30): Flow<List<DosingLogEntity>>

    fun observeRecentDosesForProduct(
        profileId: String,
        product: String,
        limit: Int = 10
    ): Flow<List<DosingLogEntity>>
}
