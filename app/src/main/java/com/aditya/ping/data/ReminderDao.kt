package com.aditya.ping.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE listId = :listId ORDER BY createdAt DESC")
    fun observeByList(listId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE listId IS NULL ORDER BY createdAt DESC")
    fun observeUnassigned(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE enabled = 1 ORDER BY createdAt DESC")
    fun observeEnabled(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE enabled = 1")
    suspend fun getEnabled(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND dueAt IS NOT NULL")
    suspend fun getEnabledWithTimeTrigger(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE reminders SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE reminders SET lastFiredAt = :timestamp WHERE id = :id")
    suspend fun markFired(id: Long, timestamp: Long)
}
