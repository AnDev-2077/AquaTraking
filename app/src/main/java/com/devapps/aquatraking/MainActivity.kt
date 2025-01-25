package com.devapps.aquatraking

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.devapps.aquatraking.adapters.ViewPagerAdapter
import com.devapps.aquatraking.databinding.ActivityMainBinding
import com.devapps.aquatraking.fragments.ChartsFragment
import com.devapps.aquatraking.fragments.HomeFragment
import com.devapps.aquatraking.fragments.SettingsFragment

enum class ProviderType{
    GOOGLE
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Set the toolbar as the action bar
        setSupportActionBar(binding.toolbar)

        //ViewPagerAdapter
        adapter = ViewPagerAdapter(this)
        adapter.addFragment(HomeFragment(), "Inicio")
        adapter.addFragment(ChartsFragment(), "Consumo")
        adapter.addFragment(SettingsFragment(), "Configuración")

        //Signing adapter to ViewPager
        binding.viewPager.adapter = adapter

        //BottomNavigationView
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> binding.viewPager.setCurrentItem(0, false)
                R.id.charts -> binding.viewPager.setCurrentItem(1, false)
                R.id.settings -> binding.viewPager.setCurrentItem(2, false)
            }
            true
        }
        
        //Sync ViewPager with BottomNavigationView
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                //Update AppBar title
                supportActionBar?.title = adapter.getPageTitle(position)
                //Update BottomNavigationView selection
                binding.bottomNavigationView.menu.getItem(position).isChecked = true
            }
        })

        //Set initial title
        supportActionBar?.title = adapter.getPageTitle(0)


    }
}