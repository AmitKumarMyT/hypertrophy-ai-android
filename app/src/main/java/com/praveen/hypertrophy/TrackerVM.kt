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
import kotlinx.coroutines.flow.StateFlow
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
)

class TrackerVM(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    private val _state = MutableStateFlow(normalize(store.load()))
    val state: StateFlow<AppState> = _state
    private val _ui = MutableStateFlow(Ui())
    val ui: StateFlow<Ui> = _ui

    init {
        store.save(_state.value)
        refreshDraft()
        viewModelScope.launch {
            while (true) {
                delay(250)
                val u = _ui.value
                if (u.restEnd != 0L && System.currentTimeMillis() >= u.restEnd) {
                    alert()
                    _ui.update { it.copy(restEnd = 0L) }
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
        try {
            ToneGenerator(AudioManager.STREAM_ALARM, 80).startTone(ToneGenerator.TONE_PROP_BEEP, 300)
        } catch (e: Exception) { /* no sound */ }
        try {
            val vm = getApplication<Application>().getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
        } catch (e: Exception) { /* no vibration */ }
    }

    // ---------- draft ----------

    private fun makeDraft(st: AppState, it: PlanItem): Draft {
        val e = EX[it.ex]!!
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
        val e = EX[item.ex]!!
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
        if (st.aiSettings.enabled && st.aiSettings.apiKey.isNotBlank() && !finished) {
            viewModelScope.launch {
                val targets = (e.primary + e.secondary).joinToString(", ") { MUSCLE[it]?.name ?: it }
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
        val e = EX[item.ex]!!
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

    fun updateAiSettings(apiKey: String, model: String, enabled: Boolean) {
        commit(_state.value.copy(aiSettings = AISettings(apiKey = apiKey.trim(), model = model, enabled = enabled)))
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
