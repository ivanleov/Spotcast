package com.example.spotcast.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spotcast.SpotCastApplication
import com.example.spotcast.data.remote.dto.CapsuleResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CapsuleListViewModel(application: Application) : AndroidViewModel(application) {

    private val capsuleRepo = (application as SpotCastApplication).capsuleRepository

    private val _capsules = MutableStateFlow<List<CapsuleResponse>>(emptyList())
    val capsules: StateFlow<List<CapsuleResponse>> = _capsules.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMyCapsules()
    }

    fun loadMyCapsules() {
        viewModelScope.launch {
            _isLoading.value = true
            capsuleRepo.getMyCapsules()
                .onSuccess { _capsules.value = it }
            _isLoading.value = false
        }
    }
}
