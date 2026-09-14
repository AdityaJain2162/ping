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

    @Query("SELECT * FROM reminders WHERE title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' OR addressLabel LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun search(query: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE dueAt IS NOT NULL AND dueAt >= :startOfDay AND dueAt < :endOfDay ORDER BY dueAt ASC")
    fun observeByDateRange(startOfDay: Long, endOfDay: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND completed = 0 ORDER BY createdAt DESC")
    fun observeEnabled(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND completed = 0")
    suspend fun getEnabled(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND completed = 0 AND dueAt IS NOT NULL")
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

    @Query("UPDATE reminders SET completed = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, completedAt: Long?)

    @Query("SELECT * FROM reminders WHERE completed = 1 AND completedAt >= :since ORDER BY completedAt DESC")
    fun observeHistory(since: Long): Flow<List<ReminderEntity>>

    @Query("DELETE FROM reminders WHERE completed = 1 AND completedAt < :before")
    suspend fun pruneOldCompleted(before: Long)

    @Query("UPDATE reminders SET lastFiredAt = :timestamp WHERE id = :id")
    suspend fun markFired(id: Long, timestamp: Long)
}
