package com.mototrip.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mototrip.core.updater.AppUpdateInfo
import com.mototrip.core.updater.AppUpdateManager
import com.mototrip.domain.model.Trip
import com.mototrip.domain.usecase.GetActiveTripUseCase
import com.mototrip.domain.usecase.StartTripUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(val activeTrip: Trip?) : HomeUiState
}

class HomeViewModel(
    getActiveTripUseCase: GetActiveTripUseCase,
    private val startTripUseCase: StartTripUseCase,
    val updateManager: AppUpdateManager
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

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

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

    fun checkForUpdates(userInitiated: Boolean = true) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            updateManager.checkForUpdates().fold(
                onSuccess = { info ->
                    _isCheckingUpdate.value = false
                    if (info.isUpdateAvailable) {
                        _updateInfo.value = info
                    } else if (userInitiated) {
                        _errorMessage.emit("Tenés instalada la versión más reciente (${info.currentVersion}).")
                    }
                },
                onFailure = { error ->
                    _isCheckingUpdate.value = false
                    if (userInitiated) {
                        _errorMessage.emit("No se pudo verificar actualizaciones: ${error.message ?: "Sin conexión"}")
                    }
                }
            )
        }
    }

    fun dismissUpdateDialog() {
        _updateInfo.value = null
        _downloadProgress.value = null
    }

    fun downloadAndInstallUpdate(info: AppUpdateInfo) {
        val url = info.apkDownloadUrl ?: return
        val fileName = info.apkFileName ?: "MotoTrip-${info.latestVersion}.apk"

        viewModelScope.launch {
            _downloadProgress.value = 0.01f
            updateManager.downloadApk(url, fileName) { progress ->
                _downloadProgress.value = progress
            }.fold(
                onSuccess = { apkFile ->
                    _downloadProgress.value = null
                    _updateInfo.value = null
                    updateManager.startInstall(apkFile)
                },
                onFailure = { error ->
                    _downloadProgress.value = null
                    _errorMessage.emit("Error al descargar actualización: ${error.message}")
                }
            )
        }
    }

    companion object {
        fun provideFactory(
            getActiveTripUseCase: GetActiveTripUseCase,
            startTripUseCase: StartTripUseCase,
            appUpdateManager: AppUpdateManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(getActiveTripUseCase, startTripUseCase, appUpdateManager) as T
            }
        }
    }
}
