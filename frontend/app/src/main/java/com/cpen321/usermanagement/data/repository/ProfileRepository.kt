package com.cpen321.usermanagement.data.repository

import android.net.Uri
import com.cpen321.usermanagement.data.remote.dto.User

interface ProfileRepository {
    suspend fun getProfile(): Result<User>
    suspend fun updateProfile(name: String, bio: String): Result<User>
    suspend fun updateProfileFull(
        name: String? = null,
        bio: String? = null,
        age: Int? = null,
        location: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        skillLevel: String? = null,
        profilePicture: String? = null
    ): Result<User>
    suspend fun uploadProfilePicture(imageUri: Uri): Result<User>
    suspend fun deleteAccount(): Result<Unit>
}