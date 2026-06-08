package com.leo.attendanceapp.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.navigation.NavController
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import com.leo.attendanceapp.TeacherSession
import com.leo.attendanceapp.ui.components.GlassCard
import com.leo.attendanceapp.ui.components.NeonButton
import com.leo.attendanceapp.ui.components.NeonTextField
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Suppress("MissingPermission")
fun sendNotification(context: Context, title: String, message: String) {
    val channelId = "attendance_channel"
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId, "Attendance Alerts", NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
}

fun isWithinAttendanceTime(startHour: Int, endHour: Int): Boolean {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.HOUR_OF_DAY) in startHour until endHour
}

// ── TEACHER LOGIN SCREEN ─────────────────────────────────
@Composable
fun TeacherLoginScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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
                    colors = listOf(Color(0x559B59FF), Color.Transparent), radius = 400f
                ), radius = 400f, center = Offset(size.width * 0.1f, size.height * 0.2f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x4400D4FF), Color.Transparent), radius = 350f
                ), radius = 350f, center = Offset(size.width * 0.9f, size.height * 0.8f)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x229B59FF))
                        .clickable { navController.popBackStack() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "← Student Login",
                        color = Color(0xFF9B59FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(
                        elevation = 40.dp,
                        shape = CircleShape,
                        ambientColor = Color(0xAA9B59FF),
                        spotColor = Color(0xAA00D4FF)
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF9B59FF), Color(0xFF6C63FF), Color(0xFF00D4FF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🏫", fontSize = 50.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Teacher Login",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "MANAGE ATTENDANCE & TRACK STUDENTS",
                fontSize = 10.sp,
                color = Color(0xFF9B59FF),
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {

                Text(
                    "Welcome, Teacher!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    "Enter your credentials",
                    fontSize = 12.sp,
                    color = Color(0xFFB0B0CC)
                )

                Spacer(modifier = Modifier.height(24.dp))

                NeonTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Teacher Email",
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

                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (message.startsWith("✅"))
                                    Color(0x2200FF9F) else Color(0x22FF4B6E)
                            )
                            .border(
                                1.dp,
                                if (message.startsWith("✅"))
                                    Color(0xFF00FF9F) else Color(0xFFFF4B6E),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = message,
                            color = if (message.startsWith("✅"))
                                Color(0xFF00FF9F) else Color(0xFFFF4B6E),
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
                            color = Color(0xFF9B59FF),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    NeonButton(
                        text = "LOGIN AS TEACHER",
                        onClick = {
                            if (email.isEmpty() || password.isEmpty()) {
                                message = "❌ Enter email and password"
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
                                                "teacher" -> navController.navigate("teacher_dashboard") {
                                                    popUpTo("teacher_login") { inclusive = true }
                                                }
                                                "student" -> {
                                                    auth.signOut()
                                                    message = "❌ This is a Student account!"
                                                }
                                                else -> {
                                                    auth.signOut()
                                                    message = "❌ Role not found"
                                                }
                                            }
                                        }
                                        .addOnFailureListener {
                                            isLoading = false
                                            message = "❌ Database error"
                                        }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    message = when {
                                        e.message?.contains("password") == true -> "❌ Incorrect password"
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
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("New teacher? ", color = Color(0xFFB0B0CC), fontSize = 13.sp)
                    Text(
                        "Register here",
                        color = Color(0xFF9B59FF),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            navController.navigate("teacher_register")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ── TEACHER REGISTER SCREEN ──────────────────────────────
@Composable
fun TeacherRegisterScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var name by remember { mutableStateOf("") }
    var teacherId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
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
                    colors = listOf(Color(0x449B59FF), Color.Transparent), radius = 350f
                ), radius = 350f, center = Offset(size.width * 0.9f, size.height * 0.1f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3300D4FF), Color.Transparent), radius = 300f
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
                            listOf(Color(0xFF9B59FF), Color(0xFF00D4FF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🏫", fontSize = 36.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Teacher Register",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                "CREATE YOUR TEACHER ACCOUNT",
                fontSize = 10.sp,
                color = Color(0xFF9B59FF),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {

                Text(
                    "Teacher Information",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9B59FF)
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
                    value = teacherId,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() }) teacherId = it
                    },
                    label = "Teacher ID",
                    icon = Icons.Default.Badge
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Department Dropdown ───────────────────
                NeonDepartmentDropdown(
                    selectedValue = department,
                    onValueChange = { department = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Account Details",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9B59FF)
                )

                Spacer(modifier = Modifier.height(12.dp))

                NeonTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
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
                                    Color(0x2200FF9F) else Color(0x22FF4B6E)
                            )
                            .border(
                                1.dp,
                                if (message.startsWith("✅"))
                                    Color(0xFF00FF9F) else Color(0xFFFF4B6E),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = message,
                            color = if (message.startsWith("✅"))
                                Color(0xFF00FF9F) else Color(0xFFFF4B6E),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF9B59FF),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    NeonButton(
                        text = "REGISTER AS TEACHER",
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
                                    db.collection("users").document(uid).set(
                                        hashMapOf(
                                            "name" to name,
                                            "teacherId" to teacherId,
                                            "email" to email,
                                            "department" to department,
                                            "role" to "teacher"
                                        )
                                    ).addOnSuccessListener {
                                        isLoading = false
                                        navController.navigate("teacher_login") {
                                            popUpTo("teacher_register") { inclusive = true }
                                        }

                                    }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    message = "❌ ${e.message}"
                                }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Already have account? ", color = Color(0xFFB0B0CC), fontSize = 13.sp)
                    Text(
                        "Login",
                        color = Color(0xFF9B59FF),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            navController.navigate("teacher_login")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ── TEACHER DASHBOARD SCREEN ─────────────────────────────
@Composable
fun TeacherDashboardScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var teacherName by remember { mutableStateOf("Teacher") }
    var teacherDepartment by remember { mutableStateOf("") }
    var presentCount by remember { mutableStateOf(0) }
    var totalCount by remember { mutableStateOf(0) }
    var pendingApprovals by remember { mutableStateOf(0) }
    val isAttendanceTime = isWithinAttendanceTime(11, 17)

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                teacherName = doc.getString("name") ?: "Teacher"
                teacherDepartment = doc.getString("department") ?: ""
                // ── TeacherSession mein save karo ─────────
                TeacherSession.department = teacherDepartment
                TeacherSession.name = teacherName
                TeacherSession.uid = uid
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ── Real-time attendance — department filter ──────────
    DisposableEffect(teacherDepartment) {
        val query = if (teacherDepartment.isEmpty()) {
            db.collection("attendance")
        } else {
            db.collection("attendance")
                .whereEqualTo("department", teacherDepartment)
        }
        val listener = query.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                presentCount = snapshot.documents.count { it.getString("status") == "present" }
                totalCount = snapshot.documents.size
            }
        }
        onDispose { listener.remove() }
    }

    // ── Pending approvals — department filter ─────────────
    DisposableEffect(teacherDepartment) {
        val query = if (teacherDepartment.isEmpty()) {
            db.collection("users")
                .whereEqualTo("role", "student")
                .whereEqualTo("approvalStatus", "pending")
        } else {
            db.collection("users")
                .whereEqualTo("role", "student")
                .whereEqualTo("approvalStatus", "pending")
                .whereEqualTo("department", teacherDepartment)
        }
        val listener = query.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                pendingApprovals = snapshot.documents.size
            }
        }
        onDispose { listener.remove() }
    }

    val attendanceRate = if (totalCount > 0) (presentCount * 100 / totalCount) else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x339B59FF), Color.Transparent), radius = 400f
                ), radius = 400f, center = Offset(size.width * 0.9f, size.height * 0.1f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x2200D4FF), Color.Transparent), radius = 350f
                ), radius = 350f, center = Offset(size.width * 0.1f, size.height * 0.7f)
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
                        "Teacher Dashboard",
                        color = Color(0xFF9B59FF),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        "Prof. $teacherName",
                        color = Color(0xFFB0B0CC),
                        fontSize = 13.sp
                    )
                    if (teacherDepartment.isNotEmpty()) {
                        Text(
                            "🏫 $teacherDepartment",
                            color = Color(0xFF00D4FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pendingApprovals > 0) {
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x15FF4B6E))
                                    .clickable {
                                        navController.navigate("approve_students")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👤", fontSize = 20.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF4B6E))
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$pendingApprovals",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF9B59FF), Color(0xFF00D4FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👩‍🏫", fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    Text(
                        "$totalCount",
                        color = Color(0xFF00D4FF),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text("Total", color = Color(0xFFB0B0CC), fontSize = 11.sp)
                }
                GlassCard(modifier = Modifier.weight(1f)) {
                    Text(
                        "$presentCount",
                        color = Color(0xFF00FF9F),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text("Present", color = Color(0xFFB0B0CC), fontSize = 11.sp)
                }
                GlassCard(modifier = Modifier.weight(1f)) {
                    Text(
                        "${totalCount - presentCount}",
                        color = Color(0xFFFF4B6E),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text("Absent", color = Color(0xFFB0B0CC), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Attendance Rate",
                            color = Color(0xFFB0B0CC),
                            fontSize = 13.sp
                        )
                        Text(
                            "$attendanceRate%",
                            color = Color(0xFF9B59FF),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isAttendanceTime) Color(0x2200FF9F)
                                else Color(0x22FF9F00)
                            )
                            .border(
                                1.dp,
                                if (isAttendanceTime) Color(0xFF00FF9F)
                                else Color(0xFFFF9F00),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            if (isAttendanceTime) "✅ Active" else "⏰ Inactive",
                            color = if (isAttendanceTime) Color(0xFF00FF9F)
                            else Color(0xFFFF9F00),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { attendanceRate / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF9B59FF),
                    trackColor = Color(0x339B59FF)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Management",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { navController.navigate("manage_zones") }
                ) {
                    Text("📍", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Manage", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Zones", color = Color(0xFFB0B0CC), fontSize = 11.sp)
                }
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { navController.navigate("view_attendance") }
                ) {
                    Text("📊", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("View", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Attendance", color = Color(0xFFB0B0CC), fontSize = 11.sp)
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
                        .clickable { navController.navigate("student_tracking") }
                ) {
                    Text("🗺️", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tracking", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Live Map", color = Color(0xFFB0B0CC), fontSize = 11.sp)
                }
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { navController.navigate("student_daily_records") }
                ) {
                    Text("📅", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Daily", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Records", color = Color(0xFFB0B0CC), fontSize = 11.sp)
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
                        .clickable { navController.navigate("approve_students") }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👤", fontSize = 28.sp)
                        if (pendingApprovals > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF4B6E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$pendingApprovals",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Approve", color = Color(0xFFFF4B6E), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Students", color = Color(0xFFB0B0CC), fontSize = 11.sp)
                }
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            auth.signOut()
                            TeacherSession.department = ""
                            TeacherSession.name = ""
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

// ── MANAGE ZONES SCREEN ──────────────────────────────────
@Composable
fun ManageZonesScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    val zoneNames = listOf("Ground", "Department", "Library", "Sardar Patel Hall", "Cafeteria", "Main Zone")
    val zoneEmojis = listOf("⚽", "🏫", "📚", "🏛️", "☕", "🎯")
    val zoneColors = listOf(
        Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFF4CAF50),
        Color(0xFFFF5722), Color(0xFF795548), Color(0xFF1565C0)
    )

    var selectedZoneIndex by remember { mutableStateOf(0) }
    var selectedShape by remember { mutableStateOf("circle") }
    var selectedLocation by remember { mutableStateOf(LatLng(19.0760, 72.8777)) }
    var radius by remember { mutableStateOf(100f) }
    var polygonPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isDrawing by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isSatellite by remember { mutableStateOf(false) }
    var savedZones by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var searchQuery by remember { mutableStateOf("") }
    var searchMessage by remember { mutableStateOf("") }
    var isMainZone by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(selectedLocation, 16f)
    }

    DisposableEffect(Unit) {
        val listener = db.collection("zones")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val zonesMap = mutableMapOf<String, Any>()
                    snapshot.documents.forEach { doc ->
                        zonesMap[doc.id] = doc.data ?: emptyMap<String, Any>()
                    }
                    savedZones = zonesMap
                }
            }
        onDispose { listener.remove() }
    }

    LaunchedEffect(selectedZoneIndex) {
        val key = zoneNames[selectedZoneIndex].lowercase().replace(" ", "_")
        val zoneData = savedZones[key] as? Map<*, *>
        if (zoneData != null) {
            val type = zoneData["type"] as? String ?: "circle"
            selectedShape = type
            val lat = zoneData["latitude"] as? Double ?: 19.0760
            val lng = zoneData["longitude"] as? Double ?: 72.8777
            selectedLocation = LatLng(lat, lng)
            radius = (zoneData["radius"] as? Double)?.toFloat() ?: 100f
            isMainZone = zoneData["isMainZone"] as? Boolean ?: false
            cameraPositionState.position = CameraPosition.fromLatLngZoom(selectedLocation, 16f)
            if (type == "polygon" || type == "rectangle") {
                val points = zoneData["polygon"] as? List<*>
                polygonPoints = points?.mapNotNull { point ->
                    val p = point as? Map<*, *>
                    val pLat = p?.get("lat") as? Double
                    val pLng = p?.get("lng") as? Double
                    if (pLat != null && pLng != null) LatLng(pLat, pLng) else null
                } ?: emptyList()
            } else polygonPoints = emptyList()
        } else {
            polygonPoints = emptyList()
            selectedShape = "circle"
            isMainZone = zoneNames[selectedZoneIndex] == "Main Zone"
        }
        saved = false
        isDrawing = false
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
                            .background(Color(0x229B59FF))
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", color = Color(0xFF9B59FF), fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Manage Zones",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            when {
                                selectedShape == "circle" -> "Move map to center, adjust radius"
                                isDrawing -> "Tap map to add points"
                                else -> "Select shape and draw zone"
                            },
                            color = Color(0xFFB0B0CC),
                            fontSize = 11.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("⭕" to "circle", "▭" to "rectangle", "✏️" to "polygon")
                            .forEach { (emoji, shapeType) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (selectedShape == shapeType)
                                                Color(0xFF9B59FF)
                                            else Color(0x229B59FF)
                                        )
                                        .clickable {
                                            selectedShape = shapeType
                                            polygonPoints = emptyList()
                                            saved = false
                                            isDrawing = shapeType != "circle"
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 16.sp)
                                }
                            }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A1A))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it; searchMessage = "" },
                    label = { Text("Search location...", color = Color(0xFF00D4FF)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00D4FF),
                        unfocusedBorderColor = Color(0x5500D4FF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFB0B0CC),
                        focusedContainerColor = Color(0x1100D4FF),
                        unfocusedContainerColor = Color(0x08000000)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00D4FF), Color(0xFF9B59FF))
                            )
                        )
                        .clickable {
                            if (searchQuery.isEmpty()) {
                                searchMessage = "❌ Enter location name"
                                return@clickable
                            }
                            try {
                                val geocoder = android.location.Geocoder(context)
                                @Suppress("DEPRECATION")
                                val results = geocoder.getFromLocationName(searchQuery, 1)
                                if (!results.isNullOrEmpty()) {
                                    val result = results[0]
                                    selectedLocation = LatLng(result.latitude, result.longitude)
                                    cameraPositionState.position =
                                        CameraPosition.fromLatLngZoom(selectedLocation, 16f)
                                    searchMessage = "✅ Found!"
                                    saved = false
                                } else {
                                    searchMessage = "❌ Location not found"
                                }
                            } catch (e: Exception) {
                                searchMessage = "❌ ${e.message}"
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text("Go", color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            }

            if (searchMessage.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (searchMessage.startsWith("✅"))
                                Color(0x2200FF9F) else Color(0x22FF4B6E)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        searchMessage,
                        color = if (searchMessage.startsWith("✅"))
                            Color(0xFF00FF9F) else Color(0xFFFF4B6E),
                        fontSize = 13.sp
                    )
                }
            }

            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A1A))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(zoneNames.size) { index ->
                    val isSelected = selectedZoneIndex == index
                    val key = zoneNames[index].lowercase().replace(" ", "_")
                    val isSavedZone = savedZones.containsKey(key)
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected)
                                    Brush.linearGradient(
                                        listOf(Color(0xFF00D4FF), Color(0xFF9B59FF))
                                    )
                                else Brush.linearGradient(
                                    listOf(Color(0x220D0D2B), Color(0x220D0D2B))
                                )
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF00D4FF) else Color(0x33FFFFFF),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedZoneIndex = index }
                            .padding(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(zoneEmojis[index], fontSize = 20.sp)
                            Text(
                                zoneNames[index],
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = if (isSelected) Color.White else Color(0xFFB0B0CC)
                            )
                            if (isSavedZone) Text("✅", fontSize = 10.sp)
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        mapType = if (isSatellite) MapType.SATELLITE else MapType.NORMAL
                    ),
                    onMapClick = { latLng ->
                        when (selectedShape) {
                            "polygon" -> {
                                if (isDrawing) {
                                    polygonPoints = polygonPoints + latLng
                                    saved = false
                                }
                            }
                            "rectangle" -> {
                                if (isDrawing) {
                                    if (polygonPoints.isEmpty()) {
                                        polygonPoints = listOf(latLng)
                                    } else {
                                        val topLeft = polygonPoints[0]
                                        val bottomRight = latLng
                                        polygonPoints = listOf(
                                            topLeft,
                                            LatLng(topLeft.latitude, bottomRight.longitude),
                                            bottomRight,
                                            LatLng(bottomRight.latitude, topLeft.longitude)
                                        )
                                        isDrawing = false
                                        saved = false
                                    }
                                }
                            }
                            else -> {
                                selectedLocation = latLng
                                saved = false
                            }
                        }
                    }
                ) {
                    if (selectedShape == "circle") {
                        Circle(
                            center = selectedLocation,
                            radius = radius.toDouble(),
                            fillColor = zoneColors[selectedZoneIndex].copy(alpha = 0.2f),
                            strokeColor = zoneColors[selectedZoneIndex],
                            strokeWidth = if (isMainZone) 5f else 3f
                        )
                        Marker(
                            state = MarkerState(position = selectedLocation),
                            title = zoneNames[selectedZoneIndex] + if (isMainZone) " 🎯" else ""
                        )
                    }
                    if (polygonPoints.size >= 3) {
                        Polygon(
                            points = polygonPoints,
                            fillColor = zoneColors[selectedZoneIndex].copy(alpha = 0.25f),
                            strokeColor = zoneColors[selectedZoneIndex],
                            strokeWidth = if (isMainZone) 5f else 3f
                        )
                    }
                    polygonPoints.forEachIndexed { index, point ->
                        Marker(
                            state = MarkerState(position = point),
                            title = "Point ${index + 1}",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_VIOLET
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xCC0D0D2B))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isSatellite) "🛰️" else "🗺️", fontSize = 16.sp)
                        Switch(
                            checked = isSatellite,
                            onCheckedChange = { isSatellite = it },
                            modifier = Modifier.size(40.dp, 24.dp)
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
                    if (selectedShape == "circle") {
                        Text(
                            "${zoneEmojis[selectedZoneIndex]} ${zoneNames[selectedZoneIndex]} — Radius: ${radius.toInt()}m",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Slider(
                            value = radius,
                            onValueChange = { radius = it; saved = false },
                            valueRange = 30f..500f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00D4FF),
                                activeTrackColor = Color(0xFF00D4FF)
                            )
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${zoneEmojis[selectedZoneIndex]} ${zoneNames[selectedZoneIndex]} — ${polygonPoints.size} points",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            if (polygonPoints.isNotEmpty()) {
                                TextButton(onClick = {
                                    polygonPoints = polygonPoints.dropLast(1)
                                    saved = false
                                }) {
                                    Text("↩ Undo", color = Color(0xFFFF4B6E), fontSize = 12.sp)
                                }
                                TextButton(onClick = {
                                    polygonPoints = emptyList()
                                    saved = false
                                    isDrawing = true
                                }) {
                                    Text("🗑 Clear", color = Color(0xFFFF4B6E), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "🎯 Main Zone",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                "Attendance tracking",
                                fontSize = 11.sp,
                                color = Color(0xFFB0B0CC)
                            )
                        }
                        Switch(
                            checked = isMainZone,
                            onCheckedChange = { isMainZone = it; saved = false },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF9B59FF)
                            )
                        )
                    }

                    if (saved) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x2200FF9F))
                                .border(1.dp, Color(0xFF00FF9F), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                "✅ ${zoneNames[selectedZoneIndex]} zone saved!",
                                color = Color(0xFF00FF9F),
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    NeonButton(
                        text = if (isLoading) "SAVING..."
                        else "💾 SAVE ${zoneNames[selectedZoneIndex].uppercase()} ${if (isMainZone) "🎯" else ""}",
                        onClick = {
                            if (selectedShape != "circle" && polygonPoints.size < 3) {
                                searchMessage = "❌ Add at least 3 points!"
                                return@NeonButton
                            }
                            isLoading = true
                            val uid = auth.currentUser?.uid ?: return@NeonButton
                            val zoneName = zoneNames[selectedZoneIndex]
                            val zoneKey = zoneName.lowercase().replace(" ", "_")
                            val saveData: HashMap<String, Any> = when (selectedShape) {
                                "circle" -> hashMapOf(
                                    "name" to zoneName, "type" to "circle",
                                    "latitude" to selectedLocation.latitude,
                                    "longitude" to selectedLocation.longitude,
                                    "radius" to radius.toDouble(),
                                    "isMainZone" to isMainZone,
                                    "createdBy" to uid,
                                    "updatedAt" to System.currentTimeMillis()
                                )
                                else -> {
                                    val polygonData = polygonPoints.map { point ->
                                        mapOf("lat" to point.latitude, "lng" to point.longitude)
                                    }
                                    val centerLat = polygonPoints.map { it.latitude }.average()
                                    val centerLng = polygonPoints.map { it.longitude }.average()
                                    hashMapOf(
                                        "name" to zoneName, "type" to selectedShape,
                                        "polygon" to polygonData,
                                        "latitude" to centerLat, "longitude" to centerLng,
                                        "isMainZone" to isMainZone,
                                        "createdBy" to uid,
                                        "updatedAt" to System.currentTimeMillis()
                                    )
                                }
                            }
                            db.collection("zones").document(zoneKey).set(saveData)
                                .addOnSuccessListener {
                                    isLoading = false
                                    saved = true
                                    isDrawing = false
                                    sendNotification(context, "✅ Zone Saved!", "$zoneName zone saved!")
                                }
                                .addOnFailureListener { isLoading = false }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ── VIEW ATTENDANCE SCREEN ───────────────────────────────
@Composable
fun ViewAttendanceScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var realStudents by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedStudent by remember { mutableStateOf<Map<String, Any>?>(null) }

    // ── Department filter ─────────────────────────────────
    DisposableEffect(Unit) {
        val dept = TeacherSession.department
        val query = if (dept.isEmpty()) {
            db.collection("attendance")
        } else {
            db.collection("attendance")
                .whereEqualTo("department", dept)
        }
        val listener = query.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                realStudents = snapshot.documents.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.apply { put("uid", doc.id) }
                }.filter { (it["name"] as? String)?.isNotEmpty() == true }
                isLoading = false
            }
        }
        onDispose { listener.remove() }
    }

    val presentCount = realStudents.count { it["status"] == "present" }
    val totalCount = realStudents.size
    val rate = if (totalCount > 0) (presentCount * 100) / totalCount else 0

    if (selectedStudent != null) {
        val student = selectedStudent!!
        val name = student["name"] as? String ?: "Student"
        val lat = student["latitude"] as? Double ?: 19.0760
        val lng = student["longitude"] as? Double ?: 72.8777
        val present = student["status"] as? String == "present"
        val zone = student["currentMainZone"] as? String
            ?: student["currentZone"] as? String ?: "Outside"
        val studentLatLng = LatLng(lat, lng)
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(studentLatLng, 17f)
        }
        var zones by remember { mutableStateOf<List<AttendanceZone>>(emptyList()) }
        LaunchedEffect(Unit) {
            try { zones = loadZonesFromFirestore(db) } catch (e: Exception) { }
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
                                .background(Color(0x229B59FF))
                                .clickable { selectedStudent = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("←", color = Color(0xFF9B59FF), fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text("${getZoneEmoji(zone)} $zone", color = Color(0xFFB0B0CC), fontSize = 12.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (present) Color(0x2200FF9F) else Color(0x22FF4B6E))
                                .border(1.dp, if (present) Color(0xFF00FF9F) else Color(0xFFFF4B6E), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                if (present) "✅ Present" else "❌ Absent",
                                color = if (present) Color(0xFF00FF9F) else Color(0xFFFF4B6E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
                        zones.forEach { z ->
                            val zoneColor = getZoneColor(z.name)
                            if (z.type == "circle") {
                                Circle(center = z.location, radius = z.radius,
                                    fillColor = zoneColor.copy(alpha = if (z.isMainZone) 0.2f else 0.08f),
                                    strokeColor = zoneColor, strokeWidth = if (z.isMainZone) 4f else 1.5f)
                            } else if (z.polygon.size >= 3) {
                                Polygon(points = z.polygon,
                                    fillColor = zoneColor.copy(alpha = if (z.isMainZone) 0.2f else 0.08f),
                                    strokeColor = zoneColor, strokeWidth = if (z.isMainZone) 4f else 1.5f)
                            }
                        }
                        Marker(state = MarkerState(position = studentLatLng),
                            title = name, snippet = "${getZoneEmoji(zone)} $zone",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (present) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED))
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF0D0D2B)).padding(16.dp)) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(getZoneEmoji(zone), fontSize = 36.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                Text("Location: $zone", color = if (present) Color(0xFF00FF9F) else Color(0xFFFF4B6E), fontWeight = FontWeight.SemiBold)
                                Text("📍 ${"%.4f".format(lat)}, ${"%.4f".format(lng)}", fontSize = 11.sp, color = Color(0xFFB0B0CC))
                            }
                        }
                    }
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050510))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF0D0D2B)).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape)
                            .background(Color(0x229B59FF))
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", color = Color(0xFF9B59FF), fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("View Attendance", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "🔴 Live • ${TeacherSession.department.ifEmpty { "All Departments" }}",
                            color = Color(0xFFB0B0CC), fontSize = 12.sp
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF9B59FF))
                }
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassCard(modifier = Modifier.weight(1f)) {
                            Text("$presentCount", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00FF9F))
                            Text("Present", fontSize = 11.sp, color = Color(0xFFB0B0CC))
                        }
                        GlassCard(modifier = Modifier.weight(1f)) {
                            Text("${totalCount - presentCount}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF4B6E))
                            Text("Absent", fontSize = 11.sp, color = Color(0xFFB0B0CC))
                        }
                        GlassCard(modifier = Modifier.weight(1f)) {
                            Text("$rate%", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00D4FF))
                            Text("Rate", fontSize = 11.sp, color = Color(0xFFB0B0CC))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Student List", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (realStudents.isEmpty()) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📭", fontSize = 40.sp)
                                Text("No attendance records found", color = Color(0xFFB0B0CC), fontSize = 14.sp)
                            }
                        }
                    } else {
                        realStudents.forEach { student ->
                            val name = student["name"] as? String ?: "Student"
                            val status = student["status"] as? String ?: "absent"
                            val present = status == "present"
                            val zone = student["currentMainZone"] as? String ?: student["currentZone"] as? String ?: "Outside"
                            val timestamp = student["timestamp"] as? Long

                            GlassCard(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    .clickable { selectedStudent = student }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(42.dp).clip(CircleShape)
                                            .background(if (present) Color(0x2200FF9F) else Color(0x22FF4B6E))
                                            .border(1.5.dp, if (present) Color(0xFF00FF9F) else Color(0xFFFF4B6E), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (name.isNotEmpty()) name.first().uppercase() else "?",
                                            color = if (present) Color(0xFF00FF9F) else Color(0xFFFF4B6E),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                                        Text("${getZoneEmoji(zone)} $zone", color = Color(0xFFB0B0CC), fontSize = 12.sp)
                                        if (timestamp != null) {
                                            Text("🕐 ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))}", color = Color(0xFFB0B0CC), fontSize = 11.sp)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            if (present) "✓ Present" else "✗ Absent",
                                            color = if (present) Color(0xFF00FF9F) else Color(0xFFFF4B6E),
                                            fontWeight = FontWeight.Bold, fontSize = 12.sp
                                        )
                                        Text("📍 tap", color = Color(0xFF9B59FF), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── STUDENT DAILY RECORDS SCREEN ─────────────────────────
@Composable
fun StudentDailyRecordsScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var allStudents by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedStudentUid by remember { mutableStateOf<String?>(null) }
    var selectedStudentName by remember { mutableStateOf("") }
    var dailyRecords by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoadingStudents by remember { mutableStateOf(true) }
    var isLoadingRecords by remember { mutableStateOf(false) }
    val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // ── Department filter ─────────────────────────────────
    DisposableEffect(Unit) {
        val dept = TeacherSession.department
        val query = if (dept.isEmpty()) {
            db.collection("users").whereEqualTo("role", "student")
        } else {
            db.collection("users")
                .whereEqualTo("role", "student")
                .whereEqualTo("department", dept)
        }
        val listener = query.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                allStudents = snapshot.documents.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.apply { put("uid", doc.id) }
                }.filter { (it["name"] as? String)?.isNotEmpty() == true }
                isLoadingStudents = false
            }
        }
        onDispose { listener.remove() }
    }

    DisposableEffect(selectedStudentUid) {
        val uid = selectedStudentUid ?: return@DisposableEffect onDispose { }
        isLoadingRecords = true
        val listener = db.collection("attendance_history")
            .whereEqualTo("uid", uid)
            .whereEqualTo("date", dateKey)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    dailyRecords = snapshot.documents.mapNotNull { it.data }
                        .sortedBy { it["timestamp"] as? Long ?: 0L }
                    isLoadingRecords = false
                }
            }
        onDispose { listener.remove() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050510))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF0D0D2B)).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape)
                            .background(Color(0x229B59FF))
                            .clickable {
                                if (selectedStudentUid != null) selectedStudentUid = null
                                else navController.popBackStack()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", color = Color(0xFF9B59FF), fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            if (selectedStudentUid == null) "Student Daily Records" else selectedStudentName,
                            color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                        )
                        Text(dateKey, color = Color(0xFF9B59FF), fontSize = 12.sp)
                    }
                }
            }

            if (selectedStudentUid == null) {
                if (isLoadingStudents) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF9B59FF))
                    }
                } else if (allStudents.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GlassCard {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📭", fontSize = 48.sp)
                                Text("No students found", color = Color(0xFFB0B0CC))
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                        Text("Select student to view today's activity:", color = Color(0xFFB0B0CC), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        allStudents.forEach { student ->
                            val name = student["name"] as? String ?: "Student"
                            val dept = student["department"] as? String ?: ""
                            val uid = student["uid"] as? String ?: ""
                            GlassCard(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    .clickable { selectedStudentUid = uid; selectedStudentName = name }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(44.dp).clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(Color(0xFF9B59FF), Color(0xFF00D4FF)))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (name.isNotEmpty()) name.first().uppercase() else "?",
                                            color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                        Text(dept, color = Color(0xFFB0B0CC), fontSize = 12.sp)
                                    }
                                    Text("📅 →", color = Color(0xFF9B59FF))
                                }
                            }
                        }
                    }
                }
            } else {
                if (isLoadingRecords) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF9B59FF))
                    }
                } else if (dailyRecords.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GlassCard {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📭", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No activity today for $selectedStudentName", color = Color(0xFFB0B0CC), fontSize = 14.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                        Text("Timeline", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        dailyRecords.forEach { record ->
                            val zone = record["zone"]?.toString() ?: "Unknown"
                            val time = record["time"]?.toString() ?: ""
                            val status = record["status"]?.toString() ?: ""
                            GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(getZoneEmoji(zone), fontSize = 26.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(zone, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                        Text(
                                            if (status == "present") "✅ Present" else "❌ Absent",
                                            fontSize = 12.sp,
                                            color = if (status == "present") Color(0xFF00FF9F) else Color(0xFFFF4B6E)
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
}

// ── STUDENT TRACKING SCREEN ──────────────────────────────
@Composable
fun StudentTrackingScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var isSatellite by remember { mutableStateOf(false) }
    var liveStudents by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var zones by remember { mutableStateOf<List<AttendanceZone>>(emptyList()) }
    val prevMainZoneMap = remember { mutableMapOf<String, String>() }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(19.0760, 72.8777), 15f)
    }

    DisposableEffect(Unit) {
        val zonesListener = db.collection("zones")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    zones = snapshot.documents.mapNotNull { doc ->
                        val lat = doc.getDouble("latitude") ?: return@mapNotNull null
                        val lng = doc.getDouble("longitude") ?: return@mapNotNull null
                        val r = doc.getDouble("radius") ?: 100.0
                        val n = doc.getString("name") ?: doc.id
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
                        AttendanceZone(doc.id, n, LatLng(lat, lng), r, type, polygon, isMainZone)
                    }
                    if (zones.isNotEmpty()) {
                        cameraPositionState.position =
                            CameraPosition.fromLatLngZoom(zones[0].location, 15f)
                    }
                }
            }

        // ── Department filter for live students ───────────
        val dept = TeacherSession.department
        val studentsQuery = if (dept.isEmpty()) {
            db.collection("live_locations")
        } else {
            db.collection("live_locations").whereEqualTo("department", dept)
        }

        val studentsListener = studentsQuery.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val newList = snapshot.documents.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.apply { put("id", doc.id) }
                }.filter { (it["name"] as? String)?.isNotEmpty() == true }

                val isAttendanceTime = isWithinAttendanceTime(11, 17)
                if (isAttendanceTime) {
                    newList.forEach { newStudent ->
                        val id = newStudent["id"] as? String ?: return@forEach
                        val name = newStudent["name"] as? String ?: "Student"
                        val newMainZone = newStudent["currentMainZone"] as? String ?: "Outside"
                        val timestamp = newStudent["timestamp"] as? Long ?: 0L
                        val now = System.currentTimeMillis()
                        val isRecent = (now - timestamp) < 30000
                        if (isRecent) {
                            val oldMainZone = prevMainZoneMap[id] ?: "Outside"
                            if (newMainZone != oldMainZone) {
                                when {
                                    oldMainZone == "Outside" && newMainZone != "Outside" ->
                                        sendNotification(context, "✅ $name Arrived!", "$name entered ${getZoneEmoji(newMainZone)} $newMainZone")
                                    oldMainZone != "Outside" && newMainZone == "Outside" ->
                                        sendNotification(context, "❌ $name Left!", "$name left ${getZoneEmoji(oldMainZone)} $oldMainZone")
                                    else ->
                                        sendNotification(context, "📍 $name Moved", "$name now in ${getZoneEmoji(newMainZone)} $newMainZone")
                                }
                                prevMainZoneMap[id] = newMainZone
                            }
                        }
                    }
                }
                liveStudents = newList
            }
        }
        onDispose {
            zonesListener.remove()
            studentsListener.remove()
        }
    }

    val presentCount = liveStudents.count { it["isPresent"] as? Boolean == true }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050510))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF0D0D2B)).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape)
                            .background(Color(0x229B59FF))
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", color = Color(0xFF9B59FF), fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Student Tracking", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("🔴 Live • $presentCount/${liveStudents.size} Present", color = Color(0xFFB0B0CC), fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isSatellite) "🛰️" else "🗺️", fontSize = 18.sp)
                        Switch(
                            checked = isSatellite,
                            onCheckedChange = { isSatellite = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF9B59FF)
                            )
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(mapType = if (isSatellite) MapType.SATELLITE else MapType.NORMAL)
                ) {
                    zones.forEach { zone ->
                        val zoneColor = getZoneColor(zone.name)
                        if (zone.type == "circle") {
                            Circle(center = zone.location, radius = zone.radius,
                                fillColor = zoneColor.copy(alpha = if (zone.isMainZone) 0.2f else 0.08f),
                                strokeColor = zoneColor, strokeWidth = if (zone.isMainZone) 4f else 1.5f)
                        } else if (zone.polygon.size >= 3) {
                            Polygon(points = zone.polygon,
                                fillColor = zoneColor.copy(alpha = if (zone.isMainZone) 0.2f else 0.08f),
                                strokeColor = zoneColor, strokeWidth = if (zone.isMainZone) 4f else 1.5f)
                        }
                        Marker(state = MarkerState(position = zone.location), title = zone.name + if (zone.isMainZone) " 🎯" else "")
                    }
                    liveStudents.forEach { student ->
                        val lat = student["latitude"] as? Double ?: return@forEach
                        val lng = student["longitude"] as? Double ?: return@forEach
                        val name = student["name"] as? String ?: "Student"
                        val present = student["isPresent"] as? Boolean ?: false
                        val zone = student["currentMainZone"] as? String ?: student["currentZone"] as? String ?: "Outside"
                        Marker(state = MarkerState(position = LatLng(lat, lng)),
                            title = name, snippet = "${getZoneEmoji(zone)} $zone",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                if (present) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_RED))
                    }
                }
                Box(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                        .clip(RoundedCornerShape(20.dp)).background(Color(0xFFFF4B6E))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("🔴 LIVE", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }
        }
    }
}

