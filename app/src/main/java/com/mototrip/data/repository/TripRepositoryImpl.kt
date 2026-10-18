package com.mototrip.data.repository

import androidx.room.withTransaction
import com.mototrip.data.local.MotoTripDatabase
import com.mototrip.data.local.entity.ExpenseEntity
import com.mototrip.data.local.entity.FuelEntryEntity
import com.mototrip.data.local.entity.StopEntity
import com.mototrip.data.local.entity.TrackPointEntity
import com.mototrip.data.local.entity.TripEntity
import com.mototrip.data.local.entity.TripEventEntity
import com.mototrip.data.local.entity.TripEventType
import com.mototrip.data.local.entity.TripStatus
import com.mototrip.domain.model.Expense
import com.mototrip.domain.model.FuelEntry
import com.mototrip.domain.model.Stop
import com.mototrip.domain.model.TrackPoint
import com.mototrip.domain.model.Trip
import com.mototrip.domain.model.TripDetail
import com.mototrip.domain.model.TripEvent
import com.mototrip.domain.repository.TripRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TripRepositoryImpl(
    private val database: MotoTripDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TripRepository {

    private val tripDao = database.tripDao()
    private val stopDao = database.stopDao()
    private val fuelEntryDao = database.fuelEntryDao()
    private val expenseDao = database.expenseDao()
    private val tripEventDao = database.tripEventDao()
    private val trackPointDao = database.trackPointDao()

    override fun getAllTrips(): Flow<List<Trip>> {
        return tripDao.getAllTripsFlow()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun getTripById(tripId: String): Flow<Trip?> {
        return tripDao.getTripByIdFlow(tripId)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
    }

    override fun getActiveTrip(): Flow<Trip?> {
        return tripDao.getActiveOrPausedTripFlow()
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
    }

    override fun getTripDetail(tripId: String): Flow<TripDetail?> {
        val tripFlow = tripDao.getTripByIdFlow(tripId)
        val stopsFlow = stopDao.getStopsByTripFlow(tripId)
        val fuelFlow = fuelEntryDao.getFuelEntriesByTripFlow(tripId)
        val expenseFlow = expenseDao.getExpensesByTripFlow(tripId)
        val eventsFlow = tripEventDao.getTripEventsByTripFlow(tripId)

        return combine(tripFlow, stopsFlow, fuelFlow, expenseFlow, eventsFlow) { tripEntity, stops, fuel, expenses, events ->
            tripEntity?.let {
                val pointCount = trackPointDao.getTrackPointsCount(tripId)
                TripDetail(
                    trip = it.toDomain(),
                    stops = stops.map { s -> s.toDomain() },
                    fuelEntries = fuel.map { f -> f.toDomain() },
                    expenses = expenses.map { e -> e.toDomain() },
                    events = events.map { ev -> ev.toDomain() },
                    trackPointCount = pointCount
                )
            }
        }.flowOn(ioDispatcher)
    }

    override suspend fun startTrip(
        id: String,
        name: String,
        originName: String?,
        destinationName: String?,
        startedAt: Long,
        latitude: Double?,
        longitude: Double?
    ): Result<Trip> = withContext(ioDispatcher) {
        runCatching {
            database.withTransaction {
                val currentActive = tripDao.getActiveOrPausedTrip()
                if (currentActive != null) {
                    error("There is already an active or paused trip: ${currentActive.id}")
                }

                val now = System.currentTimeMillis()
                val tripEntity = TripEntity(
                    id = id,
                    name = name.ifBlank { "Viaje ${java.time.Instant.ofEpochMilli(startedAt)}" },
                    originName = originName,
                    destinationName = destinationName,
                    startedAt = startedAt,
                    endedAt = null,
                    status = TripStatus.ACTIVE,
                    distanceMeters = 0L,
                    notes = null,
                    createdAt = now,
                    updatedAt = now
                )
                tripDao.insertTrip(tripEntity)

                val startEvent = TripEventEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    tripId = id,
                    timestamp = startedAt,
                    eventType = TripEventType.STARTED,
                    latitude = latitude,
                    longitude = longitude,
                    details = "Viaje iniciado"
                )
                tripEventDao.insertTripEvent(startEvent)

                tripEntity.toDomain()
            }
        }
    }

    override suspend fun pauseTrip(
        tripId: String,
        timestamp: Long,
        latitude: Double?,
        longitude: Double?
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            database.withTransaction {
                val trip = tripDao.getTripById(tripId) ?: error("Trip not found: $tripId")
                if (trip.status != TripStatus.ACTIVE) {
                    error("Cannot pause trip with status ${trip.status}")
                }
                val now = System.currentTimeMillis()
                tripDao.updateTrip(trip.copy(status = TripStatus.PAUSED, updatedAt = now))

                val pauseEvent = TripEventEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    tripId = tripId,
                    timestamp = timestamp,
                    eventType = TripEventType.PAUSED,
                    latitude = latitude,
                    longitude = longitude,
                    details = "Viaje pausado"
                )
                tripEventDao.insertTripEvent(pauseEvent)
            }
        }
    }

    override suspend fun resumeTrip(
        tripId: String,
        timestamp: Long,
        latitude: Double?,
        longitude: Double?
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            database.withTransaction {
                val trip = tripDao.getTripById(tripId) ?: error("Trip not found: $tripId")
                if (trip.status != TripStatus.PAUSED) {
                    error("Cannot resume trip with status ${trip.status}")
                }
                val now = System.currentTimeMillis()
                tripDao.updateTrip(trip.copy(status = TripStatus.ACTIVE, updatedAt = now))

                val resumeEvent = TripEventEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    tripId = tripId,
                    timestamp = timestamp,
                    eventType = TripEventType.RESUMED,
                    latitude = latitude,
                    longitude = longitude,
                    details = "Viaje reanudado"
                )
                tripEventDao.insertTripEvent(resumeEvent)
            }
        }
    }

    override suspend fun finishTrip(
        tripId: String,
        endedAt: Long,
        distanceMeters: Long,
        latitude: Double?,
        longitude: Double?
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            database.withTransaction {
                val trip = tripDao.getTripById(tripId) ?: error("Trip not found: $tripId")
                if (trip.status == TripStatus.COMPLETED) {
                    error("Trip is already completed: $tripId")
                }
                val now = System.currentTimeMillis()
                tripDao.updateTrip(
                    trip.copy(
                        status = TripStatus.COMPLETED,
                        endedAt = endedAt,
                        distanceMeters = distanceMeters,
                        updatedAt = now
                    )
                )

                val finishEvent = TripEventEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    tripId = tripId,
                    timestamp = endedAt,
                    eventType = TripEventType.COMPLETED,
                    latitude = latitude,
                    longitude = longitude,
                    details = "Viaje completado"
                )
                tripEventDao.insertTripEvent(finishEvent)
            }
        }
    }

    override suspend fun updateTripMetadata(trip: Trip): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val existing = tripDao.getTripById(trip.id) ?: error("Trip not found: ${trip.id}")
            val updated = existing.copy(
                name = trip.name,
                originName = trip.originName,
                destinationName = trip.destinationName,
                startedAt = trip.startedAt,
                endedAt = trip.endedAt,
                notes = trip.notes,
                updatedAt = System.currentTimeMillis()
            )
            tripDao.updateTrip(updated)
        }
    }

    override suspend fun updateTripDates(tripId: String, startedAt: Long, endedAt: Long?): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val existing = tripDao.getTripById(tripId) ?: error("Trip not found: $tripId")
            val updated = existing.copy(
                startedAt = startedAt,
                endedAt = endedAt,
                updatedAt = System.currentTimeMillis()
            )
            tripDao.updateTrip(updated)
        }
    }

    override suspend fun recalculateTripDistance(tripId: String): Result<Long> = withContext(ioDispatcher) {
        runCatching {
            val points = trackPointDao.getTrackPointsByTrip(tripId).sortedBy { it.timestamp }
            var totalMeters = 0.0
            var prev: com.mototrip.core.location.RawLocationPoint? = null
            for (pt in points) {
                val raw = com.mototrip.core.location.RawLocationPoint(
                    timestamp = pt.timestamp,
                    latitude = pt.latitude,
                    longitude = pt.longitude,
                    accuracyMeters = pt.accuracyMeters,
                    speedMps = pt.speedMps,
                    altitudeMeters = pt.altitudeMeters
                )
                val (shouldRecord, delta) = com.mototrip.core.location.LocationFilter.evaluateConsecutivePoint(prev, raw)
                if (shouldRecord) {
                    totalMeters += delta
                    prev = raw
                }
            }
            val finalMeters = totalMeters.toLong()
            val existing = tripDao.getTripById(tripId)
            if (existing != null) {
                tripDao.updateTrip(existing.copy(distanceMeters = finalMeters, updatedAt = System.currentTimeMillis()))
            }
            finalMeters
        }
    }

    override suspend fun deleteTrip(tripId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            tripDao.deleteTripById(tripId)
        }
    }

    override fun getStopsByTrip(tripId: String): Flow<List<Stop>> {
        return stopDao.getStopsByTripFlow(tripId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun addStop(stop: Stop): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            stopDao.insertStop(stop.toEntity())
        }
    }

    override suspend fun updateStop(stop: Stop): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            stopDao.updateStop(stop.toEntity())
        }
    }

    override suspend fun deleteStop(stopId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            stopDao.deleteStopById(stopId)
        }
    }

    override fun getFuelEntriesByTrip(tripId: String): Flow<List<FuelEntry>> {
        return fuelEntryDao.getFuelEntriesByTripFlow(tripId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun addFuelEntry(entry: FuelEntry): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            fuelEntryDao.insertFuelEntry(entry.toEntity())
        }
    }

    override suspend fun updateFuelEntry(entry: FuelEntry): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            fuelEntryDao.updateFuelEntry(entry.toEntity())
        }
    }

    override suspend fun deleteFuelEntry(entryId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            fuelEntryDao.deleteFuelEntryById(entryId)
        }
    }

    override fun getExpensesByTrip(tripId: String): Flow<List<Expense>> {
        return expenseDao.getExpensesByTripFlow(tripId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun addExpense(expense: Expense): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            expenseDao.insertExpense(expense.toEntity())
        }
    }

    override suspend fun updateExpense(expense: Expense): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            expenseDao.updateExpense(expense.toEntity())
        }
    }

    override suspend fun deleteExpense(expenseId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            expenseDao.deleteExpenseById(expenseId)
        }
    }

    override fun getTrackPointsByTrip(tripId: String): Flow<List<TrackPoint>> {
        return trackPointDao.getTrackPointsByTripFlow(tripId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override suspend fun recordTrackPoints(points: List<TrackPoint>): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            if (points.isNotEmpty()) {
                trackPointDao.insertTrackPoints(points.map { it.toEntity() })
            }
        }
    }

    // Mapping extensions
    private fun TripEntity.toDomain() = Trip(
        id = id,
        name = name,
        originName = originName,
        destinationName = destinationName,
        startedAt = startedAt,
        endedAt = endedAt,
        status = status,
        distanceMeters = distanceMeters,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun StopEntity.toDomain() = Stop(
        id = id,
        tripId = tripId,
        type = type,
        arrivedAt = arrivedAt,
        departedAt = departedAt,
        latitude = latitude,
        longitude = longitude,
        name = name,
        notes = notes
    )

    private fun Stop.toEntity() = StopEntity(
        id = id,
        tripId = tripId,
        type = type,
        arrivedAt = arrivedAt,
        departedAt = departedAt,
        latitude = latitude,
        longitude = longitude,
        name = name,
        notes = notes
    )

    private fun FuelEntryEntity.toDomain() = FuelEntry(
        id = id,
        tripId = tripId,
        stopId = stopId,
        occurredAt = occurredAt,
        amountMinorUnits = amountMinorUnits,
        currencyCode = currencyCode,
        liters = liters,
        odometerKm = odometerKm,
        stationName = stationName,
        isFullTank = isFullTank,
        notes = notes
    )

    private fun FuelEntry.toEntity() = FuelEntryEntity(
        id = id,
        tripId = tripId,
        stopId = stopId,
        occurredAt = occurredAt,
        amountMinorUnits = amountMinorUnits,
        currencyCode = currencyCode,
        liters = liters,
        odometerKm = odometerKm,
        stationName = stationName,
        isFullTank = isFullTank,
        notes = notes
    )

    private fun ExpenseEntity.toDomain() = Expense(
        id = id,
        tripId = tripId,
        stopId = stopId,
        occurredAt = occurredAt,
        amountMinorUnits = amountMinorUnits,
        currencyCode = currencyCode,
        category = category,
        description = description
    )

    private fun Expense.toEntity() = ExpenseEntity(
        id = id,
        tripId = tripId,
        stopId = stopId,
        occurredAt = occurredAt,
        amountMinorUnits = amountMinorUnits,
        currencyCode = currencyCode,
        category = category,
        description = description
    )

    private fun TrackPointEntity.toDomain() = TrackPoint(
        id = id,
        tripId = tripId,
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters,
        speedMps = speedMps,
        altitudeMeters = altitudeMeters
    )

    private fun TrackPoint.toEntity() = TrackPointEntity(
        id = id,
        tripId = tripId,
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters,
        speedMps = speedMps,
        altitudeMeters = altitudeMeters
    )

    private fun TripEventEntity.toDomain() = TripEvent(
        id = id,
        tripId = tripId,
        timestamp = timestamp,
        eventType = eventType,
        latitude = latitude,
        longitude = longitude,
        details = details
    )
}
