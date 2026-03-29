package com.example.fittrack.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fittrack.ui.theme.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        if (authState is AuthUiState.Success) {
            viewModel.resetState()
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("FitTrack", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sign in to your account",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = TextGrey
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username", color = TextGrey) },
            modifier = Modifier.fillMaxWidth().testTag("login_username_field"),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = TextGrey) },
            modifier = Modifier.fillMaxWidth().testTag("login_password_field"),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (authState is AuthUiState.Error) {
            Text(
                text = (authState as AuthUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("login_error")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.login(username, password) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("login_button"),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonBlue,
                contentColor = TextWhite,
            ),
            enabled = authState !is AuthUiState.Loading
        ) {
            if (authState is AuthUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
            } else {
                Text("Login", style = MaterialTheme.typography.bodyLarge)
            }
        }

        //Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onNavigateToRegister, modifier = Modifier.testTag("login_navigate_register")) {
            Text("Don't have an account? Register", style = MaterialTheme.typography.bodySmall)
        }
    }
}
