package com.example.spotcast.data.remote

import com.example.spotcast.data.remote.dto.CapsuleResponse
import com.example.spotcast.data.remote.dto.CompleteRequest
import com.example.spotcast.data.remote.dto.CreateCapsuleRequest
import com.example.spotcast.data.remote.dto.LayerResponse
import com.example.spotcast.data.remote.dto.LoginRequest
import com.example.spotcast.data.remote.dto.NearbyRequest
import com.example.spotcast.data.remote.dto.RegisterRequest
import com.example.spotcast.data.remote.dto.TokenResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body req: LoginRequest): TokenResponse

    @POST("auth/register")
    suspend fun register(@Body req: RegisterRequest): TokenResponse

    @POST("capsules/create")
    suspend fun createCapsule(@Body req: CreateCapsuleRequest): CapsuleResponse

    @Multipart
    @POST("capsules/create_audio")
    suspend fun createAudioCapsule(
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("radius") radius: RequestBody,
        @Part("layer") layer: RequestBody,
        @Part audio: MultipartBody.Part,
    ): CapsuleResponse

    @POST("capsules/nearby")
    suspend fun getNearby(@Body req: NearbyRequest): List<CapsuleResponse>

    @POST("capsules/complete")
    suspend fun completeCapsule(@Body req: CompleteRequest): Map<String, Any>

    @GET("capsules/my")
    suspend fun getMyCapsules(): List<CapsuleResponse>

    @GET("layers")
    suspend fun getLayers(): LayerResponse

    @GET("friends/search")
    suspend fun searchUsers(@retrofit2.http.Query("q") query: String): com.example.spotcast.data.remote.dto.UserSearchResponse

    @POST("friends/request")
    suspend fun sendFriendRequest(@Body req: com.example.spotcast.data.remote.dto.FriendRequestDto): com.example.spotcast.data.remote.dto.FriendshipResponse

    @POST("friends/accept")
    suspend fun acceptFriend(@Body req: com.example.spotcast.data.remote.dto.FriendActionDto): com.example.spotcast.data.remote.dto.FriendshipResponse

    @POST("friends/reject")
    suspend fun rejectFriend(@Body req: com.example.spotcast.data.remote.dto.FriendActionDto): Map<String, String>

    @GET("friends/list")
    suspend fun getFriends(): List<com.example.spotcast.data.remote.dto.FriendshipResponse>

    @GET("friends/pending")
    suspend fun getPendingRequests(): List<com.example.spotcast.data.remote.dto.FriendshipResponse>
}
