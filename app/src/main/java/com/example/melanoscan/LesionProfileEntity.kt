package com.example.melanoscan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lesion_profiles")
data class LesionProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val lesionId: Long = 0,
    val lesionName: String,
    val createdAt: Long
)