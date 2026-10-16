package com.mototrip.domain

import com.mototrip.data.local.entity.TripStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripStateMachineTest {

    private fun canTransition(from: TripStatus, to: TripStatus): Boolean {
        return when (from) {
            TripStatus.ACTIVE -> to == TripStatus.PAUSED || to == TripStatus.COMPLETED
            TripStatus.PAUSED -> to == TripStatus.ACTIVE || to == TripStatus.COMPLETED
            TripStatus.COMPLETED -> false
        }
    }

    @Test
    fun activeTrip_transitions() {
        assertTrue(canTransition(TripStatus.ACTIVE, TripStatus.PAUSED))
        assertTrue(canTransition(TripStatus.ACTIVE, TripStatus.COMPLETED))
        assertFalse(canTransition(TripStatus.ACTIVE, TripStatus.ACTIVE))
    }

    @Test
    fun pausedTrip_transitions() {
        assertTrue(canTransition(TripStatus.PAUSED, TripStatus.ACTIVE))
        assertTrue(canTransition(TripStatus.PAUSED, TripStatus.COMPLETED))
        assertFalse(canTransition(TripStatus.PAUSED, TripStatus.PAUSED))
    }

    @Test
    fun completedTrip_cannotTransition() {
        assertFalse(canTransition(TripStatus.COMPLETED, TripStatus.ACTIVE))
        assertFalse(canTransition(TripStatus.COMPLETED, TripStatus.PAUSED))
        assertFalse(canTransition(TripStatus.COMPLETED, TripStatus.COMPLETED))
    }
}
