package com.leo.attendanceapp.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import com.leo.attendanceapp.ui.components.GlassCard
import com.leo.attendanceapp.ui.components.NeonButton
import com.leo.attendanceapp.ui.components.NeonTextField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

const val TEACHER_PASS_KEY = "D.N.PATEL5164"

data class AttendanceZone(
    val id: String,
    val name: String,
    val location: LatLng,
    val radius: Double = 100.0,
    val type: String = "circle",
    val polygon: List<LatLng> = emptyList(),
    val isMainZone: Boolean = false
)

@Suppress("MissingPermission")
fun sendStudentNotification(context: Context, title: String, message: String) {
    val channelId = "student_channel"
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = android.app.NotificationChannel(
            channelId, "Student Alerts",
            android.app.NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }
    val notif = androidx.core.app.NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    notificationManager.notify(System.currentTimeMillis().toInt(), notif)
}

fun isWithinAttendanceTimeStudent(startHour: Int, endHour: Int): Boolean {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.HOUR_OF_DAY) in startHour until endHour
}

fun calculateDistance(loc1: LatLng, loc2: LatLng): Double {
    val earthRadius = 6371000.0
    val dLat = Math.toRadians(loc1.latitude - loc2.latitude)
    val dLng = Math.toRadians(loc1.longitude - loc2.longitude)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(loc2.latitude)) *
            Math.cos(Math.toRadians(loc1.latitude)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2)
    return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

fun isPointInPolygon(point: LatLng, polygon: List<LatLng>): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val xi = polygon[i].longitude; val yi = polygon[i].latitude
        val xj = polygon[j].longitude; val yj = polygon[j].latitude
        if (((yi > point.latitude) != (yj > point.latitude)) &&
            (point.longitude < (xj - xi) * (point.latitude - yi) / (yj - yi) + xi)) {
            inside = !inside
        }
        j = i
    }
    return inside
}

fun isInZone(studentLoc: LatLng, zone: AttendanceZone): Boolean {
    return if (zone.type == "circle") {
        calculateDistance(studentLoc, zone.location) <= zone.radius
    } else if (zone.polygon.size >= 3) {
        isPointInPolygon(studentLoc, zone.polygon)
    } else false
}

fun getCurrentZoneName(studentLoc: LatLng, zones: List<AttendanceZone>): String {
    for (zone in zones) {
        if (isInZone(studentLoc, zone)) return zone.name
    }
    return "Outside"
}

fun getCurrentMainZoneName(studentLoc: LatLng, zones: List<AttendanceZone>): String {
    val mainZones = zones.filter { it.isMainZone }
    if (mainZones.isNotEmpty()) {
        for (zone in mainZones) {
            if (isInZone(studentLoc, zone)) return zone.name
        }
        return "Outside"
    }
    return getCurrentZoneName(studentLoc, zones)
}

suspend fun loadZonesFromFirestore(db: FirebaseFirestore): List<AttendanceZone> {
    return try {
        val snapshot = db.collection("zones").get().await()
        snapshot.documents.mapNotNull { doc ->
            val lat = doc.getDouble("latitude") ?: return@mapNotNull null
            val lng = doc.getDouble("longitude") ?: return@mapNotNull null
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
            AttendanceZone(doc.id, name, LatLng(lat, lng), radius, type, polygon, isMainZone)
        }
    } catch (e: Exception) {
        listOf(
            AttendanceZone("main_zone", "Main Zone", LatLng(19.0765, 72.8780), 300.0, isMainZone = true)
        )
    }
}

