package com.mototrip.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mototrip.domain.model.Trip
import com.mototrip.domain.usecase.GetActiveTripUseCase
import com.mototrip.domain.usecase.StartTripUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(val activeTrip: Trip?) : HomeUiState
}

class HomeViewModel(
    getActiveTripUseCase: GetActiveTripUseCase,
    private val startTripUseCase: StartTripUseCase
) : ViewModel() {

    val activeTrip: StateFlow<Trip?> = getActiveTripUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private val _tripStartedEvent = MutableSharedFlow<String>()
    val tripStartedEvent: SharedFlow<String> = _tripStartedEvent.asSharedFlow()

    fun startTrip(name: String, origin: String?, destination: String?) {
        viewModelScope.launch {
            val result = startTripUseCase(
                name = name.ifBlank { "Viaje ${java.time.LocalDate.now()}" },
                originName = origin?.takeIf { it.isNotBlank() },
                destinationName = destination?.takeIf { it.isNotBlank() }
            )
            result.onSuccess { trip ->
                _tripStartedEvent.emit(trip.id)
            }.onFailure { error ->
                _errorMessage.emit(error.message ?: "Error al iniciar el viaje")
            }
        }
    }

    companion object {
        fun provideFactory(
            getActiveTripUseCase: GetActiveTripUseCase,
            startTripUseCase: StartTripUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(getActiveTripUseCase, startTripUseCase) as T
            }
        }
    }
}
