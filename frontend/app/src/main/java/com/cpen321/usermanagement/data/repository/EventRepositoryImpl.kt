package com.cpen321.usermanagement.data.repository

import com.cpen321.usermanagement.data.remote.api.EventInterface
import com.cpen321.usermanagement.data.remote.dto.Event
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventInterface: EventInterface
) : EventRepository {

    override suspend fun getAllEvents(): Result<List<Event>> {
        return try {
            val response = eventInterface.getAllEvents("") // Auth header is handled by interceptor
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.events)
            } else {
                Result.failure(Exception("Failed to fetch events: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
