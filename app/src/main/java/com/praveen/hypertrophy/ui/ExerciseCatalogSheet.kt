package com.praveen.hypertrophy.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.praveen.hypertrophy.AnimBox
import com.praveen.hypertrophy.data.db.ExerciseEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCatalogSheet(
    exercises: List<ExerciseEntity>,
    onDismiss: () -> Unit,
    onSelectExercise: (ExerciseEntity) -> Unit,
    onGenerateWithAI: (prompt: String, muscle: String) -> Unit,
    isGenerating: Boolean = false,
) {
    var showAICreatorDialog by remember { mutableStateOf(false) }
    var selectedExerciseForPreview by remember { mutableStateOf<ExerciseEntity?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exercise Catalog (${exercises.size})",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                FloatingActionButton(
                    onClick = { showAICreatorDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, contentDescription = "AI Create")
                        Text("AI Create", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            if (isGenerating) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text("Hydrogen is designing exercise & animations...", Modifier.padding(start = 12.dp))
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(exercises, key = { it.id }) { ex ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (ex.isCustom) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedExerciseForPreview = ex }
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(ex.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                if (ex.isCustom) {
                                    Text("AI CREATED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text(
                                text = "Muscles: ${ex.primaryMuscles}" + if (ex.secondaryMuscles.isNotBlank()) " · Secondary: ${ex.secondaryMuscles}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Target: ${ex.loReps}–${ex.hiReps} reps · Rest: ${ex.restSec}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAICreatorDialog) {
        var promptText by remember { mutableStateOf("") }
        var targetMuscle by remember { mutableStateOf("chest") }
        AlertDialog(
            onDismissRequest = { showAICreatorDialog = false },
            title = { Text("AI Exercise Creator (Hydrogen)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Describe the movement, equipment, or stretch focus:")
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        placeholder = { Text("e.g. Incline dumbbell fly with 3s stretch") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetMuscle,
                        onValueChange = { targetMuscle = it },
                        label = { Text("Target Muscle (biceps, chest, quads, etc.)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (promptText.isNotBlank()) {
                            onGenerateWithAI(promptText, targetMuscle)
                            showAICreatorDialog = false
                        }
                    }
                ) { Text("Generate Movement") }
            },
            dismissButton = {
                TextButton(onClick = { showAICreatorDialog = false }) { Text("Cancel") }
            }
        )
    }

    selectedExerciseForPreview?.let { ex ->
        AlertDialog(
            onDismissRequest = { selectedExerciseForPreview = null },
            title = { Text(ex.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimBox(ex.id)
                    Text("Cue: ${ex.cue}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text("Rest: ${ex.restSec} seconds · Reps: ${ex.loReps}–${ex.hiReps}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSelectExercise(ex)
                    selectedExerciseForPreview = null
                }) { Text("Close") }
            }
        )
    }
}
