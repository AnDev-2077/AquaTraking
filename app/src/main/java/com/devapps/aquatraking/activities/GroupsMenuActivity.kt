package com.devapps.aquatraking.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.devapps.aquatraking.adapters.GroupsAdapter
import com.devapps.aquatraking.databinding.ActivityGroupsMenuBinding
import com.devapps.aquatraking.objets.Group
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class GroupsMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupsMenuBinding
    private lateinit var groupsAdapter: GroupsAdapter
    private val groupsList = mutableListOf<Group>()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupsMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: MaterialToolbar = binding.toolbar
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.llOption1.setOnClickListener {
            startActivity(Intent(this, GroupsActivity::class.java))
        }

        setupGroupsRecyclerView()
        loadUserGroups()
    }

    private fun setupGroupsRecyclerView() {
        groupsAdapter = GroupsAdapter(
            groupsList,
            onLeaveGroup  = { group -> showLeaveGroupDialog(group) },
            onAdminOptions = { group -> showAdminOptions(group) }
        )
        binding.rvGroups.layoutManager = LinearLayoutManager(this)
        binding.rvGroups.adapter = groupsAdapter
    }

    private fun loadUserGroups() {
        val user      = FirebaseAuth.getInstance().currentUser ?: return
        val userEmail = user.email ?: return
        val userId    = user.uid

        firestore.collection("groups")
            .whereArrayContains("members", userEmail)
            .get()
            .addOnSuccessListener { snapshot ->
                groupsList.clear()
                for (doc in snapshot.documents) {
                    val deviceKey = doc.getString("deviceKey") ?: continue
                    val name      = doc.getString("name") ?: "Sin nombre"
                    val admins    = doc.get("admins") as? List<*> ?: emptyList<Any>()
                    val members   = (doc.get("members") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val isAdmin   = admins.contains(userId)
                    groupsList.add(Group(id = doc.id, name = name, deviceKey = deviceKey, isAdmin = isAdmin, members = members))
                }
                updateGroupsVisibility()
            }
            .addOnFailureListener { e ->
                Log.e("GroupsMenuActivity", "Error al cargar grupos: ${e.message}")
                updateGroupsVisibility()
            }
    }

    private fun updateGroupsVisibility() {
        if (groupsList.isEmpty()) {
            binding.rvGroups.visibility    = View.GONE
            binding.tvEmptyGroups.visibility = View.VISIBLE
        } else {
            binding.rvGroups.visibility    = View.VISIBLE
            binding.tvEmptyGroups.visibility = View.GONE
            groupsAdapter.notifyDataSetChanged()
        }
    }

    // ── Miembro: salir del grupo ──────────────────────────────────────────────

    private fun showLeaveGroupDialog(group: Group) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Salir del grupo")
            .setMessage("¿Deseas salir de \"${group.name}\"?\nMódulo: ${group.deviceKey}")
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Salir") { _, _ -> leaveGroup(group) }
            .show()
    }

    private fun leaveGroup(group: Group) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return

        firestore.collection("groups").document(group.id)
            .update("members", FieldValue.arrayRemove(userEmail))
            .addOnSuccessListener {
                firestore.collection("groups").document(group.id)
                    .get(Source.SERVER)
                    .addOnSuccessListener { serverDoc ->
                        val members = serverDoc.get("members") as? List<*> ?: emptyList<Any>()
                        if (!members.contains(userEmail)) {
                            groupsAdapter.removeGroup(group.id)
                            updateGroupsVisibility()
                            Toast.makeText(this, "Has salido del grupo.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "No se pudo salir del grupo. Verifica los permisos.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        groupsAdapter.removeGroup(group.id)
                        updateGroupsVisibility()
                        Toast.makeText(this, "Has salido del grupo (se sincronizará al recuperar conexión).", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al salir del grupo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ── Admin: menú de opciones ───────────────────────────────────────────────

    private fun showAdminOptions(group: Group) {
        val options = arrayOf("Cambiar nombre", "Agregar usuario", "Eliminar usuario", "Eliminar grupo")
        MaterialAlertDialogBuilder(this)
            .setTitle(group.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameGroupDialog(group)
                    1 -> showAddMemberDialog(group)
                    2 -> showRemoveMemberDialog(group)
                    3 -> showDeleteGroupDialog(group)
                }
            }
            .show()
    }

    // ── Admin: cambiar nombre ─────────────────────────────────────────────────

    private fun showRenameGroupDialog(group: Group) {
        val layout = layoutInflater.inflate(
            com.devapps.aquatraking.R.layout.dialog_input_field, null, false
        )
        val til  = layout.findViewById<TextInputLayout>(com.devapps.aquatraking.R.id.tilDialogInput)
        val edit = layout.findViewById<TextInputEditText>(com.devapps.aquatraking.R.id.etDialogInput)
        til.hint = "Nombre del grupo"
        edit.setText(group.name)

        MaterialAlertDialogBuilder(this)
            .setTitle("Cambiar nombre")
            .setView(layout)
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .setPositiveButton("Guardar") { d, _ ->
                val newName = edit.text.toString().trim()
                if (newName.isEmpty()) {
                    til.error = "El nombre no puede estar vacío"
                    return@setPositiveButton
                }
                firestore.collection("groups").document(group.id)
                    .update("name", newName)
                    .addOnSuccessListener {
                        groupsAdapter.updateGroup(group.copy(name = newName))
                        Toast.makeText(this, "Nombre actualizado.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                d.dismiss()
            }
            .show()
    }

    // ── Admin: agregar usuario ────────────────────────────────────────────────

    private fun showAddMemberDialog(group: Group) {
        val layout = layoutInflater.inflate(
            com.devapps.aquatraking.R.layout.dialog_input_field, null, false
        )
        val til  = layout.findViewById<TextInputLayout>(com.devapps.aquatraking.R.id.tilDialogInput)
        val edit = layout.findViewById<TextInputEditText>(com.devapps.aquatraking.R.id.etDialogInput)
        til.hint = "Correo del usuario"

        MaterialAlertDialogBuilder(this)
            .setTitle("Agregar usuario")
            .setView(layout)
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .setPositiveButton("Agregar") { d, _ ->
                val email = edit.text.toString().trim().lowercase()
                if (email.isEmpty()) {
                    til.error = "Ingresa un correo"
                    return@setPositiveButton
                }
                if (group.members.contains(email)) {
                    Snackbar.make(binding.root, "El usuario ya pertenece al grupo.", Snackbar.LENGTH_SHORT).show()
                    d.dismiss()
                    return@setPositiveButton
                }
                addMemberToGroup(group, email)
                d.dismiss()
            }
            .show()
    }

    private fun addMemberToGroup(group: Group, email: String) {
        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Snackbar.make(binding.root, "No se encontró ningún usuario con ese correo.", Snackbar.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                firestore.collection("groups").document(group.id)
                    .update("members", FieldValue.arrayUnion(email))
                    .addOnSuccessListener {
                        groupsAdapter.updateGroup(group.copy(members = group.members + email))
                        Toast.makeText(this, "Usuario agregado al grupo.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Snackbar.make(binding.root, "Error al buscar usuario: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
    }

    // ── Admin: eliminar usuario ───────────────────────────────────────────────

    private fun showRemoveMemberDialog(group: Group) {
        val adminEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val removable  = group.members.filter { it != adminEmail }

        if (removable.isEmpty()) {
            Snackbar.make(binding.root, "No hay otros usuarios en el grupo.", Snackbar.LENGTH_SHORT).show()
            return
        }

        var selectedIndex = 0
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar usuario")
            .setSingleChoiceItems(removable.toTypedArray(), 0) { _, which ->
                selectedIndex = which
            }
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .setPositiveButton("Eliminar") { d, _ ->
                val email = removable[selectedIndex]
                firestore.collection("groups").document(group.id)
                    .update("members", FieldValue.arrayRemove(email))
                    .addOnSuccessListener {
                        groupsAdapter.updateGroup(group.copy(members = group.members - email))
                        Toast.makeText(this, "Usuario eliminado del grupo.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                d.dismiss()
            }
            .show()
    }

    // ── Admin: eliminar grupo ─────────────────────────────────────────────────

    private fun showDeleteGroupDialog(group: Group) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar grupo")
            .setMessage("¿Seguro que deseas eliminar el grupo \"${group.name}\"? Esta acción no se puede deshacer.")
            .setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
            .setPositiveButton("Eliminar") { _, _ ->
                firestore.collection("groups").document(group.id)
                    .delete()
                    .addOnSuccessListener {
                        groupsAdapter.removeGroup(group.id)
                        updateGroupsVisibility()
                        Toast.makeText(this, "Grupo eliminado.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Snackbar.make(binding.root, "Error: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
            }
            .show()
    }
}
