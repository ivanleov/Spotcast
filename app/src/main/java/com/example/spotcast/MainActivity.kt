package com.example.spotcast

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.spotcast.data.preferences.LocaleManager
import com.example.spotcast.ui.navigation.NavGraph
import com.example.spotcast.ui.navigation.Routes
import com.example.spotcast.ui.theme.SpotcastTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
    }

    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            val localeManager = LocaleManager(newBase)
            super.attachBaseContext(localeManager.applyLocale(newBase))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val app = application as SpotCastApplication
        val startDest = if (app.tokenManager.isLoggedIn()) Routes.MAP else Routes.LOGIN

        setContent {
            SpotcastTheme {
                NavGraph(startDestination = startDest)
            }
        }
    }
}