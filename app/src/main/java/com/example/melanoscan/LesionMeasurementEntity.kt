package com.example.melanoscan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lesion_measurements")
data class LesionMeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val lesionId: Long,

    val createdAt: Long,

    val imageName: String?,
    val measurementImagePath: String?,

    val predClass: String?,
    val melanomaProb: Float,

    val coinPx: Float,
    val coinRealMm: Double,

    val roiX1: Float,
    val roiY1: Float,
    val roiX2: Float,
    val roiY2: Float,
    val roiWidthPx: Float,
    val roiHeightPx: Float,

    val lesionAreaPx: Int,
    val lesionEqDiameterPx: Double,
    val lesionLongestAxisPx: Double,

    val lesionEqDiameterMm: Double,
    val lesionLongestAxisMm: Double
)