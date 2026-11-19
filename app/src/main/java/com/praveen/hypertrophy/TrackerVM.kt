package com.praveen.hypertrophy

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max

data class Draft(
    val ex: String,
    val set: Int,
    val reps: Int,
    val rir: Int = 2,
    val kg: String = "",
    val holdStart: Long = 0L,
    val err: String = "",
)

data class FbUi(
    val tag: Int,
    val msg: String,
    val credit: Double,
    val note: String,
    val name: String,
    val week: String,
    val timeSec: Int,
)

data class Ui(
    val tab: Int = 0,
    val draft: Draft? = null,
    val fb: FbUi? = null,
    val restEnd: Long = 0L,
    val restTotal: Int = 0,
    val confirm: String = "",
    val nextDayTuningNotice: String = "",
)

class TrackerVM(app: Application) : AndroidViewModel(app) {
    private val db = com.praveen.hypertrophy.data.db.AppDatabase.getInstance(app)
    private val exerciseDao = db.exerciseDao()
    private val workoutDao = db.workoutDao()

    val dbExercises: StateFlow<List<com.praveen.hypertrophy.data.db.ExerciseEntity>> =
        exerciseDao.getAllActive()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList<com.praveen.hypertrophy.data.db.ExerciseEntity>()
            )

    private val store = Store(app)
    private val feedback = com.praveen.hypertrophy.feedback.FeedbackManager.getInstance(app)
    private val _state = MutableStateFlow(normalize(store.load()))
    val state: StateFlow<AppState> = _state
    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui
    private val _isGeneratingExercise = MutableStateFlow(false)
    val isGeneratingExercise: StateFlow<Boolean> = _isGeneratingExercise

    private var lastWarningSec = -1

    init {
        store.save(_state.value)
        // Preload any custom Room exercises into in-memory EXERCISES list so EX[id] lookup succeeds
        viewModelScope.launch {
            dbExercises.collect { list ->
                for (entity in list) {
                    registerEntityInExerciseMap(entity)
                }
            }
        }
        refreshDraft()
        viewModelScope.launch {
            while (true) {
                delay(250)
                val u = _ui.value
                if (u.restEnd != 0L) {
                    val remainingMs = u.restEnd - System.currentTimeMillis()
                    val secondsLeft = (remainingMs / 1000).toInt()
                    if (remainingMs <= 0L) {
                        alert()
                        _ui.update { it.copy(restEnd = 0L) }
                        lastWarningSec = -1
                    } else if (secondsLeft in 1..3 && secondsLeft != lastWarningSec) {
                        lastWarningSec = secondsLeft
                        feedback.playCountdownTick(secondsLeft)
                    }
                } else {
                    lastWarningSec = -1
                }
            }
        }
    }

    private fun now(): Long = System.currentTimeMillis()

    private fun normalize(s: AppState): AppState {
        val ss = s.session
        return if (ss == null || ss.day != dayKey()) s.copy(session = newSession(s, false)) else s
    }

    private fun commit(s: AppState) {
        _state.value = s
        store.save(s)
    }

    private fun alert() {
        feedback.playCompletionFanfare()
    }

    // ---------- draft ----------

    private fun makeDraft(st: AppState, it: PlanItem): Draft {
        val e = EX[it.ex] ?: getExerciseOrDefault(it.ex)
        val l = lastSessionSets(st, it.ex)
        val reps = if (l.isNotEmpty()) minOf(e.hi, l.maxOf { x -> x.reps } + (if (it.done == 0) 1 else 0)) else e.lo + 2
        val kg = if (l.isNotEmpty() && l.last().kg > 0) l.last().kg.toString().removeSuffix(".0") else ""
        return Draft(ex = it.ex, set = it.done, reps = reps, kg = kg)
    }

    private fun refreshDraft() {
        val st = _state.value
        val ss = st.session
        val item = ss?.plan?.getOrNull(ss.idx)
        _ui.update { u ->
            if (item == null) u.copy(draft = null)
            else {
                val d = u.draft
                if (d != null && d.ex == item.ex && d.set == item.done) u else u.copy(draft = makeDraft(st, item))
            }
        }
    }

    private fun setDraft(d: Draft) { _ui.update { it.copy(draft = d) } }

    fun setTab(t: Int) { _ui.update { it.copy(tab = t, confirm = "") } }

    // ---------- plan settings ----------

    fun setBudget(b: Int) { commit(_state.value.copy(budget = b)); rebuild(null) }

    fun toggleSore(m: String) {
        val st = _state.value
        commit(st.copy(sore = if (m in st.sore) st.sore - m else st.sore + m))
        rebuild(null)
    }

    fun changeRestH(d: Int) {
        val st = _state.value
        commit(st.copy(restH = max(12, minOf(72, st.restH + d))))
        rebuild(null)
    }

    fun rebuild(force: Boolean?) {
        val st = _state.value
        val ss = st.session
        if (ss != null && ss.start != 0L) return
        commit(st.copy(session = newSession(st, force ?: ss?.forced ?: false)))
        _ui.update { it.copy(draft = null, fb = null, confirm = "") }
        refreshDraft()
    }

    fun newBlock() {
        val st = _state.value
        commit(st.copy(session = newSession(st, true)))
        _ui.update { it.copy(draft = null, fb = null, restEnd = 0L, confirm = "") }
        refreshDraft()
    }

    // ---------- session flow ----------

    fun startSession() {
        val st = _state.value
        val ss = st.session ?: return
        commit(st.copy(session = ss.begin(now(), st.lvl)))
        refreshDraft()
    }

    fun repsDelta(d: Int) {
        val dr = _ui.value.draft ?: return
        if (dr.holdStart != 0L) return
        setDraft(dr.copy(reps = max(0, dr.reps + d), err = ""))
    }

    fun setRir(r: Int) { _ui.value.draft?.let { setDraft(it.copy(rir = r)) } }
    fun setKg(v: String) { _ui.value.draft?.let { setDraft(it.copy(kg = v)) } }

    fun toggleHold() {
        val dr = _ui.value.draft ?: return
        if (dr.holdStart != 0L) setDraft(dr.copy(reps = ((now() - dr.holdStart) / 1000).toInt(), holdStart = 0L))
        else setDraft(dr.copy(holdStart = now()))
    }

    fun logSet() {
        val st = _state.value
        val ss0 = st.session ?: return
        val item = ss0.plan.getOrNull(ss0.idx) ?: return
        val dr = _ui.value.draft ?: return
        val e = EX[item.ex] ?: getExerciseOrDefault(item.ex)
        var reps = dr.reps
        if (dr.holdStart != 0L) reps = ((now() - dr.holdStart) / 1000).toInt()
        if (reps <= 0) { setDraft(dr.copy(err = "Enter the reps you completed.")); return }

        val t = now()
        var ss = ss0.flush(t)
        val x = SetLog(UUID.randomUUID().toString(), t, item.ex, reps, dr.rir, dr.kg.toDoubleOrNull() ?: 0.0, st.lvl[item.ex] ?: 0)
        val plan = ss.plan.toMutableList()
        val cur = plan[ss.idx].copy(done = plan[ss.idx].done + 1)
        plan[ss.idx] = cur
        ss = ss.copy(plan = plan)
        if (ss.start == 0L) ss = ss.begin(t, st.lvl).copy(plan = plan)

        val log = st.log + x
        val fb = setFeedback(e, x)
        var lvl = st.lvl
        var times = st.times
        var note = ""
        val exDone = cur.done >= cur.sets
        if (exDone) {
            val ev = evaluateExercise(log, lvl, item.ex, dayKey())
            note = ev.first
            lvl = ev.second
            times = times + TimeLog(t, dayKey(), item.ex, ss.exSec(item.ex, t))
            ss = ss.copy(idx = ss.idx + 1)
        }
        val finished = ss.idx >= ss.plan.size
        if (!finished) ss = ss.open(t)

        val ns = st.copy(log = log, times = times, lvl = lvl, session = ss)
        val week = (e.primary + e.secondary).distinct().joinToString(", ") { m ->
            MUSCLE[m]!!.name + " " + String.format("%.1f", weekSets(ns, m)).removeSuffix(".0") + "/" + MUSCLE[m]!!.target
        }
        commit(ns)
        val restTot = e.rest + (if (dr.rir == 0) 30 else 0)
        _ui.update {
            it.copy(
                draft = null,
                fb = FbUi(fb.tag, fb.msg, fb.credit, note, e.name, week, if (exDone) ns.session!!.exSec(item.ex, t) else 0),
                restEnd = if (finished) 0L else t + restTot * 1000L,
                restTotal = restTot,
            )
        }
        refreshDraft()

        // Asynchronous Gemini Hypertrophy Intelligence invocation
        if (st.aiSettings.enabled && st.aiSettings.apiKey.isNotBlank()) {
            val targets = (e.primary + e.secondary).joinToString(", ") { MUSCLE[it]?.name ?: it }

            // Intra-workout set advice
            if (!finished) {
                viewModelScope.launch {
                    val advice = GeminiService.getAdvice(
                        apiKey = st.aiSettings.apiKey,
                        model = st.aiSettings.model,
                        exName = e.name,
                        targetMuscles = targets,
                        lastReps = reps,
                        lastRir = dr.rir,
                        lastKg = dr.kg.toDoubleOrNull() ?: 0.0,
                        setsDone = cur.done,
                        setsTotal = cur.sets,
                    )
                    if (advice != null) {
                        commit(_state.value.copy(lastAdvice = advice))
                        // If Gemini recommends adjusted rest time, tune the active rest timer
                        if (advice.suggestedRestSeconds > 0 && _ui.value.restEnd > now()) {
                            val newEnd = t + advice.suggestedRestSeconds * 1000L
                            _ui.update { it.copy(restEnd = newEnd, restTotal = advice.suggestedRestSeconds) }
                        }
                    }
                }
            }

            // Post-exercise dynamic auto-tuning for next day workout:
            if (exDone) {
                viewModelScope.launch {
                    val perfSummary = "${cur.sets} sets completed. Final set: $reps reps @ ${dr.rir} RIR with ${if (dr.kg.isNotBlank()) "${dr.kg}kg" else "bodyweight"}."
                    val tuneRes = GeminiService.tuneNextSessionAfterCompletedExercise(
                        apiKey = st.aiSettings.apiKey,
                        model = st.aiSettings.model,
                        thinkingLevel = st.aiSettings.thinkingLevel,
                        completedExName = e.name,
                        completedExTarget = targets,
                        performanceSummary = perfSummary,
                        currentTomorrowPlan = ns.session?.plan ?: emptyList()
                    )
                    if (tuneRes != null) {
                        _ui.update { it.copy(nextDayTuningNotice = "⚡ Auto-tuned next day: ${tuneRes.explanation}") }
                    }
                }
            }
        }
    }

    fun undoLast() {
        val st = _state.value
        val ss0 = st.session ?: return
        val x = st.log.lastOrNull() ?: return
        if (x.seed || dayKey(x.t) != dayKey() || x.t < ss0.start) return
        val t = now()
        var ss = ss0.flush(t)
        val plan = ss.plan.toMutableList()
        var i = -1
        for (j in plan.indices.reversed()) { if (plan[j].ex == x.ex && plan[j].done > 0) { i = j; break } }
        var times = st.times
        if (i >= 0) {
            if (plan[i].done >= plan[i].sets) {
                val tl = times.lastOrNull()
                if (tl != null && tl.ex == x.ex && tl.t >= ss.start) times = times.dropLast(1)
            }
            plan[i] = plan[i].copy(done = plan[i].done - 1)
            ss = ss.copy(plan = plan, idx = i)
        }
        ss = ss.open(t)
        commit(st.copy(log = st.log.dropLast(1), times = times, session = ss))
        _ui.update { it.copy(fb = null, draft = null, restEnd = 0L) }
        refreshDraft()
    }

    fun addSet() {
        val st = _state.value
        val ss = st.session ?: return
        val plan = ss.plan.toMutableList()
        val i = ss.idx
        if (i !in plan.indices) return
        plan[i] = plan[i].copy(sets = plan[i].sets + 1)
        commit(st.copy(session = ss.copy(plan = plan)))
    }

    fun skipExercise() {
        val st = _state.value
        val ss0 = st.session ?: return
        val item = ss0.plan.getOrNull(ss0.idx) ?: return
        val t = now()
        var ss = ss0.flush(t)
        var times = st.times
        val sec = ss.exSec(item.ex, t)
        if (sec > 0) times = times + TimeLog(t, dayKey(), item.ex, sec)
        ss = ss.copy(idx = ss.idx + 1).open(t)
        commit(st.copy(times = times, session = ss))
        _ui.update { it.copy(draft = null, fb = null, restEnd = 0L) }
        refreshDraft()
    }

    fun bumpLevel(d: Int) {
        val st = _state.value
        val ss = st.session ?: return
        val item = ss.plan.getOrNull(ss.idx) ?: return
        val e = EX[item.ex] ?: getExerciseOrDefault(item.ex)
        val l = st.lvl[item.ex] ?: 0
        commit(st.copy(lvl = st.lvl + (item.ex to max(0, minOf(e.ladder.size - 1, l + d)))))
    }

    fun adjustRest(deltaSec: Int) {
        _ui.update {
            if (deltaSec == 0) it.copy(restEnd = 0L)
            else it.copy(restEnd = it.restEnd + deltaSec * 1000L, restTotal = it.restTotal + deltaSec)
        }
    }

    // ---------- pause and restart ----------

    fun pause() {
        val st = _state.value
        val ss0 = st.session ?: return
        if (ss0.start == 0L || ss0.pausedAt != 0L) return
        val t = now()
        var ss = ss0.flush(t).copy(pausedAt = t)
        val u = _ui.value
        var dr = u.draft
        if (dr != null && dr.holdStart != 0L) {
            dr = dr.copy(reps = ((t - dr.holdStart) / 1000).toInt(), holdStart = 0L)
        }
        if (u.restEnd != 0L) ss = ss.copy(restLeftMs = max(0L, u.restEnd - t))
        commit(st.copy(session = ss))
        _ui.update { it.copy(restEnd = 0L, draft = dr) }
    }

    fun resume() {
        val st = _state.value
        val ss0 = st.session ?: return
        if (ss0.pausedAt == 0L) return
        val t = now()
        var ss = ss0.copy(pausedMs = ss0.pausedMs + (t - ss0.pausedAt), pausedAt = 0L).open(t)
        val left = ss.restLeftMs
        ss = ss.copy(restLeftMs = 0L)
        commit(st.copy(session = ss))
        if (left > 0) _ui.update { it.copy(restEnd = t + left, restTotal = max(it.restTotal, ceilSec(left))) }
    }

    fun confirmTap(kind: String, action: () -> Unit) {
        if (_ui.value.confirm == kind) {
            _ui.update { it.copy(confirm = "") }
            action()
        } else {
            _ui.update { it.copy(confirm = kind) }
        }
    }

    fun clearConfirm() { if (_ui.value.confirm.isNotEmpty()) _ui.update { it.copy(confirm = "") } }

    fun restartExercise() {
        val st = _state.value
        val ss0 = st.session ?: return
        val item = ss0.plan.getOrNull(ss0.idx) ?: return
        val t = now()
        var ss = ss0.flush(t)
        val log = st.log.filter { !(it.ex == item.ex && !it.seed && it.t >= ss.start) }
        val times = st.times.filter { !(it.ex == item.ex && it.t >= ss.start) }
        val plan = ss.plan.toMutableList()
        plan[ss.idx] = item.copy(done = 0)
        val acc = ss.exAccMs.toMutableMap()
        acc[item.ex] = 0L
        val lvl = if (item.ex in ss.lvl0) st.lvl + (item.ex to ss.lvl0[item.ex]!!) else st.lvl - item.ex
        ss = ss.copy(plan = plan, exAccMs = acc).open(t)
        commit(st.copy(log = log, times = times, lvl = lvl, session = ss))
        _ui.update { it.copy(fb = null, draft = null, restEnd = 0L) }
        refreshDraft()
    }

    fun clearAdvice() {
        commit(_state.value.copy(lastAdvice = null))
    }

    fun updateAiSettings(apiKey: String, model: String, thinkingLevel: String = "high", enabled: Boolean) {
        commit(_state.value.copy(aiSettings = AISettings(apiKey = apiKey.trim(), model = model, thinkingLevel = thinkingLevel, enabled = enabled)))
    }

    private fun registerEntityInExerciseMap(created: com.praveen.hypertrophy.data.db.ExerciseEntity) {
        if (EXERCISES.any { it.id == created.id }) return
        val ladder = try {
            GeminiService.jsonParser.decodeFromString<List<String>>(created.ladderJson)
        } catch (e: Exception) { listOf("Standard Tempo", "3s Lowering", "3s Lowering + Pause") }
        val tipsDo = try {
            GeminiService.jsonParser.decodeFromString<List<String>>(created.tipsDoJson)
        } catch (e: Exception) { listOf("Control eccentric") }
        val tipsDont = try {
            GeminiService.jsonParser.decodeFromString<List<String>>(created.tipsDontJson)
        } catch (e: Exception) { listOf("Avoid cheating") }

        val exObj = Ex(
            id = created.id,
            name = created.name,
            primary = created.primaryMuscles.split(",").map { it.trim() },
            secondary = if (created.secondaryMuscles.isNotBlank()) created.secondaryMuscles.split(",").map { it.trim() } else emptyList(),
            lo = created.loReps,
            hi = created.hiReps,
            rest = created.restSec,
            isSec = created.isSec,
            loaded = created.loaded,
            ladder = ladder,
            cue = created.cue,
            tipsDo = tipsDo,
            tipsDont = tipsDont
        )
        EXERCISES.add(exObj)
    }

    fun createExerciseWithAI(prompt: String, muscle: String) {
        val st = _state.value
        if (st.aiSettings.apiKey.isBlank()) return
        viewModelScope.launch {
            _isGeneratingExercise.value = true
            val created = GeminiService.generateExercise(
                apiKey = st.aiSettings.apiKey,
                model = st.aiSettings.model,
                thinkingLevel = st.aiSettings.thinkingLevel,
                userPrompt = prompt,
                targetMuscleKey = muscle
            )
            if (created != null) {
                exerciseDao.insert(created)
                registerEntityInExerciseMap(created)
            }
            _isGeneratingExercise.value = false
        }
    }

    /**
     * AI Auto-Tuning for Tomorrow's Session using historical sets, volume, and recovery.
     */
    fun autoTuneNextSession() {
        val st = _state.value
        val ai = st.aiSettings
        if (ai.apiKey.isBlank() || !ai.enabled) return

        viewModelScope.launch {
            _isGeneratingExercise.value = true
            val recentLogs = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(SetLog.serializer()),
                st.log.takeLast(15)
            )
            val tunedPlan = GeminiService.tuneNextDaySession(
                apiKey = ai.apiKey,
                model = ai.model,
                thinkingLevel = ai.thinkingLevel,
                recentLogsJson = recentLogs,
                soreMuscles = st.sore.toList(),
                targetBudgetMins = st.budget
            )
            if (tunedPlan != null && tunedPlan.isNotEmpty()) {
                val ss = st.session
                if (ss == null || ss.start == 0L) {
                    val updatedSession = (ss ?: newSession(st, false)).copy(plan = tunedPlan, idx = 0)
                    commit(st.copy(session = updatedSession))
                    refreshDraft()
                }
            }
            _isGeneratingExercise.value = false
        }
    }

    /**
     * AI Biomechanical Tuning for a specific exercise.
     */
    fun tuneSingleExercise(exerciseId: String, instruction: String) {
        val st = _state.value
        val ai = st.aiSettings
        if (ai.apiKey.isBlank() || !ai.enabled) return

        viewModelScope.launch {
            _isGeneratingExercise.value = true
            val current = exerciseDao.getById(exerciseId)
            if (current != null) {
                val tuned = GeminiService.tuneExercise(
                    apiKey = ai.apiKey,
                    model = ai.model,
                    thinkingLevel = ai.thinkingLevel,
                    currentExercise = current,
                    userInstruction = instruction
                )
                if (tuned != null) {
                    exerciseDao.insert(tuned)
                }
            }
            _isGeneratingExercise.value = false
        }
    }

    /**
     * Submits the deep biomechanical survey and generates dynamic personalized exercises
     * with Gemini 4 Hydrogen.
     */
    fun submitOnboarding(profile: OnboardingProfile, apiKey: String = "") {
        val st = _state.value
        val effectiveApiKey = if (apiKey.isNotBlank()) apiKey else st.aiSettings.apiKey
        val ai = st.aiSettings.copy(apiKey = effectiveApiKey, enabled = effectiveApiKey.isNotBlank())

        viewModelScope.launch {
            _isGeneratingExercise.value = true
            var dynamicPlan: List<PlanItem>? = null

            if (effectiveApiKey.isNotBlank()) {
                val result = GeminiService.generateDynamicProgramWithExercises(
                    apiKey = effectiveApiKey,
                    model = ai.model,
                    thinkingLevel = ai.thinkingLevel,
                    profile = profile
                )
                if (result != null && result.exercises.isNotEmpty()) {
                    exerciseDao.insertAll(result.exercises)
                    for (entity in result.exercises) {
                        registerEntityInExerciseMap(entity)
                    }
                    dynamicPlan = result.planItems
                }
            }

            val targetPlan = dynamicPlan ?: listOf(
                PlanItem("curl", 3, 0),
                PlanItem("bench", 3, 0),
                PlanItem("squat", 3, 0)
            )

            val newSess = Session(
                day = dayKey(),
                plan = targetPlan,
                idx = 0
            )

            commit(
                st.copy(
                    aiSettings = ai,
                    hasCompletedOnboarding = true,
                    onboardingProfile = profile,
                    session = newSess,
                    budget = profile.sessionBudgetMins
                )
            )
            refreshDraft()
            _isGeneratingExercise.value = false
        }
    }

    fun resetOnboarding() {
        commit(_state.value.copy(hasCompletedOnboarding = false))
    }

    fun restartSession() {
        val st = _state.value
        val ss0 = st.session ?: return
        val t = now()
        val ss = ss0.flush(t)
        val log = st.log.filter { it.seed || it.t < ss.start }
        val times = st.times.filter { it.t < ss.start }
        val plan = ss.plan.map { it.copy(done = 0) }
        val reset = ss.copy(
            plan = plan, idx = 0, start = 0L, pausedAt = 0L, pausedMs = 0L, restLeftMs = 0L,
            exAccMs = emptyMap(), segFrom = 0L,
        )
        commit(st.copy(log = log, times = times, lvl = if (ss0.start == 0L) st.lvl else ss.lvl0, session = reset))
        _ui.update { it.copy(fb = null, draft = null, restEnd = 0L) }
        refreshDraft()
    }

    fun resetAll() {
        val fresh = normalize(freshState())
        commit(fresh)
        _ui.update { Ui(tab = 1) }
        refreshDraft()
    }

    fun csv(): String {
        val rows = ArrayList<String>()
        rows.add("date,exercise,reps,rir,kg,level")
        val fmtDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        for (x in _state.value.log) {
            if (x.seed) continue
            rows.add(listOf(fmtDate.format(java.util.Date(x.t)), EX[x.ex]?.name ?: x.ex, x.reps, x.rir, x.kg, x.lvl).joinToString(","))
        }
        return rows.joinToString("\n")
    }
}