// ── department parameter add kiya ────────────────────────
@Suppress("MissingPermission")
suspend fun checkAndSaveAttendance(
    context: Context,
    uid: String,
    studentName: String,
    studentDepartment: String,
    db: FirebaseFirestore,
    lastMainZone: String,
    onResult: (status: String, present: Boolean, zoneName: String, mainZoneName: String) -> Unit
) {
    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = com.google.android.gms.tasks.CancellationTokenSource()
        val location = fusedClient.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            cts.token
        ).await()
        val zones = loadZonesFromFirestore(db)
        val geofenceDoc = db.collection("geofence").document("zone").get().await()
        val startHour = (geofenceDoc.getLong("startHour") ?: 11).toInt()
        val endHour = (geofenceDoc.getLong("endHour") ?: 17).toInt()
        val fixedName = studentName.trim().replaceFirstChar { it.uppercase() }

        if (location != null) {
            val studentLoc = LatLng(location.latitude, location.longitude)
            val currentZone = getCurrentZoneName(studentLoc, zones)
            val currentMainZone = getCurrentMainZoneName(studentLoc, zones)
            val isInMainZone = currentMainZone != "Outside"
            val isTime = isWithinAttendanceTimeStudent(startHour, endHour)

            val statusText = when {
                !isTime -> "⏰ Not attendance time"
                isInMainZone -> "✅ Present in $currentMainZone!"
                else -> "❌ Outside — Absent"
            }

            if (isTime && currentMainZone != lastMainZone) {
                when {
                    lastMainZone == "Outside" && isInMainZone ->
                        sendStudentNotification(context, "✅ Arrived!", "You entered $currentMainZone")
                    lastMainZone != "Outside" && !isInMainZone ->
                        sendStudentNotification(context, "❌ Left Campus!", "You left $lastMainZone")
                    lastMainZone != "Outside" && isInMainZone ->
                        sendStudentNotification(context, "📍 Zone Changed", "Now in $currentMainZone")
                }
            }

            // ── department add kiya live_locations mein ──
            db.collection("live_locations").document(uid).set(hashMapOf(
                "name" to fixedName,
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "isPresent" to (isInMainZone && isTime),
                "currentZone" to currentZone,
                "currentMainZone" to currentMainZone,
                "timestamp" to System.currentTimeMillis(),
                "uid" to uid,
                "department" to studentDepartment
            ))

            if (isTime) {
                // ── department add kiya attendance mein ──
                db.collection("attendance").document(uid).set(hashMapOf(
                    "name" to fixedName,
                    "status" to if (isInMainZone) "present" else "absent",
                    "currentZone" to currentZone,
                    "currentMainZone" to currentMainZone,
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "timestamp" to System.currentTimeMillis(),
                    "department" to studentDepartment
                ))
                val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val timeKey = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                db.collection("attendance_history")
                    .document("${uid}_${dateKey}_${timeKey}")
                    .set(hashMapOf(
                        "uid" to uid, "name" to fixedName,
                        "status" to if (isInMainZone) "present" else "absent",
                        "zone" to currentMainZone,
                        "date" to dateKey, "time" to timeKey,
                        "timestamp" to System.currentTimeMillis(),
                        "department" to studentDepartment
                    ))
            }
            onResult(statusText, isInMainZone && isTime, currentZone, currentMainZone)
        } else {
            onResult("⚠️ GPS off — Please turn on location", false, "Unknown", lastMainZone)
        }
    } catch (e: Exception) {
        onResult("❌ Error: ${e.message}", false, "Unknown", lastMainZone)
    }
}

