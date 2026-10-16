package com.mototrip.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mototrip.data.local.entity.TripEntity
import com.mototrip.data.local.entity.TripStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startedAt DESC")
    fun getAllTripsFlow(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: String): TripEntity?

    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun getTripByIdFlow(tripId: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE status = :status LIMIT 1")
    suspend fun getTripByStatus(status: TripStatus): TripEntity?

    @Query("SELECT * FROM trips WHERE status != 'COMPLETED' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveOrPausedTrip(): TripEntity?

    @Query("SELECT * FROM trips WHERE status != 'COMPLETED' ORDER BY startedAt DESC LIMIT 1")
    fun getActiveOrPausedTripFlow(): Flow<TripEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTrip(trip: TripEntity)

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Delete
    suspend fun deleteTrip(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTripById(tripId: String)
}
