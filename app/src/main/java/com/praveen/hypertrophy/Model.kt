package com.praveen.hypertrophy

import kotlinx.serialization.Serializable

data class Muscle(val key: String, val name: String, val target: Int, val tier: Int)

val MUSCLES: List<Muscle> = listOf(
    Muscle("biceps", "Biceps", 12, 1),
    Muscle("triceps", "Triceps", 12, 1),
    Muscle("forearms", "Forearms", 8, 1),
    Muscle("abs", "Abs", 12, 2),
    Muscle("quads", "Quads", 10, 3),
    Muscle("hamglutes", "Hamstrings and glutes", 10, 3),
    Muscle("calves", "Calves", 8, 3),
    Muscle("lowback", "Lower back", 8, 4),
    Muscle("chest", "Chest", 8, 5),
    Muscle("back", "Back", 8, 5),
    Muscle("shoulders", "Shoulders", 8, 5),
)
val MUSCLE: Map<String, Muscle> = MUSCLES.associateBy { it.key }
val TIER_NAMES: Map<Int, String> = mapOf(
    1 to "Arms", 2 to "Core", 3 to "Legs", 4 to "Lower back", 5 to "Chest, back, shoulders",
)

private val DB_L = listOf(
    "2 s lowering", "3 s lowering", "3 s lowering + 1 s pause in the stretch",
    "Heavier weight (+1 to 2 kg), 2 s lowering", "Heavier weight, 3 s lowering", "Heavier weight, 3 s lowering + pause",
)
private val PU_L = listOf(
    "Standard, 2 s lowering", "Standard, 3 s lowering", "Standard, 3 s lowering + 1 s pause at the bottom",
    "Feet on the chair, 2 s lowering", "Feet on the chair, 3 s lowering", "Feet on the chair, 3 s lowering + pause",
)
private val LEG_L = listOf(
    "Bodyweight, 2 s lowering", "Bodyweight, 3 s lowering + 1 s pause", "Hold 3 kg in each hand",
    "3 kg each hand, 3 s lowering + pause", "Hold 6 kg in each hand", "6 kg each hand, 3 s lowering + pause",
)
private val CALF_L = listOf(
    "Single leg, 2 s lowering", "Single leg, 1 s squeeze at top, 3 s lowering", "Single leg holding 3 kg",
    "Single leg holding 3 kg, 2 s squeeze + 3 s lowering", "Single leg holding 6 kg",
)
private val RC_L = listOf(
    "Knees bent, 2 s lowering", "Knees bent, 3 s lowering", "Legs straighter, 3 s lowering",
    "Legs straighter, 2 s squeeze at top, 3 s lowering",
)
private val DBUG_L = listOf("Slow, opposite arm and leg", "Hold 3 kg in both hands", "Hold 3 kg, 3 s per rep")
private val PL_L = listOf("Forearm plank", "Arms reaching ahead of shoulders", "Alternate lifting one foot")
private val SUP_L = listOf("2 s hold at the top", "3 s hold", "Arms in a W, 3 s hold")
private val WR_L = listOf("2 s lowering", "3 s lowering", "3 s lowering + 1 s squeeze", "Heavier weight, 2 s lowering")

data class Ex(
    val id: String,
    val name: String,
    val primary: List<String>,
    val secondary: List<String>,
    val lo: Int,
    val hi: Int,
    val rest: Int,
    val isSec: Boolean,
    val loaded: Boolean,
    val ladder: List<String>,
    val cue: String,
    val tipsDo: List<String>,
    val tipsDont: List<String>,
)

