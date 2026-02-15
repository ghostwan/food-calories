package com.ghostwan.snapcal.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghostwan.snapcal.domain.model.DailyNutrition
import com.ghostwan.snapcal.domain.model.MealEntry
import com.ghostwan.snapcal.domain.model.NutritionGoal
import com.ghostwan.snapcal.domain.model.UserProfile
import com.ghostwan.snapcal.domain.model.WeightRecord
import com.ghostwan.snapcal.domain.repository.MealRepository
import com.ghostwan.snapcal.domain.repository.UserProfileRepository
import com.ghostwan.snapcal.domain.usecase.GetNutritionHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val historyUseCase: GetNutritionHistoryUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val mealRepository: MealRepository
) : ViewModel() {

    private val _history = MutableStateFlow<List<DailyNutrition>>(emptyList())
    val history: StateFlow<List<DailyNutrition>> = _history

    private val _weightHistory = MutableStateFlow<List<WeightRecord>>(emptyList())
    val weightHistory: StateFlow<List<WeightRecord>> = _weightHistory

    private val _goal = MutableStateFlow(NutritionGoal())
    val goal: StateFlow<NutritionGoal> = _goal

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile

    private val _selectedDayMeals = MutableStateFlow<List<MealEntry>>(emptyList())
    val selectedDayMeals: StateFlow<List<MealEntry>> = _selectedDayMeals

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate

    init {
        loadData()
    }

    private fun loadData() {
        _goal.value = userProfileRepository.getGoal()
        _profile.value = userProfileRepository.getProfile()
        viewModelScope.launch {
            _history.value = historyUseCase(30)
            _weightHistory.value = userProfileRepository.getWeightHistory(30)
        }
    }

    fun selectDay(date: String) {
        if (_selectedDate.value == date) {
            _selectedDate.value = null
            _selectedDayMeals.value = emptyList()
        } else {
            _selectedDate.value = date
            viewModelScope.launch {
                _selectedDayMeals.value = mealRepository.getMealsForDate(date).first()
            }
        }
    }

    companion object {
        fun provideFactory(
            historyUseCase: GetNutritionHistoryUseCase,
            userProfileRepository: UserProfileRepository,
            mealRepository: MealRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(historyUseCase, userProfileRepository, mealRepository) as T
            }
        }
    }
}
