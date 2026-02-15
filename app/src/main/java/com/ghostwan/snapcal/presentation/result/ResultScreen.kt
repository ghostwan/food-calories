package com.ghostwan.snapcal.presentation.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostwan.snapcal.R
import com.ghostwan.snapcal.domain.model.FoodAnalysis
import com.ghostwan.snapcal.presentation.FoodAnalysisViewModel
import com.ghostwan.snapcal.presentation.model.AnalysisUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: FoodAnalysisViewModel,
    onBack: () -> Unit,
    onMealSaved: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val mealSaved by viewModel.mealSaved.collectAsState()

    androidx.compose.runtime.LaunchedEffect(mealSaved) {
        if (mealSaved) {
            onMealSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.result_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is AnalysisUiState.Idle -> IdleContent()
                is AnalysisUiState.Loading -> LoadingContent()
                is AnalysisUiState.Success -> SuccessContent(
                    result = state.result,
                    mealSaved = mealSaved,
                    onSave = { viewModel.saveMeal(state.result) },
                    onCorrect = { feedback -> viewModel.correctAnalysis(state.result, feedback) }
                )
                is AnalysisUiState.Error -> ErrorContent(state.message, onBack)
            }
        }
    }
}

@Composable
private fun IdleContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.result_idle))
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.result_loading))
    }
}

@Composable
private fun SuccessContent(
    result: FoodAnalysis,
    mealSaved: Boolean,
    onSave: () -> Unit,
    onCorrect: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = result.dishName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item { CaloriesCard(result.totalCalories) }

        if (result.macros != null) {
            item { MacrosCard(result.macros) }
        }

        item {
            Text(
                text = stringResource(R.string.result_ingredients),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(result.ingredients) { ingredient ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ingredient.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = ingredient.quantity,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(R.string.result_kcal, ingredient.calories),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (result.notes != null) {
            item { NotesCard(result.notes) }
        }

        // Correction UI
        if (!mealSaved) {
            item {
                var showCorrectionField by remember { mutableStateOf(false) }
                var feedbackText by remember { mutableStateOf("") }

                if (showCorrectionField) {
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        label = { Text(stringResource(R.string.result_correction_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (feedbackText.isNotBlank()) {
                                onCorrect(feedbackText)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.result_correct))
                    }
                } else {
                    TextButton(
                        onClick = { showCorrectionField = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.result_not_right))
                    }
                }
            }
        }

        // Save meal button
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = !mealSaved
            ) {
                if (mealSaved) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.result_meal_saved))
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.result_save_meal))
                }
            }
        }
    }
}

@Composable
private fun CaloriesCard(totalCalories: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.result_total_calories),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.result_kcal, totalCalories),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MacrosCard(macros: com.ghostwan.snapcal.domain.model.Macros) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.result_macros),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            MacroRow(stringResource(R.string.result_proteins), macros.proteins)
            MacroRow(stringResource(R.string.result_carbs), macros.carbs)
            MacroRow(stringResource(R.string.result_fats), macros.fats)
            if (macros.fiber != null) {
                MacroRow(stringResource(R.string.result_fiber), macros.fiber)
            }
        }
    }
}

@Composable
private fun MacroRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NotesCard(notes: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.result_notes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.result_error),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text(stringResource(R.string.result_button_back))
        }
    }
}
