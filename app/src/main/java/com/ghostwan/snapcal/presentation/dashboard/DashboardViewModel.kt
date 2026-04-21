package com.ghostwan.snapcal.presentation.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghostwan.snapcal.data.local.HealthConnectManager
import com.ghostwan.snapcal.data.remote.GeminiApiService
import com.ghostwan.snapcal.widget.CaloriesWidgetProvider
import com.ghostwan.snapcal.domain.model.DailyNutrition
import com.ghostwan.snapcal.domain.model.MealEntry
import com.ghostwan.snapcal.domain.model.NutritionGoal
import com.ghostwan.snapcal.domain.model.WeightRecord
import com.ghostwan.snapcal.domain.repository.DailyNoteRepository
import com.ghostwan.snapcal.domain.repository.MealRepository
import com.ghostwan.snapcal.domain.repository.SettingsRepository
import com.ghostwan.snapcal.domain.repository.UserProfileRepository
import com.ghostwan.snapcal.domain.usecase.GetDailyNutritionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

class DashboardViewModel(
    private val getDailyNutritionUseCase: GetDailyNutritionUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val mealRepository: MealRepository,
    private val healthConnectManager: HealthConnectManager,
    private val dailyNoteRepository: DailyNoteRepository,
    private val settingsRepository: SettingsRepository,
    private val geminiApiService: GeminiApiService,
    private val appContext: Context
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

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

    private val _effectiveGoal = MutableStateFlow(NutritionGoal())
    val effectiveGoal: StateFlow<NutritionGoal> = _effectiveGoal

    private val _dailyNote = MutableStateFlow<String?>(null)
    val dailyNote: StateFlow<String?> = _dailyNote

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode

    private val _selectedMealIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedMealIds: StateFlow<Set<Long>> = _selectedMealIds

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak

    private val _suggestions = MutableStateFlow<List<MealSuggestion>>(emptyList())
    val suggestions: StateFlow<List<MealSuggestion>> = _suggestions

    private val _suggestionsLoading = MutableStateFlow(false)
    val suggestionsLoading: StateFlow<Boolean> = _suggestionsLoading

    init {
        loadGoal()
        observeNutrition()
        observeMeals()
        observeFavorites()
        observeDailyNote()
        loadCaloriesBurned()
        loadLatestWeight()
        observeEffectiveGoal()
        loadStreak()
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
        _isLoading.value = true
        observeNutrition()
        observeMeals()
        observeDailyNote()
        loadCaloriesBurned()
    }

    private fun loadGoal() {
        _goal.value = userProfileRepository.getGoal()
    }

    private fun observeEffectiveGoal() {
        viewModelScope.launch {
            combine(_goal, _caloriesBurned) { goal, burned ->
                if (settingsRepository.isDynamicCalorieGoalEnabled() && burned > 0) {
                    val deficit = settingsRepository.getDailyCalorieDeficit()
                    goal.copy(calories = (burned - deficit).coerceAtLeast(0))
                } else {
                    goal
                }
            }.collect { _effectiveGoal.value = it }
        }
    }

    private var nutritionJob: Job? = null
    private var mealsJob: Job? = null
    private var dailyNoteJob: Job? = null

    private fun observeNutrition() {
        nutritionJob?.cancel()
        nutritionJob = viewModelScope.launch {
            getDailyNutritionUseCase.getNutrition(_selectedDate.value.toString()).collect {
                _nutrition.value = it
                _isLoading.value = false
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

    private fun observeDailyNote() {
        dailyNoteJob?.cancel()
        dailyNoteJob = viewModelScope.launch {
            dailyNoteRepository.getNoteForDate(_selectedDate.value.toString()).collect {
                _dailyNote.value = it
            }
        }
    }

    fun saveDailyNote(note: String) {
        viewModelScope.launch {
            val trimmed = note.trim()
            if (trimmed.isEmpty()) {
                dailyNoteRepository.deleteNote(_selectedDate.value.toString())
            } else {
                dailyNoteRepository.saveNote(_selectedDate.value.toString(), trimmed)
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
        if (_selectedDate.value != now && _selectedDate.value == now.minusDays(1)) {
        }
        loadGoal()
        observeNutrition()
        observeMeals()
        loadCaloriesBurned()
        loadLatestWeight()
        observeEffectiveGoal()
        loadStreak()
        viewModelScope.launch { CaloriesWidgetProvider.refreshAll(appContext) }
    }

    private fun loadStreak() {
        viewModelScope.launch {
            try {
                val dates = mealRepository.getAllMealDates()
                    .map { LocalDate.parse(it) }
                    .toSortedSet(compareByDescending { it })
                var streak = 0
                var checkDate = LocalDate.now()
                for (date in dates) {
                    if (date == checkDate) {
                        streak++
                        checkDate = checkDate.minusDays(1)
                    } else if (date.isBefore(checkDate)) {
                        // Gap found - but if today has no meals yet, start from yesterday
                        if (streak == 0 && date == LocalDate.now().minusDays(1)) {
                            streak++
                            checkDate = date.minusDays(1)
                        } else {
                            break
                        }
                    }
                }
                _streak.value = streak
            } catch (_: Exception) {
                _streak.value = 0
            }
        }
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
            CaloriesWidgetProvider.refreshAll(appContext)
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

    fun updateMealType(mealId: Long, mealType: String) {
        viewModelScope.launch {
            mealRepository.updateMealType(mealId, mealType)
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

    fun requestMealSuggestions() {
        val apiKey = settingsRepository.getApiKey()
        if (apiKey.isBlank()) return

        _suggestionsLoading.value = true
        _suggestions.value = emptyList()

        viewModelScope.launch {
            try {
                val profile = userProfileRepository.getProfile()
                val goal = _effectiveGoal.value
                val nutrition = _nutrition.value
                val remainingCal = goal.calories - (nutrition?.totalCalories ?: 0)
                val remainingProt = goal.proteins - (nutrition?.totalProteins ?: 0f)
                val remainingCarbs = goal.carbs - (nutrition?.totalCarbs ?: 0f)
                val remainingFats = goal.fats - (nutrition?.totalFats ?: 0f)

                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val mealType = when {
                    hour in 5..10 -> "breakfast"
                    hour in 11..14 -> "lunch"
                    hour in 15..17 -> "snack"
                    else -> "dinner"
                }

                val recentDishes = _meals.value.map { it.dishName }.take(5)

                val language = Locale.getDefault().displayLanguage
                val rawResponse = geminiApiService.suggestMeals(
                    profile, remainingCal.coerceAtLeast(0), remainingProt.coerceAtLeast(0f),
                    remainingCarbs.coerceAtLeast(0f), remainingFats.coerceAtLeast(0f),
                    mealType, recentDishes, apiKey, language
                )

                val json = JSONObject(rawResponse)
                val text = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()

                val cleanJson = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val suggestionsJson = JSONObject(cleanJson).getJSONArray("suggestions")
                val list = mutableListOf<MealSuggestion>()
                for (i in 0 until suggestionsJson.length()) {
                    val obj = suggestionsJson.getJSONObject(i)
                    list.add(MealSuggestion(
                        emoji = obj.optString("emoji", "\uD83C\uDF7D\uFE0F"),
                        dishName = obj.getString("dishName"),
                        estimatedCalories = obj.getInt("estimatedCalories"),
                        proteins = obj.optString("proteins", ""),
                        carbs = obj.optString("carbs", ""),
                        fats = obj.optString("fats", ""),
                        description = obj.optString("description", "")
                    ))
                }
                _suggestions.value = list
            } catch (_: Exception) {
                _suggestions.value = emptyList()
            } finally {
                _suggestionsLoading.value = false
            }
        }
    }

    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }

    fun quickAddFavorite(meal: MealEntry) {
        viewModelScope.launch {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val mealType = when {
                hour in 5..10 -> "breakfast"
                hour in 11..14 -> "lunch"
                hour in 15..17 -> "snack"
                else -> "dinner"
            }
            val targetMeal = meal.copy(
                id = 0,
                date = _selectedDate.value.toString(),
                isFavorite = false,
                mealType = mealType
            )
            mealRepository.saveMeal(targetMeal)
        }
    }

    companion object {
        fun provideFactory(
            getDailyNutritionUseCase: GetDailyNutritionUseCase,
            userProfileRepository: UserProfileRepository,
            mealRepository: com.ghostwan.snapcal.domain.repository.MealRepository,
            healthConnectManager: HealthConnectManager,
            dailyNoteRepository: DailyNoteRepository,
            settingsRepository: SettingsRepository,
            geminiApiService: GeminiApiService,
            appContext: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(getDailyNutritionUseCase, userProfileRepository, mealRepository, healthConnectManager, dailyNoteRepository, settingsRepository, geminiApiService, appContext) as T
            }
        }
    }
}

data class MealSuggestion(
    val emoji: String,
    val dishName: String,
    val estimatedCalories: Int,
    val proteins: String,
    val carbs: String,
    val fats: String,
    val description: String
)
