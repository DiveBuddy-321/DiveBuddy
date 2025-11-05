package com.cpen321.usermanagement.data.repository

import android.util.Log
import com.cpen321.usermanagement.data.remote.api.EventInterface
import com.cpen321.usermanagement.data.remote.api.RetrofitClient
import com.cpen321.usermanagement.data.remote.dto.Event
import com.cpen321.usermanagement.data.remote.dto.CreateEventRequest
import com.cpen321.usermanagement.utils.JsonUtils.parseErrorMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventInterface: EventInterface
) : EventRepository {

    companion object {
        private const val TAG = "EventRepositoryImpl"
    }

    override suspend fun getAllEvents(): Result<List<Event>> {
        return try {
            val response = eventInterface.getAllEvents("") // Auth header is handled by interceptor
            if (response.isSuccessful) {
                response.body()?.data?.events?.let { Result.success(it) }
                    ?: Result.failure(Exception("Empty response body"))
            } else {
                Result.failure(Exception("Failed to fetch events: ${response.code()}"))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while fetching events", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while fetching events", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while fetching events", e)
            Result.failure(e)
        }
    }

    override suspend fun createEvent(request: CreateEventRequest): Result<Event> {
        return try {
            val response = eventInterface.createEvent("", request) // Auth header is handled by interceptor
            if (response.isSuccessful && response.body()?.data?.event != null) {
                Result.success(response.body()!!.data!!.event)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to create event.")
                Log.e(TAG, "Failed to create event: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while creating event", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while creating event", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while creating event", e)
            Result.failure(e)
        }
    }

    override suspend fun updateEvent(eventId: String, request: CreateEventRequest): Result<Event> {
        return try {
            val response = eventInterface.updateEvent("", eventId, request) // Auth header is handled by interceptor
            if (response.isSuccessful && response.body()?.data?.event != null) {
                Result.success(response.body()!!.data!!.event)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to update event.")
                Log.e(TAG, "Failed to update event: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while updating event", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while updating event", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while updating event", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while updating event", e)
            Result.failure(e)
        }
    }

    override suspend fun joinEvent(eventId: String): Result<Event> {
        return try {
            val response = eventInterface.joinEvent("", eventId) // Auth header is handled by interceptor
            if (response.isSuccessful && response.body()?.data?.event != null) {
                Result.success(response.body()!!.data!!.event)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to join event.")
                Log.e(TAG, "Failed to join event: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while joining event", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while joining event", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while joining event", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while joining event", e)
            Result.failure(e)
        }
    }

    override suspend fun leaveEvent(eventId: String): Result<Event> {
        return try {
            val response = eventInterface.leaveEvent("", eventId) // Auth header is handled by interceptor
            if (response.isSuccessful && response.body()?.data?.event != null) {
                Result.success(response.body()!!.data!!.event)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to leave event.")
                Log.e(TAG, "Failed to leave event: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while leaving event", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while leaving event", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while leaving event", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while leaving event", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            val response = eventInterface.deleteEvent("", eventId) // Auth header is handled by interceptor
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to delete event.")
                Log.e(TAG, "Failed to delete event: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while deleting event", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while deleting event", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while deleting event", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while deleting event", e)
            Result.failure(e)
        }
    }
}
