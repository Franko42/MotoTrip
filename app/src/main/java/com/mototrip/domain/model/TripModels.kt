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
    val isFullTank: Boolean = false,
    val notes: String? = null
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

/**
 * Encapsulates calculation results explicitly acknowledging data quality.
 * Distinguishes verified/confirmed results, approximations, and unknown values with reasons.
 */
sealed class CalculationResult<out T> {
    data class Confirmed<T>(val value: T, val details: String? = null) : CalculationResult<T>()
    data class Estimated<T>(val value: T, val limitationReason: String) : CalculationResult<T>()
    data class Unknown(val reason: String) : CalculationResult<Nothing>()

    val valueOrNull: T?
        get() = when (this) {
            is Confirmed -> value
            is Estimated -> value
            is Unknown -> null
        }
}

enum class ConsumptionSource {
    MANUAL_REFERENCE,
    TRIP_MEASURED,
    HISTORICAL_FULL_TANKS
}

/**
 * Motorcycle configuration with editable technical parameters.
 * Provisional defaults correspond to Bajaj Boxer BM150.
 */
data class MotorcycleConfig(
    val modelName: String = "Bajaj Boxer BM150",
    val tankCapacityLiters: Double = 11.0, // Provisional, editable
    val reserveLiters: Double = 2.5,       // Provisional, editable
    val referenceConsumptionL100: Double = 1.90, // Provisional informado (~4 L en 211 km)
    val isCapacityConfirmed: Boolean = false,
    val isReserveConfirmed: Boolean = false,
    val consumptionSource: ConsumptionSource = ConsumptionSource.MANUAL_REFERENCE
) {
    val referenceEfficiencyKmL: Double
        get() = if (referenceConsumptionL100 > 0.0) 100.0 / referenceConsumptionL100 else 0.0
}

data class SpeedSample(
    val timestamp: Long,
    val speedKmh: Double
)

data class GpsSpeedStatistics(
    val averageMovingSpeedKmh: CalculationResult<Double>,
    val averageTotalSpeedKmh: CalculationResult<Double>,
    val maxValidSpeedKmh: CalculationResult<Double>,
    val speedHistory: List<SpeedSample>,
    val validPointsCount: Int,
    val discardedPointsCount: Int,
    val isCoverageSufficient: Boolean,
    val coverageNotes: String? = null
)

data class TripTimeStatistics(
    val totalDurationMillis: Long,
    val movingDurationMillis: Long,
    val confirmedStoppedDurationMillis: Long,
    val manualStopsDurationMillis: Long,
    val untrackedOrInterruptedDurationMillis: Long
)

/**
 * Comprehensive economic report separating actual disbursements from estimated consumed costs.
 */
data class TripEconomicReport(
    val currencyCode: String,
    val totalPaidFuelMinorUnits: Long,
    val totalOtherExpensesMinorUnits: Long,
    val totalDisbursedMinorUnits: Long,
    val estimatedFuelConsumedCostMinorUnits: CalculationResult<Long>,
    val estimatedTotalTripCostMinorUnits: CalculationResult<Long>,
    val notes: String? = null
)

