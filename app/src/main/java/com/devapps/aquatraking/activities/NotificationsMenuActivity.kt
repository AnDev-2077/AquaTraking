package com.devapps.aquatraking.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.devapps.aquatraking.databinding.ActivityNotificationsMenuBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class NotificationsMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsMenuBinding

    private val lowItems      = arrayOf("30%", "25%", "20%")
    private val criticalItems = arrayOf("20%", "15%", "10%")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar: MaterialToolbar = binding.toolbar
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val prefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE)

        binding.switchGeneralNotifications.isChecked = prefs.getBoolean("notifications_enabled", true)
        binding.switchGeneralNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()
        }

        binding.tvLowLevel.text    = "${prefs.getFloat("notification_threshold", 25f).toInt()}%"
        binding.tvCriticLevel.text = "${prefs.getFloat("notification_critical_threshold", 10f).toInt()}%"

        binding.llOption1.setOnClickListener {
            val currentLow      = prefs.getFloat("notification_threshold", 25f)
            val currentCritical = prefs.getFloat("notification_critical_threshold", 10f)
            val currentStr      = "${currentLow.toInt()}%"
            var selectedIndex   = lowItems.indexOf(currentStr).takeIf { it != -1 } ?: 1

            MaterialAlertDialogBuilder(this)
                .setTitle("Umbral de nivel bajo")
                .setSingleChoiceItems(lowItems, selectedIndex) { _, which ->
                    selectedIndex = which
                }
                .setPositiveButton("Aceptar") { dialog, _ ->
                    val selected = lowItems[selectedIndex].replace("%", "").toFloat()
                    if (selected <= currentCritical) {
                        Snackbar.make(
                            binding.root,
                            "El umbral bajo debe ser mayor que el umbral crítico (${currentCritical.toInt()}%)",
                            Snackbar.LENGTH_LONG
                        ).show()
                    } else {
                        prefs.edit().putFloat("notification_threshold", selected).apply()
                        binding.tvLowLevel.text = lowItems[selectedIndex]
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        binding.llOption2.setOnClickListener {
            val currentLow      = prefs.getFloat("notification_threshold", 25f)
            val currentCritical = prefs.getFloat("notification_critical_threshold", 10f)
            val currentStr      = "${currentCritical.toInt()}%"
            var selectedIndex   = criticalItems.indexOf(currentStr).takeIf { it != -1 } ?: 1

            MaterialAlertDialogBuilder(this)
                .setTitle("Umbral de nivel crítico")
                .setSingleChoiceItems(criticalItems, selectedIndex) { _, which ->
                    selectedIndex = which
                }
                .setPositiveButton("Aceptar") { dialog, _ ->
                    val selected = criticalItems[selectedIndex].replace("%", "").toFloat()
                    if (selected >= currentLow) {
                        Snackbar.make(
                            binding.root,
                            "El umbral crítico debe ser menor que el umbral bajo (${currentLow.toInt()}%)",
                            Snackbar.LENGTH_LONG
                        ).show()
                    } else {
                        prefs.edit().putFloat("notification_critical_threshold", selected).apply()
                        binding.tvCriticLevel.text = criticalItems[selectedIndex]
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}
