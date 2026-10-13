package com.physicslab.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {

    @Insert
    suspend fun insert(reading: ReadingEntity)

    @Query("SELECT * FROM readings ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getSince(since: Long): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE sensorType = :sensorType ORDER BY timestamp DESC")
    fun getBySensor(sensorType: String): Flow<List<ReadingEntity>>

    @Query("DELETE FROM readings")
    suspend fun clearAll()
}
