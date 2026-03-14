package com.example.spotcast.data.repository

import com.example.spotcast.data.remote.ApiService
import com.example.spotcast.data.remote.dto.FriendActionDto
import com.example.spotcast.data.remote.dto.FriendRequestDto
import com.example.spotcast.data.remote.dto.FriendshipResponse
import com.example.spotcast.data.remote.dto.UserInfo

class FriendsRepository(private val api: ApiService) {

    suspend fun searchUsers(query: String): Result<List<UserInfo>> = runCatching {
        api.searchUsers(query).users
    }

    suspend fun sendFriendRequest(username: String): Result<FriendshipResponse> = runCatching {
        api.sendFriendRequest(FriendRequestDto(username))
    }

    suspend fun acceptFriend(friendshipId: Int): Result<FriendshipResponse> = runCatching {
        api.acceptFriend(FriendActionDto(friendshipId))
    }

    suspend fun rejectFriend(friendshipId: Int): Result<Unit> = runCatching {
        api.rejectFriend(FriendActionDto(friendshipId))
        Unit
    }

    suspend fun getFriends(): Result<List<FriendshipResponse>> = runCatching {
        api.getFriends()
    }

    suspend fun getPendingRequests(): Result<List<FriendshipResponse>> = runCatching {
        api.getPendingRequests()
    }
}
