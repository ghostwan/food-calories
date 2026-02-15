package com.foodcalories.app.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foodcalories.app.R
import com.foodcalories.app.domain.model.DailyNutrition
import com.foodcalories.app.domain.model.NutritionGoal
import com.foodcalories.app.domain.repository.UserProfileRepository
import com.foodcalories.app.domain.usecase.GetNutritionHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    historyUseCase: GetNutritionHistoryUseCase,
    userProfileRepository: UserProfileRepository,
    onBack: () -> Unit
) {
    val history = MutableStateFlow<List<DailyNutrition>>(emptyList())
    val historyState by history.collectAsState()
    val goal = userProfileRepository.getGoal()

    LaunchedEffect(Unit) {
        history.value = historyUseCase(30)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.result_back))
                    }
                }
            )
        }
    ) { padding ->
        if (historyState.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.history_no_data),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyState) { day ->
                    DayCard(day, goal)
                }
            }
        }
    }
}

@Composable
private fun DayCard(day: DailyNutrition, goal: NutritionGoal) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val displayFormat = SimpleDateFormat("EEEE d MMMM", Locale.getDefault())
    val displayDate = try {
        displayFormat.format(dateFormat.parse(day.date)!!)
    } catch (_: Exception) {
        day.date
    }

    val calProgress = if (goal.calories > 0) (day.totalCalories.toFloat() / goal.calories).coerceIn(0f, 1f) else 0f
    val calColor = when {
        calProgress <= 0.75f -> Color(0xFF4CAF50)
        calProgress <= 1.0f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.history_calories_label, day.totalCalories),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = calColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { calProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = calColor,
                trackColor = calColor.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = stringResource(R.string.history_proteins_short, day.totalProteins),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(R.string.history_carbs_short, day.totalCarbs),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(R.string.history_fats_short, day.totalFats),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
