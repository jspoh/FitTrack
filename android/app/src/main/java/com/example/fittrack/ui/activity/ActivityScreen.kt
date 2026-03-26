package com.example.fittrack.ui.activity

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    onNavigateBack: () -> Unit,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var steps by remember { mutableStateOf("0") }
    var maxHr by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.startTracking() }

    LaunchedEffect(uiState.savedSuccess) {
        if (uiState.savedSuccess) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track Activity") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (uiState.isTracking) "Tracking..." else "Ready",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Activity: ${uiState.currentActivityType}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!uiState.isTracking) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            if (viewModel.hasPermission()) viewModel.startTracking()
                            else permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        } else {
                            viewModel.startTracking()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("activity_start_button")
                ) { Text("Start Tracking") }
            } else {
                OutlinedTextField(
                    value = steps, onValueChange = { steps = it },
                    label = { Text("Steps Taken") }, modifier = Modifier.fillMaxWidth().testTag("activity_steps_field"), singleLine = true
                )
                OutlinedTextField(
                    value = maxHr, onValueChange = { maxHr = it },
                    label = { Text("Max Heart Rate (bpm)") }, modifier = Modifier.fillMaxWidth().testTag("activity_maxhr_field"), singleLine = true
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") }, modifier = Modifier.fillMaxWidth().testTag("activity_notes_field")
                )
                Button(
                    onClick = {
                        viewModel.stopAndSave(
                            stepsTaken = steps.toIntOrNull() ?: 0,
                            maxHr = maxHr.toIntOrNull() ?: 0,
                            notes = notes
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("activity_stop_save_button"),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    else Text("Stop & Save")
                }
            }

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