val EXERCISES: List<Ex> = listOf(
    Ex("curl", "Dumbbell curl", listOf("biceps"), listOf("forearms"), 12, 20, 60, false, true, DB_L,
        "Elbows pinned to your ribs. Fully straighten at the bottom, no swinging.",
        listOf("Straighten fully at the bottom for the stretch", "Lower for 2 to 3 s and stop 1 to 2 reps short of failure"),
        listOf("Swing your torso to cheat reps", "Let your elbows drift forward")),
    Ex("hammer", "Hammer curl", listOf("biceps"), listOf("forearms"), 12, 20, 60, false, true, DB_L,
        "Thumbs up, elbows still. Squeeze at the top.",
        listOf("Keep thumbs up and wrists straight", "Squeeze for 1 s at the top"),
        listOf("Rush the lowering", "Shrug your shoulders")),
    Ex("conc", "Seated curl, elbow on knee", listOf("biceps"), emptyList(), 12, 20, 60, false, true, DB_L,
        "Elbow braced on your knee. Slow lowering, full stretch.",
        listOf("Brace your elbow firmly on your knee", "Get the full stretch at the bottom"),
        listOf("Lean back to lift", "Bounce at the bottom")),
    Ex("ohtri", "Overhead triceps extension", listOf("triceps"), emptyList(), 12, 20, 60, false, true, DB_L,
        "Both hands on one weight. Elbows point up, lower behind your head.",
        listOf("Keep elbows pointing up and close together", "Lower until you feel a deep stretch"),
        listOf("Flare your elbows out", "Arch your lower back")),
    Ex("diamond", "Diamond push-up", listOf("triceps"), listOf("chest", "shoulders"), 8, 20, 75, false, false, PU_L,
        "Hands under your chest in a diamond. Body in one line.",
        listOf("Keep your body in one straight line", "Lower for 2 to 3 s, chest to hands"),
        listOf("Let your hips sag or pike", "Stop short of the full range")),
    Ex("wrist", "Forearm extension", listOf("forearms"), emptyList(), 15, 30, 45, false, true, WR_L,
        "Forearm on your knee, palm down. Lift the back of your hand.",
        listOf("Rest your whole forearm on your thigh", "Use a light weight and full range"),
        listOf("Lift your forearm off your leg", "Jerk the weight up")),
    Ex("rcrunch", "Reverse crunch", listOf("abs"), emptyList(), 12, 20, 60, false, false, RC_L,
        "Curl hips off the floor with your abs. No swinging.",
        listOf("Curl your pelvis up, not just your legs", "Lower slowly with control"),
        listOf("Throw your legs with momentum", "Let your back arch on the way down")),
    Ex("deadbug", "Dead bug", listOf("abs"), emptyList(), 10, 16, 60, false, false, DBUG_L,
        "Lower back stays flat on the floor. Reps per side.",
        listOf("Press your lower back into the floor", "Move slowly and breathe out as you reach"),
        listOf("Let your back arch off the floor", "Rush the reps")),
    Ex("plank", "Forearm plank", listOf("abs"), listOf("shoulders"), 30, 60, 60, true, false, PL_L,
        "Squeeze glutes and abs. Stop when your hips sag.",
        listOf("Squeeze glutes and abs, ribs down", "End the set when your form breaks"),
        listOf("Let your hips sag or pike", "Hold your breath")),
    Ex("splitsq", "Split squat, back foot on chair", listOf("quads"), listOf("hamglutes"), 10, 16, 90, false, true, LEG_L,
        "Torso upright. Lower for 3 s, drive through the front heel. Reps per leg.",
        listOf("Lower for 3 s with your torso upright", "Keep your front knee over your mid-foot"),
        listOf("Let your front knee cave inward", "Bounce out of the bottom. Check the chair cannot slide")),
    Ex("chairsq", "Chair squat", listOf("quads"), listOf("hamglutes"), 15, 25, 75, false, true, LEG_L,
        "Sit back to the chair, touch lightly, stand without rocking.",
        listOf("Sit back with shins near vertical", "Touch the chair lightly, stand without rocking"),
        listOf("Drop onto the chair", "Let your knees collapse inward")),
    Ex("rdl", "Single-leg Romanian deadlift", listOf("hamglutes"), listOf("lowback"), 10, 16, 90, false, true, DB_L,
        "Hips back, flat back, soft knee. Reps per leg.",
        listOf("Push hips back with a flat back and soft knee", "Stop at the hamstring stretch"),
        listOf("Round your lower back", "Twist your hips open")),
    Ex("bridge", "Single-leg glute bridge", listOf("hamglutes"), emptyList(), 12, 20, 60, false, false, LEG_L,
        "Drive through the heel, squeeze at the top. Reps per leg.",
        listOf("Push through your heel and squeeze at the top", "Keep ribs down"),
        listOf("Overarch your lower back to go higher", "Push through your toes")),
    Ex("calf", "Single-leg calf raise", listOf("calves"), emptyList(), 15, 25, 60, false, true, CALF_L,
        "Full stretch at the bottom, pause at the top. One hand on the chair.",
        listOf("Get a full stretch at the bottom", "Pause for 1 s at the top"),
        listOf("Bounce through the reps", "Pull yourself up with your hand")),
    Ex("superman", "Superman", listOf("lowback"), listOf("hamglutes"), 12, 20, 60, false, false, SUP_L,
        "Lift chest and legs together. Look at the floor.",
        listOf("Look at the floor with a long neck", "Hold 2 s at the top"),
        listOf("Crank your head up", "Swing arms and legs up with momentum")),
    Ex("pushup", "Push-up", listOf("chest"), listOf("triceps", "shoulders"), 10, 25, 75, false, false, PU_L,
        "Chest to the floor, body in one line.",
        listOf("Keep a straight line with ribs down", "Chest to the floor, elbows about 45 degrees"),
        listOf("Flare your elbows out to 90 degrees", "Rush half reps")),
    Ex("row", "One-arm dumbbell row", listOf("back"), listOf("biceps", "forearms"), 12, 20, 75, false, true, DB_L,
        "Hand on the chair, flat back. Pull the elbow to your hip. Reps per arm.",
        listOf("Keep your back flat and pull the elbow to your hip", "Let the shoulder blade stretch forward at the bottom"),
        listOf("Twist your torso to lift", "Shrug the weight up")),
    Ex("latraise", "Lateral raise", listOf("shoulders"), emptyList(), 15, 25, 60, false, true, DB_L,
        "Lead with the elbows, stop at shoulder height.",
        listOf("Lead with your elbows and stop at shoulder height", "Lower for 2 to 3 s"),
        listOf("Swing the weights up", "Shrug your shoulders")),
)
val EX: Map<String, Ex> = EXERCISES.associateBy { it.id }

