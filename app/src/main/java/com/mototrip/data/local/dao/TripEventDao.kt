package com.mototrip.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mototrip.data.local.entity.TripEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripEventDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTripEvent(event: TripEventEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTripEvents(events: List<TripEventEntity>)

    @Query("SELECT * FROM trip_events WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getTripEventsByTrip(tripId: String): List<TripEventEntity>

    @Query("SELECT * FROM trip_events WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getTripEventsByTripFlow(tripId: String): Flow<List<TripEventEntity>>

    @Query("SELECT * FROM trip_events ORDER BY timestamp ASC")
    suspend fun getAllTripEvents(): List<TripEventEntity>

    @Query("DELETE FROM trip_events WHERE tripId = :tripId")
    suspend fun deleteTripEventsByTrip(tripId: String)
}
