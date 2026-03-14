package com.example.spotcast.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.spotcast.data.local.entity.CapsuleEntity

@Dao
interface CapsuleDao {

    @Query("SELECT * FROM capsules WHERE isCompleted = 0 AND isActive = 1")
    suspend fun getActiveCapsules(): List<CapsuleEntity>

    @Query("SELECT * FROM capsules")
    suspend fun getAll(): List<CapsuleEntity>

    @Query("SELECT * FROM capsules WHERE id = :id")
    suspend fun getById(id: Int): CapsuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(capsules: List<CapsuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(capsule: CapsuleEntity)

    @Query("UPDATE capsules SET isCompleted = 1 WHERE id = :id")
    suspend fun markCompleted(id: Int)

    @Query("DELETE FROM capsules")
    suspend fun deleteAll()
}
