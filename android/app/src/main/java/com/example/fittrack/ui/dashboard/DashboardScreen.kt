package com.example.fittrack.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fittrack.ui.theme.*
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import com.example.fittrack.domain.model.toActivityDisplayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToActivity: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showGoalDialog by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadDashboard()
    }

    Scaffold(
        containerColor = BackgroundBlue,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ButtonBlue,
                    titleContentColor = TextWhite,
                    actionIconContentColor = TextWhite
                ),
                title = { Text("FitTrack",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextWhite) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { viewModel.syncNow() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync Now")
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { padding ->
        if (showGoalDialog) {
            EditGoalDialog(
                currentGoal = uiState.todaySteps?.dailyGoal ?: 10000,
                onDismiss = { showGoalDialog = false },
                onConfirm = { newGoal ->
                    viewModel.updateDailyGoal(newGoal)
                    showGoalDialog = false
                }
            )
        }

        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Today's Summary",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextBlack
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StepProgressBar(
                            currentSteps = uiState.todaySteps?.steps ?: 0,
                            goalSteps = uiState.todaySteps?.dailyGoal ?: 0, // Placeholder
                            modifier = Modifier.padding(16.dp),
                            onEditGoalClick = { showGoalDialog = true }
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            onClick = onNavigateToActivity,
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(56.dp)
                                .testTag("dashboard_start_activity_card"),
                            shape = RoundedCornerShape(30.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.isTracking) Color(0xFFFF9800) else ButtonBlue,
                                contentColor = TextWhite
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(), // Fill the card's internal space
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.DirectionsRun, contentDescription = null)
                                Text(
                                    text = if (uiState.isTracking) "Show Activity" else "Start Activity",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }

                if (uiState.recentActivities.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp).padding(horizontal = 4.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("Today's Activities", style = MaterialTheme.typography.bodySmall, color = TextBlack)
                        }
                    }
                    items(uiState.recentActivities) { activity ->
                        Card(modifier = Modifier
                            .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp), // Softer corners
                            colors = CardDefaults.cardColors(containerColor = TextWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                            /*Column(modifier = Modifier.padding(12.dp)) {
                                Text(activity.activityType, style = MaterialTheme.typography.bodyLarge)
                                Text("Steps: ${activity.stepsTaken} · Max HR: ${activity.maxHr} bpm",
                                    style = MaterialTheme.typography.bodySmall)
                            }*/
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Default.DirectionsRun, contentDescription = null)

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activity.activityType.toActivityDisplayName(),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextBlack,
                                    )
                                    Text(
                                        text = "${activity.stepsTaken} steps",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextGrey
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = TextWhite,
                                    //modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(
                                        text = "♥ ${activity.maxHr} BPM",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextGrey,
                                        /*modifier = Modifier.padding(
                                            horizontal = 4.dp,
                                            vertical = 2.dp
                                        )*/
                                    )
                                }
                            }

                        }
                    }
                }

                uiState.error?.let {
                    item { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}
