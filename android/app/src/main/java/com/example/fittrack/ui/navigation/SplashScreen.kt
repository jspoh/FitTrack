package com.example.fittrack.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fittrack.ui.auth.AuthUiState
import com.example.fittrack.ui.auth.AuthViewModel
import com.example.fittrack.ui.theme.ButtonBlue
import com.example.fittrack.ui.theme.WarmCream

@Composable
fun SplashScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToLanding: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthUiState.Success -> {
                onNavigateToDashboard()
            }
            is AuthUiState.Error -> {
                onNavigateToLanding()
            }
            is AuthUiState.Idle -> {
                onNavigateToLanding()
            }
            is AuthUiState.Loading -> {
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmCream)
    )
}