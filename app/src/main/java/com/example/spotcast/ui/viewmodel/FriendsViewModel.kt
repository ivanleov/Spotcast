package com.example.spotcast.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spotcast.SpotCastApplication
import com.example.spotcast.data.remote.dto.FriendshipResponse
import com.example.spotcast.data.remote.dto.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendsUiState(
    val searchResults: List<UserInfo> = emptyList(),
    val friends: List<FriendshipResponse> = emptyList(),
    val pendingRequests: List<FriendshipResponse> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
)

class FriendsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as SpotCastApplication).friendsRepository

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        loadFriends()
        loadPending()
    }

    fun searchUsers(query: String) {
        if (query.length < 2) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }
        viewModelScope.launch {
            repo.searchUsers(query)
                .onSuccess { _uiState.value = _uiState.value.copy(searchResults = it) }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message) }
        }
    }

    fun sendRequest(username: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repo.sendFriendRequest(username)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Friend request sent!",
                        searchResults = emptyList(),
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = it.message,
                    )
                }
        }
    }

    fun acceptRequest(friendshipId: Int) {
        viewModelScope.launch {
            repo.acceptFriend(friendshipId)
                .onSuccess {
                    loadFriends()
                    loadPending()
                }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message) }
        }
    }

    fun rejectRequest(friendshipId: Int) {
        viewModelScope.launch {
            repo.rejectFriend(friendshipId)
                .onSuccess { loadPending() }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message) }
        }
    }

    fun removeFriend(friendshipId: Int) {
        viewModelScope.launch {
            repo.rejectFriend(friendshipId)
                .onSuccess { loadFriends() }
                .onFailure { _uiState.value = _uiState.value.copy(message = it.message) }
        }
    }

    fun loadFriends() {
        viewModelScope.launch {
            repo.getFriends()
                .onSuccess { _uiState.value = _uiState.value.copy(friends = it) }
        }
    }

    fun loadPending() {
        viewModelScope.launch {
            repo.getPendingRequests()
                .onSuccess { _uiState.value = _uiState.value.copy(pendingRequests = it) }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
