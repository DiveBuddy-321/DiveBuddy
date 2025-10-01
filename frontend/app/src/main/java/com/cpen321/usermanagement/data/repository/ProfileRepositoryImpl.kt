package com.cpen321.usermanagement.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cpen321.usermanagement.data.local.preferences.TokenManager
import com.cpen321.usermanagement.data.remote.api.HobbyInterface
import com.cpen321.usermanagement.data.remote.api.ImageInterface
import com.cpen321.usermanagement.data.remote.api.RetrofitClient
import com.cpen321.usermanagement.data.remote.api.UserInterface
import com.cpen321.usermanagement.data.remote.dto.UpdateProfileRequest
import com.cpen321.usermanagement.data.remote.dto.User
import com.cpen321.usermanagement.utils.JsonUtils.parseErrorMessage
import com.cpen321.usermanagement.utils.MediaUtils.uriToFile
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userInterface: UserInterface,
    private val imageInterface: ImageInterface,
    private val hobbyInterface: HobbyInterface,
    private val tokenManager: TokenManager
) : ProfileRepository {

    companion object {
        private const val TAG = "ProfileRepositoryImpl"
    }

    override suspend fun getProfile(): Result<User> {
        return try {
            val response = userInterface.getProfile("") // Auth header is handled by interceptor
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.user)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage =
                    parseErrorMessage(errorBodyString, "Failed to fetch user information.")
                Log.e(TAG, "Failed to get profile: $errorMessage")
                tokenManager.clearToken()
                RetrofitClient.setAuthToken(null)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while getting profile", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while getting profile", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while getting profile", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while getting profile: ${e.code()}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(name: String, bio: String): Result<User> {
        return try {
            val updateRequest = UpdateProfileRequest(name = name, bio = bio)
            val response = userInterface.updateProfile("", updateRequest) // Auth header is handled by interceptor
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.user)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to update profile.")
                Log.e(TAG, "Failed to update profile: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while updating profile", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while updating profile", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while updating profile", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while updating profile: ${e.code()}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateUserHobbies(hobbies: List<String>): Result<User> {
        return try {
            val updateRequest = UpdateProfileRequest(hobbies = hobbies)
            val response = userInterface.updateProfile("", updateRequest) // Auth header is handled by interceptor
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.user)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to update hobbies.")

                Log.e(TAG, "Failed to update hobbies: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while updating hobbies", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while updating hobbies", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while updating hobbies", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while updating hobbies: ${e.code()}", e)
            Result.failure(e)
        }
    }

    override suspend fun uploadProfilePicture(imageUri: Uri): Result<User> {
        return try {
            // First, upload the image
            val file = uriToFile(context, imageUri)
            val requestFile = file.asRequestBody("image/*".toMediaType())
            val body = MultipartBody.Part.createFormData("media", file.name, requestFile)
            
            val uploadResponse = imageInterface.uploadPicture("", body)
            if (!uploadResponse.isSuccessful || uploadResponse.body()?.data == null) {
                val errorBodyString = uploadResponse.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to upload image.")
                Log.e(TAG, "Failed to upload image: $errorMessage")
                return Result.failure(Exception(errorMessage))
            }
            
            val uploadedImagePath = uploadResponse.body()!!.data!!.image
            
            // Then, update the user's profile picture field
            val updateRequest = UpdateProfileRequest(profilePicture = uploadedImagePath)
            val updateResponse = userInterface.updateProfile("", updateRequest)
            if (updateResponse.isSuccessful && updateResponse.body()?.data != null) {
                Result.success(updateResponse.body()!!.data!!.user)
            } else {
                val errorBodyString = updateResponse.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to update profile picture.")
                Log.e(TAG, "Failed to update profile picture: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while uploading profile picture", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while uploading profile picture", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while uploading profile picture", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while uploading profile picture: ${e.code()}", e)
            Result.failure(e)
        }
    }

    override suspend fun getAvailableHobbies(): Result<List<String>> {
        return try {
            val response = hobbyInterface.getAvailableHobbies("") // Auth header is handled by interceptor
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.hobbies)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to fetch hobbies.")
                Log.e(TAG, "Failed to get available hobbies: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while getting available hobbies", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while getting available hobbies", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while getting available hobbies", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while getting available hobbies: ${e.code()}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val response = userInterface.deleteProfile("") // Auth header is handled by interceptor
            if (response.isSuccessful) {
                // Clear token and auth state after successful deletion
                tokenManager.clearToken()
                RetrofitClient.setAuthToken(null)
                Result.success(Unit)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to delete account.")
                Log.e(TAG, "Failed to delete account: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while deleting account", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while deleting account", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while deleting account", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while deleting account: ${e.code()}", e)
            Result.failure(e)
        }
    }
}
