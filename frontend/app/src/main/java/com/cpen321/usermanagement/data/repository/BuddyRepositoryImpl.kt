package com.cpen321.usermanagement.data.repository

import android.util.Log
import com.cpen321.usermanagement.data.remote.api.BuddyInterface
import com.cpen321.usermanagement.data.remote.dto.Buddy
import com.cpen321.usermanagement.utils.JsonUtils.parseErrorMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuddyRepositoryImpl @Inject constructor(
    private val buddyInterface: BuddyInterface
) : BuddyRepository {

    companion object {
        private const val TAG = "BuddyRepositoryImpl"
    }

    override suspend fun getBuddies(
        targetMinLevel: Int?,
        targetMaxLevel: Int?,
        targetMinAge: Int?,
        targetMaxAge: Int?
    ): Result<List<Buddy>> {
        return try {
            val response = buddyInterface.getBuddies(
                authHeader = "",
                targetMinLevel = targetMinLevel,
                targetMaxLevel = targetMaxLevel,
                targetMinAge = targetMinAge,
                targetMaxAge = targetMaxAge
            ) // Auth header is handled by interceptor
            if (response.isSuccessful && response.body()?.data != null) {
                val result = response.body()!!.data!!.buddies
                Result.success(result)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to fetch buddies.")
                Log.e(TAG, "Failed to get buddies: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while getting buddies", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while getting buddies", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while getting buddies", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while getting buddies: ${e.code()}", e)
            Result.failure(e)
        }
    }
}

