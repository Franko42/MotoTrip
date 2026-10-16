package com.mototrip.ui.trip_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mototrip.domain.calculations.MoneyTotal
import com.mototrip.domain.calculations.TripCalculations
import com.mototrip.domain.model.TrackPoint
import com.mototrip.domain.model.TripDetail
import com.mototrip.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TripDetailUiState(
    val detail: TripDetail? = null,
    val trackPoints: List<TrackPoint> = emptyList(),
    val activeDurationMillis: Long = 0L,
    val moneyTotals: List<MoneyTotal> = emptyList(),
    val costPerKm: Double? = null
)

class TripDetailViewModel(
    private val tripId: String,
    private val tripRepository: TripRepository
) : ViewModel() {

    val uiState: StateFlow<TripDetailUiState> = combine(
        tripRepository.getTripDetail(tripId),
        tripRepository.getTrackPointsByTrip(tripId)
    ) { detail, points ->
        if (detail == null) {
            TripDetailUiState()
        } else {
            val duration = TripCalculations.calculateActiveDurationMillis(
                startedAt = detail.trip.startedAt,
                endedAt = detail.trip.endedAt,
                events = detail.events
            )
            val moneyTotals = TripCalculations.calculateExpensesByCurrency(
                fuelEntries = detail.fuelEntries,
                expenses = detail.expenses
            )
            val primaryTotal = moneyTotals.firstOrNull()?.totalAmountMinorUnits ?: 0L
            val costPerKm = TripCalculations.calculateCostPerKm(primaryTotal, detail.trip.distanceMeters)

            TripDetailUiState(
                detail = detail,
                trackPoints = points,
                activeDurationMillis = duration,
                moneyTotals = moneyTotals,
                costPerKm = costPerKm
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TripDetailUiState()
    )

    private val _tripDeletedEvent = MutableSharedFlow<Unit>()
    val tripDeletedEvent: SharedFlow<Unit> = _tripDeletedEvent.asSharedFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    fun deleteTrip() {
        viewModelScope.launch {
            tripRepository.deleteTrip(tripId)
                .onSuccess { _tripDeletedEvent.emit(Unit) }
                .onFailure { _userMessage.emit(it.message ?: "Error al eliminar viaje") }
        }
    }

    companion object {
        fun provideFactory(
            tripId: String,
            tripRepository: TripRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TripDetailViewModel(tripId, tripRepository) as T
            }
        }
    }
}