val GENERAL_RULES: List<String> = listOf(
    "Finish every working set 1 to 2 reps from failure.",
    "Use the full range and lower for 2 to 3 s.",
    "Beat last time: one more rep, then the next level.",
    "Aim for about 10 to 12 hard sets per muscle each week.",
    "Rest 60 to 90 s so the next set stays hard.",
    "Sleep well and eat enough protein.",
    "Stop on sharp pain. Soreness is fine, pain is not.",
)

@Serializable
data class SetLog(
    val id: String,
    val t: Long,
    val ex: String,
    val reps: Int,
    val rir: Int,
    val kg: Double = 0.0,
    val lvl: Int = 0,
    val seed: Boolean = false,
)

@Serializable
data class TimeLog(val t: Long, val day: String, val ex: String, val sec: Int)

@Serializable
data class PlanItem(val ex: String, val sets: Int, val done: Int = 0)

@Serializable
data class Session(
    val day: String,
    val plan: List<PlanItem>,
    val idx: Int = 0,
    val start: Long = 0,
    val pausedAt: Long = 0,
    val pausedMs: Long = 0,
    val restLeftMs: Long = 0,
    val exAccMs: Map<String, Long> = emptyMap(),
    val segFrom: Long = 0,
    val segEx: String = "",
    val lvl0: Map<String, Int> = emptyMap(),
    val forced: Boolean = false,
)

@Serializable
data class HypertrophyAdvice(
    val reasoning: String = "",
    val action: String = "CONTINUE", // CONTINUE, DROP_LOAD, EXTRA_REST, PIVOT_EXERCISE
    val suggestedWeightDeltaKg: Double = 0.0,
    val suggestedRestSeconds: Int = 90,
    val coachingCue: String = "",
    val fatigueWarning: Boolean = false,
)

@Serializable
data class AISettings(
    val apiKey: String = "",
    val model: String = "gemini-3.8-flash",
    val enabled: Boolean = true,
)

@Serializable
data class AppState(
    val log: List<SetLog> = emptyList(),
    val times: List<TimeLog> = emptyList(),
    val lvl: Map<String, Int> = emptyMap(),
    val sore: Set<String> = emptySet(),
    val budget: Int = 15,
    val restH: Int = 42,
    val session: Session? = null,
    val aiSettings: AISettings = AISettings(),
    val lastAdvice: HypertrophyAdvice? = null,
)
