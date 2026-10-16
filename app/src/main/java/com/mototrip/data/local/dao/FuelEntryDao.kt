package com.mototrip.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mototrip.data.local.entity.FuelEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelEntryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertFuelEntry(entry: FuelEntryEntity)

    @Update
    suspend fun updateFuelEntry(entry: FuelEntryEntity)

    @Delete
    suspend fun deleteFuelEntry(entry: FuelEntryEntity)

    @Query("SELECT * FROM fuel_entries WHERE id = :entryId")
    suspend fun getFuelEntryById(entryId: String): FuelEntryEntity?

    @Query("SELECT * FROM fuel_entries ORDER BY occurredAt ASC")
    suspend fun getAllFuelEntries(): List<FuelEntryEntity>

    @Query("SELECT * FROM fuel_entries WHERE tripId = :tripId ORDER BY occurredAt ASC")
    suspend fun getFuelEntriesByTrip(tripId: String): List<FuelEntryEntity>

    @Query("SELECT * FROM fuel_entries WHERE tripId = :tripId ORDER BY occurredAt ASC")
    fun getFuelEntriesByTripFlow(tripId: String): Flow<List<FuelEntryEntity>>

    @Query("SELECT * FROM fuel_entries WHERE stopId = :stopId ORDER BY occurredAt ASC")
    suspend fun getFuelEntriesByStop(stopId: String): List<FuelEntryEntity>

    @Query("DELETE FROM fuel_entries WHERE id = :entryId")
    suspend fun deleteFuelEntryById(entryId: String)
}
