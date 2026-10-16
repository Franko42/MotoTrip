package com.mototrip.core.backup

import androidx.room.withTransaction
import com.mototrip.data.local.MotoTripDatabase
import com.mototrip.data.local.entity.ExpenseEntity
import com.mototrip.data.local.entity.FuelEntryEntity
import com.mototrip.data.local.entity.StopEntity
import com.mototrip.data.local.entity.StopType
import com.mototrip.data.local.entity.TrackPointEntity
import com.mototrip.data.local.entity.TripEntity
import com.mototrip.data.local.entity.TripEventEntity
import com.mototrip.data.local.entity.TripEventType
import com.mototrip.data.local.entity.TripStatus
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    fun serializeBackup(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("schemaVersion", payload.schemaVersion)
        root.put("exportedAt", payload.exportedAt)
        root.put("appVersion", payload.appVersion)

        val tripsArr = JSONArray()
        payload.trips.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            obj.put("originName", t.originName ?: JSONObject.NULL)
            obj.put("destinationName", t.destinationName ?: JSONObject.NULL)
            obj.put("startedAt", t.startedAt)
            obj.put("endedAt", t.endedAt ?: JSONObject.NULL)
            obj.put("status", t.status.name)
            obj.put("distanceMeters", t.distanceMeters)
            obj.put("notes", t.notes ?: JSONObject.NULL)
            obj.put("createdAt", t.createdAt)
            obj.put("updatedAt", t.updatedAt)
            tripsArr.put(obj)
        }
        root.put("trips", tripsArr)

        val trackArr = JSONArray()
        payload.trackPoints.forEach { pt ->
            val obj = JSONObject()
            obj.put("id", pt.id)
            obj.put("tripId", pt.tripId)
            obj.put("timestamp", pt.timestamp)
            obj.put("latitude", pt.latitude)
            obj.put("longitude", pt.longitude)
            obj.put("accuracyMeters", pt.accuracyMeters?.toDouble() ?: JSONObject.NULL)
            obj.put("speedMps", pt.speedMps?.toDouble() ?: JSONObject.NULL)
            obj.put("altitudeMeters", pt.altitudeMeters ?: JSONObject.NULL)
            trackArr.put(obj)
        }
        root.put("trackPoints", trackArr)

        val stopsArr = JSONArray()
        payload.stops.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("tripId", s.tripId)
            obj.put("type", s.type.name)
            obj.put("arrivedAt", s.arrivedAt)
            obj.put("departedAt", s.departedAt ?: JSONObject.NULL)
            obj.put("latitude", s.latitude ?: JSONObject.NULL)
            obj.put("longitude", s.longitude ?: JSONObject.NULL)
            obj.put("name", s.name ?: JSONObject.NULL)
            obj.put("notes", s.notes ?: JSONObject.NULL)
            stopsArr.put(obj)
        }
        root.put("stops", stopsArr)

        val fuelArr = JSONArray()
        payload.fuelEntries.forEach { f ->
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("tripId", f.tripId)
            obj.put("stopId", f.stopId ?: JSONObject.NULL)
            obj.put("occurredAt", f.occurredAt)
            obj.put("amountMinorUnits", f.amountMinorUnits)
            obj.put("currencyCode", f.currencyCode)
            obj.put("liters", f.liters ?: JSONObject.NULL)
            obj.put("odometerKm", f.odometerKm ?: JSONObject.NULL)
            obj.put("stationName", f.stationName ?: JSONObject.NULL)
            obj.put("isFullTank", f.isFullTank)
            fuelArr.put(obj)
        }
        root.put("fuelEntries", fuelArr)

        val expArr = JSONArray()
        payload.expenses.forEach { e ->
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("tripId", e.tripId)
            obj.put("stopId", e.stopId ?: JSONObject.NULL)
            obj.put("occurredAt", e.occurredAt)
            obj.put("amountMinorUnits", e.amountMinorUnits)
            obj.put("currencyCode", e.currencyCode)
            obj.put("category", e.category)
            obj.put("description", e.description ?: JSONObject.NULL)
            expArr.put(obj)
        }
        root.put("expenses", expArr)

        val eventArr = JSONArray()
        payload.tripEvents.forEach { ev ->
            val obj = JSONObject()
            obj.put("id", ev.id)
            obj.put("tripId", ev.tripId)
            obj.put("timestamp", ev.timestamp)
            obj.put("eventType", ev.eventType.name)
            obj.put("latitude", ev.latitude ?: JSONObject.NULL)
            obj.put("longitude", ev.longitude ?: JSONObject.NULL)
            obj.put("details", ev.details ?: JSONObject.NULL)
            eventArr.put(obj)
        }
        root.put("tripEvents", eventArr)

        return root.toString(2)
    }

    fun parseAndValidate(jsonString: String): BackupValidationResult {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("schemaVersion")) {
                return BackupValidationResult.Corrupted("Falta el campo schemaVersion en el archivo de respaldo.")
            }
            val version = root.getInt("schemaVersion")
            if (version != BackupPayload.CURRENT_SCHEMA_VERSION) {
                return BackupValidationResult.IncompatibleVersion(
                    foundVersion = version,
                    expectedVersion = BackupPayload.CURRENT_SCHEMA_VERSION
                )
            }

            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
            val appVersion = root.optString("appVersion", "1.0.0")

            val trips = mutableListOf<TripEntity>()
            val tripsArr = root.optJSONArray("trips") ?: JSONArray()
            for (i in 0 until tripsArr.length()) {
                val o = tripsArr.getJSONObject(i)
                trips.add(
                    TripEntity(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        originName = if (o.isNull("originName")) null else o.optString("originName"),
                        destinationName = if (o.isNull("destinationName")) null else o.optString("destinationName"),
                        startedAt = o.getLong("startedAt"),
                        endedAt = if (o.isNull("endedAt")) null else o.optLong("endedAt"),
                        status = runCatching { TripStatus.valueOf(o.getString("status")) }.getOrDefault(TripStatus.COMPLETED),
                        distanceMeters = o.optLong("distanceMeters", 0L),
                        notes = if (o.isNull("notes")) null else o.optString("notes"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val trackPoints = mutableListOf<TrackPointEntity>()
            val trackArr = root.optJSONArray("trackPoints") ?: JSONArray()
            for (i in 0 until trackArr.length()) {
                val o = trackArr.getJSONObject(i)
                trackPoints.add(
                    TrackPointEntity(
                        id = o.getString("id"),
                        tripId = o.getString("tripId"),
                        timestamp = o.getLong("timestamp"),
                        latitude = o.getDouble("latitude"),
                        longitude = o.getDouble("longitude"),
                        accuracyMeters = if (o.isNull("accuracyMeters")) null else o.getDouble("accuracyMeters").toFloat(),
                        speedMps = if (o.isNull("speedMps")) null else o.getDouble("speedMps").toFloat(),
                        altitudeMeters = if (o.isNull("altitudeMeters")) null else o.getDouble("altitudeMeters")
                    )
                )
            }

            val stops = mutableListOf<StopEntity>()
            val stopsArr = root.optJSONArray("stops") ?: JSONArray()
            for (i in 0 until stopsArr.length()) {
                val o = stopsArr.getJSONObject(i)
                stops.add(
                    StopEntity(
                        id = o.getString("id"),
                        tripId = o.getString("tripId"),
                        type = runCatching { StopType.valueOf(o.getString("type")) }.getOrDefault(StopType.OTHER),
                        arrivedAt = o.getLong("arrivedAt"),
                        departedAt = if (o.isNull("departedAt")) null else o.optLong("departedAt"),
                        latitude = if (o.isNull("latitude")) null else o.optDouble("latitude"),
                        longitude = if (o.isNull("longitude")) null else o.optDouble("longitude"),
                        name = if (o.isNull("name")) null else o.optString("name"),
                        notes = if (o.isNull("notes")) null else o.optString("notes")
                    )
                )
            }

            val fuelEntries = mutableListOf<FuelEntryEntity>()
            val fuelArr = root.optJSONArray("fuelEntries") ?: JSONArray()
            for (i in 0 until fuelArr.length()) {
                val o = fuelArr.getJSONObject(i)
                val amount = o.getLong("amountMinorUnits")
                if (amount < 0) return BackupValidationResult.Corrupted("Importe negativo en registro de combustible")
                fuelEntries.add(
                    FuelEntryEntity(
                        id = o.getString("id"),
                        tripId = o.getString("tripId"),
                        stopId = if (o.isNull("stopId")) null else o.optString("stopId"),
                        occurredAt = o.getLong("occurredAt"),
                        amountMinorUnits = amount,
                        currencyCode = o.optString("currencyCode", "ARS"),
                        liters = if (o.isNull("liters")) null else o.optDouble("liters"),
                        odometerKm = if (o.isNull("odometerKm")) null else o.optDouble("odometerKm"),
                        stationName = if (o.isNull("stationName")) null else o.optString("stationName"),
                        isFullTank = o.optBoolean("isFullTank", false)
                    )
                )
            }

            val expenses = mutableListOf<ExpenseEntity>()
            val expArr = root.optJSONArray("expenses") ?: JSONArray()
            for (i in 0 until expArr.length()) {
                val o = expArr.getJSONObject(i)
                val amount = o.getLong("amountMinorUnits")
                if (amount < 0) return BackupValidationResult.Corrupted("Importe negativo en registro de gasto")
                expenses.add(
                    ExpenseEntity(
                        id = o.getString("id"),
                        tripId = o.getString("tripId"),
                        stopId = if (o.isNull("stopId")) null else o.optString("stopId"),
                        occurredAt = o.getLong("occurredAt"),
                        amountMinorUnits = amount,
                        currencyCode = o.optString("currencyCode", "ARS"),
                        category = o.optString("category", "General"),
                        description = if (o.isNull("description")) null else o.optString("description")
                    )
                )
            }

            val events = mutableListOf<TripEventEntity>()
            val eventArr = root.optJSONArray("tripEvents") ?: JSONArray()
            for (i in 0 until eventArr.length()) {
                val o = eventArr.getJSONObject(i)
                events.add(
                    TripEventEntity(
                        id = o.getString("id"),
                        tripId = o.getString("tripId"),
                        timestamp = o.getLong("timestamp"),
                        eventType = runCatching { TripEventType.valueOf(o.getString("eventType")) }.getOrDefault(TripEventType.STARTED),
                        latitude = if (o.isNull("latitude")) null else o.optDouble("latitude"),
                        longitude = if (o.isNull("longitude")) null else o.optDouble("longitude"),
                        details = if (o.isNull("details")) null else o.optString("details")
                    )
                )
            }

            BackupValidationResult.Valid(
                BackupPayload(
                    schemaVersion = version,
                    exportedAt = exportedAt,
                    appVersion = appVersion,
                    trips = trips,
                    trackPoints = trackPoints,
                    stops = stops,
                    fuelEntries = fuelEntries,
                    expenses = expenses,
                    tripEvents = events
                )
            )
        } catch (e: Exception) {
            BackupValidationResult.Corrupted("Error al parsear el archivo JSON: ${e.message}")
        }
    }

    fun exportTripToCsv(
        trip: TripEntity,
        stops: List<StopEntity>,
        fuelEntries: List<FuelEntryEntity>,
        expenses: List<ExpenseEntity>
    ): String {
        val sb = StringBuilder()

        // Section: Trip Summary
        sb.appendLine("TIPO,ID,NOMBRE,ORIGEN,DESTINO,ESTADO,DISTANCIA_KM,FECHA_INICIO,FECHA_FIN")
        val distKm = trip.distanceMeters / 1000.0
        sb.appendLine("VIAJE,\"${escapeCsv(trip.id)}\",\"${escapeCsv(trip.name)}\",\"${escapeCsv(trip.originName.orEmpty())}\",\"${escapeCsv(trip.destinationName.orEmpty())}\",${trip.status.name},$distKm,${trip.startedAt},${trip.endedAt ?: ""}")
        sb.appendLine()

        // Section: Stops
        sb.appendLine("TIPO,ID,TIPO_PARADA,NOMBRE,LLEGADA,SALIDA,NOTAS")
        stops.forEach { s ->
            sb.appendLine("PARADA,\"${escapeCsv(s.id)}\",${s.type.name},\"${escapeCsv(s.name.orEmpty())}\",${s.arrivedAt},${s.departedAt ?: ""},\"${escapeCsv(s.notes.orEmpty())}\"")
        }
        sb.appendLine()

        // Section: Fuel
        sb.appendLine("TIPO,ID,FECHA,IMPORTE,MONEDA,LITROS,PRECIO_POR_LITRO,ODOMETRO_KM,ESTACION,TANQUE_LLENO")
        fuelEntries.forEach { f ->
            val amount = f.amountMinorUnits / 100.0
            val pricePerLiter = if (f.liters != null && f.liters > 0) amount / f.liters else 0.0
            sb.appendLine("COMBUSTIBLE,\"${escapeCsv(f.id)}\",${f.occurredAt},$amount,${f.currencyCode},${f.liters ?: ""},$pricePerLiter,${f.odometerKm ?: ""},\"${escapeCsv(f.stationName.orEmpty())}\",${f.isFullTank}")
        }
        sb.appendLine()

        // Section: Expenses
        sb.appendLine("TIPO,ID,FECHA,CATEGORIA,IMPORTE,MONEDA,DESCRIPCION")
        expenses.forEach { e ->
            val amount = e.amountMinorUnits / 100.0
            sb.appendLine("GASTO,\"${escapeCsv(e.id)}\",${e.occurredAt},\"${escapeCsv(e.category)}\",$amount,${e.currencyCode},\"${escapeCsv(e.description.orEmpty())}\"")
        }

        return sb.toString()
    }

    suspend fun createDatabaseSnapshot(database: MotoTripDatabase): BackupPayload {
        return database.withTransaction {
            val allTrips = database.tripDao().getAllTrips()
            val allStops = database.stopDao().getAllStops()
            val allFuel = database.fuelEntryDao().getAllFuelEntries()
            val allExpenses = database.expenseDao().getAllExpenses()
            val allPoints = database.trackPointDao().getAllTrackPoints()
            val allEvents = database.tripEventDao().getAllTripEvents()

            BackupPayload(
                trips = allTrips,
                trackPoints = allPoints,
                stops = allStops,
                fuelEntries = allFuel,
                expenses = allExpenses,
                tripEvents = allEvents
            )
        }
    }

    suspend fun restoreDatabase(
        database: MotoTripDatabase,
        payload: BackupPayload,
        clearExisting: Boolean
    ): Result<Unit> {
        return runCatching {
            database.withTransaction {
                if (clearExisting) {
                    database.clearAllTables()
                }

                // Insert trips
                payload.trips.forEach { trip ->
                    database.tripDao().insertTrip(trip)
                }

                // Insert stops
                payload.stops.forEach { stop ->
                    database.stopDao().insertStop(stop)
                }

                // Insert track points in batch
                if (payload.trackPoints.isNotEmpty()) {
                    database.trackPointDao().insertTrackPoints(payload.trackPoints)
                }

                // Insert fuel entries
                payload.fuelEntries.forEach { fuel ->
                    database.fuelEntryDao().insertFuelEntry(fuel)
                }

                // Insert expenses
                payload.expenses.forEach { exp ->
                    database.expenseDao().insertExpense(exp)
                }

                // Insert events
                if (payload.tripEvents.isNotEmpty()) {
                    database.tripEventDao().insertTripEvents(payload.tripEvents)
                }
            }
        }
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}
