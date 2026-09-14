package com.aditya.ping.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AutomationEntity>>

    @Query("SELECT * FROM automations WHERE id = :id")
    suspend fun getById(id: Long): AutomationEntity?

    @Query("SELECT * FROM automations WHERE enabled = 1")
    suspend fun getEnabled(): List<AutomationEntity>

    @Query("SELECT * FROM automations WHERE triggerType = :type AND enabled = 1")
    suspend fun getByTriggerType(type: Int): List<AutomationEntity>

    @Insert
    suspend fun insert(automation: AutomationEntity): Long

    @Update
    suspend fun update(automation: AutomationEntity)

    @Query("UPDATE automations SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Delete
    suspend fun delete(automation: AutomationEntity)

    @Query("DELETE FROM automations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
