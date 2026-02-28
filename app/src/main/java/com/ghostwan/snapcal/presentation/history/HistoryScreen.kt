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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: (() -> Unit)? = null,
    onMealClick: (MealEntry) -> Unit = {}
) {
    val history by viewModel.history.collectAsState()
    val weightHistory by viewModel.weightHistory.collectAsState()
    val burnedCaloriesHistory by viewModel.burnedCaloriesHistory.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedDayMeals by viewModel.selectedDayMeals.collectAsState()
    val selectedRange by viewModel.selectedRange.collectAsState()
    val chartCaloriesOrigin by viewModel.chartCaloriesOrigin.collectAsState()
    val chartWeightOrigin by viewModel.chartWeightOrigin.collectAsState()
    val showCalories by viewModel.showCalories.collectAsState()
    val showWeight by viewModel.showWeight.collectAsState()
    val showBurned by viewModel.showBurned.collectAsState()
    val showMeasurements by viewModel.showMeasurements.collectAsState()
    val measurementHistory by viewModel.measurementHistory.collectAsState()

    val rangeLabels = mapOf(
        7 to stringResource(R.string.history_range_week),
        30 to stringResource(R.string.history_range_month),
        90 to stringResource(R.string.history_range_quarter),
        365 to stringResource(R.string.history_range_year)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.result_back))
                        }
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
            val allDates = (history.map { it.date } + weightHistory.map { it.date } + burnedCaloriesHistory.keys)
                .distinct()
                .sorted()

            val chartData = allDates.map { date ->
                val nutrition = history.find { it.date == date }
                val weight = weightByDate[date]
                val burned = burnedCaloriesHistory[date]
                val label = try {
                    shortFormat.format(dateFormat.parse(date)!!)
                } catch (_: Exception) {
                    date.takeLast(5)
                }
                ChartDataPoint(
                    label = label,
                    calories = nutrition?.totalCalories,
                    weight = weight?.weight,
                    burnedCalories = burned
                )
            }

            // Compute next date label for chart right edge
            val nextDateLabel = try {
                val lastDate = allDates.lastOrNull()
                if (lastDate != null) {
                    val cal = Calendar.getInstance()
                    cal.time = dateFormat.parse(lastDate)!!
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    shortFormat.format(cal.time)
                } else null
            } catch (_: Exception) { null }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HistoryViewModel.RANGE_OPTIONS.forEach { days ->
                            FilterChip(
                                selected = selectedRange == days,
                                onClick = { viewModel.setRange(days) },
                                label = { Text(rangeLabels[days] ?: "$days") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (chartData.isNotEmpty()) {
                    item {
                        DualAxisChart(
                            data = chartData,
                            caloriesGoal = goal.calories,
                            targetWeight = profile.targetWeight.takeIf { it > 0f },
                            nextDateLabel = nextDateLabel,
                            showCalories = showCalories,
                            showWeight = showWeight,
                            showBurned = showBurned,
                            caloriesOrigin = chartCaloriesOrigin,
                            weightOrigin = chartWeightOrigin
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = showCalories,
                                onClick = { viewModel.toggleShowCalories() },
                                label = { Text(stringResource(R.string.history_consumed)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = showWeight,
                                onClick = { viewModel.toggleShowWeight() },
                                label = { Text(stringResource(R.string.history_weight)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = showBurned,
                                onClick = { viewModel.toggleShowBurned() },
                                label = { Text(stringResource(R.string.history_burned)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = showMeasurements,
                                onClick = { viewModel.toggleShowMeasurements() },
                                label = { Text(stringResource(R.string.history_measurements)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.history_chart_origin),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = if (chartCaloriesOrigin == 0) "" else chartCaloriesOrigin.toString(),
                                onValueChange = { viewModel.setChartCaloriesOrigin(it.toIntOrNull() ?: 0) },
                                label = { Text(stringResource(R.string.history_calories)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                            OutlinedTextField(
                                value = if (chartWeightOrigin == 0) "" else chartWeightOrigin.toString(),
                                onValueChange = { viewModel.setChartWeightOrigin(it.toIntOrNull() ?: 0) },
                                label = { Text(stringResource(R.string.history_weight)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (showMeasurements && measurementHistory.isNotEmpty()) {
                            val dateFormat2 = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val shortFormat2 = SimpleDateFormat("dd/MM", Locale.US)
                            val measurementChartData = measurementHistory
                                .sortedBy { it.date }
                                .map { m ->
                                    val lbl = try {
                                        shortFormat2.format(dateFormat2.parse(m.date)!!)
                                    } catch (_: Exception) {
                                        m.date.takeLast(5)
                                    }
                                    MeasurementChartPoint(
                                        label = lbl,
                                        waist = m.waist,
                                        hips = m.hips,
                                        chest = m.chest,
                                        arms = m.arms,
                                        thighs = m.thighs
                                    )
                                }
                            MeasurementsChart(data = measurementChartData)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                items(history) { day ->
                    val isExpanded = selectedDate == day.date
                    val burned = burnedCaloriesHistory[day.date]?.toInt() ?: 0
                    DayCard(
                        day = day,
                        goal = goal,
                        burned = burned,
                        isExpanded = isExpanded,
                        meals = if (isExpanded) selectedDayMeals else emptyList(),
                        onClick = { viewModel.selectDay(day.date) },
                        onMealClick = onMealClick
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
    burned: Int,
    isExpanded: Boolean,
    meals: List<MealEntry>,
    onClick: () -> Unit,
    onMealClick: (MealEntry) -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val displayFormat = SimpleDateFormat("EEEE d MMMM", Locale.getDefault())
    val displayDate = try {
        displayFormat.format(dateFormat.parse(day.date)!!)
    } catch (_: Exception) {
        day.date
    }

    val calRatio = if (goal.calories > 0) day.totalCalories.toFloat() / goal.calories else 0f
    val calProgress = calRatio.coerceIn(0f, 1f)
    val calColor = when {
        burned > 0 && day.totalCalories > burned -> Color(0xFFF44336)    // Red: surplus
        day.totalCalories > goal.calories -> Color(0xFFFF9800)            // Orange: over goal but in deficit
        burned > 0 && day.totalCalories <= burned -> Color(0xFF4CAF50)    // Green: in deficit and under goal
        day.totalCalories <= goal.calories -> Color(0xFF4CAF50)           // Green: under goal (no burn data)
        else -> Color(0xFFF44336)                                         // Red: over goal (no burn data)
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
                        MealRow(meal, onClick = { onMealClick(meal) })
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MealRow(meal: MealEntry, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = meal.emoji ?: "🍽️",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(end = 8.dp)
        )
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