// ── STUDENT REGISTER SCREEN ──────────────────────────────
@Composable
fun StudentRegisterScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var grNumber by remember { mutableStateOf("") }
    var prnNumber by remember { mutableStateOf("") }
    var studentClass by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x4400D4FF), Color.Transparent), radius = 350f
                ), radius = 350f, center = Offset(size.width * 0.9f, size.height * 0.1f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x339B59FF), Color.Transparent), radius = 300f
                ), radius = 300f, center = Offset(size.width * 0.1f, size.height * 0.8f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(50.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF00D4FF), Color(0xFF9B59FF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("📝", fontSize = 36.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Create Account",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "STUDENT REGISTRATION",
                fontSize = 11.sp,
                color = Color(0xFF00D4FF),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {

                Text(
                    text = "Personal Information",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D4FF)
                )

                Spacer(modifier = Modifier.height(16.dp))

                NeonTextField(
                    value = name,
                    onValueChange = {
                        if (it.all { c -> c.isLetter() || c.isWhitespace() }) name = it
                    },
                    label = "Full Name",
                    icon = Icons.Default.Person
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeonTextField(
                    value = studentId,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() }) studentId = it
                    },
                    label = "GR Number",
                    icon = Icons.Default.Badge
                )



                Spacer(modifier = Modifier.height(12.dp))

                NeonTextField(
                    value = prnNumber,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() }) prnNumber = it
                    },
                    label = "PRN Number",
                    icon = Icons.Default.Pin
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeonTextField(
                    value = studentClass,
                    onValueChange = { studentClass = it },
                    label = "Class (FY/SE/TE/BT)",
                    icon = Icons.Default.School
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeonDepartmentDropdown(
                    selectedValue = department,
                    onValueChange = { department = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Account Details",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D4FF)
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeonTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Student Email",
                    icon = Icons.Default.Email
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeonTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password (min 6)",
                    icon = Icons.Default.Lock,
                    isPassword = true
                )

                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (message.startsWith("✅"))
                                    Color(0x2200FF9F)
                                else Color(0x22FF4B6E)
                            )
                            .border(
                                1.dp,
                                if (message.startsWith("✅"))
                                    Color(0xFF00FF9F)
                                else Color(0xFFFF4B6E),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = message,
                            color = if (message.startsWith("✅"))
                                Color(0xFF00FF9F)
                            else Color(0xFFFF4B6E),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                NeonButton(
                    text = if (isLoading) "REGISTERING..." else "REGISTER",
                    onClick = {
                        if (name.isEmpty() || email.isEmpty() ||
                            password.isEmpty() || department.isEmpty()) {
                            message = "❌ Please fill all fields"
                            return@NeonButton
                        }
                        if (password.length < 6) {
                            message = "❌ Password min 6 characters"
                            return@NeonButton
                        }
                        isLoading = true
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener { result ->
                                val uid = result.user?.uid ?: ""
                                db.collection("users").document(uid).set(hashMapOf(
                                    "name" to name.trim().replaceFirstChar { it.uppercase() },
                                    "studentId" to studentId,
                                    "grNumber" to grNumber,
                                    "prnNumber" to prnNumber,
                                    "class" to studentClass,
                                    "email" to email,
                                    "department" to department,
                                    "role" to "student",
                                    "approvalStatus" to "pending"
                                )).addOnSuccessListener {
                                    isLoading = false
                                    navController.navigate("student_login") {
                                        popUpTo("student_register") { inclusive = true }
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                message = "❌ ${e.message}"
                            }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Already have account? ",
                        color = Color(0xFFB0B0CC),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Login",
                        color = Color(0xFF9B59FF),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            navController.navigate("student_login")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ── STUDENT LOGIN SCREEN ─────────────────────────────────
@Composable
fun StudentLoginScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showTeacherKeyDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x5500D4FF), Color.Transparent), radius = 400f
                ), radius = 400f, center = Offset(size.width * 0.1f, size.height * 0.15f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x559B59FF), Color.Transparent), radius = 350f
                ), radius = 350f, center = Offset(size.width * 0.9f, size.height * 0.8f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3300FF9F), Color.Transparent), radius = 200f
                ), radius = 200f, center = Offset(size.width * 0.5f, size.height * 0.5f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(
                        elevation = 40.dp,
                        shape = CircleShape,
                        ambientColor = Color(0xAA00D4FF),
                        spotColor = Color(0xAA9B59FF)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF00D4FF), Color(0xFF6C63FF), Color(0xFF9B59FF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("📍", fontSize = 50.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Smart Attendance",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = "GPS GEOFENCING SYSTEM",
                fontSize = 11.sp,
                color = Color(0xFF00D4FF),
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "D. N. Patel College of Engineering, Shahada",
                fontSize = 11.sp,
                color = Color(0xFFB0B0CC),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {

                Text(
                    text = "Student Login",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Welcome back!",
                    fontSize = 12.sp,
                    color = Color(0xFFB0B0CC)
                )

                Spacer(modifier = Modifier.height(28.dp))

                NeonTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Student Email",
                    icon = Icons.Default.Email
                )

                Spacer(modifier = Modifier.height(16.dp))

                NeonTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    icon = Icons.Default.Lock,
                    isPassword = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Forgot Password?",
                    color = Color(0xFF00D4FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { navController.navigate("forgot_password") }
                )

                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (message.startsWith("✅"))
                                    Color(0x2200FF9F)
                                else Color(0x22FF4B6E)
                            )
                            .border(
                                1.dp,
                                if (message.startsWith("✅"))
                                    Color(0xFF00FF9F)
                                else Color(0xFFFF4B6E),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = message,
                            color = if (message.startsWith("✅"))
                                Color(0xFF00FF9F)
                            else Color(0xFFFF4B6E),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF00D4FF),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    NeonButton(
                        text = "LOGIN",
                        onClick = {
                            if (email.isEmpty() || password.isEmpty()) {
                                message = "❌ Please enter email and password"
                                return@NeonButton
                            }
                            isLoading = true
                            message = ""
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid ?: ""
                                    db.collection("users").document(uid).get()
                                        .addOnSuccessListener { doc ->
                                            val role = doc.getString("role")
                                            isLoading = false
                                            when (role) {
                                                "student" -> {
                                                    val approvalStatus =
                                                        doc.getString("approvalStatus") ?: "approved"
                                                    when (approvalStatus) {
                                                        "pending" -> {
                                                            auth.signOut()
                                                            message = "⏳ Approval pending! Wait for teacher approval."
                                                        }
                                                        "rejected" -> {
                                                            auth.signOut()
                                                            message = "❌ Your account was rejected by teacher."
                                                        }
                                                        else -> {
                                                            navController.navigate("student_dashboard") {
                                                                popUpTo("student_login") { inclusive = true }
                                                            }
                                                        }
                                                    }
                                                }
                                                "teacher" -> {
                                                    auth.signOut()
                                                    message = "❌ This is a Teacher account!"
                                                }
                                                else -> {
                                                    auth.signOut()
                                                    message = "❌ Role not found"
                                                }
                                            }
                                        }
                                        .addOnFailureListener {
                                            isLoading = false
                                            message = "❌ DB error"
                                        }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    message = when {
                                        e.message?.contains("password") == true -> "❌ Wrong password"
                                        e.message?.contains("no user") == true -> "❌ Email not registered"
                                        else -> "❌ ${e.message}"
                                    }
                                }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0x44FFFFFF)))
                    Text("  OR  ", color = Color(0xFF666688), fontSize = 12.sp)
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0x44FFFFFF)))
                }

                Spacer(modifier = Modifier.height(16.dp))

                NeonButton(
                    text = "TEACHER LOGIN",
                    onClick = { showTeacherKeyDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("New student? ", color = Color(0xFFB0B0CC), fontSize = 14.sp)
                    Text(
                        text = "Register here",
                        color = Color(0xFF9B59FF),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable {
                            navController.navigate("student_register")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showTeacherKeyDialog) {
        var teacherKey by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTeacherKeyDialog = false },
            containerColor = Color(0xFF0D0D2B),
            title = {
                Text("Teacher Access", color = Color(0xFF00D4FF), fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Enter Teacher Secret Key", color = Color(0xFFB0B0CC), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    NeonTextField(
                        value = teacherKey,
                        onValueChange = { teacherKey = it },
                        label = "Secret Key",
                        icon = Icons.Default.Key,
                        isPassword = true
                    )
                }
            },
            confirmButton = {
                NeonButton(
                    text = "SUBMIT",
                    onClick = {
                        if (teacherKey == TEACHER_PASS_KEY) {
                            showTeacherKeyDialog = false
                            navController.navigate("teacher_login")
                        } else {
                            Toast.makeText(context, "❌ Invalid key!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showTeacherKeyDialog = false }) {
                    Text("Cancel", color = Color(0xFFB0B0CC))
                }
            }
        )
    }
}

// ── STUDENT DASHBOARD SCREEN ─────────────────────────────
@Composable
fun StudentDashboardScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var studentName by remember { mutableStateOf("") }
    var studentDepartment by remember { mutableStateOf("") }
    var gpsStatus by remember { mutableStateOf("📍 Checking location...") }
    var isPresent by remember { mutableStateOf(false) }
    var currentZone by remember { mutableStateOf("Checking...") }
    var currentMainZone by remember { mutableStateOf("Outside") }
    var hasPermission by remember { mutableStateOf(false) }
    var locationChecked by remember { mutableStateOf(false) }
    var lastMainZone by remember { mutableStateOf("Outside") }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        val serviceIntent = android.content.Intent(
            context,
            com.leo.attendanceapp.services.LocationForegroundService::class.java
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        val userDoc = db.collection("users").document(uid).get().await()
        studentName = userDoc.getString("name")?.trim()
            ?.replaceFirstChar { it.uppercase() } ?: ""
        // ── Department fetch kiya ─────────────────────────
        studentDepartment = userDoc.getString("department") ?: ""
        hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(studentName, hasPermission) {
        if (studentName.isEmpty() || !hasPermission) return@LaunchedEffect
        while (true) {
            val uid = auth.currentUser?.uid ?: break
            // ── department pass kiya ──────────────────────
            checkAndSaveAttendance(
                context, uid, studentName, studentDepartment, db, lastMainZone
            ) { status, present, zone, mainZone ->
                gpsStatus = status
                isPresent = present
                lastMainZone = currentMainZone
                currentZone = zone
                currentMainZone = mainZone
                locationChecked = true
            }
            delay(5000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3300D4FF), Color.Transparent), radius = 400f
                ), radius = 400f, center = Offset(size.width * 0.8f, size.height * 0.1f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x229B59FF), Color.Transparent), radius = 300f
                ), radius = 300f, center = Offset(size.width * 0.1f, size.height * 0.6f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome back,",
                        color = Color(0xFFB0B0CC),
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (studentName.isEmpty()) "Loading..." else studentName,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (studentDepartment.isNotEmpty()) {
                        Text(
                            text = "🏫 $studentDepartment",
                            color = Color(0xFF00D4FF),
                            fontSize = 11.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF00D4FF), Color(0xFF9B59FF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👤", fontSize = 22.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Attendance Status",
                            color = Color(0xFFB0B0CC),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isPresent) Color(0xFF00FF9F)
                                        else Color(0xFFFF4B6E)
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPresent) "Present" else "Absent",
                                color = if (isPresent) Color(0xFF00FF9F) else Color(0xFFFF4B6E),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "GPS Status",
                            color = Color(0xFFB0B0CC),
                            fontSize = 12.sp
                        )
                        if (!locationChecked) {
                            CircularProgressIndicator(
                                color = Color(0xFF00D4FF),
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "✅ Active",
                                color = Color(0xFF00D4FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0x22FFFFFF))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Current Zone", color = Color(0xFFB0B0CC), fontSize = 12.sp)
                        Text(
                            text = currentZone,
                            color = Color(0xFF00D4FF),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Main Zone", color = Color(0xFFB0B0CC), fontSize = 12.sp)
                        Text(
                            text = currentMainZone,
                            color = Color(0xFF9B59FF),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = gpsStatus,
                    color = when {
                        gpsStatus.contains("Present") -> Color(0xFF00FF9F)
                        gpsStatus.contains("Absent") -> Color(0xFFFF4B6E)
                        else -> Color(0xFFB0B0CC)
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Quick Access",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { navController.navigate("attendance_status") }
                ) {
                    Text("📊", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Attendance", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("View details", color = Color(0xFFB0B0CC), fontSize = 11.sp)
                }
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { navController.navigate("my_location") }
                ) {
                    Text("📍", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("My Location", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("GPS Map", color = Color(0xFFB0B0CC), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { navController.navigate("daily_record") }
                ) {
                    Text("📅", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Daily Log", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Zone history", color = Color(0xFFB0B0CC), fontSize = 11.sp)
                }
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            auth.signOut()
                            navController.navigate("student_login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                ) {
                    Text("🚪", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Logout", color = Color(0xFFFF4B6E), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Sign out", color = Color(0xFFB0B0CC), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ── DAILY RECORD SCREEN ──────────────────────────────────
@Composable
fun DailyRecordScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    var records by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    DisposableEffect(Unit) {
        val listener = db.collection("attendance_history")
            .whereEqualTo("uid", uid)
            .whereEqualTo("date", dateKey)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    records = snapshot.documents.mapNotNull { it.data }
                        .sortedBy { it["timestamp"] as? Long ?: 0L }
                    isLoading = false
                }
            }
        onDispose { listener.remove() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3300D4FF), Color.Transparent), radius = 300f
                ), radius = 300f, center = Offset(size.width * 0.8f, size.height * 0.2f)
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0D0D2B), Color(0xFF050510))
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x2200D4FF))
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", color = Color(0xFF00D4FF), fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Daily Zone Record",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(dateKey, color = Color(0xFF00D4FF), fontSize = 12.sp)
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00D4FF))
                }
            } else if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GlassCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📭", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No records for today",
                                color = Color(0xFFB0B0CC),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        "Today's Zone Activity",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    records.forEach { record ->
                        val zone = record["zone"]?.toString() ?: "Unknown"
                        val time = record["time"]?.toString() ?: ""
                        val status = record["status"]?.toString() ?: ""

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(getZoneEmoji(zone), fontSize = 28.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        zone,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        if (status == "present") "✅ Present" else "❌ Absent",
                                        fontSize = 12.sp,
                                        color = if (status == "present")
                                            Color(0xFF00FF9F)
                                        else Color(0xFFFF4B6E)
                                    )
                                }
                                Text(time, fontSize = 12.sp, color = Color(0xFFB0B0CC))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── MY LOCATION SCREEN ───────────────────────────────────
