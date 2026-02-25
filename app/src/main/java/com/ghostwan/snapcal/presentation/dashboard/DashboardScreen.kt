package com.ghostwan.snapcal.presentation.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ghostwan.snapcal.R
import com.ghostwan.snapcal.domain.model.DailyNutrition
import org.json.JSONArray
import com.ghostwan.snapcal.domain.model.MealEntry
import com.ghostwan.snapcal.domain.model.NutritionGoal
import java.time.Instant
import java.util.Locale
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onScanMeal: () -> Unit,
    onProfile: () -> Unit,
    onMealClick: (MealEntry) -> Unit = {},
    onMergeMeals: (List<MealEntry>) -> Unit = {}
) {
    val nutrition by viewModel.nutrition.collectAsState()
    val meals by viewModel.meals.collectAsState()
    val effectiveGoal by viewModel.effectiveGoal.collectAsState()
    val caloriesBurned by viewModel.caloriesBurned.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedMealIds by viewModel.selectedMealIds.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dailyNote by viewModel.dailyNote.collectAsState()

    val context = LocalContext.current
    val isToday = selectedDate == LocalDate.now()
    val isYesterday = selectedDate == LocalDate.now().minusDays(1)

    var showDatePicker by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }

    val dateLabel = when {
        isToday -> stringResource(R.string.dashboard_today)
        isYesterday -> stringResource(R.string.dashboard_yesterday)
        else -> selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }

    LaunchedEffect(Unit) { viewModel.refresh() }

    if (showNoteDialog) {
        DailyNoteDialog(
            currentNote = dailyNote ?: "",
            onDismiss = { showNoteDialog = false },
            onSave = { note ->
                viewModel.saveDailyNote(note)
                showNoteDialog = false
            }
        )
    }

    if (showDatePicker) {
        val todayMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val initialMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= todayMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        viewModel.goToDate(picked)
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.dialog_button_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.dialog_button_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selectionMode) {
                        Text("${selectedMealIds.size}")
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { viewModel.goToPreviousDay() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = stringResource(R.string.dashboard_date_navigate_previous)
                                )
                            }
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .clickable { showDatePicker = true }
                            )
                            IconButton(
                                onClick = { viewModel.goToNextDay() },
                                enabled = !isToday
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = stringResource(R.string.dashboard_date_navigate_next)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (selectionMode) {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.dashboard_selection_cancel))
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                },
                actions = {
                    if (selectionMode) {
                        TextButton(
                            onClick = {
                                val selected = viewModel.getSelectedMeals()
                                viewModel.exitSelectionMode()
                                onMergeMeals(selected)
                            },
                            enabled = selectedMealIds.size >= 2
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MergeType, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.dashboard_merge))
                        }
                    } else {
                        IconButton(onClick = onProfile) {
                            Icon(Icons.Default.Person, contentDescription = stringResource(R.string.nav_profile))
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CaloriesRingCard(
                    current = nutrition?.totalCalories ?: 0,
                    goal = effectiveGoal.calories,
                    burned = caloriesBurned,
                    onBurnedClick = {
                        try {
                            val fitnessIntent = Intent(Intent.ACTION_MAIN).apply {
                                component = ComponentName(
                                    "com.google.android.apps.fitness",
                                    "com.google.android.apps.fitness.welcome.WelcomeActivity"
                                )
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(fitnessIntent)
                        } catch (_: Exception) {
                            val playStore = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.fitness"))
                            context.startActivity(playStore)
                        }
                    }
                )
            }

            item {
                MacrosProgressCard(nutrition, effectiveGoal)
            }

            item {
                DailyNoteCard(
                    note = dailyNote,
                    onClick = { showNoteDialog = true }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_meals_today),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (meals.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Restaurant,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isToday) stringResource(R.string.dashboard_no_meals)
                                       else stringResource(R.string.dashboard_no_meals_past),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(meals) { meal ->
                    MealCard(
                        meal = meal,
                        selectionMode = selectionMode,
                        isSelected = meal.id in selectedMealIds,
                        onClick = {
                            if (selectionMode) {
                                viewModel.toggleMealSelection(meal.id)
                            } else {
                                onMealClick(meal)
                            }
                        },
                        onLongClick = {
                            if (!selectionMode) {
                                viewModel.enterSelectionMode(meal.id)
                            }
                        },
                        onDelete = { viewModel.deleteMeal(meal.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(meal) }
                    )
                }
            }

            item {
                Button(
                    onClick = onScanMeal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Restaurant, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isToday) stringResource(R.string.dashboard_scan_meal)
                        else stringResource(R.string.dashboard_add_meal)
                    )
                }
            }
        }
    }
}

