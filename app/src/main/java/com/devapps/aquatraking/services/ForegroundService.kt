package com.devapps.aquatraking.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.devapps.aquatraking.MainActivity


class ForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.d("ForegroundService", "Service created")
        createNotificationChannel()
        startForeground(1, createNotification("Servicio Activo", "Escuchando cambios en Firebase"))

    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE)
        // Umbral para nivel bajo (llOption1)
        val lowThreshold = prefs.getFloat("notification_threshold", 25f)
        // Umbral para nivel crítico (llOption2)
        val criticalThreshold = prefs.getFloat("notification_critical_threshold", 10f)

        val porcentaje = intent?.getFloatExtra("porcentaje", restoreState()) ?: restoreState()

        if (porcentaje != -1f) {
            saveState(porcentaje)
            if (areNotificationsEnabled()){
                when {
                    porcentaje >= 100 -> sendNotification("Tanque lleno", "El tanque está lleno al 100%.")
                    porcentaje <= lowThreshold && porcentaje > criticalThreshold ->
                        sendNotification("Nivel bajo", "El nivel de agua es bajo: $porcentaje%.")
                    porcentaje <= criticalThreshold ->
                        sendNotification("Nivel crítico", "¡Nivel crítico de agua! Solo queda el $porcentaje%.")
                }
            }
        }
        return START_STICKY
    }

    private fun areNotificationsEnabled(): Boolean {
        val sharedPreferences = getSharedPreferences("NotificationPrefs", MODE_PRIVATE)
        return sharedPreferences.getBoolean("notifications_enabled", true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "WaterLevelService",
                "Water Level Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para el servicio del nivel de agua"
                setShowBadge(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, message: String): Notification {

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "WaterLevelService")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setFullScreenIntent(pendingIntent, true)
            .build()
    }

    private fun sendNotification(title: String, message: String) {

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE)

        val notificationManager =
            getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, "WaterLevelService")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(false)
            .build()
        notificationManager?.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Este servicio no necesita comunicación directa con actividades
    }

    private fun saveState(porcentaje: Float) {
        val sharedPreferences = getSharedPreferences("ServicePrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("porcentaje", porcentaje)
        editor.apply()
    }

    private fun restoreState(): Float {
        val sharedPreferences = getSharedPreferences("ServicePrefs", MODE_PRIVATE)
        return sharedPreferences.getFloat("porcentaje", -1f)
    }
}
