package com.ghostwan.snapcal.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ghostwan.snapcal.domain.model.FoodAnalysis
import com.ghostwan.snapcal.domain.model.Ingredient
import com.ghostwan.snapcal.domain.model.Macros
import com.ghostwan.snapcal.domain.model.MealEntry
import com.ghostwan.snapcal.domain.repository.SettingsRepository
import com.ghostwan.snapcal.domain.repository.UsageRepository
import com.ghostwan.snapcal.domain.usecase.AnalyzeFoodUseCase
import com.ghostwan.snapcal.domain.usecase.CorrectAnalysisUseCase
import com.ghostwan.snapcal.domain.usecase.SaveMealUseCase
import com.ghostwan.snapcal.presentation.model.AnalysisUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.util.Locale

class FoodAnalysisViewModel(
    private val analyzeFoodUseCase: AnalyzeFoodUseCase,
    private val correctAnalysisUseCase: CorrectAnalysisUseCase,
    private val settingsRepository: SettingsRepository,
    private val usageRepository: UsageRepository,
    private val saveMealUseCase: SaveMealUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState

    private val _mealSaved = MutableStateFlow(false)
    val mealSaved: StateFlow<Boolean> = _mealSaved

    private val _readOnly = MutableStateFlow(false)
    val readOnly: StateFlow<Boolean> = _readOnly

    private var lastImageData: ByteArray? = null

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
                lastImageData = imageData
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

    fun analyzeFoodFromText(description: String) {
        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Loading
            try {
                val language = Locale.getDefault().displayLanguage
                val result = analyzeFoodUseCase.fromText(description, language)
                usageRepository.recordRequest()
                _uiState.value = AnalysisUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = AnalysisUiState.Error(
                    e.message ?: "Erreur inconnue lors de l'analyse"
                )
            }
        }
    }

    fun analyzeFoodFromBarcode(barcode: String) {
        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Loading
            try {
                val result = analyzeFoodUseCase.fromBarcode(barcode)
                _uiState.value = AnalysisUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = AnalysisUiState.Error(
                    e.message ?: "Product not found"
                )
            }
        }
    }

    fun correctAnalysis(originalAnalysis: FoodAnalysis, feedback: String) {
        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Loading
            _mealSaved.value = false
            try {
                val language = Locale.getDefault().displayLanguage
                val corrected = correctAnalysisUseCase(
                    originalAnalysis, feedback, lastImageData, language
                )
                usageRepository.recordRequest()
                _uiState.value = AnalysisUiState.Success(corrected)
            } catch (e: Exception) {
                _uiState.value = AnalysisUiState.Success(originalAnalysis)
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

    fun viewMealDetail(meal: MealEntry) {
        val ingredients = try {
            val array = JSONArray(meal.ingredientsJson)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(Ingredient(
                        name = obj.getString("name"),
                        quantity = obj.getString("quantity"),
                        calories = obj.getInt("calories")
                    ))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }

        val analysis = FoodAnalysis(
            dishName = meal.dishName,
            totalCalories = meal.calories,
            ingredients = ingredients,
            macros = Macros(
                proteins = String.format("%.0fg", meal.proteins),
                carbs = String.format("%.0fg", meal.carbs),
                fats = String.format("%.0fg", meal.fats),
                fiber = if (meal.fiber > 0) String.format("%.0fg", meal.fiber) else null
            ),
            notes = null,
            emoji = meal.emoji
        )
        _uiState.value = AnalysisUiState.Success(analysis)
        _mealSaved.value = false
        _readOnly.value = true
        lastImageData = null
    }

    fun resetMealSaved() {
        _mealSaved.value = false
    }

    fun resetState() {
        _uiState.value = AnalysisUiState.Idle
        _mealSaved.value = false
        _readOnly.value = false
        lastImageData = null
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
            correctAnalysisUseCase: CorrectAnalysisUseCase,
            settingsRepository: SettingsRepository,
            usageRepository: UsageRepository,
            saveMealUseCase: SaveMealUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FoodAnalysisViewModel(
                    analyzeFoodUseCase,
                    correctAnalysisUseCase,
                    settingsRepository,
                    usageRepository,
                    saveMealUseCase
                ) as T
            }
        }
    }
}
