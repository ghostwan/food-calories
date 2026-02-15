package com.foodcalories.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foodcalories.app.domain.model.DailyNutrition
import com.foodcalories.app.domain.model.NutritionGoal
import com.foodcalories.app.domain.model.WeightRecord
import com.foodcalories.app.domain.repository.UserProfileRepository
import com.foodcalories.app.domain.usecase.GetNutritionHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val historyUseCase: GetNutritionHistoryUseCase,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _history = MutableStateFlow<List<DailyNutrition>>(emptyList())
    val history: StateFlow<List<DailyNutrition>> = _history

    private val _weightHistory = MutableStateFlow<List<WeightRecord>>(emptyList())
    val weightHistory: StateFlow<List<WeightRecord>> = _weightHistory

    private val _goal = MutableStateFlow(NutritionGoal())
    val goal: StateFlow<NutritionGoal> = _goal

    init {
        loadData()
    }

    private fun loadData() {
        _goal.value = userProfileRepository.getGoal()
        viewModelScope.launch {
            _history.value = historyUseCase(30)
            _weightHistory.value = userProfileRepository.getWeightHistory(30)
        }
    }

    companion object {
        fun provideFactory(
            historyUseCase: GetNutritionHistoryUseCase,
            userProfileRepository: UserProfileRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(historyUseCase, userProfileRepository) as T
            }
        }
    }
}
