package com.mototrip.core.di

import android.content.Context
import com.mototrip.data.local.MotoTripDatabase
import com.mototrip.data.repository.TripRepositoryImpl
import com.mototrip.domain.repository.TripRepository
import com.mototrip.domain.usecase.AddExpenseUseCase
import com.mototrip.domain.usecase.AddFuelEntryUseCase
import com.mototrip.domain.usecase.AddStopUseCase
import com.mototrip.domain.usecase.DeleteTripUseCase
import com.mototrip.domain.usecase.FinishTripUseCase
import com.mototrip.domain.usecase.GetActiveTripUseCase
import com.mototrip.domain.usecase.GetAllTripsUseCase
import com.mototrip.domain.usecase.GetTripDetailUseCase
import com.mototrip.domain.usecase.PauseTripUseCase
import com.mototrip.domain.usecase.ResumeTripUseCase
import com.mototrip.domain.usecase.StartTripUseCase

import com.mototrip.data.repository.MotorcycleConfigRepositoryImpl
import com.mototrip.domain.repository.MotorcycleConfigRepository
import com.mototrip.domain.usecase.RecalculateTripDistanceUseCase
import com.mototrip.domain.usecase.UpdateExpenseUseCase
import com.mototrip.domain.usecase.UpdateFuelEntryUseCase
import com.mototrip.domain.usecase.UpdateStopUseCase
import com.mototrip.core.updater.AppUpdateManager
import com.mototrip.domain.usecase.UpdateTripDatesUseCase
import com.mototrip.domain.usecase.UpdateTripMetadataUseCase

interface AppContainer {
    val database: MotoTripDatabase
    val tripRepository: TripRepository
    val motorcycleConfigRepository: MotorcycleConfigRepository
    val appUpdateManager: AppUpdateManager

    val startTripUseCase: StartTripUseCase
    val pauseTripUseCase: PauseTripUseCase
    val resumeTripUseCase: ResumeTripUseCase
    val finishTripUseCase: FinishTripUseCase
    val getActiveTripUseCase: GetActiveTripUseCase
    val getAllTripsUseCase: GetAllTripsUseCase
    val getTripDetailUseCase: GetTripDetailUseCase
    val addStopUseCase: AddStopUseCase
    val updateStopUseCase: UpdateStopUseCase
    val addFuelEntryUseCase: AddFuelEntryUseCase
    val updateFuelEntryUseCase: UpdateFuelEntryUseCase
    val addExpenseUseCase: AddExpenseUseCase
    val updateExpenseUseCase: UpdateExpenseUseCase
    val deleteTripUseCase: DeleteTripUseCase
    val updateTripMetadataUseCase: UpdateTripMetadataUseCase
    val updateTripDatesUseCase: UpdateTripDatesUseCase
    val recalculateTripDistanceUseCase: RecalculateTripDistanceUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val database: MotoTripDatabase by lazy {
        MotoTripDatabase.getInstance(context)
    }

    override val tripRepository: TripRepository by lazy {
        TripRepositoryImpl(database)
    }

    override val motorcycleConfigRepository: MotorcycleConfigRepository by lazy {
        MotorcycleConfigRepositoryImpl(context)
    }

    override val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManager(context)
    }

    override val startTripUseCase: StartTripUseCase by lazy { StartTripUseCase(tripRepository) }
    override val pauseTripUseCase: PauseTripUseCase by lazy { PauseTripUseCase(tripRepository) }
    override val resumeTripUseCase: ResumeTripUseCase by lazy { ResumeTripUseCase(tripRepository) }
    override val finishTripUseCase: FinishTripUseCase by lazy { FinishTripUseCase(tripRepository) }
    override val getActiveTripUseCase: GetActiveTripUseCase by lazy { GetActiveTripUseCase(tripRepository) }
    override val getAllTripsUseCase: GetAllTripsUseCase by lazy { GetAllTripsUseCase(tripRepository) }
    override val getTripDetailUseCase: GetTripDetailUseCase by lazy { GetTripDetailUseCase(tripRepository) }
    override val addStopUseCase: AddStopUseCase by lazy { AddStopUseCase(tripRepository) }
    override val updateStopUseCase: UpdateStopUseCase by lazy { UpdateStopUseCase(tripRepository) }
    override val addFuelEntryUseCase: AddFuelEntryUseCase by lazy { AddFuelEntryUseCase(tripRepository) }
    override val updateFuelEntryUseCase: UpdateFuelEntryUseCase by lazy { UpdateFuelEntryUseCase(tripRepository) }
    override val addExpenseUseCase: AddExpenseUseCase by lazy { AddExpenseUseCase(tripRepository) }
    override val updateExpenseUseCase: UpdateExpenseUseCase by lazy { UpdateExpenseUseCase(tripRepository) }
    override val deleteTripUseCase: DeleteTripUseCase by lazy { DeleteTripUseCase(tripRepository) }
    override val updateTripMetadataUseCase: UpdateTripMetadataUseCase by lazy { UpdateTripMetadataUseCase(tripRepository) }
    override val updateTripDatesUseCase: UpdateTripDatesUseCase by lazy { UpdateTripDatesUseCase(tripRepository) }
    override val recalculateTripDistanceUseCase: RecalculateTripDistanceUseCase by lazy { RecalculateTripDistanceUseCase(tripRepository) }
}
