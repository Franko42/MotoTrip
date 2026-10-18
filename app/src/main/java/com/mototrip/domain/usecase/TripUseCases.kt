package com.mototrip.domain.usecase

import com.mototrip.domain.model.Expense
import com.mototrip.domain.model.FuelEntry
import com.mototrip.domain.model.Stop
import com.mototrip.domain.model.Trip
import com.mototrip.domain.model.TripDetail
import com.mototrip.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class StartTripUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(
        name: String,
        originName: String? = null,
        destinationName: String? = null,
        startedAt: Long = System.currentTimeMillis()
    ): Result<Trip> {
        val id = UUID.randomUUID().toString()
        return repository.startTrip(
            id = id,
            name = name,
            originName = originName,
            destinationName = destinationName,
            startedAt = startedAt,
            latitude = null,
            longitude = null
        )
    }
}

class PauseTripUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(tripId: String, timestamp: Long = System.currentTimeMillis()): Result<Unit> {
        return repository.pauseTrip(tripId, timestamp, null, null)
    }
}

class ResumeTripUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(tripId: String, timestamp: Long = System.currentTimeMillis()): Result<Unit> {
        return repository.resumeTrip(tripId, timestamp, null, null)
    }
}

class FinishTripUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(
        tripId: String,
        distanceMeters: Long,
        endedAt: Long = System.currentTimeMillis()
    ): Result<Unit> {
        return repository.finishTrip(tripId, endedAt, distanceMeters, null, null)
    }
}

class GetActiveTripUseCase(private val repository: TripRepository) {
    operator fun invoke(): Flow<Trip?> = repository.getActiveTrip()
}

class GetAllTripsUseCase(private val repository: TripRepository) {
    operator fun invoke(): Flow<List<Trip>> = repository.getAllTrips()
}

class GetTripDetailUseCase(private val repository: TripRepository) {
    operator fun invoke(tripId: String): Flow<TripDetail?> = repository.getTripDetail(tripId)
}

class AddStopUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(stop: Stop): Result<Unit> = repository.addStop(stop)
}

class AddFuelEntryUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(entry: FuelEntry): Result<Unit> = repository.addFuelEntry(entry)
}

class AddExpenseUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(expense: Expense): Result<Unit> = repository.addExpense(expense)
}

class DeleteTripUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(tripId: String): Result<Unit> = repository.deleteTrip(tripId)
}

class UpdateTripMetadataUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(trip: Trip): Result<Unit> = repository.updateTripMetadata(trip)
}

class UpdateTripDatesUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(tripId: String, startedAt: Long, endedAt: Long?): Result<Unit> =
        repository.updateTripDates(tripId, startedAt, endedAt)
}

class UpdateStopUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(stop: Stop): Result<Unit> = repository.updateStop(stop)
}

class UpdateFuelEntryUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(entry: FuelEntry): Result<Unit> = repository.updateFuelEntry(entry)
}

class UpdateExpenseUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(expense: Expense): Result<Unit> = repository.updateExpense(expense)
}

class RecalculateTripDistanceUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(tripId: String): Result<Long> = repository.recalculateTripDistance(tripId)
}
