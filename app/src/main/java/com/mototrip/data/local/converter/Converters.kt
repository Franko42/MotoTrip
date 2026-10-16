package com.mototrip.data.local.converter

import androidx.room.TypeConverter
import com.mototrip.data.local.entity.StopType
import com.mototrip.data.local.entity.TripEventType
import com.mototrip.data.local.entity.TripStatus

class Converters {
    @TypeConverter
    fun fromTripStatus(status: TripStatus): String = status.name

    @TypeConverter
    fun toTripStatus(value: String): TripStatus = runCatching {
        TripStatus.valueOf(value)
    }.getOrDefault(TripStatus.ACTIVE)

    @TypeConverter
    fun fromStopType(type: StopType): String = type.name

    @TypeConverter
    fun toStopType(value: String): StopType = runCatching {
        StopType.valueOf(value)
    }.getOrDefault(StopType.OTHER)

    @TypeConverter
    fun fromTripEventType(eventType: TripEventType): String = eventType.name

    @TypeConverter
    fun toTripEventType(value: String): TripEventType = runCatching {
        TripEventType.valueOf(value)
    }.getOrDefault(TripEventType.STARTED)
}
