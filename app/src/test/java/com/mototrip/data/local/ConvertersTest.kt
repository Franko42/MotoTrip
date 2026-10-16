package com.mototrip.data.local

import com.mototrip.data.local.converter.Converters
import com.mototrip.data.local.entity.StopType
import com.mototrip.data.local.entity.TripEventType
import com.mototrip.data.local.entity.TripStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun tripStatus_convertsBidirectionally() {
        TripStatus.entries.forEach { status ->
            val str = converters.fromTripStatus(status)
            assertEquals(status.name, str)
            val back = converters.toTripStatus(str)
            assertEquals(status, back)
        }
    }

    @Test
    fun tripStatus_unknownValue_defaultsToActive() {
        val result = converters.toTripStatus("UNKNOWN_VALUE")
        assertEquals(TripStatus.ACTIVE, result)
    }

    @Test
    fun stopType_convertsBidirectionally() {
        StopType.entries.forEach { type ->
            val str = converters.fromStopType(type)
            assertEquals(type.name, str)
            val back = converters.toStopType(str)
            assertEquals(type, back)
        }
    }

    @Test
    fun stopType_unknownValue_defaultsToOther() {
        val result = converters.toStopType("UNKNOWN_TYPE")
        assertEquals(StopType.OTHER, result)
    }

    @Test
    fun tripEventType_convertsBidirectionally() {
        TripEventType.entries.forEach { event ->
            val str = converters.fromTripEventType(event)
            assertEquals(event.name, str)
            val back = converters.toTripEventType(str)
            assertEquals(event, back)
        }
    }

    @Test
    fun tripEventType_unknownValue_defaultsToStarted() {
        val result = converters.toTripEventType("UNKNOWN_EVENT")
        assertEquals(TripEventType.STARTED, result)
    }
}
