package com.leo.attendanceapp.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()

    // Animation
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Logo animation
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(500)
        )

        delay(2000)

        // Check login status
        val user = auth.currentUser
        if (user != null) {
            // Already logged in — check role
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    val role = doc.getString("role")
                    if (role == "teacher") {
                        navController.navigate("teacher_dashboard") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("student_dashboard") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
                .addOnFailureListener {
                    navController.navigate("student_login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
        } else {
            // Not logged in
            navController.navigate("student_login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510)),
        contentAlignment = Alignment.Center
    ) {
        // Glow circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x5500D4FF), Color.Transparent),
                    radius = 400f
                ),
                radius = 400f,
                center = Offset(size.width * 0.1f, size.height * 0.2f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x559B59FF), Color.Transparent),
                    radius = 350f
                ),
                radius = 350f,
                center = Offset(size.width * 0.9f, size.height * 0.8f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x3300FF9F), Color.Transparent),
                    radius = 250f
                ),
                radius = 250f,
                center = Offset(size.width * 0.5f, size.height * 0.5f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(scale.value)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF00D4FF),
                                Color(0xFF6C63FF),
                                Color(0xFF9B59FF)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("📍", fontSize = 60.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Name
            Text(
                text = "Smart Attendance",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "GPS GEOFENCING SYSTEM",
                fontSize = 12.sp,
                color = Color(0xFF00D4FF),
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "D. N. Patel College of Engineering",
                fontSize = 12.sp,
                color = Color(0xFFB0B0CC)
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Loading dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0, 200, 400).forEach { delay ->
                    val dotScale = remember { Animatable(0.6f) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(delay.toLong())
                        while (true) {
                            dotScale.animateTo(
                                1f,
                                animationSpec = tween(400)
                            )
                            dotScale.animateTo(
                                0.6f,
                                animationSpec = tween(400)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(dotScale.value)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF00D4FF),
                                        Color(0xFF9B59FF)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}