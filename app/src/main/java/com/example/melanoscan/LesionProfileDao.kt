package com.example.melanoscan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LesionProfileDao {

    @Insert
    fun insert(profile: LesionProfileEntity): Long

    @Query("SELECT * FROM lesion_profiles ORDER BY createdAt DESC")
    fun getAll(): List<LesionProfileEntity>

    @Query("SELECT * FROM lesion_profiles WHERE lesionId = :lesionId LIMIT 1")
    fun getById(lesionId: Long): LesionProfileEntity?

    @Query("DELETE FROM lesion_profiles WHERE lesionId = :lesionId")
    fun deleteByLesionId(lesionId: Long)

    @Query("DELETE FROM lesion_profiles WHERE lesionId = :lesionId")
    fun deleteById(lesionId: Long)

}