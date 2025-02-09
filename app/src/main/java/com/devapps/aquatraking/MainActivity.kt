package com.devapps.aquatraking

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.devapps.aquatraking.adapters.ViewPagerAdapter
import com.devapps.aquatraking.databinding.ActivityMainBinding
import com.devapps.aquatraking.fragments.ChartsFragment
import com.devapps.aquatraking.fragments.HomeFragment
import com.devapps.aquatraking.fragments.SettingsFragment
import com.devapps.aquatraking.services.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


enum class ProviderType{
    GOOGLE
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ViewPagerAdapter

    private val tankViewModel: ViewModel by viewModels()

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

        loadUserKeys()
        setupKeySpinner()

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

    /*private fun loadUserKeys() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userEmail = user.email ?: return
        Log.d("MainActivity", "User email: $userEmail")

        // Obtener módulos personales
        FirebaseFirestore.getInstance().collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val personalKeys = doc.get("modules") as? List<String> ?: emptyList()
                Log.d("MainActivity", "Personal keys: $personalKeys")
                // Obtener keys de grupos
                FirebaseFirestore.getInstance().collection("groups")
                    .whereArrayContains("members", userEmail)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val groupKeys = mutableListOf<String>()
                        for (document in querySnapshot) {
                            val deviceKey = document.getString("deviceKey") ?: ""
                            if(deviceKey.isNotEmpty()){
                                groupKeys.add(deviceKey)
                                Log.d("MainActivity", "Grupo encontrado: ${document.id} - DeviceKey: $deviceKey")
                            }
                        }
                        Log.d("MainActivity", "Group keys: $groupKeys")
                        val allKeys = (personalKeys + groupKeys).distinct()
                        Log.d("MainActivity", "All keys: $allKeys")
                        tankViewModel.setAvailableKeys(allKeys)
                    }
            }
    }*/

    private fun loadUserKeys() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userEmail = user.email ?: return
        Log.d("MainActivity", "User email: $userEmail")

        // 1. Obtener módulos personales
        FirebaseFirestore.getInstance().collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val personalKeys = doc.get("modules") as? List<String> ?: emptyList()
                Log.d("MainActivity", "Personal keys: $personalKeys")

                // 2. Obtener keys de grupos
                FirebaseFirestore.getInstance().collection("groups")
                    .whereArrayContains("members", userEmail) // Buscar por email
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val groupKeys = mutableListOf<String>()
                        for (document in querySnapshot) {
                            val deviceKey = document.getString("deviceKey") ?: ""
                            if (deviceKey.isNotEmpty()) {
                                groupKeys.add(deviceKey)
                                Log.d("MainActivity", "Grupo encontrado: ${document.id} - deviceKey: $deviceKey")
                            }
                        }
                        Log.d("MainActivity", "Group keys: $groupKeys")

                        // 3. Combinar y eliminar duplicados
                        val allKeys = (personalKeys + groupKeys).distinct()
                        Log.d("MainActivity", "All keys: $allKeys")
                        tankViewModel.setAvailableKeys(allKeys)
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainActivity", "Error al cargar grupos: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error al cargar módulos: ${e.message}")
            }
    }

    private fun setupKeySpinner() {
        val spinner = findViewById<Spinner>(R.id.spinnerKeys)
        tankViewModel.availableKeys.observe(this) { keys ->
            if (keys.isNotEmpty()) {
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, keys)
                spinner.adapter = adapter
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        tankViewModel.setSelectedKey(keys[position])
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }
    }
}