package com.devapps.aquatraking.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.devapps.aquatraking.adapters.MembersAdapter
import com.devapps.aquatraking.databinding.ActivityTeamsBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.firestore.FirebaseFirestore

class TeamsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeamsBinding
    private lateinit var membersAdapter: MembersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeamsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        membersAdapter = MembersAdapter(mutableListOf())
        binding.rvMembers.apply {
            layoutManager = LinearLayoutManager(this@TeamsActivity)
            adapter = membersAdapter
        }

        val toolbar: MaterialToolbar = binding.toolbar
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btAddMember.setOnClickListener {
            addMember()
        }
    }

    private fun addMember() {
        val emailToSearch = binding.etAddMember.text.toString().trim()
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
                                this@TeamsActivity,
                                "Usuario $userEmail agregado al grupo.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@TeamsActivity,
                                "El usuario ya está en el grupo.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        Toast.makeText(
                            this@TeamsActivity,
                            "El usuario con correo $emailToSearch está registrado como $userEmail.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Usuario no encontrado
                        Toast.makeText(
                            this@TeamsActivity,
                            "No se encontró ningún usuario con ese correo.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this@TeamsActivity,
                        "Error al buscar el usuario: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Toast.makeText(this, "Por favor, ingresa un correo válido.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateRecyclerViewVisibility() {
        binding.rvMembers.visibility = if (membersAdapter.itemCount > 0) View.VISIBLE else View.GONE
    }
}
