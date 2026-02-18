package com.ghostwan.snapcal.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghostwan.snapcal.data.local.HealthConnectManager
import com.ghostwan.snapcal.domain.model.DailyNutrition
import com.ghostwan.snapcal.domain.model.MealEntry
import com.ghostwan.snapcal.domain.model.NutritionGoal
import com.ghostwan.snapcal.domain.model.WeightRecord
import com.ghostwan.snapcal.domain.repository.MealRepository
import com.ghostwan.snapcal.domain.repository.UserProfileRepository
import com.ghostwan.snapcal.domain.usecase.GetDailyNutritionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

class DashboardViewModel(
    private val getDailyNutritionUseCase: GetDailyNutritionUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val mealRepository: MealRepository,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    val isToday: Boolean
        get() = _selectedDate.value == LocalDate.now()

    private val _nutrition = MutableStateFlow<DailyNutrition?>(null)
    val nutrition: StateFlow<DailyNutrition?> = _nutrition

    private val _meals = MutableStateFlow<List<MealEntry>>(emptyList())
    val meals: StateFlow<List<MealEntry>> = _meals

    private val _goal = MutableStateFlow(NutritionGoal())
    val goal: StateFlow<NutritionGoal> = _goal

    private val _favorites = MutableStateFlow<List<MealEntry>>(emptyList())
    val favorites: StateFlow<List<MealEntry>> = _favorites

    private val _caloriesBurned = MutableStateFlow(0)
    val caloriesBurned: StateFlow<Int> = _caloriesBurned

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode

    private val _selectedMealIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedMealIds: StateFlow<Set<Long>> = _selectedMealIds

    init {
        loadGoal()
        observeNutrition()
        observeMeals()
        observeFavorites()
        loadCaloriesBurned()
        loadLatestWeight()
    }

    fun goToPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
        onDateChanged()
    }

    fun goToNextDay() {
        val next = _selectedDate.value.plusDays(1)
        if (!next.isAfter(LocalDate.now())) {
            _selectedDate.value = next
            onDateChanged()
        }
    }

    fun goToDate(date: LocalDate) {
        if (!date.isAfter(LocalDate.now())) {
            _selectedDate.value = date
            onDateChanged()
        }
    }

    private fun onDateChanged() {
        observeNutrition()
        observeMeals()
        loadCaloriesBurned()
    }

    private fun loadGoal() {
        _goal.value = userProfileRepository.getGoal()
    }

    private var nutritionJob: Job? = null
    private var mealsJob: Job? = null

    private fun observeNutrition() {
        nutritionJob?.cancel()
        nutritionJob = viewModelScope.launch {
            getDailyNutritionUseCase.getNutrition(_selectedDate.value.toString()).collect {
                _nutrition.value = it
            }
        }
    }

    private fun observeMeals() {
        mealsJob?.cancel()
        mealsJob = viewModelScope.launch {
            getDailyNutritionUseCase.getMeals(_selectedDate.value.toString()).collect {
                _meals.value = it
            }
        }
    }

    private fun loadLatestWeight() {
        if (!healthConnectManager.isAvailable()) return
        viewModelScope.launch {
            try {
                if (healthConnectManager.hasPermissions()) {
                    val records = healthConnectManager.readWeightRecords(1)
                    if (records.isNotEmpty()) {
                        val latest = records.maxByOrNull { it.time }!!
                        val profile = userProfileRepository.getProfile()
                        if (latest.weightKg != profile.weight) {
                            userProfileRepository.saveProfile(profile.copy(weight = latest.weightKg))
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val date = dateFormat.format(Date.from(latest.time))
                            userProfileRepository.saveWeightRecord(
                                WeightRecord(weight = latest.weightKg, date = date)
                            )
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun loadCaloriesBurned() {
        if (!healthConnectManager.isAvailable()) return
        viewModelScope.launch {
            try {
                if (healthConnectManager.hasPermissions()) {
                    val date = _selectedDate.value
                    val burned = healthConnectManager.readCaloriesBurnedForDateRange(date, date)
                    _caloriesBurned.value = burned.values.firstOrNull()?.toInt() ?: 0
                }
            } catch (_: Exception) { }
        }
    }

    fun refresh() {
        val now = LocalDate.now()
        // Auto-advance to today if user was viewing today and the day changed
        if (_selectedDate.value != now && _selectedDate.value == now.minusDays(1)) {
            // Only auto-advance if the user was on "today" (which became yesterday)
            // We can't distinguish this reliably, so just reload data for the current date
        }
        loadGoal()
        observeNutrition()
        observeMeals()
        loadCaloriesBurned()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            mealRepository.getFavorites().collect {
                _favorites.value = it
            }
        }
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch {
            mealRepository.deleteMeal(id)
        }
    }

    fun toggleFavorite(meal: MealEntry) {
        viewModelScope.launch {
            mealRepository.setFavorite(meal.id, !meal.isFavorite)
        }
    }

    fun updateMealEmoji(mealId: Long, emoji: String) {
        viewModelScope.launch {
            mealRepository.updateEmoji(mealId, emoji)
        }
    }

    fun enterSelectionMode(mealId: Long) {
        _selectionMode.value = true
        _selectedMealIds.value = setOf(mealId)
    }

    fun toggleMealSelection(mealId: Long) {
        val current = _selectedMealIds.value
        _selectedMealIds.value = if (mealId in current) current - mealId else current + mealId
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedMealIds.value = emptySet()
    }

    fun getSelectedMeals(): List<MealEntry> {
        val ids = _selectedMealIds.value
        return _meals.value.filter { it.id in ids }
    }

    fun quickAddFavorite(meal: MealEntry) {
        viewModelScope.launch {
            val targetMeal = meal.copy(
                id = 0,
                date = _selectedDate.value.toString(),
                isFavorite = false
            )
            mealRepository.saveMeal(targetMeal)
        }
    }

    companion object {
        fun provideFactory(
            getDailyNutritionUseCase: GetDailyNutritionUseCase,
            userProfileRepository: UserProfileRepository,
            mealRepository: com.ghostwan.snapcal.domain.repository.MealRepository,
            healthConnectManager: HealthConnectManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(getDailyNutritionUseCase, userProfileRepository, mealRepository, healthConnectManager) as T
            }
        }
    }
}
