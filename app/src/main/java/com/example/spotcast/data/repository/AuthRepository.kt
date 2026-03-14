package com.example.spotcast.data.repository

import com.example.spotcast.data.preferences.TokenManager
import com.example.spotcast.data.remote.ApiService
import com.example.spotcast.data.remote.dto.LoginRequest
import com.example.spotcast.data.remote.dto.RegisterRequest

class AuthRepository(
    private val api: ApiService,
    private val tokenManager: TokenManager,
) {

    suspend fun login(username: String, password: String): Result<String> = runCatching {
        val response = api.login(LoginRequest(username, password))
        tokenManager.saveToken(response.accessToken)
        tokenManager.saveUsername(username)
        response.accessToken
    }

    suspend fun register(username: String, password: String): Result<String> = runCatching {
        val response = api.register(RegisterRequest(username, password))
        tokenManager.saveToken(response.accessToken)
        tokenManager.saveUsername(username)
        response.accessToken
    }

    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    fun logout() {
        tokenManager.clearToken()
    }
}
