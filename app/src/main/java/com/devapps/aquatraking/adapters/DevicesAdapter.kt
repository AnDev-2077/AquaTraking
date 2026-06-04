package com.devapps.aquatraking.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.devapps.aquatraking.databinding.ItemModulesBinding
import com.devapps.aquatraking.objets.Device
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class DevicesAdapter(
    val deviceList: MutableList<Device>,
    private val onCalibrate: (Device) -> Unit,
    private val onDeviceUnlinked: (String) -> Unit
) : RecyclerView.Adapter<DevicesAdapter.ModuleViewHolder>() {
    class ModuleViewHolder(val binding: ItemModulesBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val binding = ItemModulesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ModuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        val module = deviceList[position]
        val key = module.key
        holder.binding.tvKeyModule.text = key
        holder.binding.root.setOnClickListener {
            unlinkConfirmationDialog(holder.binding.root.context, key)
        }
        holder.binding.btnCalibrate.setOnClickListener {
            onCalibrate(module)
        }
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }

    private fun unlinkConfirmationDialog(context: Context, key: String) {

        val currentUser = FirebaseAuth.getInstance().currentUser

        if(currentUser == null){
            Toast.makeText(context, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid

        MaterialAlertDialogBuilder(context)
            .setTitle("Desvincular")
            .setMessage("¿Quieres desvincular este dispositivo? \nID: $key")
            .setNegativeButton("Cancelar") { dialog, which ->
                dialog.dismiss()
            }
            .setPositiveButton("Aceptar") { dialog, _ ->
                unlinkDeviceFromUser(context, userId, key)
            }
            .show()
    }

    private fun unlinkDeviceFromUser(context: Context, userId: String, deviceKey: String) {
        val firestore = FirebaseFirestore.getInstance()

        // Bloquear si el módulo está asociado a algún grupo
        firestore.collection("groups")
            .whereEqualTo("deviceKey", deviceKey)
            .get()
            .addOnSuccessListener { groupSnapshot ->
                if (!groupSnapshot.isEmpty) {
                    val groupName = groupSnapshot.documents.firstOrNull()
                        ?.getString("name") ?: "un grupo"
                    MaterialAlertDialogBuilder(context)
                        .setTitle("No se puede desvincular")
                        .setMessage("Este módulo está asociado al grupo \"$groupName\". Elimina el grupo antes de desvincular el módulo.")
                        .setPositiveButton("Entendido") { d, _ -> d.dismiss() }
                        .show()
                    return@addOnSuccessListener
                }
                proceedWithUnlink(context, userId, deviceKey, firestore)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al verificar grupos: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("DevicesAdapter", "Error al verificar grupos: ${e.message}")
            }
    }

    private fun proceedWithUnlink(context: Context, userId: String, deviceKey: String, firestore: FirebaseFirestore) {
        val userDocRef   = firestore.collection("users").document(userId)
        val moduleDocRef = firestore.collection("modules").document(deviceKey)

        userDocRef.update("modules", FieldValue.arrayRemove(deviceKey))
            .addOnSuccessListener {
                // Confirmar desde el servidor que el cambio persistió (evita falso éxito por caché local).
                userDocRef.get(com.google.firebase.firestore.Source.SERVER)
                    .addOnSuccessListener { serverDoc ->
                        val modules = serverDoc.get("modules") as? List<*> ?: emptyList<Any>()
                        if (!modules.contains(deviceKey)) {
                            removeDeviceFromList(deviceKey)
                            onDeviceUnlinked(deviceKey)
                            Toast.makeText(context, "Módulo desvinculado correctamente.", Toast.LENGTH_SHORT).show()

                            // Quitar el owner para que el módulo quede disponible para otro usuario.
                            // No se borra el documento para mantener la key como válida en Firestore.
                            moduleDocRef.update("owner", FieldValue.delete()).addOnFailureListener { e ->
                                Log.w("DevicesAdapter", "No se pudo liberar owner de modules/$deviceKey: ${e.message}")
                            }
                        } else {
                            Toast.makeText(context, "Error al desvincular: no se pudo confirmar con el servidor.", Toast.LENGTH_SHORT).show()
                            Log.e("DevicesAdapter", "arrayRemove no persistió en el servidor para $deviceKey")
                        }
                    }
                    .addOnFailureListener { e ->
                        // Sin conexión — el cambio está encolado y se aplicará al recuperar red.
                        removeDeviceFromList(deviceKey)
                        onDeviceUnlinked(deviceKey)
                        Toast.makeText(context, "Módulo desvinculado (se sincronizará al recuperar conexión).", Toast.LENGTH_SHORT).show()
                        Log.w("DevicesAdapter", "No se pudo confirmar desde servidor: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al desvincular: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("DevicesAdapter", "Error al desvincular módulo: ${e.message}", e)
            }
    }

    private fun removeDeviceFromList(moduleKey: String) {
        val position = deviceList.indexOfFirst { it.key == moduleKey }
        if (position != -1) {
            deviceList.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}