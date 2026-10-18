package com.mototrip.ui.trip_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mototrip.domain.calculations.MoneyTotal
import com.mototrip.domain.calculations.TripCalculations
import com.mototrip.domain.model.CalculationResult
import com.mototrip.domain.model.FuelEntry
import com.mototrip.domain.model.GpsSpeedStatistics
import com.mototrip.domain.model.MotorcycleConfig
import com.mototrip.domain.model.Stop
import com.mototrip.domain.model.TrackPoint
import com.mototrip.domain.model.TripDetail
import com.mototrip.domain.model.TripEconomicReport
import com.mototrip.domain.model.TripTimeStatistics
import com.mototrip.domain.repository.MotorcycleConfigRepository
import com.mototrip.domain.repository.TripRepository
import com.mototrip.domain.usecase.RecalculateTripDistanceUseCase
import com.mototrip.domain.usecase.UpdateFuelEntryUseCase
import com.mototrip.domain.usecase.UpdateStopUseCase
import com.mototrip.domain.usecase.UpdateTripDatesUseCase
import com.mototrip.domain.usecase.UpdateTripMetadataUseCase
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
    val timeStats: TripTimeStatistics? = null,
    val speedStats: GpsSpeedStatistics? = null,
    val economicReports: List<TripEconomicReport> = emptyList(),
    val tripFuelConsumption: CalculationResult<Double>? = null,
    val tripEfficiencyKmL: CalculationResult<Double>? = null,
    val theoreticalRangeKm: CalculationResult<Double>? = null,
    val rangeToReserveKm: CalculationResult<Double>? = null,
    val motorcycleConfig: MotorcycleConfig = MotorcycleConfig(),
    val moneyTotals: List<MoneyTotal> = emptyList(),
    val costPerKm: Double? = null
)

