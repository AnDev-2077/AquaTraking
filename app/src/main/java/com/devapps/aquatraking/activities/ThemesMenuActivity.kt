package com.devapps.aquatraking.activities

import android.os.Bundle
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.devapps.aquatraking.R
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
    }
}