package com.ghostwan.snapcal.presentation.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.ghostwan.snapcal.R
import com.ghostwan.snapcal.data.local.HealthConnectManager
import com.ghostwan.snapcal.domain.model.ActivityLevel
import com.ghostwan.snapcal.domain.model.Gender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    healthConnectManager: HealthConnectManager
) {
    val profile by viewModel.profile.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val isComputing by viewModel.isComputing.collectAsState()
    val error by viewModel.error.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val healthConnectAvailable by viewModel.healthConnectAvailable.collectAsState()
    val isSyncingWeight by viewModel.isSyncingWeight.collectAsState()
    val weightSynced by viewModel.weightSynced.collectAsState()
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    val signedInEmail by viewModel.signedInEmail.collectAsState()
    val isBackingUp by viewModel.isBackingUp.collectAsState()
    val isRestoring by viewModel.isRestoring.collectAsState()
    val backupDone by viewModel.backupDone.collectAsState()
    val restoreDone by viewModel.restoreDone.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.profile_saved)
    val weightSyncedMessage = stringResource(R.string.profile_weight_synced)
    val backupDoneMessage = stringResource(R.string.profile_backup_done)
    val restoreDoneMessage = stringResource(R.string.profile_restore_done)

    val hcPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(healthConnectManager.requiredPermissions)) {
            viewModel.syncWeightFromHealthConnect()
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleSignInResult(result.data)
    }

    LaunchedEffect(saved) {
        if (saved) {
            snackbarHostState.showSnackbar(savedMessage)
            viewModel.clearSaved()
        }
    }

    LaunchedEffect(weightSynced) {
        if (weightSynced) {
            snackbarHostState.showSnackbar(weightSyncedMessage)
            viewModel.clearWeightSynced()
        }
    }

    LaunchedEffect(backupDone) {
        if (backupDone) {
            snackbarHostState.showSnackbar(backupDoneMessage)
            viewModel.clearBackupDone()
        }
    }

    LaunchedEffect(restoreDone) {
        if (restoreDone) {
            snackbarHostState.showSnackbar(restoreDoneMessage)
            viewModel.clearRestoreDone()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.profile_title)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Height
            OutlinedTextField(
                value = if (profile.height > 0) profile.height.toString() else "",
                onValueChange = {
                    viewModel.updateProfile(profile.copy(height = it.toIntOrNull() ?: 0))
                },
                label = { Text(stringResource(R.string.profile_height)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Weight
            OutlinedTextField(
                value = if (profile.weight > 0f) profile.weight.toString() else "",
                onValueChange = {
                    viewModel.updateProfile(profile.copy(weight = it.toFloatOrNull() ?: 0f))
                },
                label = { Text(stringResource(R.string.profile_weight)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Target weight
            OutlinedTextField(
                value = if (profile.targetWeight > 0f) profile.targetWeight.toString() else "",
                onValueChange = {
                    viewModel.updateProfile(profile.copy(targetWeight = it.toFloatOrNull() ?: 0f))
                },
                label = { Text(stringResource(R.string.profile_target_weight)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Age
            OutlinedTextField(
                value = if (profile.age > 0) profile.age.toString() else "",
                onValueChange = {
                    viewModel.updateProfile(profile.copy(age = it.toIntOrNull() ?: 0))
                },
                label = { Text(stringResource(R.string.profile_age)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Gender
            GenderSelector(
                selected = profile.gender,
                onSelect = { viewModel.updateProfile(profile.copy(gender = it)) }
            )

            // Activity level
            ActivityLevelSelector(
                selected = profile.activityLevel,
                onSelect = { viewModel.updateProfile(profile.copy(activityLevel = it)) }
            )

            // Save profile
            Button(
                onClick = { viewModel.saveProfile() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.profile_save))
            }

            // Health Connect section
            if (healthConnectAvailable) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = {
                        hcPermissionLauncher.launch(healthConnectManager.requiredPermissions)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSyncingWeight
                ) {
                    if (isSyncingWeight) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_syncing_weight))
                    } else {
                        Icon(Icons.Default.MonitorWeight, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_sync_weight))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Goals section
            Text(
                text = stringResource(R.string.profile_goals_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Manual calories goal
            OutlinedTextField(
                value = goal.calories.toString(),
                onValueChange = {
                    val cal = it.toIntOrNull() ?: return@OutlinedTextField
                    viewModel.updateGoalCalories(cal)
                },
                label = { Text(stringResource(R.string.profile_manual_calories)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Compute goals with AI
            OutlinedButton(
                onClick = { viewModel.computeGoals() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isComputing && profile.height > 0 && profile.weight > 0f && profile.age > 0
            ) {
                if (isComputing) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_computing))
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_compute_goals))
                }
            }

            // Display current goals
            GoalsCard(goal)

            // AI explanation
            if (goal.explanation != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.profile_ai_explanation),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = goal.explanation!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Data & Backup section
            Text(
                text = stringResource(R.string.profile_data_backup),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (isSignedIn) {
                Text(
                    text = stringResource(R.string.profile_signed_in_as, signedInEmail ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = { viewModel.backupToDrive() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isBackingUp && !isRestoring
                ) {
                    if (isBackingUp) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_backing_up))
                    } else {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_backup))
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.restoreFromDrive() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isBackingUp && !isRestoring
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_restoring))
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_restore))
                    }
                }

                TextButton(
                    onClick = { viewModel.signOut() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_sign_out))
                }
            } else {
                Button(
                    onClick = { googleSignInLauncher.launch(viewModel.getSignInIntent()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.profile_sign_in_google))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GoalsCard(goal: com.ghostwan.snapcal.domain.model.NutritionGoal) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_kcal_day, goal.calories),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(stringResource(R.string.profile_proteins_goal, goal.proteins))
            Text(stringResource(R.string.profile_carbs_goal, goal.carbs))
            Text(stringResource(R.string.profile_fats_goal, goal.fats))
            Text(stringResource(R.string.profile_fiber_goal, goal.fiber))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderSelector(selected: Gender, onSelect: (Gender) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (selected) {
        Gender.MALE -> stringResource(R.string.profile_gender_male)
        Gender.FEMALE -> stringResource(R.string.profile_gender_female)
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.profile_gender)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_gender_male)) },
                onClick = { onSelect(Gender.MALE); expanded = false }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_gender_female)) },
                onClick = { onSelect(Gender.FEMALE); expanded = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityLevelSelector(selected: ActivityLevel, onSelect: (ActivityLevel) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val labels = mapOf(
        ActivityLevel.SEDENTARY to stringResource(R.string.profile_activity_sedentary),
        ActivityLevel.LIGHT to stringResource(R.string.profile_activity_light),
        ActivityLevel.MODERATE to stringResource(R.string.profile_activity_moderate),
        ActivityLevel.ACTIVE to stringResource(R.string.profile_activity_active),
        ActivityLevel.VERY_ACTIVE to stringResource(R.string.profile_activity_very_active)
    )

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = labels[selected] ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.profile_activity_level)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ActivityLevel.entries.forEach { level ->
                DropdownMenuItem(
                    text = { Text(labels[level] ?: level.name) },
                    onClick = { onSelect(level); expanded = false }
                )
            }
        }
    }
}
