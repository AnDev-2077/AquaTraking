package com.devapps.aquatraking.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.devapps.aquatraking.databinding.ActivityThemesMenuBinding
import com.google.android.material.appbar.MaterialToolbar

class ThemesMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemesMenuBinding
    private val PREFS_NAME = "theme_prefs"
    private val KEY_NIGHT_MODE = "night_mode"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemesMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: MaterialToolbar = binding.toolbar
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        binding.switchNightMode.isChecked = prefs.getBoolean(KEY_NIGHT_MODE, false)

        binding.switchNightMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_NIGHT_MODE, isChecked).apply()
            if (isChecked) enableDarkMode() else disableDarkMode()
        }
    }

    private fun enableDarkMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        delegate.applyDayNight()
    }

    private fun disableDarkMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        delegate.applyDayNight()
    }
}