package com.example.fittrack.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fittrack.domain.model.toActivityDisplayName
import com.example.fittrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                            text = "History",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextDark
                        )
                        Text(
                            text = "Last 7 days",
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
                    onClick = onNavigateToDashboard,
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
                    selected = true,
                    onClick = {},
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
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator(color = CoralPink) }
        } else if (uiState.activities.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No activities in the last 7 days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMid
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(uiState.activities) { activity ->
                    var isEditing by remember { mutableStateOf(false) }
                    var editedName by remember { mutableStateOf(activity.activityName) }

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
                                if (isEditing) {
                                    OutlinedTextField(
                                        value = editedName,
                                        onValueChange = { editedName = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.titleSmall.copy(color = TextDark),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text(
                                        text = activity.activityName.ifBlank { "Untitled Activity" },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextDark,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "${activity.activityType.toActivityDisplayName()} · %,d steps".format(activity.stepsTaken),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMid
                                )
                                Text(
                                    text = activity.start.take(10),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextFaint
                                )
                                if (activity.notes.isNotEmpty()) {
                                    Text(
                                        text = activity.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMid
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (isEditing) {
                                    IconButton(onClick = {
                                        viewModel.updateActivity(activity.id, editedName)
                                        isEditing = false
                                    }) {
                                        Icon(Icons.Default.Check, contentDescription = "Save", tint = CoralPink, modifier = Modifier.size(20.dp))
                                    }
                                } else {
                                    IconButton(onClick = { isEditing = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextFaint, modifier = Modifier.size(20.dp))
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteActivity(activity.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}