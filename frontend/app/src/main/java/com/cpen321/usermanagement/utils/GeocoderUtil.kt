package com.cpen321.usermanagement.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class LocationInfo(
    val city: String? = null,
    val province: String? = null,
    val country: String? = null
)

object GeocoderUtil {
    private const val TAG = "GeocoderUtil"

    suspend fun getLocationFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): LocationInfo = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) {
                Log.w(TAG, "Geocoder not available on this device")
                return@withContext LocationInfo()
            }

            val geocoder = Geocoder(context)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Use newer API for Android 13+
                suspendCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            continuation.resume(extractLocationInfo(addresses[0]))
                        } else {
                            Log.w(TAG, "No addresses found for coordinates: ($latitude, $longitude)")
                            continuation.resume(LocationInfo())
                        }
                    }
                }
            } else {
                // Use older API for older Android versions
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    extractLocationInfo(addresses[0])
                } else {
                    Log.w(TAG, "No addresses found for coordinates: ($latitude, $longitude)")
                    LocationInfo()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during geocoding", e)
            LocationInfo()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid coordinates: ($latitude, $longitude)", e)
            LocationInfo()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during geocoding", e)
            LocationInfo()
        }
    }

    private fun extractLocationInfo(address: Address): LocationInfo {
        return LocationInfo(
            city = address.locality ?: address.subAdminArea,
            province = address.adminArea,
            country = address.countryName
        )
    }

    fun formatLocation(locationInfo: LocationInfo): String {
        val parts = listOfNotNull(
            locationInfo.city,
            locationInfo.province,
            locationInfo.country
        )
        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            "Unknown"
        }
    }

    fun formatLocation(city: String?, province: String?, country: String?): String {
        val parts = listOfNotNull(city, province, country)
        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            "Unknown"
        }
    }
}

