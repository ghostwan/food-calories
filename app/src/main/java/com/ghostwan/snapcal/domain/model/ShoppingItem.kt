package com.ghostwan.snapcal.domain.model

data class ShoppingItem(
    val id: Long = 0,
    val name: String,
    val quantity: String,
    val isChecked: Boolean = false,
    val addedDate: String
)
