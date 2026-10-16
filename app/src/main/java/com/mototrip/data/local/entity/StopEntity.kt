package com.mototrip.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stops",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tripId"])
    ]
)
data class StopEntity(
    @PrimaryKey
    val id: String,
    val tripId: String,
    val type: StopType,
    val arrivedAt: Long,
    val departedAt: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val name: String? = null,
    val notes: String? = null
)
