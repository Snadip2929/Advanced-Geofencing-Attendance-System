package com.leo.attendanceapp

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
import com.leo.attendanceapp.screens.*
import java.text.SimpleDateFormat
import java.util.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var lastMainZone = "Outside"

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startForegroundService()
        startLocationUpdates()
    }

    private fun startForegroundService() {
        val channelId = "location_service_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Attendance App")
            .setContentText("📍 Tracking your location...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        startForeground(1001, notification)
    }

    @Suppress("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L
        ).setMinUpdateIntervalMillis(3000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                updateLocation(location.latitude, location.longitude)
            }
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }

    private fun updateLocation(lat: Double, lng: Double) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val studentName = userDoc.getString("name")
                    ?.trim()?.replaceFirstChar { it.uppercase() } ?: return@addOnSuccessListener

                db.collection("zones").get().addOnSuccessListener { zonesSnapshot ->
                    val zones = zonesSnapshot.documents.mapNotNull { doc ->
                        val zLat = doc.getDouble("latitude") ?: return@mapNotNull null
                        val zLng = doc.getDouble("longitude") ?: return@mapNotNull null
                        val radius = doc.getDouble("radius") ?: 100.0
                        val name = doc.getString("name") ?: doc.id
                        val type = doc.getString("type") ?: "circle"
                        val isMainZone = doc.getBoolean("isMainZone") ?: false
                        val polygon = if (type != "circle") {
                            val points = doc.get("polygon") as? List<*>
                            points?.mapNotNull { point ->
                                val p = point as? Map<*, *>
                                val pLat = p?.get("lat") as? Double
                                val pLng = p?.get("lng") as? Double
                                if (pLat != null && pLng != null) LatLng(pLat, pLng) else null
                            } ?: emptyList()
                        } else emptyList()
                        AttendanceZone(doc.id, name, LatLng(zLat, zLng),
                            radius, type, polygon, isMainZone)
                    }

                    val studentLoc = LatLng(lat, lng)
                    val currentZone = getCurrentZoneName(studentLoc, zones)
                    val currentMainZone = getCurrentMainZoneName(studentLoc, zones)
                    val isInMainZone = currentMainZone != "Outside"

                    db.collection("geofence").document("zone").get()
                        .addOnSuccessListener { geofenceDoc ->
                            val startHour = (geofenceDoc.getLong("startHour") ?: 11).toInt()
                            val endHour = (geofenceDoc.getLong("endHour") ?: 17).toInt()
                            val isTime = isWithinAttendanceTimeStudent(startHour, endHour)

                            // Smart notification - only main zone change
                            if (isTime && currentMainZone != lastMainZone) {
                                when {
                                    lastMainZone == "Outside" && isInMainZone ->
                                        sendStudentNotification(this@LocationService,
                                            "✅ Arrived!",
                                            "You entered ${getZoneEmoji(currentMainZone)} $currentMainZone")
                                    lastMainZone != "Outside" && !isInMainZone ->
                                        sendStudentNotification(this@LocationService,
                                            "❌ Left Campus!",
                                            "You left ${getZoneEmoji(lastMainZone)} $lastMainZone")
                                    lastMainZone != "Outside" && isInMainZone ->
                                        sendStudentNotification(this@LocationService,
                                            "📍 Zone Changed",
                                            "Now in ${getZoneEmoji(currentMainZone)} $currentMainZone")
                                }
                                lastMainZone = currentMainZone
                            }

                            // Update Firebase live_locations
                            db.collection("live_locations").document(uid).set(hashMapOf(
                                "name" to studentName,
                                "latitude" to lat,
                                "longitude" to lng,
                                "isPresent" to (isInMainZone && isTime),
                                "currentZone" to currentZone,
                                "currentMainZone" to currentMainZone,
                                "timestamp" to System.currentTimeMillis(),
                                "uid" to uid
                            ))

                            if (isTime) {
                                db.collection("attendance").document(uid).set(hashMapOf(
                                    "name" to studentName,
                                    "status" to if (isInMainZone) "present" else "absent",
                                    "currentZone" to currentZone,
                                    "currentMainZone" to currentMainZone,
                                    "latitude" to lat,
                                    "longitude" to lng,
                                    "timestamp" to System.currentTimeMillis()
                                ))
                                val dateKey = SimpleDateFormat("yyyy-MM-dd",
                                    Locale.getDefault()).format(Date())
                                val timeKey = SimpleDateFormat("HH:mm:ss",
                                    Locale.getDefault()).format(Date())
                                db.collection("attendance_history")
                                    .document("${uid}_${dateKey}_${timeKey}")
                                    .set(hashMapOf(
                                        "uid" to uid,
                                        "name" to studentName,
                                        "status" to if (isInMainZone) "present" else "absent",
                                        "zone" to currentMainZone,
                                        "date" to dateKey,
                                        "time" to timeKey,
                                        "timestamp" to System.currentTimeMillis()
                                    ))
                            }
                        }
                }
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}