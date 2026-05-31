package com.twort.fuelapp.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.twort.fuelapp.data.model.FuelSettings
import com.twort.fuelapp.data.model.FuelStation
import com.twort.fuelapp.data.repository.FuelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class FuelUiState(
    val isLoading: Boolean = false,
    val stations: List<FuelStation> = emptyList(),
    val error: String? = null,
    val userLocation: Location? = null,
    val settings: FuelSettings = FuelSettings(),
)

class FuelViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FuelRepository()
    private val fusedLocation = LocationServices.getFusedLocationProviderClient(application)

    private val _uiState = MutableStateFlow(FuelUiState())
    val uiState: StateFlow<FuelUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = repository.loadSettings(getApplication())
            _uiState.update { it.copy(settings = saved) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val location = getLocation()
            if (location == null) {
                _uiState.update { it.copy(isLoading = false, error = "Could not get location. Check permissions.") }
                return@launch
            }
            _uiState.update { it.copy(userLocation = location) }

            val settings = _uiState.value.settings
            repository.fetch(location.latitude, location.longitude, settings)
                .onSuccess { response ->
                    val stations = listOf(response.best) + response.alternatives
                    _uiState.update { it.copy(isLoading = false, stations = stations) }
                    repository.updateWidget(getApplication(), stations.first())
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoading = false, error = err.message ?: "Unknown error") }
                }
        }
    }

    fun updateSettings(settings: FuelSettings) {
        _uiState.update { it.copy(settings = settings) }
        viewModelScope.launch {
            repository.saveSettings(getApplication(), settings)
        }
        refresh()
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLocation(): Location? = runCatching {
        fusedLocation.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            ?: fusedLocation.lastLocation.await()
    }.getOrNull()
}
