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

data class EventData(
    val events: List<Event>
)

data class CreateEventRequest(
    val title: String,
    val description: String,
    val date: Date,
    val capacity: Int,
    val skillLevel: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val attendees: List<String> = emptyList(),
    val photo: String? = null
)

data class CreateEventResponse(
    val event: Event
)