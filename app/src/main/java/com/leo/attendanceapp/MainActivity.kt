package com.leo.attendanceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.leo.attendanceapp.screens.*
import com.leo.attendanceapp.ui.theme.AttendanceAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AttendanceAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF050510)
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        composable("splash") {
                            SplashScreen(navController)
                        }
                        composable("student_register") {
                            StudentRegisterScreen(navController)
                        }
                        composable("student_login") {
                            StudentLoginScreen(navController)
                        }
                        composable("student_dashboard") {
                            StudentDashboardScreen(navController)
                        }
                        composable("attendance_status") {
                            AttendanceStatusScreen(navController)
                        }
                        composable("my_location") {
                            MyLocationScreen(navController)
                        }
                        composable("daily_record") {
                            DailyRecordScreen(navController)
                        }
                        composable("teacher_login") {
                            TeacherLoginScreen(navController)
                        }
                        composable("teacher_register") {
                            TeacherRegisterScreen(navController)
                        }
                        composable("teacher_dashboard") {
                            TeacherDashboardScreen(navController)
                        }
                        composable("manage_zones") {
                            ManageZonesScreen(navController)
                        }
                        composable("view_attendance") {
                            ViewAttendanceScreen(navController)
                        }
                        composable("student_tracking") {
                            StudentTrackingScreen(navController)
                        }
                        composable("student_daily_records") {
                            StudentDailyRecordsScreen(navController)
                        }
                        composable("forgot_password") {
                            ForgotPasswordScreen(navController)
                        }
                        composable("approve_students") {
                            ApproveStudentsScreen(navController)
                        }
                    }
                }
            }
        }
    }
}