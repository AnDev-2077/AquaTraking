package com.devapps.aquatraking.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.devapps.aquatraking.adapters.StorageAdapter
import com.devapps.aquatraking.databinding.ActivityStorageBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.OutputStream
import java.util.Calendar

class StorageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStorageBinding
    private val PERMISSION_REQUEST_CODE = 1001
    private lateinit var adapter: StorageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStorageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configuración de la Toolbar
        val toolbar: MaterialToolbar = binding.toolbar
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        adapter = StorageAdapter(this, getPdfFiles())
        binding.rvFiles.apply {
            layoutManager = LinearLayoutManager(this@StorageActivity)
            adapter = this@StorageActivity.adapter
        }

        // Botón para guardar el reporte PDF
        binding.mainFab.setOnClickListener {
            if (checkStoragePermission()) {
                savePdfToReportsFolder()
                updateRecyclerView()
            } else {
                requestStoragePermission()
            }
        }
    }

    private fun updateRecyclerView() {
        adapter.updateData(getPdfFiles())
    }

    private fun getPdfFiles(): List<File> {
        val folder = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "AquaTrackingReports")
        if (!folder.exists() || !folder.isDirectory) return emptyList()

        return folder.listFiles { file -> file.extension == "pdf" }?.toList() ?: emptyList()
    }

    /**
     * Verifica si la app tiene el permiso para escribir en el almacenamiento externo.
     */
    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Solicita el permiso de escritura en almacenamiento.
     */
    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Maneja la respuesta de la solicitud de permisos.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                savePdfToReportsFolder()
            } else {
                Toast.makeText(
                    this,
                    "Permiso denegado para escribir en almacenamiento",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Guarda el reporte PDF en la subcarpeta "AquaTrackingReports" dentro de la carpeta Downloads.
     * Si la carpeta no existe, se crea automáticamente.
     */
    private fun savePdfToReportsFolder() {
        // Obtiene la carpeta pública de Descargas
        val reportsDir = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "AquaTrackingReports")
        if (!reportsDir.exists()) {
            if (!reportsDir.mkdirs()) {
                Toast.makeText(this, "No se pudo crear la carpeta de reportes", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Define el nombre del archivo PDF
        val fileName = "reporte_${Calendar.getInstance().timeInMillis}.pdf"
        val pdfFile = File(reportsDir, fileName)
        try {
            pdfFile.outputStream().use { outputStream ->
                generateReport(outputStream)
            }

            val uri = FileProvider.getUriForFile(this, "${packageName}.provider", pdfFile)

            Snackbar.make(binding.root, "Reporte gradado con éxito", Snackbar.LENGTH_LONG)
                .setAction("Ver") {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    startActivity(Intent.createChooser(intent, "Abrir con"))
                }
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
            Snackbar.make(binding.root, "Error al guardar el reporte", Snackbar.LENGTH_LONG).show()
            Log.e("StorageActivity", "Error al guardar el reporte", e)
        }
    }

    /**
     * Genera un documento PDF simple con un título.
     */
    private fun generateReport(outputStream: OutputStream) {
        val pdfDocument = PdfDocument()
        val title = TextPaint()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        title.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        title.textSize = 20f
        canvas.drawText(textTitle, 10f, 50f, title)

        pdfDocument.finishPage(page)

        try {
            pdfDocument.writeTo(outputStream)
        } finally {
            pdfDocument.close()
        }
    }

    private val textTitle = "Reporte sis generado con éxito"
}
