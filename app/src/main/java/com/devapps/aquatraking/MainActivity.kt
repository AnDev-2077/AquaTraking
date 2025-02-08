package com.devapps.aquatraking

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
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
                // Invalidate the menu when "Inicio" is selected
                invalidateOptionsMenu()
            }
        })

        //Set initial title
        supportActionBar?.title = adapter.getPageTitle(0)


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val yourDevices = menu?.findItem(R.id.yourDevices)
        val addDevices = menu?.findItem(R.id.addDevices)
        yourDevices?.isVisible = binding.viewPager.currentItem == 0
        addDevices?.isVisible = binding.viewPager.currentItem == 0
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.addDevices -> {
                val view = findViewById<View>(R.id.addDevices) // El ID del icono
                showPopupMenu(view)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showPopupMenu(anchor: View) {
        val popupMenu = PopupMenu(this, anchor)
        popupMenu.menuInflater.inflate(R.menu.menu_add_devices, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.addDevice -> Toast.makeText(this, "Añadir dispositivo", Toast.LENGTH_SHORT).show()
                R.id.scanDevice -> Toast.makeText(this, "Nueva difusión", Toast.LENGTH_SHORT).show()
            }
            true
        }
        popupMenu.show()
    }
}