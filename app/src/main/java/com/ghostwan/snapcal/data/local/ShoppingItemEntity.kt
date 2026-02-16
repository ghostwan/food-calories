package com.ghostwan.snapcal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: String,
    val isChecked: Boolean = false,
    val addedDate: String // yyyy-MM-dd
)
