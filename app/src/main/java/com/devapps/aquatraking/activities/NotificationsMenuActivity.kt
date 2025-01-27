package com.devapps.aquatraking.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.devapps.aquatraking.R
import com.devapps.aquatraking.databinding.ActivityGroupsMenuBinding
import com.devapps.aquatraking.databinding.ActivityNotificationsMenuBinding
import com.google.android.material.appbar.MaterialToolbar

class NotificationsMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNotificationsMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: MaterialToolbar = binding.toolbar
        toolbar.setNavigationOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }

    }
}