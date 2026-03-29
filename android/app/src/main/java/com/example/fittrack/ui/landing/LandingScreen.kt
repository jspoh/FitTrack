package com.example.fittrack.ui.landing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fittrack.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun

@Composable
fun LandingScreen(
    onNavigateToLogin: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsRun,
                contentDescription = "Running Icon",
                modifier = Modifier.size(100.dp),
                tint = TextBlack
            )
            Spacer(modifier = Modifier.height(32.dp))

            // App Title
            Text(
                text = "FitTrack",
                style = MaterialTheme.typography.displayMedium,
                color = TextBlack
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App Description
            Text(
                text = "Track your progress\nanytime, anywhere",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = TextGrey
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Primary Action
            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonBlue,
                    contentColor = TextWhite
                )
            ) {
                Text(text = "Explore", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}