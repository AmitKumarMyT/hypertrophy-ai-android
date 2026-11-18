package com.praveen.hypertrophy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.praveen.hypertrophy.OnboardingProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    isLoading: Boolean,
    onComplete: (OnboardingProfile) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }

    // Survey Responses State
    val jointPainAreas = remember { mutableStateListOf<String>() }
    val priorityMuscles = remember { mutableStateListOf<String>() }
    var daysPerWeek by remember { mutableIntStateOf(4) }
    var sessionBudgetMins by remember { mutableIntStateOf(30) }
    var equipmentType by remember { mutableStateOf("Full Gym") }
    var customPriorityText by remember { mutableStateOf("") }

    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(safeInsets),
        color = Color(0xFF0E0E10)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Top Header & Step Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "HYPERTROPHY ARCHITECT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE2F952),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Initial Intake Survey",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "STEP ${step + 1} OF 6",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF8E95A5),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Step Progress Pill Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 0 until 6) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (i <= step) Color(0xFFE2F952) else Color(0xFF1E2024))
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Questionnaire Content by Step
                when (step) {
                    0 -> {
                        // Step 1: Joint Health & Injury Shield
                        Text(
                            text = "Joint Health & Injury Shield",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select any areas with prior strain or sensitivity. Gemini 4 Hydrogen will de-load stress on these joints.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E95A5),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("None", "Shoulders", "Lower Back", "Knees", "Elbows", "Wrists").forEach { area ->
                                val isSelected = jointPainAreas.contains(area) || (area == "None" && jointPainAreas.isEmpty())
                                SurveyChip(
                                    label = area,
                                    selected = isSelected,
                                    onClick = {
                                        if (area == "None") {
                                            jointPainAreas.clear()
                                        } else {
                                            if (jointPainAreas.contains(area)) jointPainAreas.remove(area)
                                            else jointPainAreas.add(area)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    1 -> {
                        // Step 2: Target Weak Points
                        Text(
                            text = "Priority Muscle Groups",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Which areas would you like to emphasize with extra volume and stimulus?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E95A5),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Upper Chest", "Biceps Peak", "Side Delts", "Lats / Back Width", "Hamstrings & Glutes", "Quads", "Triceps Long Head", "Abs / Core").forEach { muscle ->
                                val isSelected = priorityMuscles.contains(muscle)
                                SurveyChip(
                                    label = muscle,
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) priorityMuscles.remove(muscle)
                                        else priorityMuscles.add(muscle)
                                    }
                                )
                            }
                        }
                    }

                    2 -> {
                        // Step 3: Weekly Training Frequency
                        Text(
                            text = "Training Frequency",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "How many days per week can you consistently train?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E95A5),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(3, 4, 5, 6).forEach { days ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (daysPerWeek == days) Color(0xFFE2F952) else Color(0xFF1E2024))
                                        .clickable { daysPerWeek = days }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$days",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (daysPerWeek == days) Color(0xFF0E0E10) else Color.White
                                        )
                                        Text(
                                            text = "DAYS",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (daysPerWeek == days) Color(0xFF0E0E10) else Color(0xFF8E95A5)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Step 4: Session Duration Budget
                        Text(
                            text = "Session Time Budget",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "How long would you like each training session to last?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E95A5),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        listOf(
                            15 to "Express Hypertrophy (High SFR, Short Rest)",
                            30 to "Optimal Standard (Full Volume & Recovery)",
                            45 to "Comprehensive (Max Volume & Specialized Ladders)"
                        ).forEach { (mins, desc) ->
                            val isSel = sessionBudgetMins == mins
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) Color(0xFF1E2024) else Color(0xFF141519))
                                    .border(
                                        width = if (isSel) 2.dp else 1.dp,
                                        color = if (isSel) Color(0xFFE2F952) else Color(0xFF22252C),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { sessionBudgetMins = mins }
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$mins MIN",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color(0xFFE2F952) else Color.White,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF8E95A5)
                                    )
                                }
                            }
                        }
                    }

                    4 -> {
                        // Step 5: Available Equipment Access
                        Text(
                            text = "Gym Equipment Access",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select what equipment is at your disposal for 3D modeling and exercise selection.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E95A5),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        listOf(
                            "Full Gym" to "Olympic Barbells, Benches, Power Racks & Cables",
                            "Home Dumbbells & Bench" to "Hex Dumbbells and Adjustable Workout Bench",
                            "Minimalist Bodyweight" to "Bodyweight, Push-up Bars and Pull-up Bar"
                        ).forEach { (type, desc) ->
                            val isSel = equipmentType == type
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) Color(0xFF1E2024) else Color(0xFF141519))
                                    .border(
                                        width = if (isSel) 2.dp else 1.dp,
                                        color = if (isSel) Color(0xFFE2F952) else Color(0xFF22252C),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { equipmentType = type }
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = type,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color(0xFFE2F952) else Color.White
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF8E95A5)
                                    )
                                }
                            }
                        }
                    }

                    5 -> {
                        // Step 6: Open Priority Free-Form Text Box
                        Text(
                            text = "Primary Goal & Custom Priority",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tell Gemini 4 Hydrogen exactly what you want to achieve or any specific constraints (only asked once).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E95A5),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = customPriorityText,
                            onValueChange = { customPriorityText = it },
                            placeholder = {
                                Text(
                                    "e.g. Build upper chest thickness and biceps peak, keep stress off lower back, and optimize for maximal progressive overload.",
                                    color = Color(0xFF6A7080),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF16171B),
                                unfocusedContainerColor = Color(0xFF141519),
                                focusedBorderColor = Color(0xFFE2F952),
                                unfocusedBorderColor = Color(0xFF22252C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }
            }

            // Bottom Navigation Buttons (Back & Next / Generate)
            Column(modifier = Modifier.padding(top = 24.dp)) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E2024)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFFE2F952),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "GEMINI 4 HYDROGEN SYNTHESIZING PROGRAM...",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2F952)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (step > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(0.35f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1E2024))
                                    .clickable { step-- },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "BACK",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(if (step > 0) 0.65f else 1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFE2F952))
                                .clickable {
                                    if (step < 5) {
                                        step++
                                    } else {
                                        onComplete(
                                            OnboardingProfile(
                                                jointPainAreas = jointPainAreas.toList(),
                                                priorityMuscles = priorityMuscles.toList(),
                                                daysPerWeek = daysPerWeek,
                                                sessionBudgetMins = sessionBudgetMins,
                                                equipmentType = equipmentType,
                                                customPriorityText = customPriorityText
                                            )
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (step < 5) "CONTINUE →" else "⚡ GENERATE HYPERTROPHY PROGRAM",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0E0E10)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurveyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFFE2F952) else Color(0xFF1E2024))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color(0xFF0E0E10) else Color.White
        )
    }
}
