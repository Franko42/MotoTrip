package com.mototrip.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trips",
    indices = [
        Index(value = ["startedAt"]),
        Index(value = ["status"])
    ]
)
data class TripEntity(
    @PrimaryKey
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
