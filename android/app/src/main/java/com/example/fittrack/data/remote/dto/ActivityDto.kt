package com.example.fittrack.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ActivityLogPayload(
    @SerializedName("activity_name") val activityName: String,
    val start: String,
    val end: String,
    @SerializedName("activity_type") val activityType: String,
    @SerializedName("steps_taken") val stepsTaken: Int,
    @SerializedName("max_hr") val maxHr: Int,
    val notes: String
)

data class ActivityUpdatePayload(
    val id: Int,
    @SerializedName("activity_name") val activityName: String? = null,
    @SerializedName("activity_type") val activityType: String? = null,
    val start: String? = null,
    val end: String? = null,
    @SerializedName("steps_taken") val stepsTaken: Int? = null,
    @SerializedName("max_hr") val maxHr: Int? = null,
    val notes: String? = null
)

data class ActivityResponse(
    val id: Int,
    @SerializedName("activity_name") val activityName: String? = null,
    val start: String,
    val end: String,
    @SerializedName("activity_type") val activityType: String,
    @SerializedName("steps_taken") val stepsTaken: Int,
    @SerializedName("max_hr") val maxHr: Int,
    val notes: String
)
