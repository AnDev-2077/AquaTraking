package com.devapps.aquatraking

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class AquaTrackingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        val isNightMode = prefs.getBoolean("night_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