// ── HELPER FUNCTIONS ─────────────────────────────────────
fun getZoneEmoji(zone: String): String = when (zone) {
    "Ground" -> "⚽"
    "Department" -> "🏫"
    "Library" -> "📚"
    "Sardar Patel Hall" -> "🏛️"
    "Cafeteria" -> "☕"
    "Main Zone" -> "🎯"
    else -> "❌"
}

fun getZoneColor(zone: String): Color = when (zone) {
    "Ground" -> Color(0xFF2196F3)
    "Department" -> Color(0xFF9C27B0)
    "Library" -> Color(0xFF4CAF50)
    "Sardar Patel Hall" -> Color(0xFFFF5722)
    "Cafeteria" -> Color(0xFF795548)
    "Main Zone" -> Color(0xFF1565C0)
    else -> Color(0xFF6A1B9A)
}

fun getZoneBgColor(zone: String): Color = when (zone) {
    "Ground" -> Color(0xFF0D1B2A)
    "Department" -> Color(0xFF1A0D2E)
    "Library" -> Color(0xFF0D2A1A)
    "Sardar Patel Hall" -> Color(0xFF2A1A0D)
    "Cafeteria" -> Color(0xFF1A140D)
    "Main Zone" -> Color(0xFF0D1B2A)
    else -> Color(0xFF1A0D0D)
}

