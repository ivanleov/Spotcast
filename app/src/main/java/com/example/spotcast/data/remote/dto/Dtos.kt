package com.example.spotcast.data.remote.dto

import com.example.spotcast.data.local.entity.CapsuleEntity
import com.google.gson.annotations.SerializedName


data class LoginRequest(
    val username: String,
    val password: String,
)

data class RegisterRequest(
    val username: String,
    val password: String,
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
)


data class CreateCapsuleRequest(
    val latitude: Double,
    val longitude: Double,
    val radius: Double = 50.0,
    @SerializedName("capsule_type") val capsuleType: String,
    @SerializedName("text_content") val textContent: String? = null,
    val layer: String = "personal",
    @SerializedName("ttl_hours") val ttlHours: Int? = null,
    @SerializedName("recipient_username") val recipientUsername: String? = null,
)

data class CapsuleResponse(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
    @SerializedName("capsule_type") val capsuleType: String,
    @SerializedName("text_content") val textContent: String?,
    @SerializedName("audio_url") val audioUrl: String?,
    val layer: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("is_completed") val isCompleted: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("owner_username") val ownerUsername: String? = null,
    @SerializedName("recipient_username") val recipientUsername: String? = null,
)

fun CapsuleResponse.toEntity() = CapsuleEntity(
    id = id,
    latitude = latitude,
    longitude = longitude,
    radius = radius,
    capsuleType = capsuleType,
    textContent = textContent,
    audioUrl = audioUrl,
    layer = layer,
    isActive = isActive,
    isCompleted = isCompleted,
    createdAt = createdAt,
)

fun CapsuleEntity.toResponse() = CapsuleResponse(
    id = id,
    latitude = latitude,
    longitude = longitude,
    radius = radius,
    capsuleType = capsuleType,
    textContent = textContent,
    audioUrl = audioUrl,
    layer = layer,
    isActive = isActive,
    isCompleted = isCompleted,
    createdAt = createdAt,
)

data class NearbyRequest(
    val latitude: Double,
    val longitude: Double,
    val radius: Double = 500.0,
    val layers: List<String>? = null,
)

data class CompleteRequest(
    @SerializedName("capsule_id") val capsuleId: Int,
)


data class LayerResponse(
    val layers: List<String>,
)


data class FriendRequestDto(
    val username: String,
)

data class FriendActionDto(
    @SerializedName("friendship_id") val friendshipId: Int,
)

data class FriendshipResponse(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("friend_id") val friendId: Int,
    @SerializedName("friend_username") val friendUsername: String,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
)

data class UserInfo(
    val id: Int,
    val username: String,
    @SerializedName("created_at") val createdAt: String,
)

data class UserSearchResponse(
    val users: List<UserInfo>,
)
