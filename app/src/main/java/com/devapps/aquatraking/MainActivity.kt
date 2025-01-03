package com.devapps.aquatraking

import android.icu.text.Transliterator.Position
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.devapps.aquatraking.adapters.ViewPagerAdapter
import com.devapps.aquatraking.databinding.ActivityMainBinding
import com.devapps.aquatraking.fragments.ChartsFragment
import com.devapps.aquatraking.fragments.HomeFragment
import com.devapps.aquatraking.fragments.SettingsFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //ViewPagerAdapter
        val adapter = ViewPagerAdapter(this)
        adapter.addFragment(HomeFragment(), "Home")
        adapter.addFragment(ChartsFragment(), "Consumo")
        adapter.addFragment(SettingsFragment(), "Configuración")

        //Signing adapter to ViewPager
        binding.viewPager.adapter = adapter

        //BottomNavigationView
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> binding.viewPager.currentItem = 0
                R.id.charts -> binding.viewPager.currentItem = 1
                R.id.settings -> binding.viewPager.currentItem = 2
            }
            true
        }
        
        //Sync ViewPager with BottomNavigationView
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.bottomNavigationView.menu.getItem(position).isChecked = true
            }
        })
    }
}