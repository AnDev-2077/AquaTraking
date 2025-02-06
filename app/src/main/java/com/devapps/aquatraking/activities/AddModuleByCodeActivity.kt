package com.devapps.aquatraking.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.devapps.aquatraking.databinding.ActivityAddModuleByCodeBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

class AddModuleByCodeActivity : AppCompatActivity() {

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

            val databaseReference = FirebaseDatabase.getInstance().getReference("/ModulesWifi/")
            databaseReference.child(key).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()){
                        linkModuleToUser(userId,key)
                        Toast.makeText(this@AddModuleByCodeActivity, "La clave existe en la base de datos.", Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(this@AddModuleByCodeActivity, "La clave no existe en la base de datos.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AddModuleByCodeActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }

            })
        }
    }

    /*private fun linkModuleToUser(userId: String, moduleKey: String) {
        val firestore = FirebaseFirestore.getInstance()
        val userDocRef = firestore.collection("users").document(userId)

        userDocRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {

                    val userData = documentSnapshot.data
                    val modules = userData?.get("modules") as? MutableList<String> ?: mutableListOf()

                    if (modules.contains(moduleKey)) {
                        Toast.makeText(this, "El módulo ya está vinculado a tu cuenta.", Toast.LENGTH_SHORT).show()
                    } else {

                        modules.add(moduleKey)

                        userDocRef.update("modules", modules)
                            .addOnSuccessListener {
                                val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
                                sharedPreferences.edit().putString("moduleKey", moduleKey).apply()
                                Toast.makeText(this, "Módulo vinculado correctamente.", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error al vincular el módulo: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {

                    val userData = mapOf("modules" to listOf(moduleKey))
                    userDocRef.set(userData)
                        .addOnSuccessListener {
                            val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
                            sharedPreferences.edit().putString("moduleKey", moduleKey).apply()
                            Toast.makeText(this, "Módulo vinculado correctamente.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al crear el usuario: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener el usuario: ${it.message}", Toast.LENGTH_SHORT).show()
                Log.e("AddModuleByCodeActivity", "Error al obtener el usuario", it)
            }
    }*/

    private fun linkModuleToUser(userId: String, moduleKey: String) {
        val firestore = FirebaseFirestore.getInstance()
        val userDocRef = firestore.collection("users").document(userId)
        val moduleDocRef = firestore.collection("modules").document(moduleKey)

        // Verificar si el módulo ya existe en la colección "modules" fuera de la transacción
        moduleDocRef.get().addOnSuccessListener { moduleSnapshot ->
            if (moduleSnapshot.exists()) {
                // El módulo ya está registrado por otro usuario
                Toast.makeText(this, "Este módulo ya está registrado por otro usuario.", Toast.LENGTH_SHORT).show()
            } else {
                // Ejecutar la transacción
                firestore.runTransaction { transaction ->
                    // 1. Realizar todas las lecturas primero
                    val userSnapshot = transaction.get(userDocRef)
                    val moduleSnapshotInside = transaction.get(moduleDocRef)
                    // Aunque ya se verificó fuera de la transacción, se vuelve a leer para garantizar consistencia
                    if (moduleSnapshotInside.exists()) {
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

                    // 2. Una vez terminadas las lecturas, realizar las escrituras
                    transaction.set(moduleDocRef, hashMapOf("owner" to userId))
                    modules.add(moduleKey)
                    transaction.update(userDocRef, "modules", modules)
                }.addOnSuccessListener {
                    // Guardar el módulo localmente (opcional)
                    saveModuleKeyLocally(moduleKey)
                    Toast.makeText(this, "Módulo vinculado correctamente.", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { e ->
                    // Manejar errores
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("AddModuleByCodeActivity", "Error al vincular el módulo", e)
                }
            }
        }.addOnFailureListener { e ->
            // Manejar errores al verificar el módulo
            Toast.makeText(this, "Error al verificar el módulo: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("AddModuleByCodeActivity", "Error al verificar el módulo", e)
        }
    }


    private fun saveModuleKeyLocally(moduleKey: String) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("moduleKey", moduleKey)
        editor.apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AuthActivity", "Activity is being destroyed")
    }

}





