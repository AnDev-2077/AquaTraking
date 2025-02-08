package com.devapps.aquatraking.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.devapps.aquatraking.adapters.MembersAdapter
import com.devapps.aquatraking.databinding.ActivityTeamsBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class GroupsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeamsBinding
    private lateinit var membersAdapter: MembersAdapter

    private var deviceId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeamsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        membersAdapter = MembersAdapter(mutableListOf())
        binding.rvMembers.apply {
            layoutManager = LinearLayoutManager(this@GroupsActivity)
            adapter = membersAdapter
        }

        val toolbar: MaterialToolbar = binding.toolbar
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btAddMember.setOnClickListener {
            addMember()
        }

        binding.btVerifyKey.setOnClickListener{
            verifyKey()
        }
        binding.btCreateGroup.setOnClickListener {
            createGroup()
        }
    }

    private fun addMember() {
        val emailToSearch = binding.etAddMember.text.toString().trim()
        val user = FirebaseAuth.getInstance().currentUser

        if(user == null){
            Toast.makeText(this, "Debes iniciar sesión para agregar miembros.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!user.isEmailVerified) {
            Toast.makeText(this, "Debes verificar tu correo antes de buscar usuarios.", Toast.LENGTH_SHORT).show()
            return
        }

        if (user.email.equals(emailToSearch, ignoreCase = true)) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Error")
                .setMessage("No puedes agregarte a ti mismo al grupo.")
                .setPositiveButton("Aceptar") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
            return
        }

        if (emailToSearch.isNotEmpty()) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users")
                .whereEqualTo("email", emailToSearch)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val userDocument = querySnapshot.documents[0]
                        val userEmail = userDocument.getString("email") ?: ""
                        val profileImageUrl = userDocument.getString("profileImageUrl") ?: ""

                        if (!membersAdapter.containsMember(userEmail)) {
                            membersAdapter.addMember(userEmail, profileImageUrl)
                            updateRecyclerViewVisibility()
                            Toast.makeText(
                                this@GroupsActivity,
                                "Usuario $userEmail agregado al grupo.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@GroupsActivity,
                                "El usuario ya está en el grupo.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        Toast.makeText(
                            this@GroupsActivity,
                            "El usuario con correo $emailToSearch está registrado como $userEmail.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Usuario no encontrado
                        Toast.makeText(
                            this@GroupsActivity,
                            "No se encontró ningún usuario con ese correo.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this@GroupsActivity,
                        "Error al buscar el usuario: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("TeamsActivity", "Error al buscar el usuario: ${e.message}")
                }
        } else {
            Toast.makeText(this, "Por favor, ingresa un correo válido.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verifyKey() {

        val key = binding.editTextVerifyKey.text.toString().trim()

        if (key.isEmpty()) {
            binding.editTextVerifyKey.error = "El campo no puede estar vacío"
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        val databaseReference = FirebaseDatabase.getInstance().getReference("/ModulesWifi/")
        databaseReference.child(key).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Log.d("TeamsActivity", "Key exists in database")
                    deviceId = snapshot.key
                    Toast.makeText(this@GroupsActivity, "Key verificada. Tanque encontrado.", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("TeamsActivity", "Key does not exist in database")
                    deviceId = null
                    Toast.makeText(this@GroupsActivity, "Key no válida. No se encontró ningún dispositivo.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TeamsActivity", "Error: ${error.message}")
                Toast.makeText(this@GroupsActivity, "Error al verificar la key: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        /*val key = binding.editTextVerifyKey.text.toString().trim()
        if (key.isEmpty()) {
            Toast.makeText(this, "Ingresa una key válida", Toast.LENGTH_SHORT).show()
            return
        }
        FirebaseFirestore.getInstance().collection("tanks")
            .whereEqualTo("key", key)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(this, "Key no válida. No se encontró ningún tanque.", Toast.LENGTH_SHORT).show()
                    deviceId = null
                } else {
                    // Se toma el primer tanque encontrado
                    deviceId = querySnapshot.documents[0].id
                    Toast.makeText(this, "Key verificada. Tanque encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al verificar la key: ${e.message}", Toast.LENGTH_SHORT).show()
            }*/
    }

    private fun createGroup() {
        val groupName = binding.editTextGroupName.text.toString().trim()
        if (groupName.isEmpty()) {
            Toast.makeText(this, "Ingresa un nombre para el grupo", Toast.LENGTH_SHORT).show()
            return
        }
        if (deviceId == null) {
            Toast.makeText(this, "Primero verifica una key válida para el tanque", Toast.LENGTH_SHORT).show()
            return
        }
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }
        val currentUserId = currentUser.uid
        val currentUserEmail = currentUser.email ?: ""

        // Asegurarse de que el usuario creador esté incluido en el grupo
        if (!membersAdapter.containsMember(currentUserEmail)) {
            membersAdapter.addMember(currentUserEmail, currentUser.photoUrl?.toString() ?: "")
        }

        // Obtén la lista de miembros
        val membersList = membersAdapter.getMembers()

        // Datos del grupo
        val groupData = hashMapOf(
            "name" to groupName,
            "deviceKey" to deviceId, // Clave del tanque asociada
            "members" to membersList, // Lista de usuarios en el grupo
            "admins" to listOf(currentUserId), // El creador es el primer administrador
            "createdBy" to currentUserId, // Usuario que creó el grupo
            "createdAt" to System.currentTimeMillis()
        )

        // Guardar el grupo en la colección global "groups"
        FirebaseFirestore.getInstance().collection("groups")
            .add(groupData)
            .addOnSuccessListener { documentReference ->
                val groupId = documentReference.id
                Toast.makeText(this, "Grupo creado exitosamente", Toast.LENGTH_SHORT).show()
                Log.d("TeamsActivity", "Grupo creado con ID: $groupId")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al crear el grupo: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("TeamsActivity", "Error al crear el grupo: ${e.message}")
            }
    }


    private fun updateRecyclerViewVisibility() {
        binding.rvMembers.visibility = if (membersAdapter.itemCount > 0) View.VISIBLE else View.GONE
    }
}


