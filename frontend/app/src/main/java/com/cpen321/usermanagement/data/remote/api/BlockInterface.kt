package com.cpen321.usermanagement.data.remote.api

import com.cpen321.usermanagement.data.remote.dto.ApiResponse
import com.cpen321.usermanagement.data.remote.dto.BlockRequest
import com.cpen321.usermanagement.data.remote.dto.BlockedUsersData
import com.cpen321.usermanagement.data.remote.dto.BlockStatusData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface BlockInterface {
    @POST("blocks")
    suspend fun blockUser(
        @Header("Authorization") authHeader: String,
        @Body request: BlockRequest
    ): Response<ApiResponse<Unit>>

    @DELETE("blocks/{targetUserId}")
    suspend fun unblockUser(
        @Header("Authorization") authHeader: String,
        @Path("targetUserId") targetUserId: String
    ): Response<ApiResponse<Unit>>

    @GET("blocks")
    suspend fun getBlockedUsers(
        @Header("Authorization") authHeader: String
    ): Response<ApiResponse<BlockedUsersData>>

    @GET("blocks/check/{targetUserId}")
    suspend fun checkIfBlockedBy(
        @Header("Authorization") authHeader: String,
        @Path("targetUserId") targetUserId: String
    ): Response<ApiResponse<BlockStatusData>>
}

