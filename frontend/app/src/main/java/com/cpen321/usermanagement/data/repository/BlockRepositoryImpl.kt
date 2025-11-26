package com.cpen321.usermanagement.data.repository

import android.util.Log
import com.cpen321.usermanagement.data.remote.api.BlockInterface
import com.cpen321.usermanagement.data.remote.dto.BlockRequest
import com.cpen321.usermanagement.utils.JsonUtils.parseErrorMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockRepositoryImpl @Inject constructor(
    private val blockInterface: BlockInterface
) : BlockRepository {

    companion object {
        private const val TAG = "BlockRepositoryImpl"
    }

    override suspend fun blockUser(targetUserId: String): Result<Unit> {
        return try {
            val request = BlockRequest(targetUserId = targetUserId)
            val response = blockInterface.blockUser("", request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to block user.")
                Log.e(TAG, "Failed to block user: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while blocking user", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while blocking user", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while blocking user", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while blocking user: ${e.code()}", e)
            Result.failure(e)
        }
    }

    override suspend fun unblockUser(targetUserId: String): Result<Unit> {
        return try {
            val response = blockInterface.unblockUser("", targetUserId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to unblock user.")
                Log.e(TAG, "Failed to unblock user: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while unblocking user", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while unblocking user", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while unblocking user", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while unblocking user: ${e.code()}", e)
            Result.failure(e)
        }
    }

    override suspend fun getBlockedUsers(): Result<List<String>> {
        return try {
            val response = blockInterface.getBlockedUsers("")
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.blockedUserIds)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to fetch blocked users.")
                Log.e(TAG, "Failed to fetch blocked users: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while fetching blocked users", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while fetching blocked users", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while fetching blocked users", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while fetching blocked users: ${e.code()}", e)
            Result.failure(e)
        }
    }
}

