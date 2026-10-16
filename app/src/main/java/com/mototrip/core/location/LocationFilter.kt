package com.mototrip.core.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class RawLocationPoint(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val speedMps: Float?,
    val altitudeMeters: Double?
)

object LocationFilter {

    const val MAX_ALLOWED_ACCURACY_METERS = 50.0f
    const val MIN_DISPLACEMENT_METERS = 3.0 // Filter out stationary GPS jitter
    const val MAX_ALLOWED_SPEED_MPS = 70.0f // ~252 km/h (reject GPS teleport spikes)

    /**
     * Checks if a point has valid coordinates and reasonable accuracy.
     */
    fun isPointAccurate(point: RawLocationPoint): Boolean {
        if (point.latitude !in -90.0..90.0) return false
        if (point.longitude !in -180.0..180.0) return false
        val accuracy = point.accuracyMeters ?: return false
        return accuracy in 0.0f..MAX_ALLOWED_ACCURACY_METERS
    }

    /**
     * Determines whether [current] point should be recorded given [previous] valid point.
     * Returns a pair of (shouldRecord, deltaDistanceMeters).
     */
    fun evaluateConsecutivePoint(
        previous: RawLocationPoint?,
        current: RawLocationPoint
    ): Pair<Boolean, Double> {
        if (!isPointAccurate(current)) {
            return Pair(false, 0.0)
        }

        if (previous == null) {
            return Pair(true, 0.0)
        }

        val deltaMillis = current.timestamp - previous.timestamp
        if (deltaMillis <= 0) {
            // Duplicate or out-of-order timestamp
            return Pair(false, 0.0)
        }

        val distanceMeters = calculateHaversineDistance(
            previous.latitude, previous.longitude,
            current.latitude, current.longitude
        )

        // Jitter filter: ignore tiny movements when stationary
        if (distanceMeters < MIN_DISPLACEMENT_METERS) {
            return Pair(false, 0.0)
        }

        val deltaSeconds = deltaMillis / 1000.0
        val derivedSpeedMps = distanceMeters / deltaSeconds

        // Anomaly / jump filter
        if (derivedSpeedMps > MAX_ALLOWED_SPEED_MPS) {
            return Pair(false, 0.0)
        }

        return Pair(true, distanceMeters)
    }

    /**
     * Calculates great-circle distance between two coordinates in meters using the Haversine formula.
     */
    fun calculateHaversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371000.0 // Earth's mean radius in meters
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)

        val a = sin(latDistance / 2) * sin(latDistance / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(lonDistance / 2) * sin(lonDistance / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
