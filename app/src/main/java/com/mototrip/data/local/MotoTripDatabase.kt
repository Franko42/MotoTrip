package com.mototrip.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mototrip.data.local.converter.Converters
import com.mototrip.data.local.dao.ExpenseDao
import com.mototrip.data.local.dao.FuelEntryDao
import com.mototrip.data.local.dao.StopDao
import com.mototrip.data.local.dao.TrackPointDao
import com.mototrip.data.local.dao.TripDao
import com.mototrip.data.local.dao.TripEventDao
import com.mototrip.data.local.entity.ExpenseEntity
import com.mototrip.data.local.entity.FuelEntryEntity
import com.mototrip.data.local.entity.StopEntity
import com.mototrip.data.local.entity.TrackPointEntity
import com.mototrip.data.local.entity.TripEntity
import com.mototrip.data.local.entity.TripEventEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TripEntity::class,
        TrackPointEntity::class,
        StopEntity::class,
        FuelEntryEntity::class,
        ExpenseEntity::class,
        TripEventEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MotoTripDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun stopDao(): StopDao
    abstract fun fuelEntryDao(): FuelEntryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun tripEventDao(): TripEventDao

    companion object {
        const val DATABASE_NAME = "mototrip.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE fuel_entries ADD COLUMN notes TEXT")
            }
        }

        @Volatile
        private var INSTANCE: MotoTripDatabase? = null

        fun getInstance(context: Context): MotoTripDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MotoTripDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
