package com.ghostwan.snapcal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_measurements")
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val waist: Float? = null,
    val hips: Float? = null,
    val chest: Float? = null,
    val arms: Float? = null,
    val thighs: Float? = null,
    val date: String
)
