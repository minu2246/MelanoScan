package com.example.melanoscan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LesionMeasurementDao {

    @Insert
    fun insert(measurement: LesionMeasurementEntity): Long

    @Query("SELECT * FROM lesion_measurements WHERE id = :id LIMIT 1")
    fun getById(id: Long): LesionMeasurementEntity?

    @Query("SELECT * FROM lesion_measurements ORDER BY createdAt ASC")
    fun getAllAsc(): List<LesionMeasurementEntity>

    @Query("SELECT * FROM lesion_measurements ORDER BY createdAt DESC")
    fun getAllDesc(): List<LesionMeasurementEntity>

    @Query("SELECT * FROM lesion_measurements WHERE lesionId = :lesionId ORDER BY createdAt ASC")
    fun getByLesionIdAsc(lesionId: Long): List<LesionMeasurementEntity>

    @Query("SELECT * FROM lesion_measurements WHERE lesionId = :lesionId ORDER BY createdAt DESC")
    fun getByLesionIdDesc(lesionId: Long): List<LesionMeasurementEntity>

    @Query("SELECT COUNT(*) FROM lesion_measurements")
    fun count(): Int

    @Query("SELECT COUNT(*) FROM lesion_measurements WHERE lesionId = :lesionId")
    fun countByLesionId(lesionId: Long): Int

    @Query("DELETE FROM lesion_measurements WHERE lesionId = :lesionId")
    fun deleteByLesionId(lesionId: Long)
}