// ── APPROVE STUDENTS SCREEN ──────────────────────────────
@Composable
fun ApproveStudentsScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    var pendingStudents by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }

    // ── Department filter ─────────────────────────────────
    DisposableEffect(Unit) {
        val dept = TeacherSession.department
        val query = if (dept.isEmpty()) {
            db.collection("users")
                .whereEqualTo("role", "student")
                .whereEqualTo("approvalStatus", "pending")
        } else {
            db.collection("users")
                .whereEqualTo("role", "student")
                .whereEqualTo("approvalStatus", "pending")
                .whereEqualTo("department", dept)
        }
        val listener = query.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                pendingStudents = snapshot.documents.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.apply { put("uid", doc.id) }
                }
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
                    colors = listOf(Color(0x449B59FF), Color.Transparent), radius = 350f
                ), radius = 350f, center = Offset(size.width * 0.9f, size.height * 0.1f)
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {

            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF0D0D2B)).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape)
                            .background(Color(0x229B59FF))
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", color = Color(0xFF9B59FF), fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Approve Students", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "${pendingStudents.size} pending • ${TeacherSession.department.ifEmpty { "All Departments" }}",
                            color = Color(0xFFFF4B6E), fontSize = 12.sp
                        )
                    }
                }
            }

            if (message.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(if (message.startsWith("✅")) Color(0x2200FF9F) else Color(0x22FF4B6E))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = message,
                        color = if (message.startsWith("✅")) Color(0xFF00FF9F) else Color(0xFFFF4B6E),
                        fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF9B59FF))
                }
            } else if (pendingStudents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GlassCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✅", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No pending approvals!", color = Color(0xFF00FF9F), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("All students are approved", color = Color(0xFFB0B0CC), fontSize = 13.sp)
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Text("Pending Requests", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(12.dp))

                    pendingStudents.forEach { student ->
                        val uid = student["uid"] as? String ?: return@forEach
                        val name = student["name"] as? String ?: "Student"
                        val email = student["email"] as? String ?: ""
                        val dept = student["department"] as? String ?: ""
                        val grNumber = student["grNumber"] as? String ?: ""
                        val prnNumber = student["prnNumber"] as? String ?: ""
                        val studentClass = student["class"] as? String ?: ""
                        val studentId = student["studentId"] as? String ?: ""

                        GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(50.dp).clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(Color(0xFF9B59FF), Color(0xFF00D4FF)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (name.isNotEmpty()) name.first().uppercase() else "?",
                                        color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
                                    Text(email, color = Color(0xFF00D4FF), fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0x22FFFFFF))
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Department", color = Color(0xFFB0B0CC), fontSize = 11.sp)
                                    Text(dept, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Class", color = Color(0xFFB0B0CC), fontSize = 11.sp)
                                    Text(studentClass, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Reg. No.", color = Color(0xFFB0B0CC), fontSize = 11.sp)
                                    Text(studentId, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (grNumber.isNotEmpty() || prnNumber.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("GR Number", color = Color(0xFFB0B0CC), fontSize = 11.sp)
                                        Text(grNumber, color = Color(0xFF00D4FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("PRN Number", color = Color(0xFFB0B0CC), fontSize = 11.sp)
                                        Text(prnNumber, color = Color(0xFF9B59FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50.dp))
                                        .background(Color(0x22FF4B6E))
                                        .border(1.dp, Color(0xFFFF4B6E), RoundedCornerShape(50.dp))
                                        .clickable {
                                            db.collection("users").document(uid)
                                                .update("approvalStatus", "rejected")
                                                .addOnSuccessListener { message = "❌ $name rejected!" }
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("REJECT", color = Color(0xFFFF4B6E), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 1.sp)
                                }

                                Box(
                                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50.dp))
                                        .background(Brush.horizontalGradient(listOf(Color(0xFF00D4FF), Color(0xFF9B59FF))))
                                        .clickable {
                                            db.collection("users").document(uid)
                                                .update("approvalStatus", "approved")
                                                .addOnSuccessListener { message = "✅ $name approved!" }
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("APPROVE", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 1.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

