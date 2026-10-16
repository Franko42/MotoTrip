package com.mototrip.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mototrip.data.local.entity.StopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StopDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStop(stop: StopEntity)

    @Update
    suspend fun updateStop(stop: StopEntity)

    @Delete
    suspend fun deleteStop(stop: StopEntity)

    @Query("SELECT * FROM stops WHERE id = :stopId")
    suspend fun getStopById(stopId: String): StopEntity?

    @Query("SELECT * FROM stops ORDER BY arrivedAt ASC")
    suspend fun getAllStops(): List<StopEntity>

    @Query("SELECT * FROM stops WHERE tripId = :tripId ORDER BY arrivedAt ASC")
    suspend fun getStopsByTrip(tripId: String): List<StopEntity>

    @Query("SELECT * FROM stops WHERE tripId = :tripId ORDER BY arrivedAt ASC")
    fun getStopsByTripFlow(tripId: String): Flow<List<StopEntity>>

    @Query("DELETE FROM stops WHERE id = :stopId")
    suspend fun deleteStopById(stopId: String)
}
