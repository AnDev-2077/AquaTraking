package com.devapps.aquatraking.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devapps.aquatraking.adapters.DevicesAdapter
import com.devapps.aquatraking.databinding.ActivityModulesBinding
import com.devapps.aquatraking.objets.Device
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class DevicesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var devicesAdapter: DevicesAdapter
    private lateinit var binding: ActivityModulesBinding
    private lateinit var barcodeLauncher: ActivityResultLauncher<ScanOptions>
    private lateinit var addDeviceLauncher: ActivityResultLauncher<Intent>
    private val devicesList = mutableListOf<Device>()

    private val PREFS_DEVICES = "devices_cache"
    private val KEY_MODULE_KEYS = "module_keys"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModulesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = binding.rvDevices
        recyclerView.layoutManager = LinearLayoutManager(this)
        devicesAdapter = DevicesAdapter(
            devicesList,
            onCalibrate = { device ->
                val intent = Intent(this, CalibrationActivity::class.java)
                intent.putExtra("moduleKey", device.key)
                startActivity(intent)
            },
            onDeviceUnlinked = { key ->
                val prefs = getSharedPreferences(PREFS_DEVICES, MODE_PRIVATE)
                val cached = prefs.getStringSet(KEY_MODULE_KEYS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                cached.remove(key)
                prefs.edit().putStringSet(KEY_MODULE_KEYS, cached).apply()
                updateRecyclerViewVisibility()
            }
        )
        recyclerView.adapter = devicesAdapter

        addDeviceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@registerForActivityResult
                loadUserDevices(userId)
            }
        }

        barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents == null) {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Escaneado: ${result.contents}", Toast.LENGTH_LONG).show()
            }
        }

        showCachedDevices()

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            loadUserDevices(userId)
        } else {
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
        }

        val toolbar: MaterialToolbar = binding.toolbar
        toolbar.setNavigationOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }

        binding.clAddDevice.setOnClickListener {
            addDeviceLauncher.launch(Intent(this, AddDeviceActivity::class.java))
        }

        binding.clAddDeviceByQrCode.setOnClickListener{ initScanner() }
    }

    private fun initScanner() {
        val scanOptions = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Escanea un código QR")
            setCameraId(0)
            setBeepEnabled(false)
            setBarcodeImageEnabled(true)
        }
        barcodeLauncher.launch(scanOptions)
    }

    private fun loadUserDevices(userId: String) {
        val firestore = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userEmail = user.email ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val personalKeys = doc.get("modules") as? List<String> ?: emptyList()

                firestore.collection("groups")
                    .whereArrayContains("members", userEmail)
                    .get()
                    .addOnSuccessListener { groupSnapshot ->
                        val groupKeys = groupSnapshot.documents
                            .mapNotNull { it.getString("deviceKey") }
                            .filter { it.isNotEmpty() }

                        val allKeys = (personalKeys + groupKeys).distinct()
                        devicesList.clear()
                        allKeys.forEach { devicesList.add(Device(it)) }
                        devicesAdapter.notifyDataSetChanged()
                        updateRecyclerViewVisibility()
                        saveCachedDevices(allKeys)
                    }
                    .addOnFailureListener { e ->
                        devicesList.clear()
                        personalKeys.forEach { devicesList.add(Device(it)) }
                        devicesAdapter.notifyDataSetChanged()
                        updateRecyclerViewVisibility()
                        saveCachedDevices(personalKeys)
                        Log.e("DevicesActivity", "Error al cargar grupos: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener módulos: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("DevicesActivity", "Error al obtener los módulos: ${e.message}")
            }
    }

    private fun updateRecyclerViewVisibility() {
        binding.rvDevices.visibility = if (devicesAdapter.itemCount > 0) View.VISIBLE else View.GONE
    }

    private fun showCachedDevices() {
        val keys = getSharedPreferences(PREFS_DEVICES, MODE_PRIVATE)
            .getStringSet(KEY_MODULE_KEYS, emptySet()) ?: emptySet()
        if (keys.isNotEmpty()) {
            devicesList.clear()
            keys.sorted().forEach { devicesList.add(Device(it)) }
            devicesAdapter.notifyDataSetChanged()
            updateRecyclerViewVisibility()
        }
    }

    private fun saveCachedDevices(keys: List<String>) {
        getSharedPreferences(PREFS_DEVICES, MODE_PRIVATE)
            .edit().putStringSet(KEY_MODULE_KEYS, keys.toSet()).apply()
    }
}