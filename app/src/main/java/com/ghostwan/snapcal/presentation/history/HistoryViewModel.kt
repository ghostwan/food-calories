package com.ghostwan.snapcal.presentation.history

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghostwan.snapcal.MainActivity
import com.ghostwan.snapcal.R
import com.ghostwan.snapcal.SnapCalApp
import com.ghostwan.snapcal.data.local.HealthConnectManager
import com.ghostwan.snapcal.data.remote.GeminiApiService
import com.ghostwan.snapcal.domain.model.BodyMeasurement
import com.ghostwan.snapcal.domain.model.DailyNutrition
import com.ghostwan.snapcal.domain.model.MealEntry
import com.ghostwan.snapcal.domain.model.NutritionGoal
import com.ghostwan.snapcal.domain.model.UserProfile
import com.ghostwan.snapcal.domain.model.WeightRecord
import com.ghostwan.snapcal.domain.repository.MealRepository
import com.ghostwan.snapcal.domain.repository.SettingsRepository
import com.ghostwan.snapcal.domain.repository.UserProfileRepository
import com.ghostwan.snapcal.domain.usecase.GetNutritionHistoryUseCase
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

class HistoryViewModel(
    private val historyUseCase: GetNutritionHistoryUseCase,
    private val userProfileRepository: UserProfileRepository,
    private val mealRepository: MealRepository,
    private val healthConnectManager: HealthConnectManager,
    private val settingsRepository: SettingsRepository,
    private val geminiApiService: GeminiApiService,
    private val appContext: Context
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

    private val _chartCaloriesOrigin = MutableStateFlow(0)
    val chartCaloriesOrigin: StateFlow<Int> = _chartCaloriesOrigin

    private val _chartWeightOrigin = MutableStateFlow(60)
    val chartWeightOrigin: StateFlow<Int> = _chartWeightOrigin

    private val _showCalories = MutableStateFlow(true)
    val showCalories: StateFlow<Boolean> = _showCalories

    private val _showWeight = MutableStateFlow(true)
    val showWeight: StateFlow<Boolean> = _showWeight

    private val _showBurned = MutableStateFlow(true)
    val showBurned: StateFlow<Boolean> = _showBurned

    private val _measurementsEnabled = MutableStateFlow(false)
    val measurementsEnabled: StateFlow<Boolean> = _measurementsEnabled

    private val _showMeasurements = MutableStateFlow(false)
    val showMeasurements: StateFlow<Boolean> = _showMeasurements

    private val _measurementHistory = MutableStateFlow<List<BodyMeasurement>>(emptyList())
    val measurementHistory: StateFlow<List<BodyMeasurement>> = _measurementHistory

    private var historyJob: Job? = null
    private var selectedDayJob: Job? = null

    init {
        _goal.value = userProfileRepository.getGoal()
        _profile.value = userProfileRepository.getProfile()
        _chartCaloriesOrigin.value = userProfileRepository.getChartCaloriesOrigin()
        _chartWeightOrigin.value = userProfileRepository.getChartWeightOrigin()
        _showCalories.value = userProfileRepository.getChartShowCalories()
        _showWeight.value = userProfileRepository.getChartShowWeight()
        _showBurned.value = userProfileRepository.getChartShowBurned()
        _measurementsEnabled.value = settingsRepository.isMeasurementsEnabled()
        _showMeasurements.value = _measurementsEnabled.value && userProfileRepository.getChartShowMeasurements()
        val savedRange = userProfileRepository.getChartRange()
        _selectedRange.value = savedRange
        loadForRange(savedRange)
    }

    fun setChartCaloriesOrigin(value: Int) {
        _chartCaloriesOrigin.value = value
        userProfileRepository.setChartCaloriesOrigin(value)
    }

    fun setChartWeightOrigin(value: Int) {
        _chartWeightOrigin.value = value
        userProfileRepository.setChartWeightOrigin(value)
    }

    fun toggleShowCalories() {
        val v = !_showCalories.value
        _showCalories.value = v
        userProfileRepository.setChartShowCalories(v)
    }

    fun toggleShowWeight() {
        val v = !_showWeight.value
        _showWeight.value = v
        userProfileRepository.setChartShowWeight(v)
    }

    fun toggleShowBurned() {
        val v = !_showBurned.value
        _showBurned.value = v
        userProfileRepository.setChartShowBurned(v)
    }

    fun toggleShowMeasurements() {
        val v = !_showMeasurements.value
        _showMeasurements.value = v
        userProfileRepository.setChartShowMeasurements(v)
    }

    fun refresh() {
        _goal.value = userProfileRepository.getGoal()
        _profile.value = userProfileRepository.getProfile()
        _chartCaloriesOrigin.value = userProfileRepository.getChartCaloriesOrigin()
        _chartWeightOrigin.value = userProfileRepository.getChartWeightOrigin()
        _showCalories.value = userProfileRepository.getChartShowCalories()
        _showWeight.value = userProfileRepository.getChartShowWeight()
        _showBurned.value = userProfileRepository.getChartShowBurned()
        _measurementsEnabled.value = settingsRepository.isMeasurementsEnabled()
        _showMeasurements.value = _measurementsEnabled.value && userProfileRepository.getChartShowMeasurements()
        loadForRange(_selectedRange.value)
    }

    fun setRange(days: Int) {
        _selectedRange.value = days
        userProfileRepository.setChartRange(days)
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
        if (_measurementsEnabled.value) {
            viewModelScope.launch {
                _measurementHistory.value = userProfileRepository.getBodyMeasurementHistory(days)
            }
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
            selectedDayJob?.cancel()
        } else {
            _selectedDate.value = date
            selectedDayJob?.cancel()
            selectedDayJob = viewModelScope.launch {
                mealRepository.getMealsForDate(date).collect {
                    _selectedDayMeals.value = it
                }
            }
        }
    }

    private val _weeklyReport = MutableStateFlow<WeeklyReport?>(null)
    val weeklyReport: StateFlow<WeeklyReport?> = _weeklyReport

    private val _reportLoading = MutableStateFlow(false)
    val reportLoading: StateFlow<Boolean> = _reportLoading

    private val _showReportDialog = MutableStateFlow(false)
    val showReportDialog: StateFlow<Boolean> = _showReportDialog

    fun generateWeeklyReport() {
        val apiKey = settingsRepository.getApiKey()
        if (apiKey.isBlank()) return

        _reportLoading.value = true
        _weeklyReport.value = null

        viewModelScope.launch {
            try {
                val profile = userProfileRepository.getProfile()
                val goal = _goal.value
                val goalStr = "${goal.calories} kcal, ${goal.proteins}g prot, ${goal.carbs}g carbs, ${goal.fats}g fats, ${goal.fiber}g fiber"

                // Get last 7 days of nutrition data
                val last7 = _history.value.take(7)
                val summaries = last7.joinToString("\n") { day ->
                    "${day.date}: ${day.totalCalories} kcal, P:${day.totalProteins.toInt()}g, C:${day.totalCarbs.toInt()}g, F:${day.totalFats.toInt()}g"
                }

                val language = Locale.getDefault().displayLanguage
                val rawResponse = geminiApiService.generateWeeklyReport(
                    summaries, profile, goalStr, apiKey, language
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
                val report = JSONObject(cleanJson)

                val strengths = mutableListOf<String>()
                val strengthsArr = report.optJSONArray("strengths")
                if (strengthsArr != null) {
                    for (i in 0 until strengthsArr.length()) strengths.add(strengthsArr.getString(i))
                }

                val improvements = mutableListOf<String>()
                val improvementsArr = report.optJSONArray("improvements")
                if (improvementsArr != null) {
                    for (i in 0 until improvementsArr.length()) improvements.add(improvementsArr.getString(i))
                }

                _weeklyReport.value = WeeklyReport(
                    summary = report.getString("summary"),
                    avgCalories = report.getInt("avgCalories"),
                    avgProteins = report.getInt("avgProteins"),
                    avgCarbs = report.getInt("avgCarbs"),
                    avgFats = report.getInt("avgFats"),
                    strengths = strengths,
                    improvements = improvements,
                    tip = report.getString("tip")
                )
                postReportNotification(_weeklyReport.value!!)
                _showReportDialog.value = true
            } catch (_: Exception) {
                _weeklyReport.value = null
            } finally {
                _reportLoading.value = false
            }
        }
    }

    fun clearReport() {
        _weeklyReport.value = null
        _showReportDialog.value = false
    }

    private fun postReportNotification(report: WeeklyReport) {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(appContext, SnapCalApp.AI_INSIGHTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(appContext.getString(R.string.history_weekly_report))
            .setContentText(report.summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(report.summary))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(SnapCalApp.NOTIFICATION_ID_WEEKLY_REPORT, notification)
    }

    companion object {
        val RANGE_OPTIONS = listOf(7, 30, 90, 365)

        fun provideFactory(
            historyUseCase: GetNutritionHistoryUseCase,
            userProfileRepository: UserProfileRepository,
            mealRepository: MealRepository,
            healthConnectManager: HealthConnectManager,
            settingsRepository: SettingsRepository,
            geminiApiService: GeminiApiService,
            appContext: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(historyUseCase, userProfileRepository, mealRepository, healthConnectManager, settingsRepository, geminiApiService, appContext) as T
            }
        }
    }
}

data class WeeklyReport(
    val summary: String,
    val avgCalories: Int,
    val avgProteins: Int,
    val avgCarbs: Int,
    val avgFats: Int,
    val strengths: List<String>,
    val improvements: List<String>,
    val tip: String
)
