package com.mototrip.domain.repository

import com.mototrip.domain.model.Expense
import com.mototrip.domain.model.FuelEntry
import com.mototrip.domain.model.Stop
import com.mototrip.domain.model.TrackPoint
import com.mototrip.domain.model.Trip
import com.mototrip.domain.model.TripDetail
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    fun getAllTrips(): Flow<List<Trip>>
    fun getTripById(tripId: String): Flow<Trip?>
    fun getActiveTrip(): Flow<Trip?>
    fun getTripDetail(tripId: String): Flow<TripDetail?>

    suspend fun startTrip(
        id: String,
        name: String,
        originName: String?,
        destinationName: String?,
        startedAt: Long,
        latitude: Double?,
        longitude: Double?
    ): Result<Trip>

    suspend fun pauseTrip(
        tripId: String,
        timestamp: Long,
        latitude: Double?,
        longitude: Double?
    ): Result<Unit>

    suspend fun resumeTrip(
        tripId: String,
        timestamp: Long,
        latitude: Double?,
        longitude: Double?
    ): Result<Unit>

    suspend fun finishTrip(
        tripId: String,
        endedAt: Long,
        distanceMeters: Long,
        latitude: Double?,
        longitude: Double?
    ): Result<Unit>

    suspend fun updateTripMetadata(trip: Trip): Result<Unit>
    suspend fun deleteTrip(tripId: String): Result<Unit>

    // Stops
    fun getStopsByTrip(tripId: String): Flow<List<Stop>>
    suspend fun addStop(stop: Stop): Result<Unit>
    suspend fun updateStop(stop: Stop): Result<Unit>
    suspend fun deleteStop(stopId: String): Result<Unit>

    // Fuel entries
    fun getFuelEntriesByTrip(tripId: String): Flow<List<FuelEntry>>
    suspend fun addFuelEntry(entry: FuelEntry): Result<Unit>
    suspend fun updateFuelEntry(entry: FuelEntry): Result<Unit>
    suspend fun deleteFuelEntry(entryId: String): Result<Unit>

    // Expenses
    fun getExpensesByTrip(tripId: String): Flow<List<Expense>>
    suspend fun addExpense(expense: Expense): Result<Unit>
    suspend fun updateExpense(expense: Expense): Result<Unit>
    suspend fun deleteExpense(expenseId: String): Result<Unit>

    // Track points
    fun getTrackPointsByTrip(tripId: String): Flow<List<TrackPoint>>
    suspend fun recordTrackPoints(points: List<TrackPoint>): Result<Unit>
}
