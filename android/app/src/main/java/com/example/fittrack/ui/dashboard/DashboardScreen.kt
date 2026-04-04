package com.example.fittrack.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.fittrack.ui.theme.CoralPink
import com.example.fittrack.ui.theme.ErrorRed
import com.example.fittrack.ui.theme.MintGreen
import com.example.fittrack.ui.theme.SkyBlue
import com.example.fittrack.ui.theme.SoftLemon
import com.example.fittrack.ui.theme.SoftMint
import com.example.fittrack.ui.theme.SoftPeach
import com.example.fittrack.ui.theme.SoftSky
import com.example.fittrack.ui.theme.SurfaceCard
import com.example.fittrack.ui.theme.TextDark
import com.example.fittrack.ui.theme.TextFaint
import com.example.fittrack.ui.theme.TextMid
import com.example.fittrack.ui.theme.TextOnPrimary
import com.example.fittrack.ui.theme.WarmCream

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
                            text = "Good morning! \uD83D\uDC4B",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextDark
                        )
                        Text(
                            text = "Let's check your progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMid
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncNow() }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync", tint = TextMid)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextMid)
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
                    selected = true,
                    onClick = {},
                    colors = navItemColors
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DirectionsRun, contentDescription = "Activity") },
                    label = { Text("Activity") },
                    selected = false,
                    onClick = onNavigateToActivity,
                    colors = navItemColors
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    selected = false,
                    onClick = onNavigateToHistory,
                    colors = navItemColors
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = onNavigateToProfile,
                    colors = navItemColors
                )
            }
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
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CoralPink)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            StepProgressBar(
                                currentSteps = uiState.todaySteps?.steps ?: 0,
                                goalSteps = uiState.todaySteps?.dailyGoal ?: 0,
                                modifier = Modifier.padding(16.dp),
                                onEditGoalClick = { showGoalDialog = true }
                            )
                            Text(
                                text = "Tap ring to edit goal",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextFaint,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                }

                item {
                    val steps = uiState.todaySteps?.steps ?: 0
                    val goal = uiState.todaySteps?.dailyGoal ?: 10000
                    val calories = (steps * 0.04).toInt()
                    val distanceKm = steps * 0.0008

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "Steps",
                                value = "%,d".format(steps),
                                subtitle = "of %,d".format(goal),
                                icon = Icons.Default.DirectionsRun,
                                backgroundColor = SoftPeach,
                                iconColor = CoralPink
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "Calories",
                                value = "$calories",
                                subtitle = "kcal burned",
                                icon = Icons.Default.LocalFireDepartment,
                                backgroundColor = SoftLemon,
                                iconColor = Color(0xFFFF9500)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "Distance",
                                value = "%.1f".format(distanceKm),
                                subtitle = "km walked",
                                icon = Icons.Default.LocationOn,
                                backgroundColor = SoftMint,
                                iconColor = MintGreen
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                title = "Heart Rate",
                                value = "${uiState.recentActivities.firstOrNull()?.maxHr ?: "--"}",
                                subtitle = "last bpm",
                                icon = Icons.Default.Favorite,
                                backgroundColor = SoftSky,
                                iconColor = SkyBlue
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = onNavigateToActivity,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .testTag("dashboard_start_activity_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralPink,
                            contentColor = TextOnPrimary
                        )
                    ) {
                        Icon(Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Start Activity",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (uiState.recentActivities.isNotEmpty()) {
                    item {
                        Text(
                            text = "Today's Activities",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextDark,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(uiState.recentActivities) { activity ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(SoftPeach, shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.DirectionsRun,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = CoralPink
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activity.activityType,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextDark,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "%,d steps".format(activity.stepsTaken),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMid
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = CoralPink
                                    )
                                    Text(
                                        text = "${activity.maxHr} bpm",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMid,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                uiState.error?.let {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = it,
                                color = ErrorRed,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = iconColor)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = TextDark, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = TextMid, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextFaint)
        }
    }
}
