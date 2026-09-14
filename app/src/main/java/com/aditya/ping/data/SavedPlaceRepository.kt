package com.aditya.ping.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class SavedPlaceRepository private constructor(
    private val dao: SavedPlaceDao,
) {
    fun observeAll(): Flow<List<SavedPlaceEntity>> = dao.observeAll()

    suspend fun getById(id: Long): SavedPlaceEntity? = dao.getById(id)

    suspend fun insert(place: SavedPlaceEntity): Long = dao.insert(place)

    suspend fun update(place: SavedPlaceEntity) = dao.update(place)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    companion object {
        @Volatile private var INSTANCE: SavedPlaceRepository? = null

        fun from(context: Context): SavedPlaceRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SavedPlaceRepository(
                    PingDatabase.get(context).savedPlaceDao(),
                ).also { INSTANCE = it }
            }
    }
}
