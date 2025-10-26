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
import retrofit2.http.PUT
import retrofit2.http.Path

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
    
    @PUT("events/join/{eventId}")
    suspend fun joinEvent(
        @Header("Authorization") authHeader: String,
        @Path("eventId") eventId: String
    ): Response<ApiResponse<CreateEventResponse>>
    
    @PUT("events/leave/{eventId}")
    suspend fun leaveEvent(
        @Header("Authorization") authHeader: String,
        @Path("eventId") eventId: String
    ): Response<ApiResponse<CreateEventResponse>>
}
