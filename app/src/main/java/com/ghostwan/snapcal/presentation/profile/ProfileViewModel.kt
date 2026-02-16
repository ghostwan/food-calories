package com.ghostwan.snapcal.presentation.profile

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghostwan.snapcal.data.local.HealthConnectManager
import com.ghostwan.snapcal.data.local.MealReminderManager
import com.ghostwan.snapcal.data.remote.BackupInfo
import com.ghostwan.snapcal.data.remote.DriveBackupManager
import com.ghostwan.snapcal.data.remote.GoogleAuthManager
import com.ghostwan.snapcal.domain.model.NutritionGoal
import com.ghostwan.snapcal.domain.model.UserProfile
import com.ghostwan.snapcal.domain.model.WeightRecord
import com.ghostwan.snapcal.domain.repository.MealRepository
import com.ghostwan.snapcal.domain.repository.SettingsRepository
import com.ghostwan.snapcal.domain.repository.UserProfileRepository
import com.ghostwan.snapcal.domain.usecase.ComputeNutritionGoalUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val computeNutritionGoalUseCase: ComputeNutritionGoalUseCase,
    private val healthConnectManager: HealthConnectManager,
    private val googleAuthManager: GoogleAuthManager,
    private val driveBackupManager: DriveBackupManager,
    private val mealRepository: MealRepository,
    private val mealReminderManager: MealReminderManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile

    private val _goal = MutableStateFlow(NutritionGoal())
    val goal: StateFlow<NutritionGoal> = _goal

    private val _isComputing = MutableStateFlow(false)
    val isComputing: StateFlow<Boolean> = _isComputing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private val _healthConnectAvailable = MutableStateFlow(false)
    val healthConnectAvailable: StateFlow<Boolean> = _healthConnectAvailable

    private val _isSyncingWeight = MutableStateFlow(false)
    val isSyncingWeight: StateFlow<Boolean> = _isSyncingWeight

    private val _weightSynced = MutableStateFlow(false)
    val weightSynced: StateFlow<Boolean> = _weightSynced

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn

    private val _signedInEmail = MutableStateFlow<String?>(null)
    val signedInEmail: StateFlow<String?> = _signedInEmail

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring

    private val _backupDone = MutableStateFlow(false)
    val backupDone: StateFlow<Boolean> = _backupDone

    private val _restoreDone = MutableStateFlow(false)
    val restoreDone: StateFlow<Boolean> = _restoreDone

    private val _backupInfo = MutableStateFlow<BackupInfo?>(null)
    val backupInfo: StateFlow<BackupInfo?> = _backupInfo

    private val _remindersEnabled = MutableStateFlow(false)
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled

    private val _breakfastTime = MutableStateFlow(Pair(8, 0))
    val breakfastTime: StateFlow<Pair<Int, Int>> = _breakfastTime

    private val _lunchTime = MutableStateFlow(Pair(12, 30))
    val lunchTime: StateFlow<Pair<Int, Int>> = _lunchTime

    private val _dinnerTime = MutableStateFlow(Pair(20, 30))
    val dinnerTime: StateFlow<Pair<Int, Int>> = _dinnerTime

    private val _shoppingListEnabled = MutableStateFlow(false)
    val shoppingListEnabled: StateFlow<Boolean> = _shoppingListEnabled

    private val _googleAuthForGemini = MutableStateFlow(false)
    val googleAuthForGemini: StateFlow<Boolean> = _googleAuthForGemini

    init {
        loadProfile()
        checkHealthConnect()
        checkGoogleSignIn()
        loadReminderSettings()
        loadBackupInfo()
        _shoppingListEnabled.value = settingsRepository.isShoppingListEnabled()
        _googleAuthForGemini.value = settingsRepository.isGoogleAuthForGemini()
    }

    private fun loadProfile() {
        _profile.value = userProfileRepository.getProfile()
        _goal.value = userProfileRepository.getGoal()
    }

    private fun checkHealthConnect() {
        _healthConnectAvailable.value = healthConnectManager.isAvailable()
    }

    private fun checkGoogleSignIn() {
        _isSignedIn.value = googleAuthManager.isSignedIn()
        _signedInEmail.value = googleAuthManager.getSignedInEmail()
    }

    private fun loadBackupInfo() {
        if (!googleAuthManager.isSignedIn()) return
        viewModelScope.launch {
            try {
                _backupInfo.value = driveBackupManager.getBackupInfo()
            } catch (_: Exception) { }
        }
    }

    fun getSignInIntent(): Intent = googleAuthManager.getSignInIntent()

    fun handleSignInResult(data: Intent?) {
        try {
            val account = googleAuthManager.handleSignInResult(data)
            _isSignedIn.value = true
            _signedInEmail.value = account.email
            loadBackupInfo()
        } catch (e: Exception) {
            _isSignedIn.value = false
            _error.value = "Google Sign-In: ${e.message}"
        }
    }

    fun signOut() {
        viewModelScope.launch {
            googleAuthManager.signOut()
            _isSignedIn.value = false
            _signedInEmail.value = null
            _googleAuthForGemini.value = false
            settingsRepository.setGoogleAuthForGemini(false)
        }
    }

    fun updateProfile(profile: UserProfile) {
        _profile.value = profile
    }

    fun saveProfile() {
        userProfileRepository.saveProfile(_profile.value)
        _saved.value = true
        computeGoals()
    }

    fun updateGoalCalories(calories: Int) {
        val current = _goal.value
        _goal.value = current.copy(calories = calories)
        userProfileRepository.saveGoal(_goal.value)
    }

    fun computeGoals() {
        val p = _profile.value
        if (p.height == 0 || p.weight == 0f || p.age == 0) return

        viewModelScope.launch {
            _isComputing.value = true
            _error.value = null
            try {
                val language = Locale.getDefault().displayLanguage
                val goal = computeNutritionGoalUseCase(p, language)
                _goal.value = goal
                userProfileRepository.saveGoal(goal)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isComputing.value = false
            }
        }
    }

    fun syncWeightFromHealthConnect() {
        viewModelScope.launch {
            _isSyncingWeight.value = true
            _error.value = null
            try {
                val records = healthConnectManager.readWeightRecords(90)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

                for (record in records) {
                    val date = dateFormat.format(Date.from(record.time))
                    userProfileRepository.saveWeightRecord(
                        WeightRecord(weight = record.weightKg, date = date)
                    )
                }

                if (records.isNotEmpty()) {
                    val latest = records.maxByOrNull { it.time }!!
                    val currentProfile = _profile.value
                    val updated = currentProfile.copy(weight = latest.weightKg)
                    _profile.value = updated
                    userProfileRepository.saveProfile(updated)
                }

                _weightSynced.value = true
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isSyncingWeight.value = false
            }
        }
    }

    fun backupToDrive() {
        viewModelScope.launch {
            _isBackingUp.value = true
            _error.value = null
            try {
                val meals = mealRepository.getAllMeals()
                val weightRecords = userProfileRepository.getWeightHistory(9999)
                val success = driveBackupManager.backup(
                    profile = _profile.value,
                    goal = _goal.value,
                    meals = meals,
                    weightRecords = weightRecords
                )
                if (success) {
                    _backupDone.value = true
                    loadBackupInfo()
                } else {
                    _error.value = "Backup failed"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isBackingUp.value = false
            }
        }
    }

    fun restoreFromDrive() {
        viewModelScope.launch {
            _isRestoring.value = true
            _error.value = null
            try {
                val data = driveBackupManager.restore()
                if (data != null) {
                    userProfileRepository.saveProfile(data.profile)
                    userProfileRepository.saveGoal(data.goal)
                    _profile.value = data.profile
                    _goal.value = data.goal

                    for (meal in data.meals) {
                        mealRepository.saveMeal(meal)
                    }
                    for (record in data.weightRecords) {
                        userProfileRepository.saveWeightRecord(record)
                    }

                    _restoreDone.value = true
                } else {
                    _error.value = "No backup found"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isRestoring.value = false
            }
        }
    }

    private fun loadReminderSettings() {
        _remindersEnabled.value = mealReminderManager.enabled
        _breakfastTime.value = Pair(
            mealReminderManager.getHour(MealReminderManager.MealType.BREAKFAST),
            mealReminderManager.getMinute(MealReminderManager.MealType.BREAKFAST)
        )
        _lunchTime.value = Pair(
            mealReminderManager.getHour(MealReminderManager.MealType.LUNCH),
            mealReminderManager.getMinute(MealReminderManager.MealType.LUNCH)
        )
        _dinnerTime.value = Pair(
            mealReminderManager.getHour(MealReminderManager.MealType.DINNER),
            mealReminderManager.getMinute(MealReminderManager.MealType.DINNER)
        )
    }

    fun toggleReminders(enabled: Boolean) {
        _remindersEnabled.value = enabled
        mealReminderManager.enabled = enabled
    }

    fun updateReminderTime(meal: MealReminderManager.MealType, hour: Int, minute: Int) {
        mealReminderManager.setTime(meal, hour, minute)
        when (meal) {
            MealReminderManager.MealType.BREAKFAST -> _breakfastTime.value = Pair(hour, minute)
            MealReminderManager.MealType.LUNCH -> _lunchTime.value = Pair(hour, minute)
            MealReminderManager.MealType.DINNER -> _dinnerTime.value = Pair(hour, minute)
        }
    }

    fun toggleShoppingList(enabled: Boolean) {
        _shoppingListEnabled.value = enabled
        settingsRepository.setShoppingListEnabled(enabled)
    }

    fun toggleGoogleAuthForGemini(enabled: Boolean) {
        _googleAuthForGemini.value = enabled
        settingsRepository.setGoogleAuthForGemini(enabled)
    }

    fun clearSaved() { _saved.value = false }
    fun clearError() { _error.value = null }
    fun clearWeightSynced() { _weightSynced.value = false }
    fun clearBackupDone() { _backupDone.value = false }
    fun clearRestoreDone() { _restoreDone.value = false }

    companion object {
        fun provideFactory(
            userProfileRepository: UserProfileRepository,
            computeNutritionGoalUseCase: ComputeNutritionGoalUseCase,
            healthConnectManager: HealthConnectManager,
            googleAuthManager: GoogleAuthManager,
            driveBackupManager: DriveBackupManager,
            mealRepository: MealRepository,
            mealReminderManager: MealReminderManager,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(
                    userProfileRepository,
                    computeNutritionGoalUseCase,
                    healthConnectManager,
                    googleAuthManager,
                    driveBackupManager,
                    mealRepository,
                    mealReminderManager,
                    settingsRepository
                ) as T
            }
        }
    }
}
