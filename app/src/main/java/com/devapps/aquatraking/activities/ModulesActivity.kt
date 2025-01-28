package com.devapps.aquatraking.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.devapps.aquatraking.databinding.ActivityModulesBinding
import com.google.android.material.appbar.MaterialToolbar

class ModulesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModulesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModulesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: MaterialToolbar = binding.toolbar
        toolbar.setNavigationOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }

        binding.clAddDevice.setOnClickListener{
            val intent = Intent(this, AddModuleByCodeActivity::class.java)
            startActivity(intent)
        }

        binding.clAddDeviceByQrCode.setOnClickListener{

        }
    }
}