package com.mototrip.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
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
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val tripId: String,
    val stopId: String? = null,
    val occurredAt: Long,
    val amountMinorUnits: Long,
    val currencyCode: String = "ARS",
    val category: String,
    val description: String? = null
)
