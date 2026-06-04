package com.devapps.aquatraking.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.devapps.aquatraking.databinding.ActivityAddModuleByCodeBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

class AddDeviceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddModuleByCodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddModuleByCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: MaterialToolbar = binding.toolbar
        toolbar.setNavigationOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnCancel.setOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnAdd.setOnClickListener{
            val key = binding.etKey.text.toString().trim()

            if (key.isEmpty()) {
                binding.etKey.error = "El campo no puede estar vacío"
                return@setOnClickListener
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseFirestore.getInstance().collection("modules").document(key)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        linkModuleToUser(userId, key)
                        Toast.makeText(this@AddDeviceActivity, "La clave existe en la base de datos.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AddDeviceActivity, "La clave no existe en la base de datos.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this@AddDeviceActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun linkModuleToUser(userId: String, moduleKey: String) {
        val firestore = FirebaseFirestore.getInstance()
        val userDocRef = firestore.collection("users").document(userId)
        val moduleDocRef = firestore.collection("modules").document(moduleKey)

        moduleDocRef.get().addOnSuccessListener { moduleSnapshot ->
            // Un documento vacío (sin campo "owner") significa que la clave es válida pero no reclamada.
            // Solo se bloquea si ya tiene dueño.
            val alreadyOwned = moduleSnapshot.exists() && moduleSnapshot.getString("owner") != null

            if (alreadyOwned) {
                Toast.makeText(this, "Este módulo ya está registrado por otro usuario.", Toast.LENGTH_SHORT).show()
            } else {
                firestore.runTransaction { transaction ->
                    val userSnapshot = transaction.get(userDocRef)
                    val moduleSnapshotInside = transaction.get(moduleDocRef)

                    val ownedInsideTransaction = moduleSnapshotInside.exists() &&
                        moduleSnapshotInside.getString("owner") != null
                    if (ownedInsideTransaction) {
                        throw FirebaseFirestoreException(
                            "El módulo ya está registrado por otro usuario.",
                            FirebaseFirestoreException.Code.ABORTED
                        )
                    }
                    val modules = userSnapshot.get("modules") as? MutableList<String> ?: mutableListOf()
                    if (modules.contains(moduleKey)) {
                        throw FirebaseFirestoreException(
                            "El módulo ya está vinculado a tu cuenta.",
                            FirebaseFirestoreException.Code.ABORTED
                        )
                    }
                    transaction.set(moduleDocRef, hashMapOf("owner" to userId))
                    modules.add(moduleKey)
                    transaction.update(userDocRef, "modules", modules)
                }.addOnSuccessListener {
                    saveModuleKeyLocally(moduleKey)
                    setResult(RESULT_OK)
                    val intent = Intent(this, CalibrationActivity::class.java)
                    intent.putExtra("moduleKey", moduleKey)
                    startActivity(intent)
                    finish()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("AddModuleByCodeActivity", "Error al vincular el módulo", e)
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error al verificar el módulo: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("AddModuleByCodeActivity", "Error al verificar el módulo", e)
        }
    }

    /*private fun saveModuleKeyLocally(moduleKey: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("moduleKey", moduleKey)
        editor.apply()
    }*/

    private fun saveModuleKeyLocally(moduleKey: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val keys = sharedPreferences.getStringSet("moduleKeys", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        keys.add(moduleKey)
        sharedPreferences.edit().putStringSet("moduleKeys", keys).apply()
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d("AuthActivity", "Activity is being destroyed")
    }

}





