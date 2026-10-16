package com.mototrip.ui.active_trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mototrip.data.local.entity.StopType
import com.mototrip.domain.model.Expense
import com.mototrip.domain.model.FuelEntry
import com.mototrip.domain.model.Stop
import com.mototrip.domain.model.TripDetail
import com.mototrip.domain.usecase.AddExpenseUseCase
import com.mototrip.domain.usecase.AddFuelEntryUseCase
import com.mototrip.domain.usecase.AddStopUseCase
import com.mototrip.domain.usecase.FinishTripUseCase
import com.mototrip.domain.usecase.GetTripDetailUseCase
import com.mototrip.domain.usecase.PauseTripUseCase
import com.mototrip.domain.usecase.ResumeTripUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.mototrip.domain.model.TrackPoint
import com.mototrip.domain.repository.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ActiveTripViewModel(
    private val tripId: String,
    getTripDetailUseCase: GetTripDetailUseCase,
    private val pauseTripUseCase: PauseTripUseCase,
    private val resumeTripUseCase: ResumeTripUseCase,
    private val finishTripUseCase: FinishTripUseCase,
    private val addStopUseCase: AddStopUseCase,
    private val addFuelEntryUseCase: AddFuelEntryUseCase,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val tripRepository: TripRepository
) : ViewModel() {

    val tripDetail: StateFlow<TripDetail?> = getTripDetailUseCase(tripId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val trackPoints: StateFlow<List<TrackPoint>> = tripRepository.getTrackPointsByTrip(tripId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _tripFinishedEvent = MutableSharedFlow<Unit>()
    val tripFinishedEvent: SharedFlow<Unit> = _tripFinishedEvent.asSharedFlow()

    fun pauseTrip() {
        viewModelScope.launch {
            pauseTripUseCase(tripId)
                .onFailure { _userMessage.emit(it.message ?: "Error al pausar el viaje") }
        }
    }

    fun resumeTrip() {
        viewModelScope.launch {
            resumeTripUseCase(tripId)
                .onFailure { _userMessage.emit(it.message ?: "Error al reanudar el viaje") }
        }
    }

    fun finishTrip(manualDistanceKm: Double) {
        viewModelScope.launch {
            val distanceMeters = (manualDistanceKm * 1000).toLong()
            finishTripUseCase(tripId, distanceMeters)
                .onSuccess { _tripFinishedEvent.emit(Unit) }
                .onFailure { _userMessage.emit(it.message ?: "Error al finalizar el viaje") }
        }
    }

    fun addStop(type: StopType, name: String?, notes: String?) {
        viewModelScope.launch {
            val stop = Stop(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                type = type,
                arrivedAt = System.currentTimeMillis(),
                name = name?.takeIf { it.isNotBlank() },
                notes = notes?.takeIf { it.isNotBlank() }
            )
            addStopUseCase(stop)
                .onFailure { _userMessage.emit(it.message ?: "Error al registrar la parada") }
        }
    }

    fun addFuelEntry(
        amount: Double,
        currencyCode: String,
        liters: Double?,
        odometerKm: Double?,
        stationName: String?,
        isFullTank: Boolean,
        stopId: String?
    ) {
        viewModelScope.launch {
            val amountMinorUnits = (amount * 100).toLong()
            val entry = FuelEntry(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                stopId = stopId,
                occurredAt = System.currentTimeMillis(),
                amountMinorUnits = amountMinorUnits,
                currencyCode = currencyCode.ifBlank { "ARS" },
                liters = liters,
                odometerKm = odometerKm,
                stationName = stationName?.takeIf { it.isNotBlank() },
                isFullTank = isFullTank
            )
            addFuelEntryUseCase(entry)
                .onFailure { _userMessage.emit(it.message ?: "Error al registrar combustible") }
        }
    }

    fun addExpense(
        category: String,
        amount: Double,
        currencyCode: String,
        description: String?,
        stopId: String?
    ) {
        viewModelScope.launch {
            val amountMinorUnits = (amount * 100).toLong()
            val expense = Expense(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                stopId = stopId,
                occurredAt = System.currentTimeMillis(),
                amountMinorUnits = amountMinorUnits,
                currencyCode = currencyCode.ifBlank { "ARS" },
                category = category.ifBlank { "General" },
                description = description?.takeIf { it.isNotBlank() }
            )
            addExpenseUseCase(expense)
                .onFailure { _userMessage.emit(it.message ?: "Error al registrar gasto") }
        }
    }

    companion object {
        fun provideFactory(
            tripId: String,
            getTripDetailUseCase: GetTripDetailUseCase,
            pauseTripUseCase: PauseTripUseCase,
            resumeTripUseCase: ResumeTripUseCase,
            finishTripUseCase: FinishTripUseCase,
            addStopUseCase: AddStopUseCase,
            addFuelEntryUseCase: AddFuelEntryUseCase,
            addExpenseUseCase: AddExpenseUseCase,
            tripRepository: TripRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ActiveTripViewModel(
                    tripId,
                    getTripDetailUseCase,
                    pauseTripUseCase,
                    resumeTripUseCase,
                    finishTripUseCase,
                    addStopUseCase,
                    addFuelEntryUseCase,
                    addExpenseUseCase,
                    tripRepository
                ) as T
            }
        }
    }
}
