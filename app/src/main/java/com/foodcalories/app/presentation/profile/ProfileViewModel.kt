package com.foodcalories.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foodcalories.app.domain.model.NutritionGoal
import com.foodcalories.app.domain.model.UserProfile
import com.foodcalories.app.domain.repository.UserProfileRepository
import com.foodcalories.app.domain.usecase.ComputeNutritionGoalUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class ProfileViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val computeNutritionGoalUseCase: ComputeNutritionGoalUseCase
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

    init {
        loadProfile()
    }

    private fun loadProfile() {
        _profile.value = userProfileRepository.getProfile()
        _goal.value = userProfileRepository.getGoal()
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

    fun clearSaved() {
        _saved.value = false
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        fun provideFactory(
            userProfileRepository: UserProfileRepository,
            computeNutritionGoalUseCase: ComputeNutritionGoalUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(userProfileRepository, computeNutritionGoalUseCase) as T
            }
        }
    }
}
