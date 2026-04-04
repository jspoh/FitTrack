// core/utils/ActivityUtils.kt
package com.example.fittrack.core.utils

import java.time.LocalDateTime

fun generateActivityName(activityType: String): String {
    val hour = LocalDateTime.now().hour
    val timeOfDay = when {
        hour < 12 -> "Morning"
        hour < 17 -> "Afternoon"
        else -> "Evening"
    }
    val type = when (activityType) {
        "WALKING" -> "Walk"
        "RUNNING" -> "Run"
        "CYCLING" -> "Cycle"
        else -> "Activity"
    }
    return "$timeOfDay $type"
}