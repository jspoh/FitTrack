package com.example.fittrack.domain.model

data class Activity(
    val id: Int,
    val activityName: String,
    val start: String,
    val end: String,
    val activityType: String,
    val stepsTaken: Int,
    val maxHr: Int,
    val notes: String
)

fun String.toActivityDisplayName(): String = when (this) {
    "WALKING" -> "Walking"
    "RUNNING" -> "Running"
    "CYCLING" -> "Cycling"
    "GENERAL" -> "General Activity"
    else -> "General Activity"
}