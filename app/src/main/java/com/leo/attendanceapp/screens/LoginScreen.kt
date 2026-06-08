package com.leo.attendanceapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.leo.attendanceapp.ui.components.GlassCard
import com.leo.attendanceapp.ui.components.NeonButton
import com.leo.attendanceapp.ui.components.NeonTextField

@Composable
fun StudentLoginScreen(navController: NavController) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050510))
    ) {

        // ── Glow Circles ─────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x5500D4FF),
                        Color.Transparent
                    ),
                    radius = 400f
                ),
                radius = 400f,
                center = Offset(size.width * 0.1f, size.height * 0.15f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x559B59FF),
                        Color.Transparent
                    ),
                    radius = 350f
                ),
                radius = 350f,
                center = Offset(size.width * 0.9f, size.height * 0.8f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x3300FF9F),
                        Color.Transparent
                    ),
                    radius = 200f
                ),
                radius = 200f,
                center = Offset(size.width * 0.5f, size.height * 0.5f)
            )
        }

        // ── Content ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(modifier = Modifier.height(60.dp))

            // ── Logo ──────────────────────────────────────────
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
                            colors = listOf(
                                Color(0xFF00D4FF),
                                Color(0xFF6C63FF),
                                Color(0xFF9B59FF)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(55.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Title ─────────────────────────────────────────
            Text(
                text = "Smart Attendance",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

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

            // ── Login Card ────────────────────────────────────
            GlassCard(modifier = Modifier.fillMaxWidth()) {

                Text(
                    text = "Student Login",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Enter your credentials to continue",
                    fontSize = 12.sp,
                    color = Color(0xFFB0B0CC)
                )

                Spacer(modifier = Modifier.height(28.dp))

                NeonTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "College Email",
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
                        .clickable {
                            navController.navigate("forgot_password")
                        }
                )

                Spacer(modifier = Modifier.height(28.dp))

                NeonButton(
                    text = "LOGIN",
                    onClick = {
                        navController.navigate("student_dashboard")
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Divider ───────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Color(0x44FFFFFF))
                    )
                    Text(
                        text = "  OR  ",
                        color = Color(0xFF666688),
                        fontSize = 12.sp
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Color(0x44FFFFFF))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Teacher Login ─────────────────────────────
                NeonButton(
                    text = "TEACHER LOGIN",
                    onClick = {
                        navController.navigate("teacher_login")
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Register ──────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "New Student? ",
                        color = Color(0xFFB0B0CC),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Register Here",
                        color = Color(0xFF9B59FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.clickable {
                            navController.navigate("student_register")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}