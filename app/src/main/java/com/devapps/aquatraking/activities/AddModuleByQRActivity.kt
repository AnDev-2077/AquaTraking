package com.devapps.aquatraking.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.devapps.aquatraking.databinding.ActivityAddModuleByQrBinding
import com.google.android.material.appbar.MaterialToolbar

class AddModuleByQRActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddModuleByQrBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddModuleByQrBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }
}