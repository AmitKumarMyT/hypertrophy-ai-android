package com.praveen.hypertrophy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class KeyframeJoints(
    val headX: Float, val headY: Float,
    val shoulderX: Float, val shoulderY: Float,
    val elbowX: Float, val elbowY: Float,
    val wristX: Float, val wristY: Float,
    val hipX: Float, val hipY: Float,
    val kneeX: Float, val kneeY: Float,
    val ankleX: Float, val ankleY: Float,
    val dbX: Float? = null, val dbY: Float? = null,
)

@Serializable
data class PoseKeyframes(
    val start: KeyframeJoints,
    val peak: KeyframeJoints,
    val end: KeyframeJoints,
    val cues: List<String> = listOf("Start", "Peak contraction", "Lower with control"),
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val primaryMuscles: String, // comma-separated
    val secondaryMuscles: String, // comma-separated
    val loReps: Int,
    val hiReps: Int,
    val restSec: Int,
    val isSec: Boolean,
    val loaded: Boolean,
    val ladderJson: String,
    val cue: String,
    val tipsDoJson: String,
    val tipsDontJson: String,
    val poseJson: String = "",
    val videoPath: String = "",
    val isCustom: Boolean = false,
    val archived: Boolean = false,
)

@Entity(tableName = "set_logs")
data class SetLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val exerciseId: String,
    val reps: Int,
    val rir: Int,
    val kg: Double,
    val level: Int,
    val seed: Boolean = false,
)

@Entity(tableName = "time_logs")
data class TimeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val dayKey: String,
    val exerciseId: String,
    val seconds: Int,
)
