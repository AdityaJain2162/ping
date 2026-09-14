package com.aditya.ping.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class ReminderListRepository private constructor(
    private val dao: ReminderListDao,
) {
    fun observeAll(): Flow<List<ReminderListEntity>> = dao.observeAll()

    suspend fun getById(id: Long): ReminderListEntity? = dao.getById(id)

    suspend fun insert(list: ReminderListEntity): Long = dao.insert(list)

    suspend fun update(list: ReminderListEntity) = dao.update(list)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    companion object {
        @Volatile private var INSTANCE: ReminderListRepository? = null

        fun from(context: Context): ReminderListRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReminderListRepository(
                    PingDatabase.get(context).reminderListDao(),
                ).also { INSTANCE = it }
            }
    }
}
