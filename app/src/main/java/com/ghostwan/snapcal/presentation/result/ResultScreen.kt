package com.ghostwan.snapcal.presentation.result

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ghostwan.snapcal.R
import com.ghostwan.snapcal.domain.model.FoodAnalysis
import com.ghostwan.snapcal.domain.model.Ingredient
import com.ghostwan.snapcal.domain.model.ProductHealthInfo
import com.ghostwan.snapcal.presentation.FoodAnalysisViewModel
import com.ghostwan.snapcal.presentation.model.AnalysisUiState
import androidx.compose.ui.text.input.KeyboardType
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: FoodAnalysisViewModel,
    onBack: () -> Unit,
    onMealSaved: () -> Unit = {},
    onAddToShoppingList: ((Ingredient) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val mealSaved by viewModel.mealSaved.collectAsState()
    val readOnly by viewModel.readOnly.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val isEditing = viewModel.isEditing()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    androidx.compose.runtime.LaunchedEffect(mealSaved) {
        if (mealSaved) {
            onMealSaved()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = stringResource(R.string.dashboard_toggle_favorite),
                                tint = if (isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                    readOnly = readOnly,
                    isEditing = isEditing,
                    quantity = quantity,
                    onQuantityChange = { viewModel.updateQuantity(it) },
                    onSave = { viewModel.saveMeal(state.result) },
                    onCorrect = { feedback -> viewModel.correctAnalysis(state.result, feedback) },
                    onEmojiChange = { emoji -> viewModel.updateEmoji(emoji) },
                    onDishNameChange = { name -> viewModel.updateDishName(name) },
                    onRemoveIngredient = { index -> viewModel.removeIngredient(index) },
                    onUpdateIngredient = { index, qty, cal -> viewModel.updateIngredient(index, qty, cal) },
                    onAddToShoppingList = onAddToShoppingList?.let { addToList ->
                        { ingredient: Ingredient ->
                            addToList(ingredient)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.shopping_list_item_added)
                                )
                            }
                        }
                    }
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
    readOnly: Boolean = false,
    isEditing: Boolean = false,
    quantity: Int = 1,
    onQuantityChange: (Int) -> Unit = {},
    onSave: () -> Unit,
    onCorrect: (String) -> Unit,
    onEmojiChange: (String) -> Unit = {},
    onDishNameChange: (String) -> Unit = {},
    onRemoveIngredient: (Int) -> Unit = {},
    onUpdateIngredient: (Int, String, Int) -> Unit = { _, _, _ -> },
    onAddToShoppingList: ((Ingredient) -> Unit)? = null
) {
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showDishNameEditor by remember { mutableStateOf(false) }
    var editingIngredientIndex by remember { mutableStateOf<Int?>(null) }

    if (showEmojiPicker) {
        EmojiPickerDialog(
            dishName = result.dishName,
            currentEmoji = result.emoji ?: "🍽️",
            onEmojiSelected = { emoji ->
                onEmojiChange(emoji)
                showEmojiPicker = false
            },
            onDismiss = { showEmojiPicker = false }
        )
    }

    if (showDishNameEditor) {
        DishNameEditDialog(
            currentName = result.dishName,
            onConfirm = { newName ->
                onDishNameChange(newName)
                showDishNameEditor = false
            },
            onDismiss = { showDishNameEditor = false }
        )
    }

    editingIngredientIndex?.let { index ->
        if (index < result.ingredients.size) {
            IngredientEditDialog(
                ingredient = result.ingredients[index],
                onConfirm = { newQuantity, newCalories ->
                    onUpdateIngredient(index, newQuantity, newCalories)
                    editingIngredientIndex = null
                },
                onDismiss = { editingIngredientIndex = null }
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = result.emoji ?: "🍽️",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clickable { showEmojiPicker = true }
                )
                Text(
                    text = result.dishName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showDishNameEditor = true }
                )
            }
        }

        if (!mealSaved || readOnly) {
            item {
                QuantitySelector(
                    quantity = quantity,
                    onQuantityChange = onQuantityChange
                )
            }
        }

        item { CaloriesCard(result.totalCalories * quantity, result.ingredients) }

        if (result.healthInfo != null) {
            item { HealthInfoCard(result.healthInfo) }
        }

        if (result.macros != null) {
            item {
                MacrosCard(
                    macros = if (quantity > 1) com.ghostwan.snapcal.domain.model.Macros(
                        proteins = scaleGrams(result.macros.proteins, quantity),
                        carbs = scaleGrams(result.macros.carbs, quantity),
                        fats = scaleGrams(result.macros.fats, quantity),
                        fiber = result.macros.fiber?.let { scaleGrams(it, quantity) }
                    ) else result.macros
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.result_ingredients),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        itemsIndexed(result.ingredients) { index, ingredient ->
            IngredientCard(
                ingredient = ingredient,
                editable = true,
                deletable = !readOnly,
                onEdit = { editingIngredientIndex = index },
                onDelete = { onRemoveIngredient(index) },
                onAddToCart = onAddToShoppingList?.let { { it(ingredient) } }
            )
        }

        if (result.notes != null) {
            item { NotesCard(result.notes) }
        }

        // Correction UI (always shown unless meal was just saved)
        if (!mealSaved) {
            item {
                CorrectionSection(onCorrect = onCorrect)
            }
        }

        // Save meal button (hidden in read-only mode, shows after correction)
        if (!readOnly) {
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
                        Text(stringResource(
                            if (isEditing) R.string.result_update_meal
                            else R.string.result_save_meal
                        ))
                    }
                }
            }
        }
    }
}

