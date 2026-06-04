package com.devapps.aquatraking.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.devapps.aquatraking.MainActivity

class ForegroundService : Service() {

    companion object {
        private const val CHANNEL_SERVICE = "WaterLevelService"
        private const val CHANNEL_ALERTS  = "WaterLevelAlerts"
        private const val NOTIF_ID_LOW      = 101
        private const val NOTIF_ID_CRITICAL = 102
        private const val NOTIF_ID_FULL     = 103
        private const val COOLDOWN_CRITICAL_MS = 5  * 60 * 1000L   // 5 minutos
        private const val COOLDOWN_LOW_MS      = 60 * 60 * 1000L   // 60 minutos
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("ForegroundService", "Service created")
        createNotificationChannels()
        startForeground(1, createPersistentNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE)
        val porcentaje = intent?.getFloatExtra("porcentaje", restoreState()) ?: restoreState()

        if (porcentaje != -1f) {
            saveState(porcentaje)
            if (prefs.getBoolean("notifications_enabled", true)) {
                evaluarYNotificar(porcentaje, prefs)
            }
        }
        return START_STICKY
    }

    private fun evaluarYNotificar(porcentaje: Float, prefs: SharedPreferences) {
        val lowThreshold      = prefs.getFloat("notification_threshold", 25f)
        val criticalThreshold = prefs.getFloat("notification_critical_threshold", 10f)

        val newState = when {
            porcentaje >= 100f              -> "full"
            porcentaje <= criticalThreshold -> "critical"
            porcentaje <= lowThreshold      -> "low"
            else                            -> "normal"
        }

        val prevState    = prefs.getString("prev_level_state", "normal") ?: "normal"
        val stateChanged = newState != prevState
        val now          = System.currentTimeMillis()

        when (newState) {
            "critical" -> {
                val last = prefs.getLong("last_critical_notif_time", 0L)
                if (stateChanged || (now - last) >= COOLDOWN_CRITICAL_MS) {
                    sendAlertNotification(
                        NOTIF_ID_CRITICAL,
                        "Nivel crítico",
                        "¡Nivel crítico de agua! Solo queda el ${porcentaje.toInt()}%.",
                        isUrgent = true
                    )
                    prefs.edit().putLong("last_critical_notif_time", now).apply()
                }
            }
            "low" -> {
                val last = prefs.getLong("last_low_notif_time", 0L)
                if (stateChanged || (now - last) >= COOLDOWN_LOW_MS) {
                    sendAlertNotification(
                        NOTIF_ID_LOW,
                        "Nivel bajo",
                        "El nivel de agua es bajo: ${porcentaje.toInt()}%.",
                        isUrgent = false
                    )
                    prefs.edit().putLong("last_low_notif_time", now).apply()
                }
            }
            "full" -> {
                if (stateChanged) {
                    sendAlertNotification(
                        NOTIF_ID_FULL,
                        "Tanque lleno",
                        "El tanque está lleno al 100%.",
                        isUrgent = false
                    )
                }
            }
            "normal" -> { /* sin notificación */ }
        }

        if (stateChanged) {
            prefs.edit().putString("prev_level_state", newState).apply()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Servicio de nivel de agua",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación persistente del servicio"
                setShowBadge(false)
            }
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Alertas de nivel de agua",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas de nivel bajo y crítico"
                setShowBadge(true)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(serviceChannel)
            nm?.createNotificationChannel(alertsChannel)
        }
    }

    private fun createPersistentNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("AquaTracking activo")
            .setContentText("Monitoreando el nivel de agua")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun sendAlertNotification(id: Int, title: String, message: String, isUrgent: Boolean) {
        val pendingIntent = PendingIntent.getActivity(
            this, id,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (isUrgent) {
            builder.setFullScreenIntent(pendingIntent, true)
        }

        getSystemService(NotificationManager::class.java)?.notify(id, builder.build())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun saveState(porcentaje: Float) {
        getSharedPreferences("ServicePrefs", MODE_PRIVATE)
            .edit().putFloat("porcentaje", porcentaje).apply()
    }

    private fun restoreState(): Float =
        getSharedPreferences("ServicePrefs", MODE_PRIVATE)
            .getFloat("porcentaje", -1f)
}
