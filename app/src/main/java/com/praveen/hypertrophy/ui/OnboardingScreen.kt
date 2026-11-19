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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.praveen.hypertrophy.OnboardingProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    isLoading: Boolean,
    thinkingStatus: String = "",
    initialApiKey: String = "",
    onComplete: (OnboardingProfile, String) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 7

    // API Key State
    var apiKeyText by remember { mutableStateOf(initialApiKey) }

    // Biometrics State
    var selectedGender by remember { mutableStateOf("Male") }
    var ageText by remember { mutableStateOf("25") }
    var heightCmText by remember { mutableStateOf("175") }
    var weightKgText by remember { mutableStateOf("75") }

    // Goal & Fat Loss State
    var selectedGoal by remember { mutableStateOf("Hypertrophy") }
    var targetWeightText by remember { mutableStateOf("70") }
    val targetFatLossAreas = remember { mutableStateListOf<String>("Belly & Deep Core") }

    // Training & Safety State
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
                            text = "Intake & Body Blueprint",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "STEP ${step + 1} OF $totalSteps",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF8E95A5),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Step Progress Pill Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    for (i in 0 until totalSteps) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (i <= step) Color(0xFFE2F952) else Color(0xFF1E2024))
                        )
                    }
                }

                Spacer(Modifier.height(26.dp))

                // Step 0: Anthropometrics (Age, Height, Weight)
                when (step) {
                    0 -> {
                        Text(
                            text = "Your Physical Profile",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gemini uses your biometrics to calculate metabolic recovery, lever arm physics, and joint strain thresholds.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E95A5),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        // Gender Selector
                        Text("Gender (for metabolic & recovery baselines):", style = MaterialTheme.typography.labelMedium, color = Color(0xFFE2F952), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Male", "Female", "Prefer not to say").forEach { g ->
                                val isSel = selectedGender == g
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) Color(0xFFE2F952) else Color(0xFF1E2024))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSel) Color(0xFFE2F952) else Color(0xFF2D3139),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedGender = g },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = g,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color(0xFF0E0E10) else Color.White
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Age Field
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Age (years)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF141519),
                                unfocusedContainerColor = Color(0xFF141519),
                                focusedBorderColor = Color(0xFFE2F952),
                                unfocusedBorderColor = Color(0xFF22252C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        // Height Field (with approx feet/inches hint)
                        val hVal = heightCmText.toFloatOrNull() ?: 175f
                        val totalInches = (hVal / 2.54f).toInt()
                        val feet = totalInches / 12
                        val inches = totalInches % 12

                        OutlinedTextField(
                            value = heightCmText,
                            onValueChange = { heightCmText = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Height (cm)") },
                            supportingText = { Text("≈ $feet ft $inches in", color = Color(0xFF8E95A5)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF141519),
                                unfocusedContainerColor = Color(0xFF141519),
                                focusedBorderColor = Color(0xFFE2F952),
                                unfocusedBorderColor = Color(0xFF22252C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(Modifier.height(8.dp))

                        // Current Weight Field (with approx lbs hint)
                        val wVal = weightKgText.toFloatOrNull() ?: 75f
                        val lbs = (wVal * 2.20462f).toInt()

                        OutlinedTextField(
                            value = weightKgText,
                            onValueChange = { weightKgText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                            label = { Text("Current Weight (kg)") },
                            supportingText = { Text("≈ $lbs lbs", color = Color(0xFF8E95A5)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF141519),
                                unfocusedContainerColor = Color(0xFF141519),
                                focusedBorderColor = Color(0xFFE2F952),
                                unfocusedBorderColor = Color(0xFF22252C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }

                    // Step 1: Primary Goal Selection
                    1 -> {
                        Text(
                            text = "Primary Transformation Goal",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "How would you like to direct your stimulus and energy expenditure?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E95A5),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        listOf(
                            "Hypertrophy" to "Maximal Muscle Hypertrophy & Density (Surplus / Maintenance)",
                            "Fat Loss & Lean Toning" to "Drop Body Fat, Tighten Regional Muscle & Retain Mass",
                            "Body Recomposition" to "Simultaneous Fat Oxidation & Lean Muscle Gain",
                            "Strength & Power" to "Peak Force Output, Neurological Drive & Low RPE"
                        ).forEach { (goal, desc) ->
                            val isSel = selectedGoal == goal
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
                                    .clickable { selectedGoal = goal }
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = goal,
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

                    // Step 2: Weight Loss Target & Targeted Fat Loss Zones
                    2 -> {
                        Text(
                            text = "Targeted Weight Loss & Toning Zones",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Specify your target goal weight and select the body regions to prioritize for metabolic conditioning and muscle tightening.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E95A5),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = targetWeightText,
                            onValueChange = { targetWeightText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                            label = { Text("Target Goal Weight (kg)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF141519),
                                unfocusedContainerColor = Color(0xFF141519),
                                focusedBorderColor = Color(0xFFE2F952),
                                unfocusedBorderColor = Color(0xFF22252C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Targeted Fat Loss & Toning Areas:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFE2F952),
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Belly & Deep Core",
                                "Love Handles & Lower Back",
                                "Chest / Upper Body",
                                "Thighs & Glutes",
                                "Arms & Shoulders",
                                "Full Body Overall"
                            ).forEach { area ->
                                val isSelected = targetFatLossAreas.contains(area)
                                SurveyChip(
                                    label = area,
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) targetFatLossAreas.remove(area)
                                        else targetFatLossAreas.add(area)
                                    }
                                )
                            }
                        }
                    }

                    // Step 3: Joint Health & Injury Shield
                    3 -> {
                        Text(
                            text = "Joint Health & Injury Shield",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select any areas with prior strain or sensitivity. Gemini will adapt exercise biomechanics to protect these joints.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E95A5),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Spacer(Modifier.height(10.dp))
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

                    // Step 4: Muscle Hypertrophy Weak Points
                    4 -> {
                        Text(
                            text = "Priority Muscle Groups",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select muscles where you want specialized progressive overload ladders.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E95A5),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Spacer(Modifier.height(10.dp))
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

                    // Step 5: Frequency & Equipment
                    5 -> {
                        Text(
                            text = "Schedule & Equipment",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))

                        Text("Days Per Week", style = MaterialTheme.typography.labelMedium, color = Color(0xFF8E95A5))
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(3, 4, 5, 6).forEach { d ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (daysPerWeek == d) Color(0xFFE2F952) else Color(0xFF1E2024))
                                        .clickable { daysPerWeek = d },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$d DAYS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (daysPerWeek == d) Color(0xFF0E0E10) else Color.White
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text("Session Duration", style = MaterialTheme.typography.labelMedium, color = Color(0xFF8E95A5))
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(15, 30, 45).forEach { m ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (sessionBudgetMins == m) Color(0xFFE2F952) else Color(0xFF1E2024))
                                        .clickable { sessionBudgetMins = m },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$m MIN",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sessionBudgetMins == m) Color(0xFF0E0E10) else Color.White
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text("Available Equipment (Text Input)", style = MaterialTheme.typography.labelMedium, color = Color(0xFFE2F952), fontWeight = FontWeight.Bold)
                        Text(
                            text = "Type your exact gym or home gear (e.g. 10kg dumbbells, pull-up bar, flat bench) or use quick tags below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8E95A5),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(Modifier.height(4.dp))

                        OutlinedTextField(
                            value = equipmentType,
                            onValueChange = { equipmentType = it },
                            placeholder = { Text("e.g. Pair of 12kg hex dumbbells, pull-up bar, resistance bands, adjustable incline bench", color = Color(0xFF6A7080)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF141519),
                                unfocusedContainerColor = Color(0xFF141519),
                                focusedBorderColor = Color(0xFFE2F952),
                                unfocusedBorderColor = Color(0xFF22252C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(Modifier.height(8.dp))

                        // Quick-Add Tag Chips
                        Text("Quick Add Gear:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E95A5))
                        Spacer(Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "+ Dumbbells",
                                "+ Barbell & Plates",
                                "+ Incline Bench",
                                "+ Pull-Up Bar",
                                "+ Resistance Bands",
                                "+ Power Rack",
                                "+ Cable Machine",
                                "+ Bodyweight Only"
                            ).forEach { tag ->
                                val cleanTag = tag.removePrefix("+ ")
                                val isPresent = equipmentType.contains(cleanTag, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isPresent) Color(0xFFE2F952).copy(alpha = 0.2f) else Color(0xFF1E2024))
                                        .border(
                                            width = 1.dp,
                                            color = if (isPresent) Color(0xFFE2F952) else Color(0xFF2D3139),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            equipmentType = if (equipmentType.isBlank() || equipmentType == "Full Gym") {
                                                cleanTag
                                            } else if (!isPresent) {
                                                "$equipmentType, $cleanTag"
                                            } else {
                                                equipmentType
                                            }
                                        }
                                        .padding(horizontal = 9.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isPresent) Color(0xFFE2F952) else Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // 1-Tap Complete Setups
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Commercial Gym" to "Full Commercial Gym (Barbells, Dumbbells, Benches, Cables, Racks)",
                                "Home Dumbbells" to "Home Gym: Adjustable Dumbbells, Incline Bench & Pull-Up Bar",
                                "Bodyweight" to "Bodyweight & Calisthenics: Push-up Station, Floor Mat"
                            ).forEach { (presetName, presetDesc) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF141519))
                                        .border(1.dp, Color(0xFF22252C), RoundedCornerShape(8.dp))
                                        .clickable { equipmentType = presetDesc }
                                        .padding(vertical = 8.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = presetName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Step 6: Custom Priority Prompt & Gemini API Key
                    6 -> {
                        Text(
                            text = "AI Engine & Custom Priority",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Provide your Gemini API Key so Gemini 4 Hydrogen can synthesize truly dynamic exercises, equipment kinematics, and next-day tuning.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF8E95A5),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Gemini API Key (Gemini 4 Hydrogen)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFE2F952),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = apiKeyText,
                            onValueChange = { apiKeyText = it },
                            placeholder = { Text("AIzaSy...", color = Color(0xFF6A7080)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF16171B),
                                unfocusedContainerColor = Color(0xFF141519),
                                focusedBorderColor = Color(0xFFE2F952),
                                unfocusedBorderColor = Color(0xFF22252C),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = "Custom Training Priorities / Bio-Constraints",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))

                        OutlinedTextField(
                            value = customPriorityText,
                            onValueChange = { customPriorityText = it },
                            placeholder = {
                                Text(
                                    "e.g. Focus on burning stubborn belly fat while maintaining upper chest and bicep size, no heavy spinal loading.",
                                    color = Color(0xFF6A7080),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
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

            // Bottom Buttons
            Column(modifier = Modifier.padding(top = 24.dp)) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E2024))
                            .border(1.dp, Color(0xFFE2F952).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFFE2F952),
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = if (thinkingStatus.isNotBlank()) thinkingStatus else "Thinking: Synthesizing Gemini 4 Hydrogen blueprint...",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE2F952),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                                    if (step < totalSteps - 1) {
                                        step++
                                    } else {
                                        onComplete(
                                            OnboardingProfile(
                                                gender = selectedGender,
                                                age = ageText.toIntOrNull() ?: 25,
                                                heightCm = heightCmText.toFloatOrNull() ?: 175f,
                                                currentWeightKg = weightKgText.toFloatOrNull() ?: 75f,
                                                goalType = selectedGoal,
                                                targetWeightKg = targetWeightText.toFloatOrNull(),
                                                targetFatLossAreas = targetFatLossAreas.toList(),
                                                jointPainAreas = jointPainAreas.toList(),
                                                priorityMuscles = priorityMuscles.toList(),
                                                daysPerWeek = daysPerWeek,
                                                sessionBudgetMins = sessionBudgetMins,
                                                equipmentType = equipmentType,
                                                customPriorityText = customPriorityText
                                            ),
                                            apiKeyText.trim()
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (step < totalSteps - 1) "CONTINUE →" else "⚡ GENERATE HYPERTROPHY PROGRAM",
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
