package com.ghostwan.snapcal.data.repository

import com.ghostwan.snapcal.data.local.ShoppingItemDao
import com.ghostwan.snapcal.data.local.ShoppingItemEntity
import com.ghostwan.snapcal.domain.model.ShoppingItem
import com.ghostwan.snapcal.domain.repository.ShoppingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShoppingRepositoryImpl(private val dao: ShoppingItemDao) : ShoppingRepository {

    override suspend fun addItem(item: ShoppingItem) {
        dao.insert(
            ShoppingItemEntity(
                name = item.name,
                quantity = item.quantity,
                isChecked = item.isChecked,
                addedDate = item.addedDate
            )
        )
    }

    override fun getAll(): Flow<List<ShoppingItem>> {
        return dao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun setChecked(id: Long, checked: Boolean) {
        dao.setChecked(id, checked)
    }

    override suspend fun delete(id: Long) {
        dao.delete(id)
    }

    override suspend fun deleteChecked() {
        dao.deleteChecked()
    }

    private fun ShoppingItemEntity.toDomain() = ShoppingItem(
        id = id,
        name = name,
        quantity = quantity,
        isChecked = isChecked,
        addedDate = addedDate
    )
}
