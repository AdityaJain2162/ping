package com.aditya.ping.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderListDao {

    @Query("SELECT * FROM reminder_lists ORDER BY name ASC")
    fun observeAll(): Flow<List<ReminderListEntity>>

    @Query("SELECT * FROM reminder_lists WHERE id = :id")
    suspend fun getById(id: Long): ReminderListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: ReminderListEntity): Long

    @Update
    suspend fun update(list: ReminderListEntity)

    @Delete
    suspend fun delete(list: ReminderListEntity)

    @Query("DELETE FROM reminder_lists WHERE id = :id")
    suspend fun deleteById(id: Long)
}
