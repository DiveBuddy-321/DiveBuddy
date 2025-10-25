package com.cpen321.usermanagement.data.remote.api

import com.cpen321.usermanagement.data.remote.dto.ApiResponse
import com.cpen321.usermanagement.data.remote.dto.BuddiesData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface BuddyInterface {
    @GET("buddy")
    suspend fun getBuddies(
        @Header("Authorization") authHeader: String,
        @Query("targetMinLevel") targetMinLevel: Int? = null,
        @Query("targetMaxLevel") targetMaxLevel: Int? = null,
        @Query("targetMinAge") targetMinAge: Int? = null,
        @Query("targetMaxAge") targetMaxAge: Int? = null
    ): Response<ApiResponse<BuddiesData>>
}

