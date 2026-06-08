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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leo.attendanceapp.ui.components.GlassCard

@Composable
fun StudentDashboardScreen(
    studentName: String,
    currentZone: String,
    currentMainZone: String,
    attendanceStatus: String,
    attendancePercent: Int,
    gpsAccuracy: Float,
    onLocationClick: () -> Unit,
    onAttendanceClick: () -> Unit,
    onZoneRecordClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF050510),
                        Color(0xFF0A0A1F),
                        Color(0xFF0D0D2B)
                    )
                )
            )
    ) {

        // ── Glow Background ──────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x3300D4FF),
                        Color.Transparent
                    ),
                    radius = 400f
                ),
                radius = 400f,
                center = Offset(size.width * 0.8f, size.height * 0.1f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x229B59FF),
                        Color.Transparent
                    ),
                    radius = 300f
                ),
                radius = 300f,
                center = Offset(size.width * 0.1f, size.height * 0.6f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(modifier = Modifier.height(48.dp))

            // ── Header ───────────────────────────────────────
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
                        text = studentName,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Notification Icon
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x1500D4FF))
                            .clickable { onNotificationClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF00D4FF),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Profile Icon
                    Box(
                        modifier = Modifier
                            .size(42.dp)
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
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Status Card ───────────────────────────────────
            GlassCard(modifier = Modifier.fillMaxWidth()) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status
                    Column {
                        Text(
                            text = "Attendance Status",
                            color = Color(0xFFB0B0CC),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (attendanceStatus == "Present")
                                            Color(0xFF00FF9F)
                                        else Color(0xFFFF4B6E)
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = attendanceStatus,
                                color = if (attendanceStatus == "Present")
                                    Color(0xFF00FF9F)
                                else Color(0xFFFF4B6E),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // GPS Accuracy
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "GPS Accuracy",
                            color = Color(0xFFB0B0CC),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${gpsAccuracy.toInt()}m",
                            color = Color(0xFF00D4FF),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0x22FFFFFF))
                Spacer(modifier = Modifier.height(16.dp))

                // Zone Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Current Zone",
                            color = Color(0xFFB0B0CC),
                            fontSize = 12.sp
                        )
                        Text(
                            text = currentZone,
                            color = Color(0xFF00D4FF),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Main Zone",
                            color = Color(0xFFB0B0CC),
                            fontSize = 12.sp
                        )
                        Text(
                            text = currentMainZone,
                            color = Color(0xFF9B59FF),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Attendance Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Attendance",
                        color = Color(0xFFB0B0CC),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "$attendancePercent%",
                        color = if (attendancePercent >= 75)
                            Color(0xFF00FF9F)
                        else Color(0xFFFF4B6E),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { attendancePercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (attendancePercent >= 75)
                        Color(0xFF00FF9F)
                    else Color(0xFFFF4B6E),
                    trackColor = Color(0x22FFFFFF)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Quick Access ──────────────────────────────────
            Text(
                text = "Quick Access",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // My Location
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onLocationClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF00D4FF),
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "My Location",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "GPS Map",
                        color = Color(0xFFB0B0CC),
                        fontSize = 11.sp
                    )
                }

                // Attendance Status
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onAttendanceClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF9B59FF),
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Attendance",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Status",
                        color = Color(0xFFB0B0CC),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Zone Record
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onZoneRecordClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF00FF9F),
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Zone Record",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Daily History",
                        color = Color(0xFFB0B0CC),
                        fontSize = 11.sp
                    )
                }

                // Logout
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onLogoutClick() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = Color(0xFFFF4B6E),
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Logout",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Sign Out",
                        color = Color(0xFFB0B0CC),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
