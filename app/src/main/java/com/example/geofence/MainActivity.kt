package com.example.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.location.LocationManagerCompat.getCurrentLocation
import com.example.geofence.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.google.android.gms.location.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private var geoClient: GeofencingClient? = null
    private val geofenceList =ArrayList<Geofence>()
    private var latitude = 29.854021
    private var longitude =  78.130589
    private var radius = 10.0f // in meters
    private var locationInterval = 500L //millis

    private lateinit var longitudeView: EditText
    private lateinit var latitudeView: EditText
    private lateinit var radiusView: EditText
    private lateinit var locationCallback: LocationCallback

    private val geofenceIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun locationUpdateCallback() {
        println("location update callback")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermissions()
        latitudeView = findViewById(R.id.latitude)
        longitudeView = findViewById(R.id.longitude)
        radiusView = findViewById(R.id.radius)

        listeners()

        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.create()
        locationRequest.setInterval(this.locationInterval)
        this.locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations){
                    Log.d("locationCallback", "$location")
                }
            }
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, this.locationCallback, Looper.getMainLooper())

    }

    private fun requestPermissions() {
        if(Utils.hasLocationPermissions(this)) {
            askForBaseLocation()
            return
        }
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept location permissions to use this app.",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept location permissions to use this app.",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermissions()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        askForBaseLocation()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun askForBaseLocation() {

        createChannel(this)
        getUserCurrentLocation()

        geoClient = LocationServices.getGeofencingClient(this)

    }

    @SuppressLint("MissingPermission")
    private fun getUserCurrentLocation() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val locationResult = fusedLocationProviderClient.lastLocation
        locationResult.addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val lastKnownLocation = task.result
                if (lastKnownLocation != null) {
                    latitude = lastKnownLocation.latitude
                    longitude = lastKnownLocation.longitude
                    latitudeView.setText(latitude.toString())
                    longitudeView.setText(longitude.toString())

                    addGeoFence()
                }
                Log.d("loc", "location ${lastKnownLocation.latitude} ${lastKnownLocation.longitude}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeoFence(){
        removeGeofences()
        geofenceList.add(Geofence.Builder()
            .setRequestId("entry.key")
            .setCircularRegion(getLatitude(),getLongitude(),getRadius())
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_ENTER)
            .build())

        geoClient?.addGeofences(getGeofencingRequest(), geofenceIntent)?.run {
            addOnSuccessListener {
                Toast.makeText(this@MainActivity, "Geofence Added", Toast.LENGTH_LONG).show()
                Log.d("location", "addGeoFence: success")
            }
            addOnFailureListener {
                Toast.makeText(this@MainActivity, "Geofence Add Failed", Toast.LENGTH_LONG).show()
                Log.d("location", "addGeoFence: $it")
            }
        }

    }

    private fun getLatitude(): Double {
        return  latitudeView.text.toString().toDouble()
    }

    private fun getLongitude(): Double {
        return  longitudeView.text.toString().toDouble()
    }

    private fun getRadius(): Float {
        return radiusView.text.toString().toFloat()
    }


    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT or GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()
    }

    private fun removeGeofences(){
        geoClient?.removeGeofences(geofenceIntent)?.run {
            addOnSuccessListener {
                Toast.makeText(this@MainActivity, "Geofence Removed", Toast.LENGTH_LONG).show()
                Log.d("location", "removeGeoFence: success")
            }
            addOnFailureListener {
                Toast.makeText(this@MainActivity, "Geofence Remove Failed", Toast.LENGTH_LONG).show()
                Log.d("location", "removeGeoFence: $it")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeGeofences()
    }

    fun listeners(){
        findViewById<Button>(R.id.useCurrentLocation).setOnClickListener {
            getUserCurrentLocation()
        }

        findViewById<Button>(R.id.update).setOnClickListener {
            addGeoFence()
        }

    }

}