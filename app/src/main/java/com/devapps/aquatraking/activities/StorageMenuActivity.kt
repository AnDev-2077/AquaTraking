package com.devapps.aquatraking.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.devapps.aquatraking.R
import com.devapps.aquatraking.databinding.ActivityStorageMenuBinding

class StorageMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStorageMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStorageMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }
}