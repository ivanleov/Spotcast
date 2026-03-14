package com.example.spotcast.ui.viewmodel

import android.app.Application
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spotcast.SpotCastApplication
import com.example.spotcast.data.remote.dto.CapsuleResponse
import com.example.spotcast.data.remote.dto.CreateCapsuleRequest
import com.example.spotcast.data.remote.dto.toEntity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SpotCastApplication
    private val capsuleRepo = app.capsuleRepository
    private val geofenceManager = app.geofenceManager
    val ttsManager = app.ttsManager
    val audioPlayer = app.audioPlayer


    private val _capsules = MutableStateFlow<List<CapsuleResponse>>(emptyList())
    val capsules: StateFlow<List<CapsuleResponse>> = _capsules.asStateFlow()

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    private val _activeLayers = MutableStateFlow(
        setOf("personal", "work", "city", "logistics", "social")
    )
    val activeLayers: StateFlow<Set<String>> = _activeLayers.asStateFlow()

    private val _availableLayers = MutableStateFlow(
        listOf("personal", "work", "city", "logistics", "social")
    )
    val availableLayers: StateFlow<List<String>> = _availableLayers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var locationCallback: LocationCallback? = null
    private var fetchJob: Job? = null
    private var pollingJob: Job? = null

    init {
        loadLayers()
        startPolling()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(15_000L) // Poll every 15 seconds
                _userLocation.value?.let { (lat, lon) ->
                    fetchNearbyCapsules(lat, lon, showLoading = false)
                }
            }
        }
    }

    @Suppress("MissingPermission")
    fun startLocationUpdates() {
        val client = LocationServices.getFusedLocationProviderClient(
            getApplication<Application>()
        )

        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null && _userLocation.value == null) {
                _userLocation.value = loc.latitude to loc.longitude
                fetchNearbyCapsules(loc.latitude, loc.longitude)
            }
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            15_000L,
        )
            .setMinUpdateIntervalMillis(10_000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    _userLocation.value = loc.latitude to loc.longitude
                    fetchNearbyCapsules(loc.latitude, loc.longitude)
                }
            }
        }
        locationCallback = callback

        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            _error.value = "Location permission required"
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let { cb ->
            LocationServices.getFusedLocationProviderClient(getApplication<Application>())
                .removeLocationUpdates(cb)
        }
    }


fun fetchNearbyCapsules(lat: Double, lon: Double, showLoading: Boolean = true) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            if (showLoading) {
                delay(300)
                _isLoading.value = true
            }
            capsuleRepo.getNearby(lat, lon, 25_000.0, _activeLayers.value.toList())
                .onSuccess { list ->
                    _capsules.value = list
                    geofenceManager.registerGeofences(list.map { it.toEntity() })
                }
                .onFailure { _error.value = it.message }
            if (showLoading) {
                _isLoading.value = false
            }
        }
    }

    fun createTextCapsule(
        lat: Double,
        lon: Double,
        radius: Double,
        text: String,
        layer: String,
        ttlHours: Int?,
        recipientUsername: String? = null,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            capsuleRepo.createCapsule(
                CreateCapsuleRequest(
                    latitude = lat,
                    longitude = lon,
                    radius = radius,
                    capsuleType = "TEXT",
                    textContent = text,
                    layer = layer,
                    ttlHours = ttlHours,
                    recipientUsername = recipientUsername,
                )
            )
                .onSuccess { _capsules.value = _capsules.value + it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun createAudioCapsule(
        lat: Double,
        lon: Double,
        radius: Float,
        layer: String,
        audioPath: String,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            capsuleRepo.createAudioCapsule(lat, lon, radius, layer, audioPath)
                .onSuccess { _capsules.value = _capsules.value + it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun completeCapsule(id: Int) {
        viewModelScope.launch {
            capsuleRepo.completeCapsule(id)
                .onSuccess {
                    _capsules.value = _capsules.value.map {
                        if (it.id == id) it.copy(isCompleted = true) else it
                    }
                }
        }
    }


    private fun loadLayers() {
        viewModelScope.launch {
            capsuleRepo.getLayers()
                .onSuccess { _availableLayers.value = it }
        }
    }

    fun toggleLayer(layer: String) {
        val current = _activeLayers.value.toMutableSet()
        if (current.contains(layer)) current.remove(layer) else current.add(layer)
        _activeLayers.value = current

        _userLocation.value?.let { (lat, lon) ->
            fetchNearbyCapsules(lat, lon)
        }
    }

    fun clearError() {
        _error.value = null
    }


    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}
