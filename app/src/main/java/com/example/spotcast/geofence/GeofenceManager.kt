package com.example.spotcast.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.spotcast.data.local.entity.CapsuleEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {

    private val client: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    @SuppressLint("MissingPermission")
    fun registerGeofences(capsules: List<CapsuleEntity>) {
        client.removeGeofences(pendingIntent)

        if (capsules.isEmpty()) return

        val geofences = capsules.map { capsule ->
            Geofence.Builder()
                .setRequestId(capsule.id.toString())
                .setCircularRegion(
                    capsule.latitude,
                    capsule.longitude,
                    capsule.radius.toFloat(),
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        client.addGeofences(request, pendingIntent)
    }

    fun removeAll() {
        client.removeGeofences(pendingIntent)
    }


    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }
}
