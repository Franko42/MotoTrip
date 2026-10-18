package com.mototrip.domain.calculations

import com.mototrip.data.local.entity.StopType
import com.mototrip.data.local.entity.TripEventType
import com.mototrip.domain.model.CalculationResult
import com.mototrip.domain.model.Expense
import com.mototrip.domain.model.FuelEntry
import com.mototrip.domain.model.Stop
import com.mototrip.domain.model.TrackPoint
import com.mototrip.domain.model.TripEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripCalculationsTest {

    // ==========================================
    // 1. FECHAS Y PARADAS
    // ==========================================

    @Test
    fun calculateStopDurationMillis_validArrivalAndDeparture_calculatesExactDuration() {
        val arrivedAt = 1700000000000L
        val departedAt = 1700001800000L // 1800 seconds = 30 minutes
        val duration = TripCalculations.calculateStopDurationMillis(arrivedAt, departedAt)
        assertNotNull(duration)
        assertEquals(1800000L, duration!!)
    }

    @Test
    fun calculateStopDurationMillis_nullDeparture_returnsNullUnknownDuration() {
        val arrivedAt = 1700000000000L
        val duration = TripCalculations.calculateStopDurationMillis(arrivedAt, null)
        assertNull("Stop without registered departure must have unknown duration", duration)
    }

    @Test
    fun calculateStopDurationMillis_departureBeforeArrival_returnsNull() {
        val arrivedAt = 1700002000000L
        val departedAt = 1700001000000L // invalid: departure before arrival
        val duration = TripCalculations.calculateStopDurationMillis(arrivedAt, departedAt)
        assertNull("Negative stop duration must be rejected", duration)
    }

    @Test
    fun calculateActiveDurationMillis_discountsPauses() {
        val start = 10000L
        val end = 25000L // gross duration 15000 ms

        val events = listOf(
            TripEvent("ev1", "t1", 10000L, TripEventType.STARTED),
            TripEvent("ev2", "t1", 12000L, TripEventType.PAUSED),
            TripEvent("ev3", "t1", 15000L, TripEventType.RESUMED), // paused 3000 ms
            TripEvent("ev4", "t1", 18000L, TripEventType.PAUSED),
            TripEvent("ev5", "t1", 20000L, TripEventType.RESUMED), // paused 2000 ms
            TripEvent("ev6", "t1", 25000L, TripEventType.COMPLETED)
        )

        val duration = TripCalculations.calculateActiveDurationMillis(start, end, events)
        // 15000 - 3000 - 2000 = 10000 ms
        assertEquals(10000L, duration)
    }

    // ==========================================
    // 2. COMBUSTIBLE: REGISTRO Y CÁLCULOS
    // ==========================================

    @Test
    fun calculatePricePerLiter_withAmountAndWithoutLiters_returnsNull() {
        // Amount paid 10,000 ARS, but liters unknown
        val price = TripCalculations.calculatePricePerLiter(1000000L, null)
        assertNull(price)
    }

    @Test
    fun calculatePricePerLiter_withZeroOrNegativeLiters_returnsNull() {
        assertNull(TripCalculations.calculatePricePerLiter(1000000L, 0.0))
        assertNull(TripCalculations.calculatePricePerLiter(1000000L, -5.0))
        assertNull(TripCalculations.calculatePricePerLiter(1000000L, Double.NaN))
        assertNull(TripCalculations.calculatePricePerLiter(1000000L, Double.POSITIVE_INFINITY))
    }

    @Test
    fun calculatePricePerLiter_validInputs_computesCorrectly() {
        // 10,000 ARS (1000000 minor units) for 10 liters = 1000 ARS/L
        val price = TripCalculations.calculatePricePerLiter(1000000L, 10.0)
        assertNotNull(price)
        assertEquals(1000.0, price!!, 0.001)
    }

    @Test
    fun calculateTripFuelConsumption_consecutiveFullTanks_returnsConfirmed() {
        val entry1 = FuelEntry("f1", "t1", null, 1000L, 1000000L, "ARS", 10.0, 5000.0, "YPF", true)
        val entry2 = FuelEntry("f2", "t1", null, 2000L, 1500000L, "ARS", 12.0, 5300.0, "Shell", true)

        // Delta distance = 300 km, Liters = 12.0 => 4.0 L / 100 km
        val result = TripCalculations.calculateTripFuelConsumption(300_000L, listOf(entry1, entry2))
        assertTrue(result is CalculationResult.Confirmed)
        val confirmed = result as CalculationResult.Confirmed
        assertEquals(4.0, confirmed.value, 0.001)
    }

    @Test
    fun calculateTripFuelConsumption_withoutConsecutiveFullTanks_returnsEstimated() {
        // Partial load without full tank
        val entry = FuelEntry("f1", "t1", null, 1000L, 500000L, "ARS", 4.0, null, "Puma", false)

        // 4 liters in 200 km => 2.0 L / 100 km (estimated)
        val result = TripCalculations.calculateTripFuelConsumption(200_000L, listOf(entry))
        assertTrue(result is CalculationResult.Estimated)
        val estimated = result as CalculationResult.Estimated
        assertEquals(2.0, estimated.value, 0.001)
        assertTrue(estimated.limitationReason.contains("Estimación"))
    }

    @Test
    fun calculateTripFuelConsumption_insufficientData_returnsUnknown() {
        // Distance 0
        val resZeroDist = TripCalculations.calculateTripFuelConsumption(0L, emptyList())
        assertTrue(resZeroDist is CalculationResult.Unknown)

        // Fuel entry with amount but no liters
        val entryWithoutLiters = FuelEntry("f1", "t1", null, 1000L, 500000L, "ARS", null, null, null, false)
        val resNoLiters = TripCalculations.calculateTripFuelConsumption(100_000L, listOf(entryWithoutLiters))
        assertTrue(resNoLiters is CalculationResult.Unknown)
    }

    @Test
    fun calculateTripEfficiencyKmL_computesCorrectly() {
        val entry = FuelEntry("f1", "t1", null, 1000L, 500000L, "ARS", 4.0, null, "Puma", false)
        // 200 km / 4 L = 50 km/L
        val result = TripCalculations.calculateTripEfficiencyKmL(200_000L, listOf(entry))
        assertTrue(result is CalculationResult.Estimated)
        assertEquals(50.0, (result as CalculationResult.Estimated).value, 0.001)
    }

    // ==========================================
    // 3. AUTONOMÍA Y CONFIGURACIÓN DE MOTO
    // ==========================================

    @Test
    fun calculateTheoreticalRangeKm_computesFromFullTankCapacity() {
        // Boxer BM150: 11.0 L tank, 50 km/L efficiency => 550 km range
        val range = TripCalculations.calculateTheoreticalRangeKm(11.0, 50.0)
        assertTrue(range is CalculationResult.Confirmed)
        assertEquals(550.0, (range as CalculationResult.Confirmed).value, 0.001)
    }

    @Test
    fun calculateRangeToReserveKm_computesUntilReserveThreshold() {
        // Boxer BM150: 11.0 L tank, 2.5 L reserve => 8.5 L usable before reserve * 50 km/L = 425 km
        val range = TripCalculations.calculateRangeToReserveKm(11.0, 2.5, 50.0)
        assertTrue(range is CalculationResult.Confirmed)
        assertEquals(425.0, (range as CalculationResult.Confirmed).value, 0.001)
    }

    @Test
    fun calculateCurrentEstimatedRangeKm_nullRemainingFuel_returnsUnknown() {
        val range = TripCalculations.calculateCurrentEstimatedRangeKm(null, 50.0)
        assertTrue(range is CalculationResult.Unknown)
        assertTrue((range as CalculationResult.Unknown).reason.contains("desconocido"))
    }

    @Test
    fun calculateCurrentEstimatedRangeKm_knownRemainingFuel_returnsEstimated() {
        // 3.5 liters remaining in tank * 50 km/L = 175 km
        val range = TripCalculations.calculateCurrentEstimatedRangeKm(3.5, 50.0)
        assertTrue(range is CalculationResult.Estimated)
        assertEquals(175.0, (range as CalculationResult.Estimated).value, 0.001)
    }

    // ==========================================
    // 4. SEPARAR DESEMBOLSOS Y COSTO REAL (SECCIÓN 5)
    // ==========================================

    @Test
    fun calculateEconomicReports_strictlySeparatesDisbursementFromConsumedCost() {
        // Trip: 200 km
        // Fuel load 1: 10,000 ARS (1000000 minor units), 10 liters (tank filled at end of trip, price = 1000 ARS/L)
        // Motorcycle reference consumption: 2.0 L / 100 km (i.e. motorcycle only consumed ~4 liters during the 200 km trip!)
        // Other expense: 2,000 ARS toll
        val fuel = FuelEntry("f1", "t1", null, 1000L, 1000000L, "ARS", 10.0, null, "YPF", true)
        val exp = Expense("e1", "t1", null, 1500L, 200000L, "ARS", "Peaje")

        val reports = TripCalculations.calculateEconomicReports(
            fuelEntries = listOf(fuel),
            expenses = listOf(exp),
            distanceMeters = 200_000L,
            referenceConsumptionL100 = 2.0
        )

        assertEquals(1, reports.size)
        val report = reports.first()

        // 1. Total disbursed in fuel load: 10,000 ARS (1000000 minor units)
        assertEquals(1000000L, report.totalPaidFuelMinorUnits)
        // 2. Total other expenses: 2,000 ARS (200000 minor units)
        assertEquals(200000L, report.totalOtherExpensesMinorUnits)
        // 3. Total cash outlay: 12,000 ARS (1200000 minor units)
        assertEquals(1200000L, report.totalDisbursedMinorUnits)

        // 4. Estimated fuel consumed cost:
        // Liters consumed = (200 km * 2.0 L/100km) / 100 = 4.0 L
        // Price per liter = 1000 ARS/L => Cost = 4.0 * 1000 = 4,000 ARS (400000 minor units)
        assertTrue(report.estimatedFuelConsumedCostMinorUnits is CalculationResult.Estimated)
        val fuelCost = (report.estimatedFuelConsumedCostMinorUnits as CalculationResult.Estimated).value
        assertEquals(400000L, fuelCost)

        // 5. Estimated total economic cost of the trip: 4,000 ARS (fuel) + 2,000 ARS (other) = 6,000 ARS
        assertTrue(report.estimatedTotalTripCostMinorUnits is CalculationResult.Estimated)
        val totalCost = (report.estimatedTotalTripCostMinorUnits as CalculationResult.Estimated).value
        assertEquals(600000L, totalCost)

        // Assert that cash outlay (12,000 ARS) is strictly different from estimated economic cost (6,000 ARS)
        assertTrue(report.totalDisbursedMinorUnits > totalCost)
    }

    // ==========================================
    // 5. GPS Y ESTADÍSTICAS DE VELOCIDAD
    // ==========================================

    @Test
    fun calculateGpsSpeedStatistics_accuratelyFiltersAnomaliesAndSeparatesAverages() {
        val start = 10_000L
        val end = 60_000L // 50-second continuous tracking segment without signal gaps

        val p1 = TrackPoint("p1", "t1", 10_000L, -34.6000, -58.3800, 5.0f, 10.0f, 20.0) // 10 m/s = 36 km/h
        val p2 = TrackPoint("p2", "t1", 25_000L, -34.6025, -58.3800, 8.0f, 20.0f, 20.0) // 20 m/s = 72 km/h (delta = 15s < 90s)
        // Inaccurate point (> 50m accuracy) -> should be discarded
        val pBad = TrackPoint("pBad", "t1", 35_000L, -34.6030, -58.3800, 80.0f, 25.0f, 20.0)
        // Teleport spike -> impossible speed jump (> 70 m/s) -> should be discarded
        val pJump = TrackPoint("pJump", "t1", 40_000L, -30.0000, -58.3800, 5.0f, 200.0f, 20.0)
        val p3 = TrackPoint("p3", "t1", 60_000L, -34.6060, -58.3800, 10.0f, 15.0f, 20.0) // (delta from p2 = 35s < 90s)

        val stats = TripCalculations.calculateGpsSpeedStatistics(listOf(p1, p2, pBad, pJump, p3), start, end)

        assertEquals(3, stats.validPointsCount)
        assertEquals(2, stats.discardedPointsCount)

        // Max speed must be 20 m/s * 3.6 = 72 km/h (rejecting the 200 m/s spike)
        assertTrue(stats.maxValidSpeedKmh is CalculationResult.Confirmed)
        assertEquals(72.0, (stats.maxValidSpeedKmh as CalculationResult.Confirmed).value, 0.5)

        // Moving speed vs total speed
        assertTrue(stats.averageMovingSpeedKmh is CalculationResult.Confirmed)
        assertTrue(stats.averageTotalSpeedKmh is CalculationResult.Confirmed)
        assertTrue(stats.isCoverageSufficient)
    }

    @Test
    fun calculateGpsSpeedStatistics_whenSignalGapsDetected_marksMaxSpeedEstimatedAndReportsNotes() {
        val start = 1000L
        val end = 1000L + 3600_000L // 1 hour trip

        // Large time gap (1800s > 90s threshold) between p1 and p2 represents a tracking/signal interruption
        val p1 = TrackPoint("p1", "t1", 1000L, -34.6037, -58.3816, 5.0f, 10.0f, 20.0) // 10 m/s = 36 km/h
        val p2 = TrackPoint("p2", "t1", 1000L + 1800_000L, -34.8000, -58.3816, 8.0f, 20.0f, 20.0) // 20 m/s = 72 km/h

        val stats = TripCalculations.calculateGpsSpeedStatistics(listOf(p1, p2), start, end)

        assertEquals(2, stats.validPointsCount)
        assertEquals(0, stats.discardedPointsCount)
        assertFalse(stats.isCoverageSufficient)
        assertNotNull(stats.coverageNotes)

        // Max speed must be marked as Estimated due to signal gaps
        assertTrue(stats.maxValidSpeedKmh is CalculationResult.Estimated)
        assertEquals(72.0, (stats.maxValidSpeedKmh as CalculationResult.Estimated).value, 0.5)
    }

    // ==========================================
    // 6. TIEMPOS DEL VIAJE Y PARADAS
    // ==========================================

    @Test
    fun calculateTripTimeStatistics_computesSeparateIntervals() {
        val start = 10000L
        val end = 70000L // 60,000 ms total (60 sec)

        val stops = listOf(
            Stop("s1", "t1", StopType.REST, 20000L, 35000L, null, null, "Parador") // 15 sec stop
        )

        val events = listOf(
            TripEvent("ev1", "t1", 20000L, TripEventType.PAUSED),
            TripEvent("ev2", "t1", 35000L, TripEventType.RESUMED)
        )

        val points = listOf(
            TrackPoint("p1", "t1", 10000L, -34.6037, -58.3816, 5.0f, 15.0f), // moving
            TrackPoint("p2", "t1", 20000L, -34.6050, -58.3816, 5.0f, 15.0f), // moving for 10 sec
            TrackPoint("p3", "t1", 35000L, -34.6050, -58.3816, 5.0f, 0.0f),  // stopped
            TrackPoint("p4", "t1", 50000L, -34.6050, -58.3816, 5.0f, 0.0f),  // stopped for 15 sec
            TrackPoint("p5", "t1", 70000L, -34.6200, -58.3816, 5.0f, 20.0f)  // moving for 20 sec
        )

        val timeStats = TripCalculations.calculateTripTimeStatistics(start, end, points, stops, events)

        assertEquals(60000L, timeStats.totalDurationMillis)
        assertEquals(15000L, timeStats.manualStopsDurationMillis)
        assertTrue(timeStats.movingDurationMillis > 0)
        assertTrue(timeStats.confirmedStoppedDurationMillis > 0)
    }
}