private fun scaleGrams(value: String, factor: Int): String {
    if (factor <= 1) return value
    val numeric = value.replace(Regex("[^0-9.,]"), "").replace(",", ".").toFloatOrNull()
        ?: return value
    val scaled = (numeric * factor).let { if (it == it.toLong().toFloat()) it.toLong().toString() else String.format("%.1f", it) }
    return "${scaled}g"
}

@Composable
private fun QuantitySelector(
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.meal_quantity_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    enabled = quantity > 1
                ) {
                    Text("−", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "$quantity",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = { onQuantityChange(quantity + 1) },
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CorrectionSection(onCorrect: (String) -> Unit) {
    val context = LocalContext.current
    var showCorrectionField by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                feedbackText = matches[0]
                showCorrectionField = true
            }
        }
    }

    if (showCorrectionField) {
        OutlinedTextField(
            value = feedbackText,
            onValueChange = { feedbackText = it },
            label = { Text(stringResource(R.string.result_correction_hint)) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            trailingIcon = {
                IconButton(onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.result_correction_hint))
                    }
                    speechLauncher.launch(intent)
                }) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = stringResource(R.string.home_button_voice),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
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

@Composable
private fun CaloriesCard(totalCalories: Int, ingredients: List<Ingredient> = emptyList()) {
    val ratings = ingredients.mapNotNull { it.healthRating }
    val overallRating = if (ratings.isNotEmpty()) {
        val scores = ratings.map { when (it) { "healthy" -> 2; "moderate" -> 1; else -> 0 } }
        val avg = scores.average()
        when {
            avg >= 1.5 -> "healthy"
            avg >= 0.75 -> "moderate"
            else -> "unhealthy"
        }
    } else null

    val healthEmoji = when (overallRating) {
        "healthy" -> "\uD83D\uDE0A"
        "moderate" -> "\uD83D\uDE10"
        "unhealthy" -> "\uD83D\uDE1F"
        else -> null
    }

    val healthColor = when (overallRating) {
        "healthy" -> Color(0xFF4CAF50)
        "moderate" -> Color(0xFFFF9800)
        "unhealthy" -> Color(0xFFF44336)
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            if (healthColor != null) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(healthColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                )
            }
            Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.result_total_calories),
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (healthEmoji != null) {
                        Text(
                            text = healthEmoji,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.result_kcal, totalCalories),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
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
private fun HealthInfoCard(healthInfo: ProductHealthInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.health_indicators_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (healthInfo.nutriScore != null) {
                NutriScoreRow(healthInfo.nutriScore)
            }

            if (healthInfo.novaGroup != null) {
                NovaGroupRow(healthInfo.novaGroup)
            }

            if (healthInfo.nutrientLevels != null) {
                NutrientLevelsSection(healthInfo.nutrientLevels)
            }
        }
    }
}

