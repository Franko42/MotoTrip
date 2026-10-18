package com.mototrip.domain.model

import com.mototrip.data.local.entity.StopType
import com.mototrip.data.local.entity.TripStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class TripModelsTest {

    @Test
    fun validTrip_createdSuccessfully() {
        val now = 1700000000000L
        val trip = Trip(
            id = "trip-1",
            name = "Viaje a las Sierras",
            originName = "Córdoba",
            destinationName = "Villa General Belgrano",
            startedAt = now,
            status = TripStatus.ACTIVE,
            createdAt = now,
            updatedAt = now
        )
        assertEquals("trip-1", trip.id)
        assertEquals(TripStatus.ACTIVE, trip.status)
        assertEquals(0L, trip.distanceMeters)
    }

    @Test
    fun fuelEntry_withValidValues_createdSuccessfully() {
        val entry = FuelEntry(
            id = "fuel-1",
            tripId = "trip-1",
            stopId = "stop-1",
            occurredAt = 1700000100000L,
            amountMinorUnits = 1500000L, // 15,000.00
            currencyCode = "ARS",
            liters = 15.5,
            odometerKm = 12500.0,
            isFullTank = true
        )
        assertEquals(1500000L, entry.amountMinorUnits)
        assertEquals(15.5, entry.liters)
    }

    @Test
    fun fuelEntry_withNegativeAmount_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            FuelEntry(
                id = "fuel-2",
                tripId = "trip-1",
                occurredAt = 1700000100000L,
                amountMinorUnits = -100L
            )
        }
    }

    @Test
    fun fuelEntry_withZeroOrNegativeLiters_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            FuelEntry(
                id = "fuel-3",
                tripId = "trip-1",
                occurredAt = 1700000100000L,
                amountMinorUnits = 1000L,
                liters = 0.0
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            FuelEntry(
                id = "fuel-4",
                tripId = "trip-1",
                occurredAt = 1700000100000L,
                amountMinorUnits = 1000L,
                liters = -5.0
            )
        }
    }

    @Test
    fun expense_withNegativeAmount_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            Expense(
                id = "exp-1",
                tripId = "trip-1",
                occurredAt = 1700000200000L,
                amountMinorUnits = -500L,
                category = "Peaje"
            )
        }
    }

    @Test
    fun fuelEntry_withAmountAndWithoutLiters_createdSuccessfully() {
        val entry = FuelEntry(
            id = "fuel-only-amount",
            tripId = "trip-1",
            occurredAt = 1700000100000L,
            amountMinorUnits = 500000L, // 5000 ARS
            currencyCode = "ARS",
            notes = "Carga rápida sin recordar los litros"
        )
        assertEquals(500000L, entry.amountMinorUnits)
        assertEquals(null, entry.liters)
        assertEquals("Carga rápida sin recordar los litros", entry.notes)
    }

    @Test
    fun motorcycleConfig_boxerBM150_defaultsMatchSpecification() {
        val moto = MotorcycleConfig()
        assertEquals("Bajaj Boxer BM150", moto.modelName)
        assertEquals(11.0, moto.tankCapacityLiters, 0.001)
        assertEquals(2.5, moto.reserveLiters, 0.001)
        assertEquals(1.90, moto.referenceConsumptionL100, 0.001)
        // Reference efficiency: 100 / 1.90 ≈ 52.63 km/L
        assertEquals(52.63, moto.referenceEfficiencyKmL, 0.1)
        assertEquals(false, moto.isCapacityConfirmed)
        assertEquals(false, moto.isReserveConfirmed)
    }
}
