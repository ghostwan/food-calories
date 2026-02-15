package com.foodcalories.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dishName: String,
    val calories: Int,
    val proteins: Float,
    val carbs: Float,
    val fats: Float,
    val fiber: Float,
    val date: String, // yyyy-MM-dd
    val ingredientsJson: String
)
