package com.ghostwan.snapcal.presentation.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghostwan.snapcal.domain.model.ShoppingItem
import com.ghostwan.snapcal.domain.repository.ShoppingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingListViewModel(
    private val shoppingRepository: ShoppingRepository
) : ViewModel() {

    val items: StateFlow<List<ShoppingItem>> = shoppingRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleChecked(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingRepository.setChecked(item.id, !item.isChecked)
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            shoppingRepository.delete(id)
        }
    }

    fun deleteChecked() {
        viewModelScope.launch {
            shoppingRepository.deleteChecked()
        }
    }

    companion object {
        fun provideFactory(
            shoppingRepository: ShoppingRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ShoppingListViewModel(shoppingRepository) as T
            }
        }
    }
}
