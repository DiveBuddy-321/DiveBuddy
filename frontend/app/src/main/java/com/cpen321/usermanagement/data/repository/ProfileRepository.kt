package com.cpen321.usermanagement.data.repository

import android.net.Uri
import com.cpen321.usermanagement.data.remote.dto.User

data class ProfileUpdateParams(
    val name: String? = null,
    val bio: String? = null,
    val age: Int? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val skillLevel: String? = null,
    val profilePicture: String? = null
)

interface ProfileRepository {
    suspend fun getProfile(): Result<User>
    suspend fun getProfileById(userId: String): Result<User>
    suspend fun updateProfile(params: ProfileUpdateParams): Result<User>
    suspend fun uploadProfilePicture(imageUri: Uri): Result<User>
    suspend fun deleteAccount(): Result<Unit>
}