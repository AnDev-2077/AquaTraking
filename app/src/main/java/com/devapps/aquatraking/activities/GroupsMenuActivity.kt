package com.devapps.aquatraking.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.devapps.aquatraking.databinding.ActivityGroupsMenuBinding
import com.google.android.material.appbar.MaterialToolbar

class GroupsMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupsMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGroupsMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: MaterialToolbar = binding.toolbar
        toolbar.setNavigationOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }

        binding.llOption1.setOnClickListener{
            val intent = Intent(this, GroupsActivity::class.java)
            startActivity(intent)
        }
    }
}