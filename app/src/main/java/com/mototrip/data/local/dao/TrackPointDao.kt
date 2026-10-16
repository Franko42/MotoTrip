package com.mototrip.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mototrip.data.local.entity.TrackPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoint(point: TrackPointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoints(points: List<TrackPointEntity>)

    @Query("SELECT * FROM track_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getTrackPointsByTrip(tripId: String): List<TrackPointEntity>

    @Query("SELECT * FROM track_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getTrackPointsByTripFlow(tripId: String): Flow<List<TrackPointEntity>>

    @Query("SELECT * FROM track_points ORDER BY timestamp ASC")
    suspend fun getAllTrackPoints(): List<TrackPointEntity>

    @Query("SELECT COUNT(*) FROM track_points WHERE tripId = :tripId")
    suspend fun getTrackPointsCount(tripId: String): Int

    @Query("SELECT * FROM track_points WHERE tripId = :tripId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestTrackPoint(tripId: String): TrackPointEntity?

    @Query("DELETE FROM track_points WHERE tripId = :tripId")
    suspend fun deleteTrackPointsByTrip(tripId: String)
}
