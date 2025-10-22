package com.cpen321.usermanagement.data.repository

import com.cpen321.usermanagement.data.remote.dto.Event

interface EventRepository {
    suspend fun getAllEvents(): Result<List<Event>>
}
