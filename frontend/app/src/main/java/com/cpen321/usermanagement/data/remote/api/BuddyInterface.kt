package com.cpen321.usermanagement.data.remote.api

import com.cpen321.usermanagement.data.remote.dto.ApiResponse
import com.cpen321.usermanagement.data.remote.dto.BuddiesData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface BuddyInterface {
    @GET("buddy")
    suspend fun getBuddies(@Header("Authorization") authHeader: String): Response<ApiResponse<BuddiesData>>
}

