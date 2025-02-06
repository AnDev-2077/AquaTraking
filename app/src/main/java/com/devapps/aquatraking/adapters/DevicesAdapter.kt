package com.devapps.aquatraking.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.devapps.aquatraking.databinding.ItemModulesBinding
import com.devapps.aquatraking.objets.Device
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

class DevicesAdapter (
    val deviceList: MutableList<Device>
) : RecyclerView.Adapter<DevicesAdapter.ModuleViewHolder> (){
    class ModuleViewHolder(val binding: ItemModulesBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val binding = ItemModulesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ModuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        val module = deviceList[position]
        val key = module.key
        holder.binding.tvKeyModule.text = key
        holder.binding.root.setOnClickListener{
            unlinkConfirmationDialog(holder.binding.root.context, key)
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
        val userDocRef = firestore.collection("users").document(userId)
        val moduleDocRef = firestore.collection("modules").document(deviceKey)

        firestore.runTransaction { transaction ->
            val userSnapshot = transaction.get(userDocRef)
            val modules = userSnapshot.get("modules") as? MutableList<String> ?: mutableListOf()

            //Eliminar el módulo de la lista del usuario
            if (modules.remove(deviceKey)) {
                transaction.update(userDocRef, "modules", modules)
            }

            // Desvincular el módulo del usuario actual
            transaction.delete(moduleDocRef)
        }.addOnSuccessListener {
            Toast.makeText(context, "Módulo desvinculado correctamente.", Toast.LENGTH_SHORT).show()
            removeDeviceFromList(deviceKey)
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("DevicesAdapter", "${e.message}", e)
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