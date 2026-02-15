package com.ghostwan.snapcal.presentation.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostwan.snapcal.R
import com.ghostwan.snapcal.domain.model.DailyNutrition
import com.ghostwan.snapcal.domain.model.MealEntry
import com.ghostwan.snapcal.domain.model.NutritionGoal
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val history by viewModel.history.collectAsState()
    val weightHistory by viewModel.weightHistory.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedDayMeals by viewModel.selectedDayMeals.collectAsState()

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
        if (history.isEmpty()) {
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
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val shortFormat = SimpleDateFormat("dd/MM", Locale.US)

            val weightByDate = weightHistory.associateBy { it.date }
            val allDates = (history.map { it.date } + weightHistory.map { it.date })
                .distinct()
                .sorted()

            val chartData = allDates.map { date ->
                val nutrition = history.find { it.date == date }
                val weight = weightByDate[date]
                val label = try {
                    shortFormat.format(dateFormat.parse(date)!!)
                } catch (_: Exception) {
                    date.takeLast(5)
                }
                ChartDataPoint(
                    label = label,
                    calories = nutrition?.totalCalories,
                    weight = weight?.weight
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (chartData.isNotEmpty()) {
                    item {
                        DualAxisChart(
                            data = chartData,
                            caloriesGoal = goal.calories,
                            targetWeight = profile.targetWeight.takeIf { it > 0f }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                items(history) { day ->
                    val isExpanded = selectedDate == day.date
                    DayCard(
                        day = day,
                        goal = goal,
                        isExpanded = isExpanded,
                        meals = if (isExpanded) selectedDayMeals else emptyList(),
                        onClick = { viewModel.selectDay(day.date) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCard(
    day: DailyNutrition,
    goal: NutritionGoal,
    isExpanded: Boolean,
    meals: List<MealEntry>,
    onClick: () -> Unit
) {
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.history_calories_label, day.totalCalories),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = calColor
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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

            // Expanded meal details
            AnimatedVisibility(visible = isExpanded && meals.isNotEmpty()) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    meals.forEach { meal ->
                        MealRow(meal)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MealRow(meal: MealEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = meal.dishName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.history_meal_macros, meal.proteins, meal.carbs, meal.fats),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = stringResource(R.string.history_calories_label, meal.calories),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
