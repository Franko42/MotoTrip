package com.mototrip.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mototrip.domain.model.Trip
import com.mototrip.domain.usecase.DeleteTripUseCase
import com.mototrip.domain.usecase.GetAllTripsUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    getAllTripsUseCase: GetAllTripsUseCase,
    private val deleteTripUseCase: DeleteTripUseCase
) : ViewModel() {

    val trips: StateFlow<List<Trip>> = getAllTripsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            deleteTripUseCase(tripId)
                .onFailure { _userMessage.emit(it.message ?: "Error al eliminar el viaje") }
        }
    }

    companion object {
        fun provideFactory(
            getAllTripsUseCase: GetAllTripsUseCase,
            deleteTripUseCase: DeleteTripUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(getAllTripsUseCase, deleteTripUseCase) as T
            }
        }
    }
}
