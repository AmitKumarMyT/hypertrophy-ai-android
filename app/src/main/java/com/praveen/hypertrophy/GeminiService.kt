package com.praveen.hypertrophy

import com.praveen.hypertrophy.data.db.ExerciseEntity
import com.praveen.hypertrophy.data.db.KeyframeJoints
import com.praveen.hypertrophy.data.db.PoseKeyframes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

data class DynamicProgramResult(
    val exercises: List<ExerciseEntity>,
    val planItems: List<PlanItem>
)

data class AutoTuneResult(
    val updatedPlan: List<PlanItem>,
    val explanation: String
)

object GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val jsonParser = Json { ignoreUnknownKeys = true }

    suspend fun getAdvice(
        apiKey: String,
        model: String,
        thinkingLevel: String = "high",
        exName: String,
        targetMuscles: String,
        lastReps: Int,
        lastRir: Int,
        lastKg: Double,
        setsDone: Int,
        setsTotal: Int,
    ): HypertrophyAdvice? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null

        val prompt = """
            You are an elite hypertrophy scientist and biomechanist (applying principles of mechanical tension, proximity to failure, and stimulus-to-fatigue ratio).
            Current workout context:
            - Exercise: $exName (targeting: $targetMuscles)
            - Set just finished: $lastReps reps with ${if (lastKg > 0) "$lastKg kg" else "bodyweight"} @ $lastRir RIR (Reps in Reserve).
            - Progress in this exercise: Set $setsDone of $setsTotal sets.

            Analyze the fatigue state and proximity to failure. Provide immediate intra-workout recommendations for maximal hypertrophy.
            - action must be one of: CONTINUE, DROP_LOAD, EXTRA_REST, PIVOT_EXERCISE.
            - suggestedWeightDeltaKg: positive, negative, or 0.0.
            - suggestedRestSeconds: recommended seconds to rest before the next set.
            - coachingCue: 1 concise actionable cue emphasizing eccentric control or stretch under tension.
            - fatigueWarning: boolean indicating excessive systemic or joint fatigue.
        """.trimIndent()

        val payload = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", prompt) })
                    }
                })
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
                if (model.contains("hydrogen") || model.contains("flash")) {
                    putJsonObject("thinkingConfig") {
                        put("thinkingLevel", thinkingLevel)
                    }
                }
                putJsonObject("responseSchema") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("reasoning") { put("type", "STRING") }
                        putJsonObject("action") { put("type", "STRING") }
                        putJsonObject("suggestedWeightDeltaKg") { put("type", "NUMBER") }
                        putJsonObject("suggestedRestSeconds") { put("type", "INTEGER") }
                        putJsonObject("coachingCue") { put("type", "STRING") }
                        putJsonObject("fatigueWarning") { put("type", "BOOLEAN") }
                    }
                    putJsonArray("required") {
                        add(kotlinx.serialization.json.JsonPrimitive("reasoning"))
                        add(kotlinx.serialization.json.JsonPrimitive("action"))
                        add(kotlinx.serialization.json.JsonPrimitive("suggestedRestSeconds"))
                        add(kotlinx.serialization.json.JsonPrimitive("coachingCue"))
                    }
                }
            }
        }.toString()

        val selectedModel = if (model.isNotBlank()) model else "gemini-4-hydrogen"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$selectedModel:generateContent"

        try {
            val req = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey.trim())
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string() ?: return@withContext null

            val root = jsonParser.parseToJsonElement(body) as? kotlinx.serialization.json.JsonObject ?: return@withContext null
            val candArray = root["candidates"] as? kotlinx.serialization.json.JsonArray ?: return@withContext null
            if (candArray.isEmpty()) return@withContext null

            val content = (candArray[0] as? kotlinx.serialization.json.JsonObject)?.get("content") as? kotlinx.serialization.json.JsonObject ?: return@withContext null
            val parts = content["parts"] as? kotlinx.serialization.json.JsonArray ?: return@withContext null
            if (parts.isEmpty()) return@withContext null

            val text = ((parts[0] as? kotlinx.serialization.json.JsonObject)?.get("text") as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@withContext null
            jsonParser.decodeFromString<HypertrophyAdvice>(text)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Synthesizes 3 to 5 completely personalized, non-static exercises tailored to user's
     * equipment, biometrics, gender, and goal using Gemini 4 Hydrogen.
     */
    suspend fun generateDynamicProgramWithExercises(
        apiKey: String,
        model: String,
        thinkingLevel: String = "high",
        profile: OnboardingProfile
    ): DynamicProgramResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null

        val prompt = """
            You are Gemini 4 Hydrogen, an elite AI hypertrophy scientist & biomechanist.
            Synthesize a brand new, personalized workout session with custom exercises for this lifter:
            - Gender: ${profile.gender} | Age: ${profile.age} yrs | Height: ${profile.heightCm} cm | Weight: ${profile.currentWeightKg} kg
            - Primary Objective: ${profile.goalType} ${if (profile.targetWeightKg != null) "| Target Weight: ${profile.targetWeightKg} kg" else ""}
            - Target Fat Loss / Toning Focus: ${profile.targetFatLossAreas.joinToString(", ")}
            - Available Equipment: "${profile.equipmentType}" (CRITICAL: Every exercise MUST use ONLY this equipment!)
            - Injury Shield / Joint Sensitivities: ${profile.jointPainAreas.joinToString(", ").ifEmpty { "None" }}
            - Priority Muscle Groups: ${profile.priorityMuscles.joinToString(", ").ifEmpty { "Full Body" }}
            - Target Time Budget: ${profile.sessionBudgetMins} minutes

            Generate 3 to 5 realistic, high-SFR exercises. For each exercise:
            - id: short unique slug e.g. "h_incline_curl", "h_floor_press", "h_goblet_squat"
            - name: descriptive exercise name (e.g. "Incline Dumbbell Curl with 3s Lowering")
            - primaryMuscles: e.g. "biceps" or "chest" or "quads"
            - secondaryMuscles: e.g. "forearms" or "triceps"
            - sets: optimal set count (2 to 4)
            - loReps: e.g. 8
            - hiReps: e.g. 15
            - restSec: e.g. 60 to 90
            - loaded: true if using weights/bands
            - cue: 1 elite biomechanical execution cue
            - tipsDo: array of 2 essential execution pointers
            - tipsDont: array of 2 common mistakes to avoid
            - equipmentType: one of "DUMBBELL", "BARBELL", "BENCH", "PULLUP", "BODYWEIGHT", "BANDS"
        """.trimIndent()

        val payload = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", prompt) })
                    }
                })
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
                if (model.contains("hydrogen") || model.contains("flash")) {
                    putJsonObject("thinkingConfig") { put("thinkingLevel", thinkingLevel) }
                }
                putJsonObject("responseSchema") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("id") { put("type", "STRING") }
                            putJsonObject("name") { put("type", "STRING") }
                            putJsonObject("primaryMuscles") { put("type", "STRING") }
                            putJsonObject("secondaryMuscles") { put("type", "STRING") }
                            putJsonObject("sets") { put("type", "INTEGER") }
                            putJsonObject("loReps") { put("type", "INTEGER") }
                            putJsonObject("hiReps") { put("type", "INTEGER") }
                            putJsonObject("restSec") { put("type", "INTEGER") }
                            putJsonObject("loaded") { put("type", "BOOLEAN") }
                            putJsonObject("cue") { put("type", "STRING") }
                            putJsonObject("equipmentType") { put("type", "STRING") }
                            putJsonObject("tipsDo") {
                                put("type", "ARRAY")
                                putJsonObject("items") { put("type", "STRING") }
                            }
                            putJsonObject("tipsDont") {
                                put("type", "ARRAY")
                                putJsonObject("items") { put("type", "STRING") }
                            }
                        }
                        putJsonArray("required") {
                            add(kotlinx.serialization.json.JsonPrimitive("id"))
                            add(kotlinx.serialization.json.JsonPrimitive("name"))
                            add(kotlinx.serialization.json.JsonPrimitive("primaryMuscles"))
                            add(kotlinx.serialization.json.JsonPrimitive("sets"))
                            add(kotlinx.serialization.json.JsonPrimitive("loReps"))
                            add(kotlinx.serialization.json.JsonPrimitive("hiReps"))
                            add(kotlinx.serialization.json.JsonPrimitive("restSec"))
                            add(kotlinx.serialization.json.JsonPrimitive("cue"))
                        }
                    }
                }
            }
        }.toString()

        val selectedModel = if (model.isNotBlank()) model else "gemini-4-hydrogen"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$selectedModel:generateContent"

        try {
            val req = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey.trim())
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string() ?: return@withContext null

            val root = jsonParser.parseToJsonElement(body) as? kotlinx.serialization.json.JsonObject ?: return@withContext null
            val candArray = root["candidates"] as? kotlinx.serialization.json.JsonArray ?: return@withContext null
            if (candArray.isEmpty()) return@withContext null

            val content = (candArray[0] as? kotlinx.serialization.json.JsonObject)?.get("content") as? kotlinx.serialization.json.JsonObject ?: return@withContext null
            val parts = content["parts"] as? kotlinx.serialization.json.JsonArray ?: return@withContext null
            if (parts.isEmpty()) return@withContext null

            val text = ((parts[0] as? kotlinx.serialization.json.JsonObject)?.get("text") as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@withContext null
            val arr = jsonParser.parseToJsonElement(text) as? kotlinx.serialization.json.JsonArray ?: return@withContext null

            val exercises = mutableListOf<ExerciseEntity>()
            val planItems = mutableListOf<PlanItem>()

            for (elem in arr) {
                val obj = elem as? kotlinx.serialization.json.JsonObject ?: continue
                val rawId = (obj["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "h_${UUID.randomUUID().toString().take(6)}"
                val id = if (rawId.startsWith("h_")) rawId else "h_$rawId"
                val name = (obj["name"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "Dynamic Exercise"
                val prim = (obj["primaryMuscles"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "biceps"
                val sec = (obj["secondaryMuscles"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
                val sets = (obj["sets"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 3
                val lo = (obj["loReps"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 8
                val hi = (obj["hiReps"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 15
                val rest = (obj["restSec"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 60
                val loaded = (obj["loaded"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true
                val cue = (obj["cue"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "Control the eccentric lowering."
                val tipsDo = obj["tipsDo"]?.toString() ?: "[\"Maintain strict control\"]"
                val tipsDont = obj["tipsDont"]?.toString() ?: "[\"Avoid momentum\"]"

                exercises.add(
                    ExerciseEntity(
                        id = id,
                        name = name,
                        primaryMuscles = prim,
                        secondaryMuscles = sec,
                        loReps = lo,
                        hiReps = hi,
                        restSec = rest,
                        isSec = false,
                        loaded = loaded,
                        ladderJson = "[\"Standard Tempo\", \"3s Lowering\", \"3s Lowering + 1s Pause\"]",
                        cue = cue,
                        tipsDoJson = tipsDo,
                        tipsDontJson = tipsDont,
                        isCustom = true
                    )
                )

                planItems.add(PlanItem(ex = id, sets = sets, done = 0))
            }

            if (exercises.isNotEmpty()) DynamicProgramResult(exercises, planItems) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Automatic Next-Day Tuning immediately after completing an exercise:
     * Analyzes logged reps, weight, RIR, and fatigue from the finished exercise to update tomorrow's plan.
     */
    suspend fun tuneNextSessionAfterCompletedExercise(
        apiKey: String,
        model: String,
        thinkingLevel: String = "high",
        completedExName: String,
        completedExTarget: String,
        performanceSummary: String,
        currentTomorrowPlan: List<PlanItem>
    ): AutoTuneResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null

        val prompt = """
            You are Gemini 4 Hydrogen. The lifter just finished all sets of an exercise:
            - Completed Exercise: $completedExName ($completedExTarget)
            - Performance: $performanceSummary
            - Current Tomorrow Plan: ${currentTomorrowPlan.joinToString { "${it.ex} (${it.sets} sets)" }}

            Evaluate localized & systemic fatigue:
            - If performance was very hard (RIR 0-1, heavy failure): reduce volume or adjust recovery buffer for overlapping muscles tomorrow.
            - If performance was easy (high reps, RIR 3+): capitalize on stimulus readiness.
            - Return updated exercise plan for tomorrow with a 1-sentence explanation of what was tuned.
        """.trimIndent()

        val payload = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", prompt) })
                    }
                })
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
                if (model.contains("hydrogen") || model.contains("flash")) {
                    putJsonObject("thinkingConfig") { put("thinkingLevel", thinkingLevel) }
                }
                putJsonObject("responseSchema") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("explanation") { put("type", "STRING") }
                        putJsonObject("plan") {
                            put("type", "ARRAY")
                            putJsonObject("items") {
                                put("type", "OBJECT")
                                putJsonObject("properties") {
                                    putJsonObject("ex") { put("type", "STRING") }
                                    putJsonObject("sets") { put("type", "INTEGER") }
                                }
                                putJsonArray("required") {
                                    add(kotlinx.serialization.json.JsonPrimitive("ex"))
                                    add(kotlinx.serialization.json.JsonPrimitive("sets"))
                                }
                            }
                        }
                    }
                    putJsonArray("required") {
                        add(kotlinx.serialization.json.JsonPrimitive("explanation"))
                        add(kotlinx.serialization.json.JsonPrimitive("plan"))
                    }
                }
            }
        }.toString()

        val selectedModel = if (model.isNotBlank()) model else "gemini-4-hydrogen"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$selectedModel:generateContent"

        try {
            val req = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey.trim())
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string() ?: return@withContext null

            val root = jsonParser.parseToJsonElement(body) as? kotlinx.serialization.json.JsonObject ?: return@withContext null
            val candArray = root["candidates"] as? kotlinx.serialization.json.JsonArray ?: return@withContext null
            if (candArray.isEmpty()) return@withContext null

            val content = (candArray[0] as? kotlinx.serialization.json.JsonObject)?.get("content") as? kotlinx.serialization.json.JsonObject ?: return@withContext null
            val parts = content["parts"] as? kotlinx.serialization.json.JsonArray ?: return@withContext null
            if (parts.isEmpty()) return@withContext null

            val text = ((parts[0] as? kotlinx.serialization.json.JsonObject)?.get("text") as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@withContext null
            val obj = jsonParser.parseToJsonElement(text) as? kotlinx.serialization.json.JsonObject ?: return@withContext null

            val explanation = (obj["explanation"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "Optimized volume and recovery buffer for tomorrow."
            val planArr = obj["plan"] as? kotlinx.serialization.json.JsonArray ?: return@withContext null

            val items = mutableListOf<PlanItem>()
            for (elem in planArr) {
                val itemObj = elem as? kotlinx.serialization.json.JsonObject ?: continue
                val ex = (itemObj["ex"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: continue
                val sets = (itemObj["sets"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 3
                items.add(PlanItem(ex = ex, sets = sets, done = 0))
            }
            if (items.isNotEmpty()) AutoTuneResult(items, explanation) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Synthesizes a single targeted exercise from user prompt & target muscle.
     */
    suspend fun generateExercise(
        apiKey: String,
        model: String,
        thinkingLevel: String = "high",
        userPrompt: String,
        targetMuscleKey: String
    ): ExerciseEntity? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null

        val prompt = """
            Create a complete, evidence-based exercise for hypertrophy based on user request: "$userPrompt".
            Primary target muscle: "$targetMuscleKey".
            Provide:
            - id: short slug e.g. "h_exercise_name"
            - name: Short, clear exercise name.
            - primaryMuscles: comma separated muscle keys (e.g. biceps, chest, quads, etc.).
            - secondaryMuscles: comma separated secondary keys.
            - loReps: minimum recommended reps (e.g. 8).
            - hiReps: maximum recommended reps (e.g. 15).
            - restSec: rest period in seconds (e.g. 60 to 90).
            - isSec: false for dynamic reps, true for timed hold.
            - loaded: true if hand weights / dumbbells are used.
            - cue: 1 single-sentence master biomechanical cue.
            - tipsDo: array of 2 essential execution tips.
            - tipsDont: array of 2 common mistakes to avoid.
        """.trimIndent()

        val payload = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", prompt) })
                    }
                })
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
                if (model.contains("hydrogen") || model.contains("flash")) {
                    putJsonObject("thinkingConfig") { put("thinkingLevel", thinkingLevel) }
                }
                putJsonObject("responseSchema") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("id") { put("type", "STRING") }
                        putJsonObject("name") { put("type", "STRING") }
                        putJsonObject("primaryMuscles") { put("type", "STRING") }
                        putJsonObject("secondaryMuscles") { put("type", "STRING") }
                        putJsonObject("loReps") { put("type", "INTEGER") }
                        putJsonObject("hiReps") { put("type", "INTEGER") }
                        putJsonObject("restSec") { put("type", "INTEGER") }
                        putJsonObject("isSec") { put("type", "BOOLEAN") }
                        putJsonObject("loaded") { put("type", "BOOLEAN") }
                        putJsonObject("cue") { put("type", "STRING") }
                        putJsonObject("tipsDo") {
                            put("type", "ARRAY")
                            putJsonObject("items") { put("type", "STRING") }
                        }
                        putJsonObject("tipsDont") {
                            put("type", "ARRAY")
                            putJsonObject("items") { put("type", "STRING") }
                        }
                    }
                    putJsonArray("required") {
                        add(kotlinx.serialization.json.JsonPrimitive("id"))
                        add(kotlinx.serialization.json.JsonPrimitive("name"))
                        add(kotlinx.serialization.json.JsonPrimitive("primaryMuscles"))
                        add(kotlinx.serialization.json.JsonPrimitive("loReps"))
                        add(kotlinx.serialization.json.JsonPrimitive("hiReps"))
                        add(kotlinx.serialization.json.JsonPrimitive("restSec"))
                        add(kotlinx.serialization.json.JsonPrimitive("cue"))
                    }
                }
            }
        }.toString()

        val selectedModel = if (model.isNotBlank()) model else "gemini-4-hydrogen"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$selectedModel:generateContent"

        try {
            val req = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey.trim())
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string() ?: return@withContext null

            val root = jsonParser.parseToJsonElement(body) as? kotlinx.serialization.json.JsonObject ?: return@withContext null
            val candArray = root["candidates"] as? kotlinx.serialization.json.JsonArray ?: return@withContext null
            if (candArray.isEmpty()) return@withContext null

            val content = (candArray[0] as? kotlinx.serialization.json.JsonObject)?.get("content") as? kotlinx.serialization.json.JsonObject ?: return@withContext null
            val parts = content["parts"] as? kotlinx.serialization.json.JsonArray ?: return@withContext null
            if (parts.isEmpty()) return@withContext null

            val text = ((parts[0] as? kotlinx.serialization.json.JsonObject)?.get("text") as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@withContext null
            val obj = jsonParser.parseToJsonElement(text) as? kotlinx.serialization.json.JsonObject ?: return@withContext null

            val rawId = (obj["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "h_${UUID.randomUUID().toString().take(6)}"
            val id = if (rawId.startsWith("h_")) rawId else "h_$rawId"
            val name = (obj["name"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "Custom Exercise"
            val prim = (obj["primaryMuscles"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: targetMuscleKey
            val sec = (obj["secondaryMuscles"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
            val lo = (obj["loReps"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 8
            val hi = (obj["hiReps"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 15
            val rest = (obj["restSec"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 60
            val isSec = (obj["isSec"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
            val loaded = (obj["loaded"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: true
            val cue = (obj["cue"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "Control the eccentric lowering."
            val tipsDo = obj["tipsDo"]?.toString() ?: "[\"Full range of motion\"]"
            val tipsDont = obj["tipsDont"]?.toString() ?: "[\"Avoid momentum\"]"

            ExerciseEntity(
                id = id,
                name = name,
                primaryMuscles = prim,
                secondaryMuscles = sec,
                loReps = lo,
                hiReps = hi,
                restSec = rest,
                isSec = isSec,
                loaded = loaded,
                ladderJson = "[\"Standard Tempo\", \"3s Lowering\", \"3s Lowering + 1s Pause\"]",
                cue = cue,
                tipsDoJson = tipsDo,
                tipsDontJson = tipsDont,
                poseJson = "",
                videoPath = "",
                isCustom = true,
                archived = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * AI Exercise Auto-Tuning: Modifies an existing exercise's parameters based on user instruction.
     */
    suspend fun tuneExercise(
        apiKey: String,
        model: String,
        thinkingLevel: String = "high",
        currentExercise: ExerciseEntity,
        userInstruction: String
    ): ExerciseEntity? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null

        val prompt = """
            You are an elite hypertrophy scientist and biomechanist.
            Modify and tune the following exercise based on the user's explicit biomechanical instruction.
            Current Exercise:
            - Name: ${currentExercise.name}
            - Target: ${currentExercise.primaryMuscles}
            - Rep Range: ${currentExercise.loReps} - ${currentExercise.hiReps}
            - Current Cue: ${currentExercise.cue}

            User Tuning Instruction: "$userInstruction"

            Produce a tuned variant with refined name, cue, loReps, hiReps, and restSec.
        """.trimIndent()

        val payload = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", prompt) })
                    }
                })
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
                if (model.contains("hydrogen") || model.contains("flash")) {
                    putJsonObject("thinkingConfig") { put("thinkingLevel", thinkingLevel) }
                }
                putJsonObject("responseSchema") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("name") { put("type", "STRING") }
                        putJsonObject("cue") { put("type", "STRING") }
                        putJsonObject("loReps") { put("type", "INTEGER") }
                        putJsonObject("hiReps") { put("type", "INTEGER") }
                        putJsonObject("restSec") { put("type", "INTEGER") }
                    }
                    putJsonArray("required") {
                        add(kotlinx.serialization.json.JsonPrimitive("name"))
                        add(kotlinx.serialization.json.JsonPrimitive("cue"))
                        add(kotlinx.serialization.json.JsonPrimitive("loReps"))
                        add(kotlinx.serialization.json.JsonPrimitive("hiReps"))
                        add(kotlinx.serialization.json.JsonPrimitive("restSec"))
                    }
                }
            }
        }.toString()

        val selectedModel = if (model.isNotBlank()) model else "gemini-4-hydrogen"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$selectedModel:generateContent"

        try {
            val req = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey.trim())
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string() ?: return@withContext null

            val root = jsonParser.parseToJsonElement(body) as? kotlinx.serialization.json.JsonObject ?: return@withContext null
            val candArray = root["candidates"] as? kotlinx.serialization.json.JsonArray ?: return@withContext null
            if (candArray.isEmpty()) return@withContext null

            val content = (candArray[0] as? kotlinx.serialization.json.JsonObject)?.get("content") as? kotlinx.serialization.json.JsonObject ?: return@withContext null
            val parts = content["parts"] as? kotlinx.serialization.json.JsonArray ?: return@withContext null
            if (parts.isEmpty()) return@withContext null

            val text = ((parts[0] as? kotlinx.serialization.json.JsonObject)?.get("text") as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@withContext null
            val obj = jsonParser.parseToJsonElement(text) as? kotlinx.serialization.json.JsonObject ?: return@withContext null

            val name = (obj["name"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: currentExercise.name
            val cue = (obj["cue"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: currentExercise.cue
            val lo = (obj["loReps"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: currentExercise.loReps
            val hi = (obj["hiReps"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: currentExercise.hiReps
            val rest = (obj["restSec"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: currentExercise.restSec

            currentExercise.copy(
                name = name,
                cue = cue,
                loReps = lo,
                hiReps = hi,
                restSec = rest,
                isCustom = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Next-Day AI Auto-Tuning Engine from historical logs.
     */
    suspend fun tuneNextDaySession(
        apiKey: String,
        model: String,
        thinkingLevel: String = "high",
        recentLogsJson: String,
        soreMuscles: List<String>,
        targetBudgetMins: Int = 20
    ): List<PlanItem>? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null

        val prompt = """
            You are an elite hypertrophy scientist.
            Analyze the lifter's recent workout history and active soreness to design the optimal workout plan for TOMORROW.
            - Active Soreness: $soreMuscles
            - Available Session Time: $targetBudgetMins minutes
            - Recent Sets & Loads Log: $recentLogsJson

            Apply maximum stimulus-to-fatigue ratio (SFR):
            - De-prioritize sore or heavily fatigued muscle groups.
            - Target muscle groups with high recovery readiness.
            - Choose 2 to 4 exercises from available pool.
            - Output array of exercises with optimal set counts (2 to 4 sets each).
        """.trimIndent()

        val payload = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", prompt) })
                    }
                })
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
                if (model.contains("hydrogen") || model.contains("flash")) {
                    putJsonObject("thinkingConfig") { put("thinkingLevel", thinkingLevel) }
                }
                putJsonObject("responseSchema") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("ex") { put("type", "STRING") }
                            putJsonObject("sets") { put("type", "INTEGER") }
                        }
                        putJsonArray("required") {
                            add(kotlinx.serialization.json.JsonPrimitive("ex"))
                            add(kotlinx.serialization.json.JsonPrimitive("sets"))
                        }
                    }
                }
            }
        }.toString()

        val selectedModel = if (model.isNotBlank()) model else "gemini-4-hydrogen"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$selectedModel:generateContent"

        try {
            val req = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey.trim())
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val body = resp.body?.string() ?: return@withContext null

            val root = jsonParser.parseToJsonElement(body) as? kotlinx.serialization.json.JsonObject ?: return@withContext null
            val candArray = root["candidates"] as? kotlinx.serialization.json.JsonArray ?: return@withContext null
            if (candArray.isEmpty()) return@withContext null

            val content = (candArray[0] as? kotlinx.serialization.json.JsonObject)?.get("content") as? kotlinx.serialization.json.JsonObject ?: return@withContext null
            val parts = content["parts"] as? kotlinx.serialization.json.JsonArray ?: return@withContext null
            if (parts.isEmpty()) return@withContext null

            val text = ((parts[0] as? kotlinx.serialization.json.JsonObject)?.get("text") as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@withContext null
            val arr = jsonParser.parseToJsonElement(text) as? kotlinx.serialization.json.JsonArray ?: return@withContext null

            val items = mutableListOf<PlanItem>()
            for (element in arr) {
                val itemObj = element as? kotlinx.serialization.json.JsonObject ?: continue
                val ex = (itemObj["ex"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: continue
                val sets = (itemObj["sets"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 3
                items.add(PlanItem(ex = ex, sets = sets, done = 0))
            }
            if (items.isNotEmpty()) items else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
