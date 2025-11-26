package com.cpen321.usermanagement.data.remote.dto

data class UpdateProfileRequest(
    val name: String? = null,          // keep if backend still accepts it here
    val bio: String? = null,
    val age: Int? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val skillLevel: String? = null,
    val profilePicture: String? = null
)


data class ProfileData(
    val user: User
)

data class User(
    val _id: String,
    val email: String,
    val name: String,
    val bio: String?,
    val profilePicture: String?,
    val age: Int? = null,
    val skillLevel: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val eventsJoined: List<String>? = null,
    val eventsCreated: List<String>? = null,
)


data class UploadImageData(
    val image: String
)