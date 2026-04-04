package com.example.fittrack.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fittrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(uiState.user) {
        uiState.user?.let {
            username = it.username
            email = it.email
        }
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
                            text = "Profile",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextDark
                        )
                        Text(
                            text = "Manage your account",
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
                    selected = false,
                    onClick = onNavigateToHistory,
                    colors = navItemColors
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = true,
                    onClick = {},
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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("New Password (leave blank to keep)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                uiState.error?.let {
                    Text(it, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                }
                if (uiState.saveSuccess) {
                    Text("Changes saved!", color = MintGreen, style = MaterialTheme.typography.bodySmall)
                }
                if (uiState.noChanges) {
                    Text("No changes were made.", color = TextMid, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        val currentUser = uiState.user
                        val newUsername = username.takeIf { it != currentUser?.username }
                        val newEmail = email.takeIf { it != currentUser?.email }
                        val newPassword = password.ifBlank { null }
                        if (newUsername != null || newEmail != null || newPassword != null) {
                            viewModel.resetNoChanges()
                            viewModel.updateUser(newUsername, newEmail, newPassword)
                        } else {
                            viewModel.noChanges()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
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
                    else Text("Save Changes", style = MaterialTheme.typography.titleSmall)
                }

                OutlinedButton(
                    onClick = { viewModel.logout(onLogout) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                ) {
                    Text("Logout", style = MaterialTheme.typography.titleSmall)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}