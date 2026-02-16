package com.ghostwan.snapcal.domain.repository

import com.ghostwan.snapcal.domain.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

interface ShoppingRepository {
    suspend fun addItem(item: ShoppingItem)
    fun getAll(): Flow<List<ShoppingItem>>
    suspend fun setChecked(id: Long, checked: Boolean)
    suspend fun delete(id: Long)
    suspend fun deleteChecked()
}
