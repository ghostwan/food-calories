package com.ghostwan.snapcal.presentation.shopping

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghostwan.snapcal.domain.model.ShoppingItem
import com.ghostwan.snapcal.domain.repository.ShoppingRepository
import com.ghostwan.snapcal.widget.ShoppingWidgetProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ShoppingListViewModel(
    private val shoppingRepository: ShoppingRepository,
    private val appContext: Context
) : ViewModel() {

    val items: StateFlow<List<ShoppingItem>> = shoppingRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addItem(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            shoppingRepository.addItem(
                ShoppingItem(
                    name = trimmed,
                    quantity = "",
                    addedDate = LocalDate.now().toString()
                )
            )
            ShoppingWidgetProvider.updateAllWidgets(appContext)
        }
    }

    fun toggleChecked(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingRepository.setChecked(item.id, !item.isChecked)
            ShoppingWidgetProvider.updateAllWidgets(appContext)
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            shoppingRepository.delete(id)
            ShoppingWidgetProvider.updateAllWidgets(appContext)
        }
    }

    fun deleteChecked() {
        viewModelScope.launch {
            shoppingRepository.deleteChecked()
            ShoppingWidgetProvider.updateAllWidgets(appContext)
        }
    }

    companion object {
        fun provideFactory(
            shoppingRepository: ShoppingRepository,
            appContext: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ShoppingListViewModel(shoppingRepository, appContext) as T
            }
        }
    }
}
