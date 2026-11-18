package com.praveen.hypertrophy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonParser = Json { ignoreUnknownKeys = true }

    suspend fun getAdvice(
        apiKey: String,
        model: String,
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

        // JSON schema using uppercase types as required by Gemini REST API v1beta
        val payload = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject {
                            put("text", prompt)
                        })
                    }
                })
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
                putJsonObject("responseSchema") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("reasoning") {
                            put("type", "STRING")
                            put("description", "Brief scientific reasoning regarding SFR and muscle stimulus")
                        }
                        putJsonObject("action") {
                            put("type", "STRING")
                            put("description", "One of: CONTINUE, DROP_LOAD, EXTRA_REST, PIVOT_EXERCISE")
                        }
                        putJsonObject("suggestedWeightDeltaKg") {
                            put("type", "NUMBER")
                            put("description", "Load change recommendation in kg (e.g. -2.0 or 0.0)")
                        }
                        putJsonObject("suggestedRestSeconds") {
                            put("type", "INTEGER")
                            put("description", "Suggested rest time in seconds before next set")
                        }
                        putJsonObject("coachingCue") {
                            put("type", "STRING")
                            put("description", "Concise form cue for maximum tension in the lengthened position")
                        }
                        putJsonObject("fatigueWarning") {
                            put("type", "BOOLEAN")
                            put("description", "Whether fatigue is compromising hypertrophy stimulus")
                        }
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

        val selectedModel = if (model.isNotBlank()) model else "gemini-3.8-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$selectedModel:generateContent"

        try {
            val req = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey.trim())
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) {
                return@withContext null
            }
            val body = resp.body?.string() ?: return@withContext null

            val root = jsonParser.parseToJsonElement(body)
            val candidates = root as? kotlinx.serialization.json.JsonObject ?: return@withContext null
            val candArray = candidates["candidates"] as? kotlinx.serialization.json.JsonArray ?: return@withContext null
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
}
