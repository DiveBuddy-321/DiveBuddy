package com.cpen321.usermanagement.data.repository

import com.cpen321.usermanagement.data.remote.dto.Event
import com.cpen321.usermanagement.data.remote.dto.CreateEventRequest

interface EventRepository {
    suspend fun getAllEvents(): Result<List<Event>>
    suspend fun createEvent(request: CreateEventRequest): Result<Event>
}