@Composable
private fun NutriScoreRow(grade: String) {
    val grades = listOf("a", "b", "c", "d", "e")
    val colors = listOf(
        Color(0xFF038141), // A - dark green
        Color(0xFF85BB2F), // B - light green
        Color(0xFFFECB02), // C - yellow
        Color(0xFFEE8100), // D - orange
        Color(0xFFE63E11)  // E - red
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.health_nutriscore_label),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(90.dp)
        )
        grades.forEachIndexed { index, g ->
            val isActive = g == grade.lowercase()
            Box(
                modifier = Modifier
                    .size(if (isActive) 36.dp else 28.dp)
                    .background(
                        color = if (isActive) colors[index] else colors[index].copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = g.uppercase(),
                    color = Color.White,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    style = if (isActive) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun NovaGroupRow(group: Int) {
    val description = when (group) {
        1 -> stringResource(R.string.health_nova_1)
        2 -> stringResource(R.string.health_nova_2)
        3 -> stringResource(R.string.health_nova_3)
        4 -> stringResource(R.string.health_nova_4)
        else -> ""
    }
    val color = when (group) {
        1 -> Color(0xFF038141)
        2 -> Color(0xFF85BB2F)
        3 -> Color(0xFFEE8100)
        4 -> Color(0xFFE63E11)
        else -> Color.Gray
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.health_nova_label),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(90.dp)
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = group.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NutrientLevelsSection(levels: com.ghostwan.snapcal.domain.model.NutrientLevels) {
    val items = listOfNotNull(
        levels.fat?.let { stringResource(R.string.health_fat) to it },
        levels.saturatedFat?.let { stringResource(R.string.health_saturated_fat) to it },
        levels.sugars?.let { stringResource(R.string.health_sugars) to it },
        levels.salt?.let { stringResource(R.string.health_salt) to it }
    )
    if (items.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { (label, level) ->
            NutrientLevelRow(label, level)
        }
    }
}

@Composable
private fun NutrientLevelRow(label: String, level: String) {
    val color = when (level) {
        "low" -> Color(0xFF038141)
        "moderate" -> Color(0xFFEE8100)
        "high" -> Color(0xFFE63E11)
        else -> Color.Gray
    }
    val levelText = when (level) {
        "low" -> stringResource(R.string.health_level_low)
        "moderate" -> stringResource(R.string.health_level_moderate)
        "high" -> stringResource(R.string.health_level_high)
        else -> level
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(50))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = levelText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
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
private fun IngredientCard(
    ingredient: Ingredient,
    editable: Boolean = false,
    deletable: Boolean = editable,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onAddToCart: (() -> Unit)? = null
) {
    val healthColor = when (ingredient.healthRating) {
        "healthy" -> Color(0xFF4CAF50)
        "moderate" -> Color(0xFFFF9800)
        "unhealthy" -> Color(0xFFF44336)
        else -> null
    }
    val healthEmoji = when (ingredient.healthRating) {
        "healthy" -> "\uD83D\uDE0A"
        "moderate" -> "\uD83D\uDE10"
        "unhealthy" -> "\uD83D\uDE1F"
        else -> null
    }

    val hasActions = deletable || onAddToCart != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (editable) onEdit else ({})
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (healthColor != null) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(healthColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = if (hasActions) 4.dp else 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (healthEmoji != null) {
                            Text(
                                text = healthEmoji,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                        Text(
                            text = ingredient.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
            if (hasActions) {
                Row(
                    modifier = Modifier.padding(end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onAddToCart != null) {
                        IconButton(
                            onClick = onAddToCart,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.AddShoppingCart,
                                contentDescription = stringResource(R.string.shopping_list_add),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (deletable) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseNumericValue(text: String): Float? {
    return text.replace(Regex("[^0-9.,]"), "")
        .replace(",", ".")
        .toFloatOrNull()
}

@Composable
private fun IngredientEditDialog(
    ingredient: Ingredient,
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val originalNumeric = remember { parseNumericValue(ingredient.quantity) }
    var quantity by remember { mutableStateOf(ingredient.quantity) }
    var calories by remember { mutableStateOf(ingredient.calories.toString()) }
    var caloriesManuallyEdited by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ingredient_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { newQuantity ->
                        quantity = newQuantity
                        if (!caloriesManuallyEdited && originalNumeric != null && originalNumeric > 0f) {
                            val newNumeric = parseNumericValue(newQuantity)
                            if (newNumeric != null) {
                                val ratio = newNumeric / originalNumeric
                                calories = (ingredient.calories * ratio).toInt().toString()
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.ingredient_quantity_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = {
                        calories = it
                        caloriesManuallyEdited = true
                    },
                    label = { Text(stringResource(R.string.ingredient_calories_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cal = calories.toIntOrNull() ?: ingredient.calories
                    onConfirm(quantity, cal)
                }
            ) {
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

@Composable
private fun EmojiPickerDialog(
    dishName: String,
    currentEmoji: String,
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var emojiText by remember { mutableStateOf(currentEmoji) }
    val suggestions = listOf("🍕", "🥗", "🍣", "🍔", "🥩", "🍝", "🍲", "🥘", "🍜", "🍱", "🍛", "🥐", "🍰", "🍎", "🥤")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.emoji_picker_title)) },
        text = {
            Column {
                Text(
                    text = dishName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = emojiText,
                    onValueChange = { newValue ->
                        if (newValue.length <= 12) emojiText = newValue
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    suggestions.take(5).forEach { emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.clickable { emojiText = emoji }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    suggestions.drop(5).take(5).forEach { emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.clickable { emojiText = emoji }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    suggestions.drop(10).forEach { emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.clickable { emojiText = emoji }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (emojiText.isNotBlank()) onEmojiSelected(emojiText) }
            ) {
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

@Composable
private fun DishNameEditDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.result_edit_name_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) }
            ) {
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
