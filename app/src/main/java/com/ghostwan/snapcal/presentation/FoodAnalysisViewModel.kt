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
import com.ghostwan.snapcal.data.remote.GoogleAuthManager
import com.ghostwan.snapcal.domain.repository.MealRepository
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
    private val saveMealUseCase: SaveMealUseCase,
    private val mealRepository: MealRepository,
    private val googleAuthManager: GoogleAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState

    private val _mealSaved = MutableStateFlow(false)
    val mealSaved: StateFlow<Boolean> = _mealSaved

    private val _readOnly = MutableStateFlow(false)
    val readOnly: StateFlow<Boolean> = _readOnly

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite

    private var lastImageData: ByteArray? = null
    private var editingMealId: Long? = null
    private var editingMealIds: List<Long>? = null
    private var editingMealDate: String? = null

    fun getApiKey(): String = settingsRepository.getApiKey()

    fun setApiKey(key: String) = settingsRepository.setApiKey(key)

    fun isGeminiConfigured(): Boolean =
        settingsRepository.isGoogleAuthForGemini() && googleAuthManager.isSignedIn()

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
            _readOnly.value = false
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
                val mealIds = editingMealIds
                val mealId = editingMealId
                val mealDate = editingMealDate
                if (mealIds != null && mealDate != null) {
                    saveMealUseCase.replaceMultipleAndSave(mealIds, analysis, mealDate)
                } else if (mealId != null && mealDate != null) {
                    saveMealUseCase.replaceAndSave(mealId, analysis, mealDate)
                } else {
                    saveMealUseCase(analysis)
                }
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
                        calories = obj.getInt("calories"),
                        healthRating = if (obj.has("healthRating") && !obj.isNull("healthRating")) obj.getString("healthRating") else null
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
        _isFavorite.value = meal.isFavorite
        editingMealId = meal.id
        editingMealDate = meal.date
        lastImageData = null
    }

    fun viewMergedMeals(meals: List<MealEntry>) {
        val dishName = meals.joinToString(" + ") { it.dishName }
        val totalCalories = meals.sumOf { it.calories }
        val totalProteins = meals.sumOf { it.proteins.toDouble() }.toFloat()
        val totalCarbs = meals.sumOf { it.carbs.toDouble() }.toFloat()
        val totalFats = meals.sumOf { it.fats.toDouble() }.toFloat()
        val totalFiber = meals.sumOf { it.fiber.toDouble() }.toFloat()

        val allIngredients = meals.flatMap { meal ->
            try {
                val array = JSONArray(meal.ingredientsJson)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        add(Ingredient(
                            name = obj.getString("name"),
                            quantity = obj.getString("quantity"),
                            calories = obj.getInt("calories"),
                            healthRating = if (obj.has("healthRating") && !obj.isNull("healthRating")) obj.getString("healthRating") else null
                        ))
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        val emoji = meals.firstOrNull()?.emoji

        val analysis = FoodAnalysis(
            dishName = dishName,
            totalCalories = totalCalories,
            ingredients = allIngredients,
            macros = Macros(
                proteins = String.format("%.0fg", totalProteins),
                carbs = String.format("%.0fg", totalCarbs),
                fats = String.format("%.0fg", totalFats),
                fiber = if (totalFiber > 0) String.format("%.0fg", totalFiber) else null
            ),
            notes = null,
            emoji = emoji
        )
        _uiState.value = AnalysisUiState.Success(analysis)
        _mealSaved.value = false
        _readOnly.value = false
        _isFavorite.value = false
        editingMealIds = meals.map { it.id }
        editingMealDate = meals.firstOrNull()?.date
        lastImageData = null
    }

    fun removeIngredient(index: Int) {
        val current = _uiState.value
        if (current is AnalysisUiState.Success) {
            val result = current.result
            val newIngredients = result.ingredients.toMutableList().apply { removeAt(index) }
            val newCalories = newIngredients.sumOf { it.calories }
            _uiState.value = AnalysisUiState.Success(
                result.copy(ingredients = newIngredients, totalCalories = newCalories)
            )
        }
    }

    fun updateIngredient(index: Int, newQuantity: String, newCalories: Int) {
        val current = _uiState.value
        if (current is AnalysisUiState.Success) {
            val result = current.result
            val newIngredients = result.ingredients.toMutableList().apply {
                set(index, get(index).copy(quantity = newQuantity, calories = newCalories))
            }
            val newTotalCalories = newIngredients.sumOf { it.calories }
            _uiState.value = AnalysisUiState.Success(
                result.copy(ingredients = newIngredients, totalCalories = newTotalCalories)
            )
        }
    }

    fun updateDishName(name: String) {
        val current = _uiState.value
        if (current is AnalysisUiState.Success) {
            _uiState.value = AnalysisUiState.Success(current.result.copy(dishName = name))
        }
    }

    fun updateEmoji(emoji: String) {
        val current = _uiState.value
        if (current is AnalysisUiState.Success) {
            _uiState.value = AnalysisUiState.Success(current.result.copy(emoji = emoji))
        }
        val mealId = editingMealId ?: return
        viewModelScope.launch {
            mealRepository.updateEmoji(mealId, emoji)
        }
    }

    fun toggleFavorite() {
        val mealId = editingMealId ?: return
        val newValue = !_isFavorite.value
        _isFavorite.value = newValue
        viewModelScope.launch {
            mealRepository.setFavorite(mealId, newValue)
        }
    }

    fun isEditing(): Boolean = editingMealId != null

    fun resetMealSaved() {
        _mealSaved.value = false
    }

    fun resetState() {
        _uiState.value = AnalysisUiState.Idle
        _mealSaved.value = false
        _readOnly.value = false
        _isFavorite.value = false
        editingMealId = null
        editingMealIds = null
        editingMealDate = null
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
            saveMealUseCase: SaveMealUseCase,
            mealRepository: MealRepository,
            googleAuthManager: GoogleAuthManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FoodAnalysisViewModel(
                    analyzeFoodUseCase,
                    correctAnalysisUseCase,
                    settingsRepository,
                    usageRepository,
                    saveMealUseCase,
                    mealRepository,
                    googleAuthManager
                ) as T
            }
        }
    }
}