class TripDetailViewModel(
    private val tripId: String,
    private val tripRepository: TripRepository,
    private val motorcycleConfigRepository: MotorcycleConfigRepository,
    private val updateTripMetadataUseCase: UpdateTripMetadataUseCase,
    private val updateTripDatesUseCase: UpdateTripDatesUseCase,
    private val recalculateTripDistanceUseCase: RecalculateTripDistanceUseCase,
    private val updateStopUseCase: UpdateStopUseCase,
    private val updateFuelEntryUseCase: UpdateFuelEntryUseCase
) : ViewModel() {

    val uiState: StateFlow<TripDetailUiState> = combine(
        tripRepository.getTripDetail(tripId),
        tripRepository.getTrackPointsByTrip(tripId),
        motorcycleConfigRepository.config
    ) { detail, points, motoConfig ->
        if (detail == null) {
            TripDetailUiState(motorcycleConfig = motoConfig)
        } else {
            val duration = TripCalculations.calculateActiveDurationMillis(
                startedAt = detail.trip.startedAt,
                endedAt = detail.trip.endedAt,
                events = detail.events
            )
            val timeStats = TripCalculations.calculateTripTimeStatistics(
                startedAt = detail.trip.startedAt,
                endedAt = detail.trip.endedAt,
                points = points,
                stops = detail.stops,
                events = detail.events
            )
            val speedStats = TripCalculations.calculateGpsSpeedStatistics(
                points = points,
                startedAt = detail.trip.startedAt,
                endedAt = detail.trip.endedAt
            )
            val economicReports = TripCalculations.calculateEconomicReports(
                fuelEntries = detail.fuelEntries,
                expenses = detail.expenses,
                distanceMeters = detail.trip.distanceMeters,
                referenceConsumptionL100 = motoConfig.referenceConsumptionL100
            )
            val tripConsumption = TripCalculations.calculateTripFuelConsumption(
                distanceMeters = detail.trip.distanceMeters,
                fuelEntries = detail.fuelEntries
            )
            val tripEfficiency = TripCalculations.calculateTripEfficiencyKmL(
                distanceMeters = detail.trip.distanceMeters,
                fuelEntries = detail.fuelEntries
            )
            val effectiveEfficiency = tripEfficiency.valueOrNull ?: motoConfig.referenceEfficiencyKmL
            val theoreticalRange = TripCalculations.calculateTheoreticalRangeKm(
                tankCapacityLiters = motoConfig.tankCapacityLiters,
                efficiencyKmL = effectiveEfficiency
            )
            val rangeToReserve = TripCalculations.calculateRangeToReserveKm(
                tankCapacityLiters = motoConfig.tankCapacityLiters,
                reserveLiters = motoConfig.reserveLiters,
                efficiencyKmL = effectiveEfficiency
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
                timeStats = timeStats,
                speedStats = speedStats,
                economicReports = economicReports,
                tripFuelConsumption = tripConsumption,
                tripEfficiencyKmL = tripEfficiency,
                theoreticalRangeKm = theoreticalRange,
                rangeToReserveKm = rangeToReserve,
                motorcycleConfig = motoConfig,
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

    fun updateTripDates(startedAt: Long, endedAt: Long?) {
        viewModelScope.launch {
            updateTripDatesUseCase(tripId, startedAt, endedAt)
                .onSuccess { _userMessage.emit("Fechas del viaje actualizadas con éxito") }
                .onFailure { _userMessage.emit(it.message ?: "Error al actualizar fechas del viaje") }
        }
    }

    fun updateTripMetadata(name: String, originName: String?, destinationName: String?, notes: String?) {
        val currentTrip = uiState.value.detail?.trip ?: return
        viewModelScope.launch {
            val updated = currentTrip.copy(
                name = name.ifBlank { currentTrip.name },
                originName = originName?.takeIf { it.isNotBlank() },
                destinationName = destinationName?.takeIf { it.isNotBlank() },
                notes = notes?.takeIf { it.isNotBlank() }
            )
            updateTripMetadataUseCase(updated)
                .onSuccess { _userMessage.emit("Datos del viaje actualizados") }
                .onFailure { _userMessage.emit(it.message ?: "Error al actualizar viaje") }
        }
    }

    fun recalculateDistance() {
        viewModelScope.launch {
            recalculateTripDistanceUseCase(tripId)
                .onSuccess { _userMessage.emit("Distancia recalculada a partir de puntos GPS válidos: ${it / 1000.0} km") }
                .onFailure { _userMessage.emit(it.message ?: "Error al recalcular distancia") }
        }
    }

    fun updateStop(stop: Stop) {
        viewModelScope.launch {
            updateStopUseCase(stop)
                .onSuccess { _userMessage.emit("Parada actualizada") }
                .onFailure { _userMessage.emit(it.message ?: "Error al actualizar parada") }
        }
    }

    fun updateFuelEntry(entry: FuelEntry) {
        viewModelScope.launch {
            updateFuelEntryUseCase(entry)
                .onSuccess { _userMessage.emit("Carga de combustible actualizada") }
                .onFailure { _userMessage.emit(it.message ?: "Error al actualizar carga") }
        }
    }

    fun updateMotorcycleConfig(config: MotorcycleConfig) {
        viewModelScope.launch {
            motorcycleConfigRepository.updateConfig(config)
                .onSuccess { _userMessage.emit("Configuración de motocicleta actualizada") }
                .onFailure { _userMessage.emit(it.message ?: "Error al guardar configuración") }
        }
    }

    companion object {
        fun provideFactory(
            tripId: String,
            tripRepository: TripRepository,
            motorcycleConfigRepository: MotorcycleConfigRepository,
            updateTripMetadataUseCase: UpdateTripMetadataUseCase,
            updateTripDatesUseCase: UpdateTripDatesUseCase,
            recalculateTripDistanceUseCase: RecalculateTripDistanceUseCase,
            updateStopUseCase: UpdateStopUseCase,
            updateFuelEntryUseCase: UpdateFuelEntryUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TripDetailViewModel(
                    tripId,
                    tripRepository,
                    motorcycleConfigRepository,
                    updateTripMetadataUseCase,
                    updateTripDatesUseCase,
                    recalculateTripDistanceUseCase,
                    updateStopUseCase,
                    updateFuelEntryUseCase
                ) as T
            }
        }
    }
}
