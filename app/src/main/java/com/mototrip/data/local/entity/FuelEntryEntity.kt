package com.mototrip.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fuel_entries",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StopEntity::class,
            parentColumns = ["id"],
            childColumns = ["stopId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["tripId"]),
        Index(value = ["stopId"])
    ]
)
data class FuelEntryEntity(
    @PrimaryKey
    val id: String,
    val tripId: String,
    val stopId: String? = null,
    val occurredAt: Long,
    val amountMinorUnits: Long,
    val currencyCode: String = "ARS",
    val liters: Double? = null,
    val odometerKm: Double? = null,
    val stationName: String? = null,
    val isFullTank: Boolean = false,
    val notes: String? = null
)
