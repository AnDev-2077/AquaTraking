package com.devapps.aquatraking.activities

import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import com.devapps.aquatraking.databinding.ActivityNotificationsMenuBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NotificationsMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val singleItems = arrayOf("30%", "25%", "20%")
        val checkedItem = 1

        val _singleItems = arrayOf("20%", "15%", "10%")
        val _checkedItem = 1

        val toolbar: MaterialToolbar = binding.toolbar
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val sharedPreferences = getSharedPreferences("NotificationPrefs", MODE_PRIVATE)
        val isNotificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)
        binding.switchGeneralNotifications.isChecked = isNotificationsEnabled

        // Actualiza los TextView con los valores guardados al iniciar la app
        val savedThreshold = sharedPreferences.getFloat("notification_threshold", 25f)
        binding.tvLowLevel.text = "${savedThreshold.toInt()}%"

        val savedCriticalThreshold = sharedPreferences.getFloat("notification_critical_threshold", 10f)
        binding.tvCriticLevel.text = "${savedCriticalThreshold.toInt()}%"

        // Guarda el estado del switch cuando cambia
        binding.switchGeneralNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()
        }

        binding.llOption1.setOnClickListener {
            val prefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE)
            val savedThreshold = prefs.getFloat("notification_threshold", 25f)
            val savedThresholdString = "${savedThreshold.toInt()}%"
            var initialIndex = singleItems.indexOf(savedThresholdString)
            if (initialIndex == -1) {
                initialIndex = checkedItem
            }
            var selectedThresholdIndex = initialIndex

            MaterialAlertDialogBuilder(this)
                .setTitle("Porcentajes de notificación")
                .setSingleChoiceItems(singleItems, initialIndex) { _, which ->
                    selectedThresholdIndex = which
                }
                .setPositiveButton("Aceptar") { dialog, _ ->
                    val selectedThresholdString = singleItems[selectedThresholdIndex]
                    val selectedThreshold = selectedThresholdString.replace("%", "").toFloat()
                    prefs.edit().putFloat("notification_threshold", selectedThreshold).apply()
                    binding.tvLowLevel.text = selectedThresholdString
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        binding.llOption2.setOnClickListener {
            val prefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE)
            val savedCriticalThreshold = prefs.getFloat("notification_critical_threshold", 10f)
            val savedCriticalThresholdString = "${savedCriticalThreshold.toInt()}%"
            var initialCriticalIndex = _singleItems.indexOf(savedCriticalThresholdString)
            if (initialCriticalIndex == -1) {
                initialCriticalIndex = _checkedItem
            }
            var selectedCriticalIndex = initialCriticalIndex

            MaterialAlertDialogBuilder(this)
                .setTitle("Porcentajes de notificación")
                .setSingleChoiceItems(_singleItems, initialCriticalIndex) { _, which ->
                    selectedCriticalIndex = which
                }
                .setPositiveButton("Aceptar") { dialog, _ ->
                    val selectedCriticalThresholdString = _singleItems[selectedCriticalIndex]
                    val selectedCriticalThreshold = selectedCriticalThresholdString.replace("%", "").toFloat()
                    prefs.edit().putFloat("notification_critical_threshold", selectedCriticalThreshold).apply()
                    binding.tvCriticLevel.text = selectedCriticalThresholdString
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
}