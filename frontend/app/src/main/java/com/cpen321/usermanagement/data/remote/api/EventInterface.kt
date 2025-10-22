package com.cpen321.usermanagement.data.remote.api

import com.cpen321.usermanagement.data.remote.dto.ApiResponse
import com.cpen321.usermanagement.data.remote.dto.EventData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface EventInterface {
    @GET("events/")
    suspend fun getAllEvents(@Header("Authorization") authHeader: String): Response<ApiResponse<EventData>>
}
