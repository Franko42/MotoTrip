package com.mototrip.core.backup

import com.mototrip.data.local.entity.ExpenseEntity
import com.mototrip.data.local.entity.FuelEntryEntity
import com.mototrip.data.local.entity.StopEntity
import com.mototrip.data.local.entity.StopType
import com.mototrip.data.local.entity.TripEntity
import com.mototrip.data.local.entity.TripStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    @Test
    fun serializeAndValidate_roundtrip_isSuccessful() {
        val trip = TripEntity(
            id = "t1",
            name = "Ruta de los 7 Lagos",
            originName = "San Martín de los Andes",
            destinationName = "Villa La Angostura",
            startedAt = 1000L,
            endedAt = 5000L,
            status = TripStatus.COMPLETED,
            distanceMeters = 110000L,
            notes = "Día soleado",
            createdAt = 900L,
            updatedAt = 5000L
        )
        val stop = StopEntity(
            id = "s1",
            tripId = "t1",
            type = StopType.FOOD,
            arrivedAt = 2000L,
            departedAt = 3500L,
            name = "Lago Hermoso"
        )
        val fuel = FuelEntryEntity(
            id = "f1",
            tripId = "t1",
            occurredAt = 1500L,
            amountMinorUnits = 1200000L,
            currencyCode = "ARS",
            liters = 10.0,
            stationName = "YPF",
            isFullTank = true,
            notes = "Carga completa antes de salir a la ruta"
        )
        val exp = ExpenseEntity(
            id = "e1",
            tripId = "t1",
            occurredAt = 2500L,
            amountMinorUnits = 300000L,
            currencyCode = "ARS",
            category = "Almuerzo"
        )

        val payload = BackupPayload(
            trips = listOf(trip),
            trackPoints = emptyList(),
            stops = listOf(stop),
            fuelEntries = listOf(fuel),
            expenses = listOf(exp),
            tripEvents = emptyList()
        )

        val json = BackupManager.serializeBackup(payload)
        assertTrue(json.contains("Ruta de los 7 Lagos"))
        assertTrue(json.contains("Carga completa antes de salir a la ruta"))

        val result = BackupManager.parseAndValidate(json)
        assertTrue(result is BackupValidationResult.Valid)

        val validPayload = (result as BackupValidationResult.Valid).payload
        assertEquals(1, validPayload.trips.size)
        assertEquals("Ruta de los 7 Lagos", validPayload.trips.first().name)
        assertEquals(900L, validPayload.trips.first().createdAt)
        assertEquals(1000L, validPayload.trips.first().startedAt)
        assertEquals(1, validPayload.stops.size)
        assertEquals(3500L, validPayload.stops.first().departedAt)
        assertEquals(1, validPayload.fuelEntries.size)
        assertEquals(1200000L, validPayload.fuelEntries.first().amountMinorUnits)
        assertEquals("Carga completa antes de salir a la ruta", validPayload.fuelEntries.first().notes)
    }

    @Test
    fun parseAndValidate_version1Legacy_isAcceptedForBackwardCompatibility() {
        val legacyV1Json = """
            {
                "schemaVersion": 1,
                "exportedAt": 1000,
                "appVersion": "1.0.0",
                "trips": [
                    {
                        "id": "t1",
                        "name": "Viaje V1",
                        "startedAt": 1000,
                        "status": "COMPLETED",
                        "createdAt": 1000,
                        "updatedAt": 1000
                    }
                ],
                "trackPoints": [],
                "stops": [],
                "fuelEntries": [
                    {
                        "id": "f1",
                        "tripId": "t1",
                        "occurredAt": 1000,
                        "amountMinorUnits": 50000,
                        "currencyCode": "ARS"
                    }
                ],
                "expenses": [],
                "tripEvents": []
            }
        """.trimIndent()

        val result = BackupManager.parseAndValidate(legacyV1Json)
        assertTrue("Legacy v1 backup should be accepted cleanly", result is BackupValidationResult.Valid)
        val payload = (result as BackupValidationResult.Valid).payload
        assertEquals("Viaje V1", payload.trips.first().name)
        assertEquals(null, payload.fuelEntries.first().notes)
    }

    @Test
    fun parseAndValidate_incompatibleVersion_rejected() {
        val json = """
            {
                "schemaVersion": 99,
                "exportedAt": 1000,
                "trips": []
            }
        """.trimIndent()

        val result = BackupManager.parseAndValidate(json)
        assertTrue(result is BackupValidationResult.IncompatibleVersion)
        assertEquals(99, (result as BackupValidationResult.IncompatibleVersion).foundVersion)
    }

    @Test
    fun parseAndValidate_corruptedJson_rejected() {
        val json = "this is not valid json {"
        val result = BackupManager.parseAndValidate(json)
        assertTrue(result is BackupValidationResult.Corrupted)
    }

    @Test
    fun parseAndValidate_negativeFuelAmount_rejected() {
        val json = """
            {
                "schemaVersion": 2,
                "exportedAt": 1000,
                "trips": [],
                "fuelEntries": [
                    {
                        "id": "f1",
                        "tripId": "t1",
                        "occurredAt": 1000,
                        "amountMinorUnits": -500,
                        "currencyCode": "ARS"
                    }
                ]
            }
        """.trimIndent()

        val result = BackupManager.parseAndValidate(json)
        assertTrue(result is BackupValidationResult.Corrupted)
    }

    @Test
    fun exportTripToCsv_formatsCorrectly() {
        val trip = TripEntity(
            id = "t1",
            name = "Paseo",
            originName = "A",
            destinationName = "B",
            startedAt = 1000L,
            endedAt = 2000L,
            status = TripStatus.COMPLETED,
            distanceMeters = 50000L,
            notes = null,
            createdAt = 800L,
            updatedAt = 2000L
        )
        val stop = StopEntity("s1", "t1", StopType.REST, 1500L, null, null, null, "Mirador", null)
        val fuel = FuelEntryEntity("f1", "t1", null, 1100L, 500000L, "ARS", 5.0, 1000.0, "YPF", true, "Nafta premium")
        val exp = ExpenseEntity("e1", "t1", null, 1200L, 100000L, "ARS", "Peaje", null)

        val csv = BackupManager.exportTripToCsv(trip, listOf(stop), listOf(fuel), listOf(exp))
        assertTrue(csv.contains("VIAJE,\"t1\",\"Paseo\""))
        assertTrue(csv.contains(",1000,2000,800"))
        assertTrue(csv.contains("PARADA,\"s1\",REST,\"Mirador\""))
        assertTrue(csv.contains("COMBUSTIBLE,\"f1\",1100,5000.0,ARS,5.0,1000.0,1000.0,\"YPF\",true,\"Nafta premium\""))
        assertTrue(csv.contains("GASTO,\"e1\",1200,\"Peaje\",1000.0,ARS"))
    }
}
