package com.mototrip.core.backup

import com.mototrip.data.local.entity.ExpenseEntity
import com.mototrip.data.local.entity.FuelEntryEntity
import com.mototrip.data.local.entity.StopEntity
import com.mototrip.data.local.entity.TrackPointEntity
import com.mototrip.data.local.entity.TripEntity
import com.mototrip.data.local.entity.TripEventEntity

data class BackupPayload(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val trips: List<TripEntity>,
    val trackPoints: List<TrackPointEntity>,
    val stops: List<StopEntity>,
    val fuelEntries: List<FuelEntryEntity>,
    val expenses: List<ExpenseEntity>,
    val tripEvents: List<TripEventEntity>
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }
}

sealed interface BackupValidationResult {
    data class Valid(val payload: BackupPayload) : BackupValidationResult
    data class IncompatibleVersion(val foundVersion: Int, val expectedVersion: Int) : BackupValidationResult
    data class Corrupted(val reason: String) : BackupValidationResult
}
