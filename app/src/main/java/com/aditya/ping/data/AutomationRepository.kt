package com.aditya.ping.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class AutomationRepository private constructor(
    private val dao: AutomationDao,
) {
    fun observeAll(): Flow<List<AutomationEntity>> = dao.observeAll()

    suspend fun getById(id: Long): AutomationEntity? = dao.getById(id)

    suspend fun getEnabled(): List<AutomationEntity> = dao.getEnabled()

    suspend fun getByTriggerType(type: Int): List<AutomationEntity> = dao.getByTriggerType(type)

    suspend fun insert(automation: AutomationEntity): Long = dao.insert(automation)

    suspend fun update(automation: AutomationEntity) = dao.update(automation)

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    suspend fun delete(id: Long) = dao.deleteById(id)

    companion object {
        @Volatile private var instance: AutomationRepository? = null
        fun from(context: Context): AutomationRepository =
            instance ?: synchronized(this) {
                instance ?: AutomationRepository(
                    PingDatabase.get(context).automationDao(),
                ).also { instance = it }
            }
    }
}
