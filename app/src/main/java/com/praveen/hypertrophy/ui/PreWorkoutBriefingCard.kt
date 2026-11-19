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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import com.praveen.hypertrophy.AnimBox
import com.praveen.hypertrophy.EX

/**
 * Pre-Workout Briefing Card showing required gym equipment checklist
 * and 1-tap interactive 3D setup posture preview modal.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreWorkoutBriefingCard(
    activeExerciseId: String,
    profile: com.praveen.hypertrophy.OnboardingProfile? = null,
    modifier: Modifier = Modifier
) {
    var showPreviewModal by remember { mutableStateOf(false) }
    val ex = EX[activeExerciseId]

    // Determine equipment checklist based on exercise
    val equipmentList = remember(activeExerciseId) {
        when (activeExerciseId) {
            "curl", "bicep", "db_curl" -> listOf(
                "🔩 Pair of Hex Dumbbells",
                "⚖️ 2.5 - 15 kg Progression",
                "📐 Standing Gym Floor Space"
            )
            "bench", "pushup", "db_bench" -> listOf(
                "🏋️ Olympic Barbell (20kg)",
                "📐 Flat Workout Bench",
                "⚖️ 2x 20kg + 2x 10kg Plates",
                "🛡️ Spring Collar Clamps"
            )
            "squat", "split_squat" -> listOf(
                "🏗️ Power Cage / Squat Rack",
                "🏋️ Olympic Barbell (20kg)",
                "🛡️ Safety Spotter Pins @ Hip Height",
                "⚖️ Full Olympic Bumper Plates"
            )
            "row" -> listOf(
                "🏋️ Barbell or Heavy Dumbbell",
                "📐 Flat/Incline Support Bench",
                "⚖️ Plates & Weight Collars"
            )
            "latraise" -> listOf(
                "🔩 Light-to-Moderate Dumbbells (4-12kg)",
                "📐 Floor Space / Mirror"
            )
            else -> listOf(
                "🏋️ Free Weight Equipment",
                "📐 Dedicated Floor Station"
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF16171B)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE2F952))
                    )
                    Text(
                        text = "TODAY'S EQUIPMENT & SETUP",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE2F952),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // 3D Preview CTA Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                        .clickable { showPreviewModal = true }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "👁️ 3D PREVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Equipment Checklist Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // If profile has goal/weight targets, display high-contrast transformation badge
                if (profile != null) {
                    val goalBadgeText = when {
                        profile.targetWeightKg != null -> "🎯 ${profile.goalType}: ${profile.currentWeightKg.toInt()}kg ➔ ${profile.targetWeightKg.toInt()}kg"
                        else -> "⚡ ${profile.goalType}"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE2F952).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFE2F952), RoundedCornerShape(8.dp))
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = goalBadgeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2F952),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (profile.targetFatLossAreas.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "🔥 Focus: ${profile.targetFatLossAreas.first()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                for (item in equipmentList) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2024))
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Interactive 3D Setup & Posture Preview Modal
    if (showPreviewModal) {
        Dialog(onDismissRequest = { showPreviewModal = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0E0E10),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "3D SETUP INSPECTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE2F952),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = ex?.name ?: activeExerciseId,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E2024))
                                .clickable { showPreviewModal = false }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "✕ CLOSE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Live 3D Canvas Player
                    AnimBox(id = activeExerciseId)

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Biomechanical Cue:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8E95A5)
                    )
                    Text(
                        text = ex?.cue ?: "Maintain spinal neutrality and control the eccentric tempo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFE2F952),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
