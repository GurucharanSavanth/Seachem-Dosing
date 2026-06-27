package com.example.seachem_dosing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.seachem_dosing.data.local.entity.DosingLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DosingLogDao {

    @Insert
    suspend fun insert(log: DosingLogEntity): Long

    @Update
    suspend fun update(log: DosingLogEntity)

    @Query("SELECT * FROM dosing_log WHERE profile_id = :profileId ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(profileId: String, limit: Int): Flow<List<DosingLogEntity>>

    @Query("SELECT * FROM dosing_log WHERE profile_id = :profileId AND product = :product ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentForProduct(profileId: String, product: String, limit: Int): Flow<List<DosingLogEntity>>

    @Query("DELETE FROM dosing_log WHERE id = :id")
    suspend fun deleteById(id: Long)
}
