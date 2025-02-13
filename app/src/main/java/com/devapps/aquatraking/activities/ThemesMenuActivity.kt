package com.devapps.aquatraking.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.devapps.aquatraking.databinding.ActivityThemesMenuBinding
import com.google.android.material.appbar.MaterialToolbar

class ThemesMenuActivity : AppCompatActivity() {

    lateinit var binding: ActivityThemesMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemesMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: MaterialToolbar = binding.toolbar
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.switchNightMode.setOnCheckedChangeListener{_, isSelected ->
            if (isSelected){
                enableDarkMode()
            } else {
                disableDarkMode()
            }
        }
    }

    private fun enableDarkMode(){
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        delegate.applyDayNight()
    }

    private fun disableDarkMode(){
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        delegate.applyDayNight()
    }
}