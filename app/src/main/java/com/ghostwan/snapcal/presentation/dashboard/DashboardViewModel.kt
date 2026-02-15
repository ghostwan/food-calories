package com.ghostwan.snapcal.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghostwan.snapcal.domain.model.DailyNutrition
import com.ghostwan.snapcal.domain.model.MealEntry
import com.ghostwan.snapcal.domain.model.NutritionGoal
import com.ghostwan.snapcal.domain.repository.MealRepository
import com.ghostwan.snapcal.domain.repository.UserProfileRepository
import com.ghostwan.snapcal.domain.usecase.GetDailyNutritionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardViewModel(
    private val getDailyNutritionUseCase: GetDailyNutritionUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val mealRepository: MealRepository
) : ViewModel() {

    private val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val _nutrition = MutableStateFlow<DailyNutrition?>(null)
    val nutrition: StateFlow<DailyNutrition?> = _nutrition

    private val _meals = MutableStateFlow<List<MealEntry>>(emptyList())
    val meals: StateFlow<List<MealEntry>> = _meals

    private val _goal = MutableStateFlow(NutritionGoal())
    val goal: StateFlow<NutritionGoal> = _goal

    init {
        loadGoal()
        observeNutrition()
        observeMeals()
    }

    private fun loadGoal() {
        _goal.value = userProfileRepository.getGoal()
    }

    private fun observeNutrition() {
        viewModelScope.launch {
            getDailyNutritionUseCase.getNutrition(today).collect {
                _nutrition.value = it
            }
        }
    }

    private fun observeMeals() {
        viewModelScope.launch {
            getDailyNutritionUseCase.getMeals(today).collect {
                _meals.value = it
            }
        }
    }

    fun refresh() {
        loadGoal()
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch {
            mealRepository.deleteMeal(id)
        }
    }

    companion object {
        fun provideFactory(
            getDailyNutritionUseCase: GetDailyNutritionUseCase,
            userProfileRepository: UserProfileRepository,
            mealRepository: com.ghostwan.snapcal.domain.repository.MealRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(getDailyNutritionUseCase, userProfileRepository, mealRepository) as T
            }
        }
    }
}
