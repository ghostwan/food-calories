package com.ghostwan.snapcal.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghostwan.snapcal.data.local.HealthConnectManager
import com.ghostwan.snapcal.domain.model.DailyNutrition
import com.ghostwan.snapcal.domain.model.MealEntry
import com.ghostwan.snapcal.domain.model.NutritionGoal
import com.ghostwan.snapcal.domain.model.UserProfile
import com.ghostwan.snapcal.domain.model.WeightRecord
import com.ghostwan.snapcal.domain.repository.MealRepository
import com.ghostwan.snapcal.domain.repository.UserProfileRepository
import com.ghostwan.snapcal.domain.usecase.GetNutritionHistoryUseCase
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val historyUseCase: GetNutritionHistoryUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val mealRepository: MealRepository,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _history = MutableStateFlow<List<DailyNutrition>>(emptyList())
    val history: StateFlow<List<DailyNutrition>> = _history

    private val _weightHistory = MutableStateFlow<List<WeightRecord>>(emptyList())
    val weightHistory: StateFlow<List<WeightRecord>> = _weightHistory

    private val _burnedCaloriesHistory = MutableStateFlow<Map<String, Int>>(emptyMap())
    val burnedCaloriesHistory: StateFlow<Map<String, Int>> = _burnedCaloriesHistory

    private val _goal = MutableStateFlow(NutritionGoal())
    val goal: StateFlow<NutritionGoal> = _goal

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile

    private val _selectedDayMeals = MutableStateFlow<List<MealEntry>>(emptyList())
    val selectedDayMeals: StateFlow<List<MealEntry>> = _selectedDayMeals

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate

    private val _selectedRange = MutableStateFlow(30)
    val selectedRange: StateFlow<Int> = _selectedRange

    private var historyJob: Job? = null

    init {
        _goal.value = userProfileRepository.getGoal()
        _profile.value = userProfileRepository.getProfile()
        loadForRange(30)
    }

    fun setRange(days: Int) {
        _selectedRange.value = days
        loadForRange(days)
    }

    private fun loadForRange(days: Int) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            historyUseCase(days).collect {
                _history.value = it
            }
        }
        viewModelScope.launch {
            _weightHistory.value = userProfileRepository.getWeightHistory(days)
        }
        loadBurnedCalories(days)
    }

    private fun loadBurnedCalories(days: Int) {
        if (!healthConnectManager.isAvailable()) return
        viewModelScope.launch {
            try {
                if (healthConnectManager.hasPermissions()) {
                    val endDate = LocalDate.now()
                    val startDate = endDate.minusDays(days.toLong())
                    val burned = healthConnectManager.readCaloriesBurnedForDateRange(startDate, endDate)
                    _burnedCaloriesHistory.value = burned.mapValues { it.value.toInt() }
                }
            } catch (_: Exception) { }
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
        val RANGE_OPTIONS = listOf(7, 30, 90, 365)

        fun provideFactory(
            historyUseCase: GetNutritionHistoryUseCase,
            userProfileRepository: UserProfileRepository,
            mealRepository: MealRepository,
            healthConnectManager: HealthConnectManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(historyUseCase, userProfileRepository, mealRepository, healthConnectManager) as T
            }
        }
    }
}
