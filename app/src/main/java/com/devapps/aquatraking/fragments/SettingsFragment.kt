package com.devapps.aquatraking.fragments

import com.devapps.aquatraking.activities.TeamsActivity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.devapps.aquatraking.R
import com.devapps.aquatraking.activities.AuthActivity
import com.devapps.aquatraking.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.squareup.picasso.Picasso

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()

        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            updateUI(user)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = firebaseAuth.currentUser
        updateUI(user)
        binding.btLogin.setOnClickListener {
            val intent = Intent(activity, AuthActivity::class.java)
            startActivity(intent)
        }
        binding.btLogOut.setOnClickListener {
            logoutConfirmationDialog()
        }

        binding.llOption3.setOnClickListener{
            val intent = Intent(activity, TeamsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun logoutConfirmationDialog(){
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setNegativeButton("Cancelar") { dialog, which ->

            }
            .setPositiveButton("Cerrar Sesión") { dialog, which ->
                firebaseAuth.signOut()
            }
            .show()
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        firebaseAuth.removeAuthStateListener(authStateListener)
    }

    private fun updateUI(user: FirebaseUser?) {
        user?.let {
            binding.btLogin.visibility = View.GONE
            binding.btLogOut.visibility = View.VISIBLE

            val name = user.displayName
            val photoUrl = user.photoUrl
            Picasso.get().load(photoUrl).into(binding.ivUser)
        } ?: run {
            binding.btLogin.visibility = View.VISIBLE
            binding.btLogOut.visibility = View.GONE
            binding.ivUser.setImageResource(R.drawable.ic_person_circle)
        }
    }

    companion object {
        @JvmStatic fun newInstance() =
                SettingsFragment().apply {
                }
    }
}