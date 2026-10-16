package com.mototrip.core.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationFilterTest {

    @Test
    fun isPointAccurate_validatesProperly() {
        // Valid point
        val valid = RawLocationPoint(1000L, -34.6037, -58.3816, 10.0f, 15.0f, 25.0)
        assertTrue(LocationFilter.isPointAccurate(valid))

        // Inaccurate point (> 50m)
        val inaccurate = valid.copy(accuracyMeters = 75.0f)
        assertFalse(LocationFilter.isPointAccurate(inaccurate))

        // Null accuracy
        val nullAcc = valid.copy(accuracyMeters = null)
        assertFalse(LocationFilter.isPointAccurate(nullAcc))

        // Out of bounds coordinates
        val invalidLat = valid.copy(latitude = 95.0)
        assertFalse(LocationFilter.isPointAccurate(invalidLat))

        val invalidLon = valid.copy(longitude = -185.0)
        assertFalse(LocationFilter.isPointAccurate(invalidLon))
    }

    @Test
    fun evaluateConsecutivePoint_firstPoint_acceptedWithZeroDelta() {
        val p1 = RawLocationPoint(1000L, -34.6037, -58.3816, 12.0f, 10.0f, 20.0)
        val (shouldRecord, deltaMeters) = LocationFilter.evaluateConsecutivePoint(null, p1)
        assertTrue(shouldRecord)
        assertEquals(0.0, deltaMeters, 0.001)
    }

    @Test
    fun evaluateConsecutivePoint_filtersStationaryJitter() {
        val p1 = RawLocationPoint(1000L, -34.603700, -58.381600, 5.0f, 0.0f, 20.0)
        // Shift by ~1 meter
        val p2 = RawLocationPoint(4000L, -34.603705, -58.381605, 5.0f, 0.0f, 20.0)

        val (shouldRecord, _) = LocationFilter.evaluateConsecutivePoint(p1, p2)
        assertFalse("Stationary micro-jitter should be rejected", shouldRecord)
    }

    @Test
    fun evaluateConsecutivePoint_filtersUnrealisticSpeedJump() {
        val p1 = RawLocationPoint(1000L, -34.6037, -58.3816, 5.0f, 10.0f, 20.0)
        // Moved 500 meters in 1 second (500 m/s = 1800 km/h)
        val p2 = RawLocationPoint(2000L, -34.6080, -58.3816, 5.0f, 10.0f, 20.0)

        val (shouldRecord, _) = LocationFilter.evaluateConsecutivePoint(p1, p2)
        assertFalse("Impossible speed jump should be rejected", shouldRecord)
    }

    @Test
    fun evaluateConsecutivePoint_rejectsNonIncreasingTimestamps() {
        val p1 = RawLocationPoint(5000L, -34.6037, -58.3816, 5.0f, 10.0f, 20.0)
        val p2 = RawLocationPoint(4000L, -34.6040, -58.3820, 5.0f, 10.0f, 20.0)

        val (shouldRecord, _) = LocationFilter.evaluateConsecutivePoint(p1, p2)
        assertFalse(shouldRecord)
    }

    @Test
    fun calculateHaversineDistance_matchesRealDistance() {
        // Obelisco Buenos Aires: (-34.6037, -58.3816)
        // Monumento a la Bandera Rosario: (-32.9477, -60.6288)
        // Distance is ~280 km (approx 280,000 meters)
        val distance = LocationFilter.calculateHaversineDistance(
            -34.6037, -58.3816,
            -32.9477, -60.6288
        )
        // Assert within 1% error margin
        assertEquals(280_000.0, distance, 5_000.0)
    }
}
