package com.devapps.aquatraking.adapters

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.devapps.aquatraking.databinding.ItemBottomSheetContentBinding
import com.devapps.aquatraking.databinding.ItemPdfBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StorageAdapter (
    val context: Context,
    private var pdfList: List<File>) :
RecyclerView.Adapter<StorageAdapter.StorageViewHolder>() {

    class StorageViewHolder(val binding: ItemPdfBinding) : RecyclerView.ViewHolder(binding.root){
        val fileName = binding.tvReportName
        val fileDate = binding.tvCreationDate
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StorageAdapter.StorageViewHolder {
        val view = ItemPdfBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StorageViewHolder(view)
    }

    override fun onBindViewHolder(holder: StorageAdapter.StorageViewHolder, position: Int) {
        val file = pdfList[position]
        holder.fileName.text = file.name

        val lastModified = file.lastModified()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastModified

        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        val formattedDay = String.format("%02d", day)

        val monthName = DateFormatSymbols(Locale("es", "ES")).shortMonths[month] // Get month name in Spanish

        val formattedDate = "Creado $formattedDay de $monthName. de $year"
        holder.fileDate.text = formattedDate

        holder.itemView.setOnClickListener {
            /*val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Abrir con"))*/
            showBottomSheet(file)
        }
    }

    override fun getItemCount(): Int = pdfList.size

    fun updateData(newList: List<File>) {
        pdfList = newList
        notifyDataSetChanged()
    }

    private fun showBottomSheet(file: File) {
        val dialog = BottomSheetDialog(context)
        val view = ItemBottomSheetContentBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(view.root)

        // Obtener referencias a las vistas dentro del Bottom Sheet
        view.llOption1.setOnClickListener{
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf" // Especifica el tipo de archivo
                putExtra(Intent.EXTRA_STREAM, uri) // Adjunta el archivo
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION // Permiso de lectura
            }
            context.startActivity(Intent.createChooser(intent, "Compartir archivo"))
            dialog.dismiss()
        }
        view.llOption2.setOnClickListener{
            showEditName(file)
            dialog.dismiss()
        }
        view.llOption3.setOnClickListener{
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showEditName(file: File) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Editar nombre")

        val input = EditText(context)
        input.setText(file.name)
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val newName = input.text.toString()
            if (newName.isNotBlank()) {
                val newFile = File(file.parentFile, newName)
                if (file.renameTo(newFile)) {
                    Toast.makeText(context, "Nombre cambiado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al cambiar el nombre", Toast.LENGTH_SHORT).show()
                }

                /**
                 * Falta actualizar el recycler view con el nuevo nombre
                 */
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }


}