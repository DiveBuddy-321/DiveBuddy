package com.cpen321.usermanagement

import android.app.Application
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp
import com.cpen321.usermanagement.BuildConfig

@HiltAndroidApp
class UserManagementApplication : Application() {
    private val GOOGLE_MAPS_API_KEY = BuildConfig.GOOGLE_MAPS_API_KEY

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Places API early
        if (!Places.isInitialized()) {
            Places.initialize(this, GOOGLE_MAPS_API_KEY)
        }
    }
}
