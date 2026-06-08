package com.leo.attendanceapp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.leo.attendanceapp.screens.getCurrentMainZoneName
import com.leo.attendanceapp.screens.getCurrentZoneName
import com.leo.attendanceapp.screens.loadZonesFromFirestore
import com.leo.attendanceapp.screens.sendStudentNotification
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class LocationForegroundService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastMainZone = "Outside"
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            serviceScope.launch {
                try {
                    val uid = auth.currentUser?.uid ?: return@launch
                    val userDoc = db.collection("users").document(uid).get().await()
                    val studentName = userDoc.getString("name")
                        ?.trim()?.replaceFirstChar { it.uppercase() } ?: return@launch

                    val zones = loadZonesFromFirestore(db)
                    val studentLoc = LatLng(location.latitude, location.longitude)

                    val currentZone = getCurrentZoneName(studentLoc, zones)
                    val currentMainZone = getCurrentMainZoneName(studentLoc, zones)
                    val isInMainZone = currentMainZone != "Outside"

                    val geofenceDoc = db.collection("geofence")
                        .document("zone").get().await()
                    val startHour = geofenceDoc.getLong("startHour")?.toInt() ?: 11
                    val endHour = geofenceDoc.getLong("endHour")?.toInt() ?: 17

                    val cal = Calendar.getInstance()
                    val isTime = cal.get(Calendar.HOUR_OF_DAY) in startHour until endHour

                    // Smart Notification
                    if (isTime && currentMainZone != lastMainZone) {
                        when {
                            lastMainZone == "Outside" ->
                                sendStudentNotification(
                                    this@LocationForegroundService,
                                    "✅ Arrived!",
                                    "You entered $currentMainZone"
                                )
                            currentMainZone == "Outside" ->
                                sendStudentNotification(
                                    this@LocationForegroundService,
                                    "❌ Left Campus!",
                                    "You left $lastMainZone"
                                )
                            else ->
                                sendStudentNotification(
                                    this@LocationForegroundService,
                                    "📍 Zone Changed",
                                    "Now in $currentMainZone"
                                )
                        }
                        lastMainZone = currentMainZone
                    }

                    // Live Location Update
                    db.collection("live_locations").document(uid).set(
                        hashMapOf(
                            "name" to studentName,
                            "latitude" to location.latitude,
                            "longitude" to location.longitude,
                            "isPresent" to (isInMainZone && isTime),
                            "currentZone" to currentZone,
                            "currentMainZone" to currentMainZone,
                            "timestamp" to System.currentTimeMillis(),
                            "uid" to uid
                        )
                    )

                    // Attendance Update
                    if (isTime) {
                        db.collection("attendance").document(uid).set(
                            hashMapOf(
                                "name" to studentName,
                                "status" to if (isInMainZone) "present" else "absent",
                                "currentZone" to currentZone,
                                "currentMainZone" to currentMainZone,
                                "latitude" to location.latitude,
                                "longitude" to location.longitude,
                                "timestamp" to System.currentTimeMillis()
                            )
                        )

                        val dateKey = SimpleDateFormat(
                            "yyyy-MM-dd", Locale.getDefault()
                        ).format(Date())
                        val timeKey = SimpleDateFormat(
                            "HH:mm:ss", Locale.getDefault()
                        ).format(Date())

                        db.collection("attendance_history")
                            .document("${uid}_${dateKey}_${timeKey}")
                            .set(
                                hashMapOf(
                                    "uid" to uid,
                                    "name" to studentName,
                                    "status" to if (isInMainZone) "present" else "absent",
                                    "zone" to currentMainZone,
                                    "date" to dateKey,
                                    "time" to timeKey,
                                    "timestamp" to System.currentTimeMillis()
                                )
                            )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        startForegroundNotification()
        startLocationUpdates()
    }

    private fun startForegroundNotification() {
        val channelId = "location_service"
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("📍 Smart Attendance")
            .setContentText("GPS tracking active — Attendance being monitored")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    @Suppress("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        )
            .setMinUpdateIntervalMillis(5000L)
            .build()

        fusedClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
