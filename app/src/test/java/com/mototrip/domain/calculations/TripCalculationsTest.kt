package com.mototrip.domain.calculations

import com.mototrip.data.local.entity.TripEventType
import com.mototrip.domain.model.Expense
import com.mototrip.domain.model.FuelEntry
import com.mototrip.domain.model.TripEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TripCalculationsTest {

    @Test
    fun calculateExpensesByCurrency_groupsSeparately() {
        val fuel1 = FuelEntry("f1", "t1", null, 1000L, 500000L, "ARS", 10.0) // 5000 ARS
        val fuel2 = FuelEntry("f2", "t1", null, 2000L, 2000L, "USD", 5.0)   // 20 USD
        val exp1 = Expense("e1", "t1", null, 1500L, 100000L, "ARS", "Peaje") // 1000 ARS
        val exp2 = Expense("e2", "t1", null, 2500L, 1000L, "USD", "Snack")  // 10 USD

        val result = TripCalculations.calculateExpensesByCurrency(listOf(fuel1, fuel2), listOf(exp1, exp2))
        assertEquals(2, result.size)

        val ars = result.first { it.currencyCode == "ARS" }
        assertEquals(500000L, ars.fuelAmountMinorUnits)
        assertEquals(100000L, ars.otherExpensesMinorUnits)
        assertEquals(600000L, ars.totalAmountMinorUnits)

        val usd = result.first { it.currencyCode == "USD" }
        assertEquals(2000L, usd.fuelAmountMinorUnits)
        assertEquals(1000L, usd.otherExpensesMinorUnits)
        assertEquals(3000L, usd.totalAmountMinorUnits)
    }

    @Test
    fun calculatePricePerLiter_computesAccurately() {
        // 10000 ARS (1000000 minor units) for 10 liters = 1000 ARS/L
        val price = TripCalculations.calculatePricePerLiter(1000000L, 10.0)
        assertNotNull(price)
        assertEquals(1000.0, price!!, 0.001)

        // Null or non-positive liters
        assertNull(TripCalculations.calculatePricePerLiter(1000000L, null))
        assertNull(TripCalculations.calculatePricePerLiter(1000000L, 0.0))
        assertNull(TripCalculations.calculatePricePerLiter(1000000L, -5.0))
    }

    @Test
    fun calculateCostPerKm_computesAccurately() {
        // 2000 ARS (200000 minor units) for 50000 meters (50 km) = 40 ARS/km
        val cost = TripCalculations.calculateCostPerKm(200000L, 50000L)
        assertNotNull(cost)
        assertEquals(40.0, cost!!, 0.001)

        // 0 or negative distance
        assertNull(TripCalculations.calculateCostPerKm(200000L, 0L))
        assertNull(TripCalculations.calculateCostPerKm(200000L, -100L))
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

    @Test
    fun calculateFuelConsumptionBetweenFullTanks_computesCorrectly() {
        // Full tank 1 at 10,000 km
        val entry1 = FuelEntry("f1", "t1", null, 1000L, 1000000L, "ARS", 12.0, 10000.0, null, true)
        // Full tank 2 at 10,300 km, took 15 liters to fill
        val entry2 = FuelEntry("f2", "t1", null, 2000L, 1500000L, "ARS", 15.0, 10300.0, null, true)

        // Distance = 300 km, Liters = 15.0 => (15 / 300) * 100 = 5.0 L / 100 km
        val consumption = TripCalculations.calculateFuelConsumptionBetweenFullTanks(entry1, entry2)
        assertNotNull(consumption)
        assertEquals(5.0, consumption!!, 0.001)
    }

    @Test
    fun calculateFuelConsumptionBetweenFullTanks_returnsNullIfNotBothFull() {
        val entry1 = FuelEntry("f1", "t1", null, 1000L, 1000000L, "ARS", 12.0, 10000.0, null, false)
        val entry2 = FuelEntry("f2", "t1", null, 2000L, 1500000L, "ARS", 15.0, 10300.0, null, true)

        assertNull(TripCalculations.calculateFuelConsumptionBetweenFullTanks(entry1, entry2))
    }
}
