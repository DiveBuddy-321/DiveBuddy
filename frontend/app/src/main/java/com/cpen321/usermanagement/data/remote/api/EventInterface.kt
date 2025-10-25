package com.cpen321.usermanagement.data.remote.api

import com.cpen321.usermanagement.data.remote.dto.ApiResponse
import com.cpen321.usermanagement.data.remote.dto.EventData
import com.cpen321.usermanagement.data.remote.dto.CreateEventRequest
import com.cpen321.usermanagement.data.remote.dto.CreateEventResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface EventInterface {
    @GET("events/")
    suspend fun getAllEvents(
        @Header("Authorization") authHeader: String
    ): Response<ApiResponse<EventData>>
    
    @POST("events/")
    suspend fun createEvent(
        @Header("Authorization") authHeader: String,
        @Body request: CreateEventRequest
    ): Response<ApiResponse<CreateEventResponse>>
}
