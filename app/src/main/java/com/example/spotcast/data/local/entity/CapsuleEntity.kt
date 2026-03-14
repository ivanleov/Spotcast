package com.example.spotcast.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "capsules")
data class CapsuleEntity(
    @PrimaryKey val id: Int,
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
    val capsuleType: String,
    val textContent: String?,
    val audioUrl: String?,
    val layer: String,
    val isActive: Boolean,
    val isCompleted: Boolean,
    val createdAt: String,
)
