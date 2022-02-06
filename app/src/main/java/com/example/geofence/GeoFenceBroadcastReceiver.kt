package com.example.geofence

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings.Global.getString
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    val TAG = "Geofence"

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, errorMessage)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            val notificationManager = ContextCompat.getSystemService(
                context!!,
                NotificationManager::class.java
            ) as NotificationManager

            notificationManager.sendGeofenceExitedNotification(context)
            Log.d(TAG, "transition : $geofenceTransition")

        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            val notificationManager = ContextCompat.getSystemService(
                context!!,
                NotificationManager::class.java
            ) as NotificationManager

            notificationManager.sendGeofenceEnteredNotification(context)
            Log.d(TAG, "transition : $geofenceTransition")
        }
        else {
            // Log the error.
            Log.e(TAG, "Invalid type transition $geofenceTransition")
        }
    }
}