package com.example.fittrack.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.fittrack.ui.theme.CoralPink
import com.example.fittrack.ui.theme.MintGreen
import com.example.fittrack.ui.theme.SoftPeach
import com.example.fittrack.ui.theme.SoftYellow
import com.example.fittrack.ui.theme.TextDark
import com.example.fittrack.ui.theme.TextMid
import com.example.fittrack.ui.theme.TextOnPrimary

@Composable
fun StepProgressBar(
    currentSteps: Int,
    goalSteps: Int,
    modifier: Modifier = Modifier,
    onEditGoalClick: () -> Unit
) {
    val progress = if (goalSteps > 0) (currentSteps.toFloat() / goalSteps).coerceIn(0f, 1f) else 0f
    val sweepAngle = 300f * progress
    val isGoalReached = currentSteps >= goalSteps && goalSteps > 0

    val arcColors = if (isGoalReached)
        listOf(MintGreen, Color(0xFF06D6A0))
    else
        listOf(CoralPink, SoftYellow)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onEditGoalClick() }
    ) {
        Canvas(modifier = Modifier.size(220.dp)) {
            drawArc(
                color = SoftPeach,
                startAngle = 120f,
                sweepAngle = 300f,
                useCenter = false,
                style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
            )
            if (sweepAngle > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(colors = arcColors),
                    startAngle = 120f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsRun,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isGoalReached) MintGreen else CoralPink
            )
            Text(
                text = "%,d".format(currentSteps),
                style = MaterialTheme.typography.headlineLarge,
                color = TextDark,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "of %,d steps".format(goalSteps),
                style = MaterialTheme.typography.bodySmall,
                color = TextMid
            )
            if (isGoalReached) {
                Text(
                    text = "Goal reached! \uD83C\uDF89",
                    style = MaterialTheme.typography.labelSmall,
                    color = MintGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun EditGoalDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf(currentGoal.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Daily Step Goal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter your daily step target",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMid
                )
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { if (it.all { char -> char.isDigit() }) textValue = it },
                    label = { Text("Steps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(textValue.toIntOrNull() ?: 10000) },
                colors = ButtonDefaults.buttonColors(containerColor = CoralPink),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save", color = TextOnPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMid)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
