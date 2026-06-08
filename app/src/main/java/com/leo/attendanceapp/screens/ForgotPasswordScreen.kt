package com.leo.attendanceapp.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.leo.attendanceapp.ui.components.GlassCard
import com.leo.attendanceapp.ui.components.NeonButton
import com.leo.attendanceapp.ui.components.NeonTextField

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
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
                    colors = listOf(Color(0x5500D4FF), Color.Transparent),
                    radius = 350f
                ),
                radius = 350f,
                center = Offset(size.width * 0.1f, size.height * 0.2f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x559B59FF), Color.Transparent),
                    radius = 300f
                ),
                radius = 300f,
                center = Offset(size.width * 0.9f, size.height * 0.8f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x2200D4FF))
                        .clickable { navController.popBackStack() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "← Back",
                        color = Color(0xFF00D4FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF00D4FF),
                                Color(0xFF9B59FF)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🔑", fontSize = 46.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Forgot Password?",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your email to reset password",
                fontSize = 13.sp,
                color = Color(0xFFB0B0CC)
            )

            Spacer(modifier = Modifier.height(40.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {

                Text(
                    "Reset Password",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                NeonTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Student Email",
                    icon = Icons.Default.Email
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
                        text = "SEND RESET EMAIL",
                        onClick = {
                            if (email.isEmpty()) {
                                message = "❌ Please enter email"
                                return@NeonButton
                            }
                            isLoading = true
                            message = ""
                            auth.sendPasswordResetEmail(email)
                                .addOnSuccessListener {
                                    isLoading = false
                                    message = "✅ Reset email sent! Check inbox."
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
                    Text(
                        "Remember password? ",
                        color = Color(0xFFB0B0CC),
                        fontSize = 13.sp
                    )
                    Text(
                        "Login",
                        color = Color(0xFF9B59FF),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}