package com.example.spotcast.data.repository

import com.example.spotcast.data.local.dao.CapsuleDao
import com.example.spotcast.data.remote.ApiService
import com.example.spotcast.data.remote.dto.CapsuleResponse
import com.example.spotcast.data.remote.dto.CompleteRequest
import com.example.spotcast.data.remote.dto.CreateCapsuleRequest
import com.example.spotcast.data.remote.dto.NearbyRequest
import com.example.spotcast.data.remote.dto.toEntity
import com.example.spotcast.data.remote.dto.toResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class CapsuleRepository(
    private val api: ApiService,
    private val dao: CapsuleDao,
) {


    suspend fun createCapsule(request: CreateCapsuleRequest): Result<CapsuleResponse> =
        runCatching {
            val response = api.createCapsule(request)
            dao.insert(response.toEntity())
            response
        }


    suspend fun createAudioCapsule(
        lat: Double,
        lon: Double,
        radius: Float,
        layer: String,
        audioFilePath: String,
    ): Result<CapsuleResponse> = runCatching {
        val file = File(audioFilePath)
        val audioBody = file.asRequestBody("audio/3gpp".toMediaType())
        val audioPart = MultipartBody.Part.createFormData("audio", file.name, audioBody)
        val plain = "text/plain".toMediaType()

        val response = api.createAudioCapsule(
            latitude = lat.toString().toRequestBody(plain),
            longitude = lon.toString().toRequestBody(plain),
            radius = radius.toString().toRequestBody(plain),
            layer = layer.toRequestBody(plain),
            audio = audioPart,
        )
        dao.insert(response.toEntity())
        response
    }


    suspend fun getNearby(
        lat: Double,
        lon: Double,
        radius: Double,
        layers: List<String>?,
    ): Result<List<CapsuleResponse>> {
        return try {
            val response = api.getNearby(NearbyRequest(lat, lon, radius, layers))
            dao.insertAll(response.map { it.toEntity() })
            Result.success(response)
        } catch (e: Exception) {
            val cached = dao.getActiveCapsules()
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toResponse() })
            } else {
                Result.failure(e)
            }
        }
    }


    suspend fun completeCapsule(capsuleId: Int): Result<Unit> = runCatching {
        api.completeCapsule(CompleteRequest(capsuleId))
        dao.markCompleted(capsuleId)
    }


    suspend fun getLayers(): Result<List<String>> = runCatching {
        api.getLayers().layers
    }


    suspend fun getMyCapsules(): Result<List<CapsuleResponse>> = runCatching {
        api.getMyCapsules()
    }
}
