package com.example.seachem_dosing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.seachem_dosing.data.local.entity.ParameterLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParameterLogDao {

    @Insert
    suspend fun insert(log: ParameterLogEntity): Long

    @Query("SELECT * FROM parameter_log WHERE profile_id = :profileId AND timestamp >= :since ORDER BY timestamp ASC")
    fun observeSince(profileId: String, since: Long): Flow<List<ParameterLogEntity>>

    @Query("SELECT * FROM parameter_log WHERE profile_id = :profileId ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(profileId: String, limit: Int): Flow<List<ParameterLogEntity>>

    @Query("DELETE FROM parameter_log WHERE profile_id = :profileId AND timestamp < :before")
    suspend fun deleteOlderThan(profileId: String, before: Long): Int

    @Query("SELECT COUNT(*) FROM parameter_log WHERE profile_id = :profileId")
    suspend fun countForProfile(profileId: String): Int
}
