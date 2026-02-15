package com.foodcalories.app.presentation.model

import com.foodcalories.app.domain.model.FoodAnalysis

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data object Loading : AnalysisUiState
    data class Success(val result: FoodAnalysis) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}
