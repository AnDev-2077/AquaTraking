package com.devapps.aquatraking.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
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
    private val devicesList = mutableListOf<Device>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModulesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = binding.rvDevices
        recyclerView.layoutManager = LinearLayoutManager(this)
        devicesAdapter = DevicesAdapter(devicesList)
        recyclerView.adapter = devicesAdapter

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            loadUserDevices(userId)
        } else {
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
        }

        barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
            if (result.contents == null) {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Escaneado: ${result.contents}", Toast.LENGTH_LONG).show()
            }
        }

        val toolbar: MaterialToolbar = binding.toolbar
        toolbar.setNavigationOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }

        binding.clAddDevice.setOnClickListener{
            val intent = Intent(this, AddDeviceActivity::class.java)
            startActivity(intent)
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
        val userDocRef = firestore.collection("users").document(userId)
        userDocRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Se asume que "modules" es un Array o List de String
                    val modules = documentSnapshot.get("modules") as? List<String>
                    if (modules != null) {
                        devicesList.clear()
                        modules.forEach { moduleKey ->
                            devicesList.add(Device(moduleKey))
                        }
                        devicesAdapter.notifyDataSetChanged()
                        updateRecyclerViewVisibility()
                    } else {
                        Toast.makeText(this, "No se encontraron módulos.", Toast.LENGTH_SHORT).show()
                        updateRecyclerViewVisibility()
                    }
                } else {
                    Toast.makeText(this, "El documento del usuario no existe.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener los módulos: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ModulesActivity", "Error al obtener los módulos", e)
            }
    }

    private fun updateRecyclerViewVisibility() {
        binding.rvDevices.visibility = if (devicesAdapter.itemCount > 0) View.VISIBLE else View.GONE
    }
}