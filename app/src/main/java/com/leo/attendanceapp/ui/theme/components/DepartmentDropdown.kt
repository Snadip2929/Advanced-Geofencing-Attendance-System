package com.leo.attendanceapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonDepartmentDropdown(
    selectedValue: String,
    onValueChange: (String) -> Unit
) {
    val departments = listOf(
        "Computer Engineering",
        "Information Technology",
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
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
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