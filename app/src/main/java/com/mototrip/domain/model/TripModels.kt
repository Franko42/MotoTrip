package com.mototrip.domain.model

import com.mototrip.data.local.entity.StopType
import com.mototrip.data.local.entity.TripEventType
import com.mototrip.data.local.entity.TripStatus

data class Trip(
    val id: String,
    val name: String,
    val originName: String? = null,
    val destinationName: String? = null,
    val startedAt: Long,
    val endedAt: Long? = null,
    val status: TripStatus = TripStatus.ACTIVE,
    val distanceMeters: Long = 0L,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

data class TrackPoint(
    val id: String,
    val tripId: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val speedMps: Float? = null,
    val altitudeMeters: Double? = null
)

data class Stop(
    val id: String,
    val tripId: String,
    val type: StopType,
    val arrivedAt: Long,
    val departedAt: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val name: String? = null,
    val notes: String? = null
)

data class FuelEntry(
    val id: String,
    val tripId: String,
    val stopId: String? = null,
    val occurredAt: Long,
    val amountMinorUnits: Long,
    val currencyCode: String = "ARS",
    val liters: Double? = null,
    val odometerKm: Double? = null,
    val stationName: String? = null,
    val isFullTank: Boolean = false
) {
    init {
        require(amountMinorUnits >= 0) { "Amount cannot be negative" }
        liters?.let { require(it > 0) { "Liters must be positive when provided" } }
        odometerKm?.let { require(it >= 0) { "Odometer cannot be negative" } }
    }
}

data class Expense(
    val id: String,
    val tripId: String,
    val stopId: String? = null,
    val occurredAt: Long,
    val amountMinorUnits: Long,
    val currencyCode: String = "ARS",
    val category: String,
    val description: String? = null
) {
    init {
        require(amountMinorUnits >= 0) { "Amount cannot be negative" }
    }
}

data class TripEvent(
    val id: String,
    val tripId: String,
    val timestamp: Long,
    val eventType: TripEventType,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val details: String? = null
)

data class TripDetail(
    val trip: Trip,
    val stops: List<Stop> = emptyList(),
    val fuelEntries: List<FuelEntry> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val events: List<TripEvent> = emptyList(),
    val trackPointCount: Int = 0
)
