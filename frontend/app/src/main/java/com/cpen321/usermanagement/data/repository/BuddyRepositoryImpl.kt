package com.cpen321.usermanagement.data.repository

import android.app.Application
import android.util.Log
import com.cpen321.usermanagement.data.remote.api.BuddyInterface
import com.cpen321.usermanagement.data.remote.dto.Buddy
import com.cpen321.usermanagement.utils.GeocoderUtil
import com.cpen321.usermanagement.utils.JsonUtils.parseErrorMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuddyRepositoryImpl @Inject constructor(
    private val buddyInterface: BuddyInterface,
    private val application: Application
) : BuddyRepository {

    companion object {
        private const val TAG = "BuddyRepositoryImpl"
    }

    override suspend fun getBuddies(): Result<List<Buddy>> {
        return try {
            val response = buddyInterface.getBuddies("") // Auth header is handled by interceptor
            if (response.isSuccessful && response.body()?.data != null) {
                val buddies = response.body()!!.data!!.buddies
                
                // Perform geocoding for all buddies in parallel
                coroutineScope {
                    buddies.map { buddy ->
                        async {
                            val lat = buddy.user.lat
                            val long = buddy.user.long
                            if (lat != null && long != null) {
                                val locationInfo = GeocoderUtil.getLocationFromCoordinates(
                                    application.applicationContext,
                                    lat,
                                    long
                                )
                                buddy.user.city = locationInfo.city
                                buddy.user.province = locationInfo.province
                                buddy.user.country = locationInfo.country
                            }
                        }
                    }.awaitAll()
                }
                
                Result.success(buddies)
            } else {
                val errorBodyString = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBodyString, "Failed to fetch buddies.")
                Log.e(TAG, "Failed to get buddies: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Network timeout while getting buddies", e)
            Result.failure(e)
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Network connection failed while getting buddies", e)
            Result.failure(e)
        } catch (e: java.io.IOException) {
            Log.e(TAG, "IO error while getting buddies", e)
            Result.failure(e)
        } catch (e: retrofit2.HttpException) {
            Log.e(TAG, "HTTP error while getting buddies: ${e.code()}", e)
            Result.failure(e)
        }
    }
}

