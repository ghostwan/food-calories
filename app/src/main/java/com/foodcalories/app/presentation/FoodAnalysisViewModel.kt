package com.foodcalories.app.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foodcalories.app.domain.model.FoodAnalysis
import com.foodcalories.app.domain.repository.SettingsRepository
import com.foodcalories.app.domain.repository.UsageRepository
import com.foodcalories.app.domain.usecase.AnalyzeFoodUseCase
import com.foodcalories.app.domain.usecase.SaveMealUseCase
import com.foodcalories.app.presentation.model.AnalysisUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Locale

class FoodAnalysisViewModel(
    private val analyzeFoodUseCase: AnalyzeFoodUseCase,
    private val settingsRepository: SettingsRepository,
    private val usageRepository: UsageRepository,
    private val saveMealUseCase: SaveMealUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState

    private val _mealSaved = MutableStateFlow(false)
    val mealSaved: StateFlow<Boolean> = _mealSaved

    fun getApiKey(): String = settingsRepository.getApiKey()

    fun setApiKey(key: String) = settingsRepository.setApiKey(key)

    fun isQuotaExceeded(): Boolean =
        usageRepository.getDailyRequestCount() >= FREE_DAILY_LIMIT

    fun getDailyRequestCount(): Int =
        usageRepository.getDailyRequestCount()

    fun analyzeFood(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Loading
            try {
                val imageData = readAndCompressImage(context, imageUri)
                val language = Locale.getDefault().displayLanguage
                val result = analyzeFoodUseCase(imageData, language)
                usageRepository.recordRequest()
                _uiState.value = AnalysisUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = AnalysisUiState.Error(
                    e.message ?: "Erreur inconnue lors de l'analyse"
                )
            }
        }
    }

    fun saveMeal(analysis: FoodAnalysis) {
        viewModelScope.launch {
            try {
                saveMealUseCase(analysis)
                _mealSaved.value = true
            } catch (_: Exception) {
                // silently fail
            }
        }
    }

    fun resetMealSaved() {
        _mealSaved.value = false
    }

    fun resetState() {
        _uiState.value = AnalysisUiState.Idle
        _mealSaved.value = false
    }

    private fun readAndCompressImage(context: Context, uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Impossible de lire l'image")

        val bitmap = inputStream.use { BitmapFactory.decodeStream(it) }
            ?: throw IllegalStateException("Impossible de décoder l'image")

        val resized = resizeBitmap(bitmap, 1024)
        val output = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 85, output)
        return output.toByteArray()
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    companion object {
        const val FREE_DAILY_LIMIT = 1500

        fun provideFactory(
            analyzeFoodUseCase: AnalyzeFoodUseCase,
            settingsRepository: SettingsRepository,
            usageRepository: UsageRepository,
            saveMealUseCase: SaveMealUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FoodAnalysisViewModel(
                    analyzeFoodUseCase,
                    settingsRepository,
                    usageRepository,
                    saveMealUseCase
                ) as T
            }
        }
    }
}
