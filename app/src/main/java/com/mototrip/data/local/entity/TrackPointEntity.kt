package com.mototrip.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tripId", "timestamp"]),
        Index(value = ["tripId"])
    ]
)
data class TrackPointEntity(
    @PrimaryKey
    val id: String,
    val tripId: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val speedMps: Float? = null,
    val altitudeMeters: Double? = null
)