@Composable
private fun CaloriesRingCard(current: Int, goal: Int, burned: Int, onBurnedClick: () -> Unit = {}) {
    val progress = if (goal > 0) (current.toFloat() / goal).coerceIn(0f, 1.5f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "calories_progress"
    )

    val ringColor = when {
        current > burned && burned > 0 -> Color(0xFFF44336)              // Red: consumed > burned (surplus)
        current > goal -> Color(0xFFFF9800)                               // Orange: consumed > goal but in deficit
        burned > 0 && current <= burned -> Color(0xFF4CAF50)              // Green: in deficit and under goal
        current <= goal -> Color(0xFF4CAF50)                              // Green: under goal (no burn data)
        else -> Color(0xFFF44336)                                         // Red: over goal (no burn data)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.dashboard_calories),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    val strokeWidth = 16.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val topLeft = Offset(
                        (size.width - radius * 2) / 2,
                        (size.height - radius * 2) / 2
                    )
                    val arcSize = Size(radius * 2, radius * 2)

                    // Background ring
                    drawArc(
                        color = ringColor.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress ring
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f / 1.5f * (if (progress > 1f) 1.5f else 1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$current",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = ringColor
                    )
                    Text(
                        text = stringResource(R.string.dashboard_of_goal, current, goal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val remaining = goal - current
            val remainingColor = when {
                current > burned && burned > 0 -> Color(0xFFF44336)
                current > goal -> Color(0xFFFF9800)
                burned > 0 && current <= burned -> Color(0xFF4CAF50)
                current <= goal -> Color(0xFF4CAF50)
                else -> Color(0xFFF44336)
            }
            Text(
                text = if (remaining >= 0)
                    stringResource(R.string.dashboard_remaining, remaining)
                else
                    stringResource(R.string.dashboard_exceeded, -remaining),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = remainingColor
            )

            if (burned > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.clickable { onBurnedClick() }
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF9C27B0)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.dashboard_burned, burned),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9C27B0)
                    )
                }
            }
        }
    }
}

@Composable
private fun MacrosProgressCard(nutrition: DailyNutrition?, goal: NutritionGoal) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.result_macros),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            MacroProgressBar(
                label = stringResource(R.string.result_proteins),
                current = nutrition?.totalProteins ?: 0f,
                goal = goal.proteins,
                color = Color(0xFFF44336)
            )
            MacroProgressBar(
                label = stringResource(R.string.result_carbs),
                current = nutrition?.totalCarbs ?: 0f,
                goal = goal.carbs,
                color = Color(0xFF2196F3)
            )
            MacroProgressBar(
                label = stringResource(R.string.result_fats),
                current = nutrition?.totalFats ?: 0f,
                goal = goal.fats,
                color = Color(0xFFFF9800)
            )
            MacroProgressBar(
                label = stringResource(R.string.result_fiber),
                current = nutrition?.totalFiber ?: 0f,
                goal = goal.fiber,
                color = Color(0xFF6B8E23)
            )
        }
    }
}

@Composable
private fun MacroProgressBar(label: String, current: Float, goal: Float, color: Color) {
    val progress = if (goal > 0f) (current / goal).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800),
        label = "macro_progress"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.unit_grams, current) + " / " + stringResource(R.string.unit_grams, goal),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MealCard(
    meal: MealEntry,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val healthInfo = remember(meal.ingredientsJson) {
        computeHealthInfo(meal.ingredientsJson)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (healthInfo != null) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(healthInfo.color, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                )
            }
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (selectionMode) 4.dp else 16.dp,
                        top = 16.dp,
                        bottom = 16.dp,
                        end = 16.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = meal.emoji ?: "\uD83C\uDF7D\uFE0F",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = meal.dishName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (meal.quantity > 1) {
                            Text(
                                text = " \u00D7${meal.quantity}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.result_kcal, meal.calories * meal.quantity),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (healthInfo != null) {
                            Text(
                                text = " ${healthInfo.emoji}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                if (!selectionMode) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (meal.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = stringResource(R.string.dashboard_toggle_favorite),
                            tint = if (meal.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyNoteCard(note: String?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (note != null)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.EditNote,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (note != null)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = note ?: stringResource(R.string.dashboard_note_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = if (note != null)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DailyNoteDialog(
    currentNote: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentNote) }
    val context = LocalContext.current

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                text = if (text.isBlank()) matches[0] else "$text ${matches[0]}"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dashboard_note_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.dashboard_note_hint)) },
                minLines = 3,
                maxLines = 6,
                trailingIcon = {
                    IconButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.dashboard_note_voice_prompt))
                        }
                        speechLauncher.launch(intent)
                    }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = stringResource(R.string.home_button_voice)
                        )
                    }
                }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text(stringResource(R.string.dialog_button_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_button_cancel))
            }
        }
    )
}

private data class HealthInfo(val emoji: String, val color: Color)

private fun computeHealthInfo(ingredientsJson: String): HealthInfo? {
    return try {
        val array = JSONArray(ingredientsJson)
        val scores = mutableListOf<Int>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.has("healthRating") && !obj.isNull("healthRating")) {
                scores.add(when (obj.getString("healthRating")) {
                    "healthy" -> 2
                    "moderate" -> 1
                    else -> 0
                })
            }
        }
        if (scores.isEmpty()) return null
        val avg = scores.average()
        when {
            avg >= 1.5 -> HealthInfo("\uD83D\uDE0A", Color(0xFF4CAF50))
            avg >= 0.75 -> HealthInfo("\uD83D\uDE10", Color(0xFFFF9800))
            else -> HealthInfo("\uD83D\uDE1F", Color(0xFFF44336))
        }
    } catch (_: Exception) {
        null
    }
}
