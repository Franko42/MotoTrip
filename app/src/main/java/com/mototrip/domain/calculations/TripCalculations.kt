package com.mototrip.domain.calculations

import com.mototrip.data.local.entity.TripEventType
import com.mototrip.domain.model.Expense
import com.mototrip.domain.model.FuelEntry
import com.mototrip.domain.model.TripEvent

data class MoneyTotal(
    val currencyCode: String,
    val fuelAmountMinorUnits: Long,
    val otherExpensesMinorUnits: Long,
    val totalAmountMinorUnits: Long
)

object TripCalculations {

    /**
     * Group expenses and fuel entries by currency code.
     * Prevents adding together amounts with different currencies.
     */
    fun calculateExpensesByCurrency(
        fuelEntries: List<FuelEntry>,
        expenses: List<Expense>
    ): List<MoneyTotal> {
        val currencies = (fuelEntries.map { it.currencyCode } + expenses.map { it.currencyCode }).toSet()

        return currencies.map { currency ->
            val fuelTotal = fuelEntries
                .filter { it.currencyCode == currency }
                .sumOf { it.amountMinorUnits }

            val otherTotal = expenses
                .filter { it.currencyCode == currency }
                .sumOf { it.amountMinorUnits }

            MoneyTotal(
                currencyCode = currency,
                fuelAmountMinorUnits = fuelTotal,
                otherExpensesMinorUnits = otherTotal,
                totalAmountMinorUnits = fuelTotal + otherTotal
            )
        }
    }

    /**
     * Calculates price per liter.
     * Returns null if liters is null or non-positive.
     */
    fun calculatePricePerLiter(amountMinorUnits: Long, liters: Double?): Double? {
        if (liters == null || liters <= 0.0 || amountMinorUnits < 0) return null
        return (amountMinorUnits.toDouble() / 100.0) / liters
    }

    /**
     * Calculates cost per kilometer.
     * Returns null if distance is 0 or less.
     */
    fun calculateCostPerKm(totalAmountMinorUnits: Long, distanceMeters: Long): Double? {
        if (distanceMeters <= 0 || totalAmountMinorUnits < 0) return null
        val distanceKm = distanceMeters.toDouble() / 1000.0
        val amount = totalAmountMinorUnits.toDouble() / 100.0
        return amount / distanceKm
    }

    /**
     * Calculates active duration (excluding paused intervals) from startedAt to endedAt/now.
     */
    fun calculateActiveDurationMillis(
        startedAt: Long,
        endedAt: Long?,
        events: List<TripEvent>,
        currentTimestamp: Long = System.currentTimeMillis()
    ): Long {
        val endTime = endedAt ?: currentTimestamp
        if (endTime <= startedAt) return 0L

        var totalPausedMillis = 0L
        var pauseStart: Long? = null

        // Events ordered by timestamp
        val sortedEvents = events.sortedBy { it.timestamp }

        for (event in sortedEvents) {
            when (event.eventType) {
                TripEventType.PAUSED -> {
                    if (pauseStart == null && event.timestamp in startedAt..endTime) {
                        pauseStart = event.timestamp
                    }
                }
                TripEventType.RESUMED, TripEventType.COMPLETED -> {
                    if (pauseStart != null) {
                        val pauseEnd = minOf(event.timestamp, endTime)
                        if (pauseEnd >= pauseStart) {
                            totalPausedMillis += (pauseEnd - pauseStart)
                        }
                        pauseStart = null
                    }
                }
                else -> {}
            }
        }

        // If currently still paused
        if (pauseStart != null && pauseStart <= endTime) {
            totalPausedMillis += (endTime - pauseStart)
        }

        val totalGross = endTime - startedAt
        return maxOf(0L, totalGross - totalPausedMillis)
    }

    /**
     * Calculates fuel consumption in L/100km between two full-tank fuel entries with odometers.
     * Returns null if condition is not met (insufficient full tanks or missing odometers).
     */
    fun calculateFuelConsumptionBetweenFullTanks(
        earlierEntry: FuelEntry,
        laterEntry: FuelEntry
    ): Double? {
        if (!earlierEntry.isFullTank || !laterEntry.isFullTank) return null
        val odoStart = earlierEntry.odometerKm ?: return null
        val odoEnd = laterEntry.odometerKm ?: return null
        val liters = laterEntry.liters ?: return null

        val deltaDistance = odoEnd - odoStart
        if (deltaDistance <= 0.0 || liters <= 0.0) return null

        return (liters / deltaDistance) * 100.0
    }
}
