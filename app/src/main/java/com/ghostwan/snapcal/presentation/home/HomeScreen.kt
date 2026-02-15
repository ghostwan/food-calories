package com.ghostwan.snapcal.presentation.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.ghostwan.snapcal.R
import com.ghostwan.snapcal.domain.model.MealEntry
import com.ghostwan.snapcal.presentation.FoodAnalysisViewModel
import com.ghostwan.snapcal.presentation.FoodAnalysisViewModel.Companion.FREE_DAILY_LIMIT
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FoodAnalysisViewModel,
    onAnalysisStarted: () -> Unit,
    favorites: List<MealEntry> = emptyList(),
    onQuickAddFavorite: (MealEntry) -> Unit = {},
    onMealClick: (MealEntry) -> Unit = {}
) {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var foodDescription by remember { mutableStateOf("") }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showQuotaWarning by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf(viewModel.getApiKey()) }
    var pendingTextAnalysis by remember { mutableStateOf(false) }

    val imageFile = remember {
        File(File(context.cacheDir, "images").apply { mkdirs() }, "food_photo.jpg")
    }

    val fileProviderUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri = fileProviderUri
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUri = uri
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(fileProviderUri)
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                foodDescription = matches[0]
            }
        }
    }

    if (showQuotaWarning) {
        QuotaWarningDialog(
            dailyCount = viewModel.getDailyRequestCount(),
            dailyLimit = FREE_DAILY_LIMIT,
            onConfirm = {
                showQuotaWarning = false
                viewModel.resetState()
                if (pendingTextAnalysis) {
                    viewModel.analyzeFoodFromText(foodDescription)
                    pendingTextAnalysis = false
                } else {
                    viewModel.analyzeFood(context, photoUri!!)
                }
                onAnalysisStarted()
            },
            onDismiss = {
                showQuotaWarning = false
                pendingTextAnalysis = false
            }
        )
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = apiKey,
            onDismiss = { showApiKeyDialog = false },
            onConfirm = { key ->
                apiKey = key
                viewModel.setApiKey(key)
                showApiKeyDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showApiKeyDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_description))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.home_description),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    if (photoUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(photoUri),
                            contentDescription = stringResource(R.string.home_photo_description),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.height(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.home_no_photo),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(stringResource(R.string.home_button_photo))
                    }

                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(stringResource(R.string.home_button_gallery))
                    }
                }
            }

            if (photoUri != null) {
                item {
                    Button(
                        onClick = {
                            if (viewModel.isQuotaExceeded()) {
                                showQuotaWarning = true
                            } else {
                                viewModel.resetState()
                                viewModel.analyzeFood(context, photoUri!!)
                                onAnalysisStarted()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = apiKey.isNotBlank()
                    ) {
                        Text(stringResource(R.string.home_button_analyze))
                    }
                }
            }

            // Text/voice input section
            item {
                Text(
                    text = stringResource(R.string.home_or_describe),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                OutlinedTextField(
                    value = foodDescription,
                    onValueChange = { foodDescription = it },
                    label = { Text(stringResource(R.string.home_text_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    trailingIcon = {
                        Row {
                            IconButton(onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.home_voice_prompt))
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
                    }
                )
            }

            if (foodDescription.isNotBlank()) {
                item {
                    Button(
                        onClick = {
                            if (viewModel.isQuotaExceeded()) {
                                pendingTextAnalysis = true
                                showQuotaWarning = true
                            } else {
                                viewModel.resetState()
                                viewModel.analyzeFoodFromText(foodDescription)
                                onAnalysisStarted()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = apiKey.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(stringResource(R.string.home_button_analyze_text))
                    }
                }
            }

            // Barcode scanner section
            item {
                Text(
                    text = stringResource(R.string.home_or_scan_barcode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                OutlinedButton(
                    onClick = {
                        val options = GmsBarcodeScannerOptions.Builder()
                            .setBarcodeFormats(
                                Barcode.FORMAT_EAN_13,
                                Barcode.FORMAT_EAN_8,
                                Barcode.FORMAT_UPC_A,
                                Barcode.FORMAT_UPC_E
                            )
                            .build()
                        val scanner = GmsBarcodeScanning.getClient(context, options)
                        scanner.startScan()
                            .addOnSuccessListener { barcode ->
                                barcode.rawValue?.let { value ->
                                    viewModel.resetState()
                                    viewModel.analyzeFoodFromBarcode(value)
                                    onAnalysisStarted()
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(stringResource(R.string.home_button_barcode))
                }
            }

            if (apiKey.isBlank()) {
                item {
                    Text(
                        text = stringResource(R.string.home_api_key_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Favorites section
            if (favorites.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = stringResource(R.string.dashboard_favorites),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(favorites) { meal ->
                    FavoriteCard(
                        meal = meal,
                        onQuickAdd = { onQuickAddFavorite(meal) },
                        onClick = { onMealClick(meal) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiKeyDialog(
    currentKey: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var key by remember { mutableStateOf(currentKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_api_key_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.dialog_api_key_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(stringResource(R.string.dialog_api_key_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(key) }) {
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
private fun QuotaWarningDialog(
    dailyCount: Int,
    dailyLimit: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quota_warning_title)) },
        text = {
            Text(
                stringResource(R.string.quota_warning_message, dailyCount, dailyLimit),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.quota_warning_continue))
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
private fun FavoriteCard(meal: MealEntry, onQuickAdd: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = meal.emoji ?: "\uD83C\uDF7D\uFE0F",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.dishName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.result_kcal, meal.calories),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onQuickAdd) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.dashboard_add_favorite),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
