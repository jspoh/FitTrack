package com.example.fittrack.data.remote.api

import com.example.fittrack.data.remote.dto.ActivityLogPayload
import com.example.fittrack.data.remote.dto.ActivityResponse
import com.example.fittrack.data.remote.dto.ActivityUpdatePayload
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ActivityApiService {
    @POST("activity/log")
    suspend fun logActivity(@Body payload: ActivityLogPayload): ActivityResponse

    @PATCH("activity/")
    suspend fun updateActivity(@Body payload: ActivityUpdatePayload): ActivityResponse
    @GET("activity/date/{day}")
    suspend fun getActivitiesForDate(@Path("day") day: String): List<ActivityResponse>

    @GET("activity/range")
    suspend fun getActivitiesInRange(
        @Query("start") start: String,
        @Query("end") end: String
    ): List<ActivityResponse>

    @DELETE("activity/{activity_id}")
    suspend fun deleteActivity(@Path("activity_id") id: Int)
}
