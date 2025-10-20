package com.cpen321.usermanagement.data.remote.dto

import java.util.Date

data class Event(
    val _id: String,
    val title: String,
    val description: String,
    val date: Date,
    val capacity: Int,
    val skillLevel: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val createdBy: String,
    val attendees: List<String>,
    val photo: String?,
    val createdAt: Date,
    val updatedAt: Date
)