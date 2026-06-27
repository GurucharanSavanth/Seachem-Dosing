package com.example.seachem_dosing.data.repository

import com.example.seachem_dosing.data.local.dao.DosingLogDao
import com.example.seachem_dosing.data.local.dao.ParameterLogDao
import com.example.seachem_dosing.data.local.entity.DosingLogEntity
import com.example.seachem_dosing.data.local.entity.ParameterLogEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepositoryImpl(
    private val parameterLogDao: ParameterLogDao,
    private val dosingLogDao: DosingLogDao
) : HistoryRepository {

    override suspend fun recordParameters(snapshot: ParameterLogEntity): Long =
        parameterLogDao.insert(snapshot)

    override fun observeParametersSince(
        profileId: String,
        sinceMillis: Long
    ): Flow<List<ParameterLogEntity>> =
        parameterLogDao.observeSince(profileId, sinceMillis)

    override fun observeRecentParameters(
        profileId: String,
        limit: Int
    ): Flow<List<ParameterLogEntity>> =
        parameterLogDao.observeRecent(profileId, limit)

    override suspend fun pruneParameters(profileId: String, olderThanMillis: Long): Int =
        parameterLogDao.deleteOlderThan(profileId, olderThanMillis)

    override suspend fun recordDose(dose: DosingLogEntity): Long =
        dosingLogDao.insert(dose)

    override fun observeRecentDoses(profileId: String, limit: Int): Flow<List<DosingLogEntity>> =
        dosingLogDao.observeRecent(profileId, limit)

    override fun observeRecentDosesForProduct(
        profileId: String,
        product: String,
        limit: Int
    ): Flow<List<DosingLogEntity>> =
        dosingLogDao.observeRecentForProduct(profileId, product, limit)
}
