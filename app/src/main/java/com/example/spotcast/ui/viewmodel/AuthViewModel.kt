package com.example.spotcast.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spotcast.SpotCastApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as SpotCastApplication).authRepository

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            repo.login(username, password)
                .onSuccess { _uiState.value = AuthUiState(isSuccess = true) }
                .onFailure { _uiState.value = AuthUiState(error = it.message ?: "Login failed") }
        }
    }

    fun register(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            repo.register(username, password)
                .onSuccess { _uiState.value = AuthUiState(isSuccess = true) }
                .onFailure { _uiState.value = AuthUiState(error = it.message ?: "Registration failed") }
        }
    }
}
