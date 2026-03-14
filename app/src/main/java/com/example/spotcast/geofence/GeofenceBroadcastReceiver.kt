package com.example.spotcast.geofence

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.spotcast.R
import com.example.spotcast.data.local.AppDatabase
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = event.triggeringGeofences ?: return

            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(
                VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE)
            )

            val capsuleIds = triggeringGeofences.mapNotNull { it.requestId.toIntOrNull() }

            val app = try {
                context.applicationContext as com.example.spotcast.SpotCastApplication
            } catch (_: Exception) {
                null
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    capsuleIds.forEach { id ->
                        if (app?.hasShownNotification(id) == true) {
                            return@forEach
                        }

                        val capsule = db.capsuleDao().getById(id)
                        capsule?.let {
                            showNotification(context, it.id, it.textContent, it.capsuleType)
                            app?.markNotificationShown(id)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun showNotification(
        context: Context,
        capsuleId: Int,
        textContent: String?,
        capsuleType: String,
    ) {
        ensureChannel(context)

        val title = if (capsuleType == "AUDIO") {
            context.getString(R.string.capsule_audio_title)
        } else {
            context.getString(R.string.capsule_text_title)
        }

        val layerName = if (capsuleType == "AUDIO") "Audio" else "Text"
        val subtitle = context.getString(R.string.capsule_entered, layerName)
        val body = if (textContent.isNullOrBlank()) {
            context.getString(R.string.capsule_notification_body)
        } else {
            "$subtitle\n\n${textContent.take(200)}"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(capsuleId, notification)
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Capsule Alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications when you enter a capsule zone"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "capsule_alerts"
    }
}
