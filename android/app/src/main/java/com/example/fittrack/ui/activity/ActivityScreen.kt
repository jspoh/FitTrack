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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fittrack.domain.model.toActivityDisplayName
import com.example.fittrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.startTracking() }

    LaunchedEffect(uiState.savedSuccess) {
        if (uiState.savedSuccess) onNavigateToDashboard()
    }

    Scaffold(
        containerColor = WarmCream,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WarmCream,
                    titleContentColor = TextDark
                ),
                title = {
                    Column {
                        Text(
                            text = "Activity",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextDark
                        )
                        Text(
                            text = if (uiState.isTracking) "Tracking in progress" else "Ready to start",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMid
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceCard,
                tonalElevation = 0.dp,
                modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CoralPink,
                    selectedTextColor = CoralPink,
                    indicatorColor = SoftPeach,
                    unselectedIconColor = TextMid,
                    unselectedTextColor = TextMid
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = false,
                    onClick = onNavigateToDashboard,  // ← was onNavigateBack
                    colors = navItemColors
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DirectionsRun, contentDescription = "Activity") },
                    label = { Text("Activity") },
                    selected = true,
                    onClick = {},
                    colors = navItemColors
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    selected = false,
                    onClick = onNavigateToHistory,  // ← was onNavigateBack
                    colors = navItemColors
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = onNavigateToProfile,  // ← was onNavigateBack
                    colors = navItemColors
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Current Activity: ${uiState.currentActivityType.toActivityDisplayName()}",
                style = MaterialTheme.typography.titleMedium,
                color = TextMid
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("activity_start_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralPink,
                        contentColor = TextOnPrimary
                    )
                ) {
                    Text(
                        "Start Tracking",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "${uiState.stepCount}",
                    style = MaterialTheme.typography.displayLarge,
                    color = TextDark,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("activity_live_steps")
                )
                Text(
                    text = "steps",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextMid
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatElapsedTime(uiState.elapsedSeconds),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextMid,
                    modifier = Modifier.testTag("activity_elapsed_time")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.stopAndSave() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("activity_stop_save_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralPink,
                        contentColor = TextOnPrimary
                    ),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = TextOnPrimary
                    )
                    else Text(
                        "Stop & Save",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            uiState.error?.let {
                Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun formatElapsedTime(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%02d:%02d", minutes, seconds)
}