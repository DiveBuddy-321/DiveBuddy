package com.cpen321.usermanagement

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cpen321.usermanagement.ui.navigation.AppNavigation
import com.cpen321.usermanagement.ui.theme.ProvideFontSizes
import com.cpen321.usermanagement.ui.theme.ProvideSpacing
import com.cpen321.usermanagement.ui.theme.UserManagementTheme
import dagger.hilt.android.AndroidEntryPoint
import android.util.Log
import com.google.android.libraries.places.api.Places

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mapsKey = BuildConfig.MAPS_API_KEY
        if (mapsKey.isBlank()) {
            Log.e("MainActivity", "MAPS_API_KEY is missing; skipping Places initialization")
        } else if (!Places.isInitialized()) {
            Places.initialize(applicationContext, mapsKey)
        }

        setContent {
            UserManagementTheme {
                UserManagementApp()
            }
        }
    }
}

@Composable
fun UserManagementApp() {
    ProvideSpacing {
        ProvideFontSizes {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AppNavigation()
            }
        }
    }
}
