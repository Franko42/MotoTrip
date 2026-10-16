package com.mototrip.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mototrip.data.local.entity.ExpenseEntity
import com.mototrip.data.local.entity.FuelEntryEntity
import com.mototrip.data.local.entity.StopEntity
import com.mototrip.data.local.entity.StopType
import com.mototrip.data.local.entity.TrackPointEntity
import com.mototrip.data.local.entity.TripEntity
import com.mototrip.data.local.entity.TripEventEntity
import com.mototrip.data.local.entity.TripEventType
import com.mototrip.data.local.entity.TripStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDatabaseIntegrationTest {

    private lateinit var db: MotoTripDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MotoTripDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetTrip_success() = runBlocking {
        val trip = TripEntity(
            id = "trip-1",
            name = "Ruta 40",
            originName = "Mendoza",
            destinationName = "Bariloche",
            startedAt = 1000L,
            status = TripStatus.ACTIVE,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        db.tripDao().insertTrip(trip)

        val retrieved = db.tripDao().getTripById("trip-1")
        assertNotNull(retrieved)
        assertEquals("Ruta 40", retrieved?.name)
        assertEquals(TripStatus.ACTIVE, retrieved?.status)

        val active = db.tripDao().getActiveOrPausedTrip()
        assertEquals("trip-1", active?.id)
    }

    @Test
    fun cascadeDeleteTrip_deletesTrackPointsStopsAndEvents() = runBlocking {
        val trip = TripEntity(
            id = "trip-2",
            name = "Test Trip",
            startedAt = 2000L,
            status = TripStatus.ACTIVE,
            createdAt = 2000L,
            updatedAt = 2000L
        )
        db.tripDao().insertTrip(trip)

        val point = TrackPointEntity(
            id = "point-1",
            tripId = "trip-2",
            timestamp = 2001L,
            latitude = -34.6037,
            longitude = -58.3816
        )
        db.trackPointDao().insertTrackPoint(point)

        val stop = StopEntity(
            id = "stop-1",
            tripId = "trip-2",
            type = StopType.FUEL,
            arrivedAt = 2005L
        )
        db.stopDao().insertStop(stop)

        val event = TripEventEntity(
            id = "event-1",
            tripId = "trip-2",
            timestamp = 2000L,
            eventType = TripEventType.STARTED
        )
        db.tripEventDao().insertTripEvent(event)

        assertEquals(1, db.trackPointDao().getTrackPointsCount("trip-2"))
        assertEquals(1, db.stopDao().getStopsByTrip("trip-2").size)
        assertEquals(1, db.tripEventDao().getTripEventsByTrip("trip-2").size)

        // Delete trip
        db.tripDao().deleteTripById("trip-2")

        // Relations should be cascade deleted
        assertEquals(0, db.trackPointDao().getTrackPointsCount("trip-2"))
        assertEquals(0, db.stopDao().getStopsByTrip("trip-2").size)
        assertEquals(0, db.tripEventDao().getTripEventsByTrip("trip-2").size)
    }

    @Test
    fun deleteStop_setsFuelEntryStopIdToNull() = runBlocking {
        val trip = TripEntity(
            id = "trip-3",
            name = "Stop relation test",
            startedAt = 3000L,
            status = TripStatus.ACTIVE,
            createdAt = 3000L,
            updatedAt = 3000L
        )
        db.tripDao().insertTrip(trip)

        val stop = StopEntity(
            id = "stop-3",
            tripId = "trip-3",
            type = StopType.FUEL,
            arrivedAt = 3005L
        )
        db.stopDao().insertStop(stop)

        val fuel = FuelEntryEntity(
            id = "fuel-3",
            tripId = "trip-3",
            stopId = "stop-3",
            occurredAt = 3006L,
            amountMinorUnits = 500000L
        )
        db.fuelEntryDao().insertFuelEntry(fuel)

        // Delete stop
        db.stopDao().deleteStopById("stop-3")

        // Fuel entry remains, but stopId becomes null due to SET_NULL foreign key
        val retrievedFuel = db.fuelEntryDao().getFuelEntryById("fuel-3")
        assertNotNull(retrievedFuel)
        assertNull(retrievedFuel?.stopId)
    }

    @Test
    fun trackPoints_retrievedInChronologicalOrder() = runBlocking {
        val trip = TripEntity(
            id = "trip-4",
            name = "Chronology test",
            startedAt = 4000L,
            status = TripStatus.ACTIVE,
            createdAt = 4000L,
            updatedAt = 4000L
        )
        db.tripDao().insertTrip(trip)

        val p2 = TrackPointEntity(id = "p-2", tripId = "trip-4", timestamp = 4020L, latitude = 1.0, longitude = 1.0)
        val p1 = TrackPointEntity(id = "p-1", tripId = "trip-4", timestamp = 4010L, latitude = 1.0, longitude = 1.0)
        val p3 = TrackPointEntity(id = "p-3", tripId = "trip-4", timestamp = 4030L, latitude = 1.0, longitude = 1.0)

        // Insert unordered
        db.trackPointDao().insertTrackPoints(listOf(p2, p3, p1))

        val retrieved = db.trackPointDao().getTrackPointsByTrip("trip-4")
        assertEquals(3, retrieved.size)
        assertEquals("p-1", retrieved[0].id)
        assertEquals("p-2", retrieved[1].id)
        assertEquals("p-3", retrieved[2].id)
    }
}
