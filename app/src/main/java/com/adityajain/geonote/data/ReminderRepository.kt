package com.adityajain.geonote.data

import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val dao: ReminderDao) {

    fun observeAll(): Flow<List<ReminderEntity>> = dao.observeAll()
    fun observeEnabled(): Flow<List<ReminderEntity>> = dao.observeEnabled()

    suspend fun getById(id: Long): ReminderEntity? = dao.getById(id)
    suspend fun getEnabled(): List<ReminderEntity> = dao.getEnabled()

    suspend fun insert(reminder: ReminderEntity): Long = dao.insert(reminder)
    suspend fun update(reminder: ReminderEntity) = dao.update(reminder)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)
    suspend fun markFired(id: Long, timestamp: Long) = dao.markFired(id, timestamp)

    companion object {
        fun from(context: android.content.Context): ReminderRepository =
            ReminderRepository(GeoNoteDatabase.get(context).reminderDao())
    }
}
