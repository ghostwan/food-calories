package com.ghostwan.snapcal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_records")
data class WeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weight: Float,
    val date: String
)
