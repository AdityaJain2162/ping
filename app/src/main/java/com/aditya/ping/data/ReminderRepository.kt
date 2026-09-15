package com.aditya.ping.data

import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val dao: ReminderDao) {

    fun observeAll(): Flow<List<ReminderEntity>> = dao.observeAll()
    fun observeEnabled(): Flow<List<ReminderEntity>> = dao.observeEnabled()
    fun search(query: String): Flow<List<ReminderEntity>> = dao.search(query)
    fun observeByDateRange(startOfDay: Long, endOfDay: Long): Flow<List<ReminderEntity>> = dao.observeByDateRange(startOfDay, endOfDay)

    suspend fun getById(id: Long): ReminderEntity? = dao.getById(id)
    suspend fun getEnabled(): List<ReminderEntity> = dao.getEnabled()
    suspend fun getEnabledWithTimeTrigger(): List<ReminderEntity> = dao.getEnabledWithTimeTrigger()

    fun observeLocationOnly(): Flow<List<ReminderEntity>> = dao.observeLocationOnly()

    suspend fun insert(reminder: ReminderEntity): Long = dao.insert(reminder)
    suspend fun update(reminder: ReminderEntity) = dao.update(reminder)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)
    suspend fun setCompleted(id: Long, completed: Boolean, completedAt: Long?) = dao.setCompleted(id, completed, completedAt)
    fun observeHistory(since: Long): Flow<List<ReminderEntity>> = dao.observeHistory(since)
    suspend fun pruneOldCompleted(before: Long) = dao.pruneOldCompleted(before)
    suspend fun markFired(id: Long, timestamp: Long) = dao.markFired(id, timestamp)

    companion object {
        fun from(context: android.content.Context): ReminderRepository =
            ReminderRepository(PingDatabase.get(context).reminderDao())
    }
}
