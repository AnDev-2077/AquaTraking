package com.devapps.aquatraking.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.devapps.aquatraking.MainActivity
import com.devapps.aquatraking.ProviderType
import com.devapps.aquatraking.R
import com.devapps.aquatraking.databinding.ActivityAuthBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val GOOGLE_SIGN_IN = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUp()
    }

    private fun setUp() {
        title = "Autenticación"
        binding.btGoogle.setOnClickListener {
            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleClient = GoogleSignIn.getClient(this, googleConf)
            googleClient.signOut()
            startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN)
        }
    }

    private fun showAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Se ha producido un error autenticando al usuario")
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showHome(email: String, provider: ProviderType) {
        val homeIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(homeIntent)

        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider.name)
        prefs.apply()

        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val email = account.email ?: ""
                            val name = account.displayName ?: ""
                            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                            val profileImageUrl = account.photoUrl?.toString()
                            saveUserToFirestore(email, uid, profileImageUrl, name)

                            showHome(email, ProviderType.GOOGLE)
                        } else {
                            showAlert()
                        }
                    }
                }
            } catch (e: ApiException) {
                showAlert()
                Log.e("AuthActivity", "Error al autenticar con Google", e)
            }
        }
    }

    private fun saveUserToFirestore(email: String, uid: String, profileImageUrl: String?, name: String) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val userData = mapOf(
                    "email" to email,
                    "modules" to listOf<String>(),
                    "name" to name,
                    "profileImageUrl" to (profileImageUrl ?: "")
                )

                Log.d("AuthActivity", "Guardando datos: $userData")

                userRef.set(userData)
                    .addOnSuccessListener {
                        Log.d("AuthActivity", "Usuario guardado en Firestore.")
                        // Crear la subcolección después de confirmar que el usuario existe
                    }
                    .addOnFailureListener { e ->
                        Log.e("AuthActivity", "Error al guardar usuario: ${e.message}")
                    }
            } else {
                Log.d("AuthActivity", "Usuario ya existe en Firestore.")
            }
        }.addOnFailureListener { e ->
            Log.e("AuthActivity", "Error al verificar usuario: ${e.message}")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d("AuthActivity", "Activity is being destroyed")
    }
}