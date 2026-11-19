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

object GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
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

            return@withContext jsonParser.decodeFromString<HypertrophyAdvice>(text)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

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
            Canvas coordinates are in a 380x250 space (floor is y=200, head is around y=32..50, x is centered around 190).
            Provide:
            - name: Short, clear exercise name.
            - primaryMuscles: comma separated muscle keys (e.g. biceps, chest, quads, etc.).
            - secondaryMuscles: comma separated secondary keys.
            - loReps: minimum recommended reps (e.g. 8).
            - hiReps: maximum recommended reps (e.g. 15).
            - restSec: rest period in seconds (e.g. 60 to 90).
            - isSec: true if timed hold (like plank), false for dynamic reps.
            - loaded: true if hand weights / dumbbells are used.
            - ladder: array of 6 progressive overload step descriptions.
            - cue: 1 single-sentence master biomechanical cue.
            - tipsDo: array of 2 essential "Do" execution tips (stretch emphasis).
            - tipsDont: array of 2 common mistakes to avoid.
            - pose: 3 keyframes (start, peak, end) containing (head, shoulder, elbow, wrist, hip, knee, ankle) in the 380x250 coordinate space.
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
                        putJsonObject("primaryMuscles") { put("type", "STRING") }
                        putJsonObject("secondaryMuscles") { put("type", "STRING") }
                        putJsonObject("loReps") { put("type", "INTEGER") }
                        putJsonObject("hiReps") { put("type", "INTEGER") }
                        putJsonObject("restSec") { put("type", "INTEGER") }
                        putJsonObject("isSec") { put("type", "BOOLEAN") }
                        putJsonObject("loaded") { put("type", "BOOLEAN") }
                        putJsonObject("cue") { put("type", "STRING") }
                        putJsonObject("ladder") {
                            put("type", "ARRAY")
                            putJsonObject("items") { put("type", "STRING") }
                        }
                        putJsonObject("tipsDo") {
                            put("type", "ARRAY")
                            putJsonObject("items") { put("type", "STRING") }
                        }
                        putJsonObject("tipsDont") {
                            put("type", "ARRAY")
                            putJsonObject("items") { put("type", "STRING") }
                        }
                        putJsonObject("startJoints") {
                            put("type", "OBJECT")
                            putJsonObject("properties") {
                                putJsonObject("headX") { put("type", "NUMBER") }; putJsonObject("headY") { put("type", "NUMBER") }
                                putJsonObject("shoulderX") { put("type", "NUMBER") }; putJsonObject("shoulderY") { put("type", "NUMBER") }
                                putJsonObject("elbowX") { put("type", "NUMBER") }; putJsonObject("elbowY") { put("type", "NUMBER") }
                                putJsonObject("wristX") { put("type", "NUMBER") }; putJsonObject("wristY") { put("type", "NUMBER") }
                                putJsonObject("hipX") { put("type", "NUMBER") }; putJsonObject("hipY") { put("type", "NUMBER") }
                                putJsonObject("kneeX") { put("type", "NUMBER") }; putJsonObject("kneeY") { put("type", "NUMBER") }
                                putJsonObject("ankleX") { put("type", "NUMBER") }; putJsonObject("ankleY") { put("type", "NUMBER") }
                            }
                        }
                        putJsonObject("peakJoints") {
                            put("type", "OBJECT")
                            putJsonObject("properties") {
                                putJsonObject("headX") { put("type", "NUMBER") }; putJsonObject("headY") { put("type", "NUMBER") }
                                putJsonObject("shoulderX") { put("type", "NUMBER") }; putJsonObject("shoulderY") { put("type", "NUMBER") }
                                putJsonObject("elbowX") { put("type", "NUMBER") }; putJsonObject("elbowY") { put("type", "NUMBER") }
                                putJsonObject("wristX") { put("type", "NUMBER") }; putJsonObject("wristY") { put("type", "NUMBER") }
                                putJsonObject("hipX") { put("type", "NUMBER") }; putJsonObject("hipY") { put("type", "NUMBER") }
                                putJsonObject("kneeX") { put("type", "NUMBER") }; putJsonObject("kneeY") { put("type", "NUMBER") }
                                putJsonObject("ankleX") { put("type", "NUMBER") }; putJsonObject("ankleY") { put("type", "NUMBER") }
                            }
                        }
                    }
                    putJsonArray("required") {
                        add(kotlinx.serialization.json.JsonPrimitive("name"))
                        add(kotlinx.serialization.json.JsonPrimitive("primaryMuscles"))
                        add(kotlinx.serialization.json.JsonPrimitive("loReps"))
                        add(kotlinx.serialization.json.JsonPrimitive("hiReps"))
                        add(kotlinx.serialization.json.JsonPrimitive("restSec"))
                        add(kotlinx.serialization.json.JsonPrimitive("ladder"))
                        add(kotlinx.serialization.json.JsonPrimitive("cue"))
                        add(kotlinx.serialization.json.JsonPrimitive("tipsDo"))
                        add(kotlinx.serialization.json.JsonPrimitive("tipsDont"))
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

            val newId = "custom_" + UUID.randomUUID().toString().take(8)
            val name = (obj["name"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "Custom Exercise"
            val prim = (obj["primaryMuscles"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: targetMuscleKey
            val sec = (obj["secondaryMuscles"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
            val lo = (obj["loReps"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 8
            val hi = (obj["hiReps"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 15
            val rest = (obj["restSec"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 60
            val isSec = (obj["isSec"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
            val loaded = (obj["loaded"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false
            val cue = (obj["cue"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
            val ladder = obj["ladder"]?.toString() ?: "[\"Step 1\"]"
            val tipsDo = obj["tipsDo"]?.toString() ?: "[\"Focus on form\"]"
            val tipsDont = obj["tipsDont"]?.toString() ?: "[\"Avoid swinging\"]"

            // Construct 3-phase keyframe pose JSON
            val startJ = obj["startJoints"] as? kotlinx.serialization.json.JsonObject
            val peakJ = obj["peakJoints"] as? kotlinx.serialization.json.JsonObject

            val poseKeyframes = if (startJ != null && peakJ != null) {
                fun parseJ(j: kotlinx.serialization.json.JsonObject) = KeyframeJoints(
                    headX = (j["headX"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull() ?: 190f,
                    headY = (j["headY"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull() ?: 32f,
                    shoulderX = (j["shoulderX"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull() ?: 190f,
                    shoulderY = (j["shoulderY"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull() ?: 48f,
                    elbowX = (j["elbowX"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull() ?: 193f,
                    elbowY = (j["elbowY"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull() ?: 82f,
                    wristX = (j["wristX"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull() ?: 195f,
                    wristY = (j["wristY"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull() ?: 116f,
                    hipX = (j["hipX"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull() ?: 190f,
                    hipY = (j["hipY"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull() ?: 104f,
                    kneeX = (j["kneeX"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull() ?: 190f,
                    kneeY = (j["kneeY"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull() ?: 152f,
                    ankleX = (j["ankleX"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull() ?: 190f,
                    ankleY = (j["ankleY"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull() ?: 200f,
                )
                val sKey = parseJ(startJ)
                val pKey = parseJ(peakJ)
                Json.encodeToString(PoseKeyframes(start = sKey, peak = pKey, end = sKey))
            } else ""

            return@withContext ExerciseEntity(
                id = newId,
                name = name,
                primaryMuscles = prim,
                secondaryMuscles = sec,
                loReps = lo,
                hiReps = hi,
                restSec = rest,
                isSec = isSec,
                loaded = loaded,
                ladderJson = ladder,
                cue = cue,
                tipsDoJson = tipsDo,
                tipsDontJson = tipsDont,
                poseJson = poseKeyframes,
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
     * AI Exercise Auto-Tuning: Modifies an existing exercise's 3D kinematic parameters,
     * rep ladders, cues, and equipment configurations based on lifter prompt.
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

            Produce a tuned variant with:
            - name: refined exercise title
            - cue: updated coaching cue emphasizing the biomechanical change
            - loReps / hiReps: adapted rep target
            - restSec: recommended recovery seconds
            - tipsDo / tipsDont: 2 updated actionable pointers
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
            val tipsDo = obj["tipsDo"]?.toString() ?: currentExercise.tipsDoJson
            val tipsDont = obj["tipsDont"]?.toString() ?: currentExercise.tipsDontJson

            return@withContext currentExercise.copy(
                name = name,
                cue = cue,
                loReps = lo,
                hiReps = hi,
                restSec = rest,
                tipsDoJson = tipsDo,
                tipsDontJson = tipsDont,
                isCustom = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Next-Day AI Auto-Tuning Engine:
     * Analyzes past 7 days of training logs (sets, reps, loads kg, RIR, soreness tags)
     * and formulates the optimal workout session for tomorrow for maximal hypertrophy.
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
            - Choose 2 to 4 exercises from available pool: curl, conc, bicep, bench, pushup, squat, split_squat, row, latraise, ohip, dip.
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

    /**
     * Synthesizes an entire 7-day personalized dynamic hypertrophy program
     * based on the 6-step deep biomechanical survey and free-form priority text.
     */
    suspend fun generateWeekProgram(
        apiKey: String,
        model: String,
        thinkingLevel: String = "high",
        profile: OnboardingProfile
    ): List<PlanItem>? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null

        val prompt = """
            You are an elite hypertrophy scientist and biomechanist using Gemini 4 Hydrogen.
            Design the optimal foundational day-1 workout session and 7-day progression framework for this lifter:
            - Age: ${profile.age} years | Height: ${profile.heightCm} cm | Weight: ${profile.currentWeightKg} kg
            - Primary Objective: ${profile.goalType} ${if (profile.targetWeightKg != null) "| Target Weight: ${profile.targetWeightKg} kg" else ""}
            - Targeted Fat Loss & Regional Toning Zones: ${profile.targetFatLossAreas.joinToString(", ").ifEmpty { "Balanced Full Body" }}
            - Joint Pain / Injury Shield: ${profile.jointPainAreas.joinToString(", ").ifEmpty { "None" }}
            - Target Weak Points / Priority Muscles: ${profile.priorityMuscles.joinToString(", ").ifEmpty { "Balanced Full Body" }}
            - Training Frequency: ${profile.daysPerWeek} days / week
            - Session Time Budget: ${profile.sessionBudgetMins} minutes
            - Equipment Access: ${profile.equipmentType}
            - Custom Priority Request: "${profile.customPriorityText}"

            Biomechanical Guidelines:
            - If Goal is 'Fat Loss & Lean Toning' or 'Body Recomposition': incorporate metabolic compound movements (e.g. pushups, squats) with strict 3-4s eccentric cadence to maximize lean muscle sparing in a caloric deficit.
            - If specific Fat Loss/Toning areas are selected (e.g. Belly & Core, Love Handles): ensure dedicated core/abdominal stability exercises (e.g. deadbug, plank, reverse crunch) and compound hinge/squat movements are prioritized.
            - If Age > 40: reduce axial spinal compression and provide joint-friendly movement variations.
            - Avoid exercises that exacerbate the lifter's joint pain areas (e.g. if shoulder pain, prefer neutral grip / pushup progression; if lower back, avoid excessive axial shear).
            - Strongly bias sets toward target weak points and custom priority.
            - Choose 3 to 5 optimal exercises from the catalog: curl, conc, bicep, bench, pushup, squat, split_squat, row, latraise, ohip, dip.
            - Assign optimal set count (2 to 4 sets per exercise) fitting within the ${profile.sessionBudgetMins} min session budget.
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

            val plan = mutableListOf<PlanItem>()
            for (elem in arr) {
                val obj = elem as? kotlinx.serialization.json.JsonObject ?: continue
                val ex = (obj["ex"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: continue
                val sets = (obj["sets"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 3
                plan.add(PlanItem(ex = ex, sets = sets, done = 0))
            }
            if (plan.isNotEmpty()) plan else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