@Composable
fun MyLocationScreen(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()
    var studentLocation by remember { mutableStateOf<LatLng?>(null) }
    var zones by remember { mutableStateOf<List<AttendanceZone>>(emptyList()) }
    var currentZone by remember { mutableStateOf("Checking...") }
    var currentMainZone by remember { mutableStateOf("Outside") }
    var isPresent by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("📍 Loading...") }
    var isRefreshing by remember { mutableStateOf(false) }
    var lastUpdated by remember { mutableStateOf("") }
    var hasPermission by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(19.0760, 72.8777), 16f)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) statusText = "❌ Please allow location access"
    }

    @Suppress("MissingPermission")
    suspend fun loadMyLocation() {
        if (!hasPermission) return
        isRefreshing = true
        try {
            zones = loadZonesFromFirestore(db)
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = com.google.android.gms.tasks.CancellationTokenSource()
            val location = fusedClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).await()
            if (location != null) {
                studentLocation = LatLng(location.latitude, location.longitude)
                currentZone = getCurrentZoneName(studentLocation!!, zones)
                currentMainZone = getCurrentMainZoneName(studentLocation!!, zones)
                isPresent = currentMainZone != "Outside"
                statusText = if (isPresent) "✅ Present in $currentMainZone!"
                else "❌ Outside campus"
                cameraPositionState.position =
                    CameraPosition.fromLatLngZoom(studentLocation!!, 17f)
                lastUpdated = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
            } else {
                statusText = "⚠️ GPS off"
            }
        } catch (e: Exception) {
            statusText = "❌ Error: ${e.message}"
        }
        isRefreshing = false
    }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        else scope.launch { loadMyLocation() }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        while (true) {
            loadMyLocation()
            delay(5000)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050510))) {

        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0D2B))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x2200D4FF))
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", color = Color(0xFF00D4FF), fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "My Location",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "📍 $currentZone",
                            color = Color(0xFF00D4FF),
                            fontSize = 12.sp
                        )
                    }
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color = Color(0xFF00D4FF),
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFFF4B6E))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "🔴 LIVE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    zones.forEach { zone ->
                        val zoneColor = getZoneColor(zone.name)
                        if (zone.type == "circle") {
                            Circle(
                                center = zone.location,
                                radius = zone.radius,
                                fillColor = zoneColor.copy(alpha = if (zone.isMainZone) 0.2f else 0.08f),
                                strokeColor = zoneColor,
                                strokeWidth = if (zone.isMainZone) 4f else 1.5f
                            )
                        } else if (zone.polygon.size >= 3) {
                            Polygon(
                                points = zone.polygon,
                                fillColor = zoneColor.copy(alpha = if (zone.isMainZone) 0.2f else 0.08f),
                                strokeColor = zoneColor,
                                strokeWidth = if (zone.isMainZone) 4f else 1.5f
                            )
                        }
                        Marker(
                            state = MarkerState(position = zone.location),
                            title = zone.name + if (zone.isMainZone) " 🎯" else ""
                        )
                    }
                    studentLocation?.let { loc ->
                        Marker(
                            state = MarkerState(position = loc),
                            title = "My Location",
                            snippet = "Zone: $currentZone",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (isPresent) BitmapDescriptorFactory.HUE_GREEN
                                else BitmapDescriptorFactory.HUE_RED
                            )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0D2B))
                    .padding(16.dp)
            ) {
                Column {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(getZoneEmoji(currentZone), fontSize = 32.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "You are in: $currentZone",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPresent) Color(0xFF00FF9F) else Color(0xFFFF4B6E)
                                )
                                Text(statusText, fontSize = 12.sp, color = Color(0xFFB0B0CC))
                                if (lastUpdated.isNotEmpty()) {
                                    Text(
                                        "🕐 $lastUpdated",
                                        fontSize = 11.sp,
                                        color = Color(0xFFB0B0CC)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    NeonButton(
                        text = if (isRefreshing) "REFRESHING..." else "🔄 REFRESH LOCATION",
                        onClick = { scope.launch { loadMyLocation() } },
                        enabled = !isRefreshing,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ── ATTENDANCE STATUS SCREEN ─────────────────────────────
@Composable
fun AttendanceStatusScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: ""
    var presentCount by remember { mutableIntStateOf(0) }
    var absentCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val listener = db.collection("attendance_history")
            .whereEqualTo("uid", uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    presentCount = snapshot.documents.count { it.getString("status") == "present" }
                    absentCount = snapshot.documents.count { it.getString("status") == "absent" }
                    isLoading = false
                }
            }
        onDispose { listener.remove() }
    }

    val total = presentCount + absentCount
    val rate = if (total > 0) (presentCount * 100) / total else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3300D4FF), Color.Transparent), radius = 350f
                ), radius = 350f, center = Offset(size.width * 0.9f, size.height * 0.2f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x229B59FF), Color.Transparent), radius = 300f
                ), radius = 300f, center = Offset(size.width * 0.1f, size.height * 0.7f)
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0D2B))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x2200D4FF))
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", color = Color(0xFF00D4FF), fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Attendance Status",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00D4FF))
                }
            } else {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassCard(modifier = Modifier.weight(1f)) {
                            Text(
                                "$presentCount",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00FF9F)
                            )
                            Text("Present", fontSize = 11.sp, color = Color(0xFFB0B0CC))
                        }
                        GlassCard(modifier = Modifier.weight(1f)) {
                            Text(
                                "$absentCount",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFF4B6E)
                            )
                            Text("Absent", fontSize = 11.sp, color = Color(0xFFB0B0CC))
                        }
                        GlassCard(modifier = Modifier.weight(1f)) {
                            Text(
                                "$rate%",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00D4FF)
                            )
                            Text("Rate", fontSize = 11.sp, color = Color(0xFFB0B0CC))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (total == 0) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📭", fontSize = 40.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No attendance records yet",
                                    color = Color(0xFFB0B0CC),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Overall",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        "$total total records",
                                        color = Color(0xFFB0B0CC),
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    "$rate%",
                                    color = if (rate >= 75) Color(0xFF00FF9F) else Color(0xFFFF4B6E),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { rate / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (rate >= 75) Color(0xFF00FF9F) else Color(0xFFFF4B6E),
                                trackColor = Color(0x22FFFFFF)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (rate >= 75) "✅ Good attendance!"
                                else "⚠️ Low attendance! $presentCount/$total",
                                fontSize = 13.sp,
                                color = if (rate >= 75) Color(0xFF00FF9F) else Color(0xFFFF4B6E)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── HELPER COMPOSABLES ───────────────────────────────────
@Composable
fun MyTextField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color(0xFF00D4FF)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation()
        else VisualTransformation.None,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00D4FF),
            unfocusedBorderColor = Color(0x5500D4FF),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color(0xFFB0B0CC),
            cursorColor = Color(0xFF00D4FF),
            focusedContainerColor = Color(0x1100D4FF),
            unfocusedContainerColor = Color(0x08000000)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonDepartmentDropdown(selectedValue: String, onValueChange: (String) -> Unit) {
    val departments = listOf(
        "Computer Engineering",
        "Artificial Intelligence and Data Science",
        "Civil Engineering",
        "Mechanical Engineering",
        "Electrical Engineering",
        "Electronics & Telecommunication Engineering",
        "Instrumentation Engineering"
    )
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text("Department", color = Color(0xFF00D4FF)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00D4FF),
                unfocusedBorderColor = Color(0x5500D4FF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color(0xFFB0B0CC),
                focusedContainerColor = Color(0x1100D4FF),
                unfocusedContainerColor = Color(0x08000000),
                focusedLabelColor = Color(0xFF00D4FF),
                unfocusedLabelColor = Color(0xFF9B59FF)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF0D0D2B))
        ) {
            departments.forEach { dept ->
                DropdownMenuItem(
                    text = {
                        Text(
                            dept,
                            color = if (selectedValue == dept) Color(0xFF00D4FF) else Color.White,
                            fontWeight = if (selectedValue == dept) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = { onValueChange(dept); expanded = false },
                    modifier = Modifier.background(
                        if (selectedValue == dept) Color(0x1500D4FF) else Color.Transparent
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentDropdown(selectedValue: String, onValueChange: (String) -> Unit) {
    NeonDepartmentDropdown(selectedValue = selectedValue, onValueChange = onValueChange)
}