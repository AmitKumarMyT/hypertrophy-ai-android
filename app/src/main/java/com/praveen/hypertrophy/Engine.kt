package com.praveen.hypertrophy

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.floor

// ---------- time helpers ----------

fun dayKey(t: Long = System.currentTimeMillis()): String {
    val c = Calendar.getInstance()
    c.timeInMillis = t
    return "" + c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH)
}

fun weekStart(): Long {
    val c = Calendar.getInstance()
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    val back = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0
    c.add(Calendar.DAY_OF_MONTH, -back)
    return c.timeInMillis
}

fun fmt(sec: Int): String {
    val s = if (sec < 0) 0 else sec
    return (s / 60).toString() + ":" + (s % 60).toString().padStart(2, '0')
}

// ---------- scoring ----------

fun hardCredit(rir: Int): Double = if (rir <= 2) 1.0 else if (rir == 3) 0.5 else 0.0

fun cred(x: SetLog, m: String): Double {
    val e = EX[x.ex] ?: return 0.0
    val h = hardCredit(x.rir)
    return if (m in e.primary) h else if (m in e.secondary) h * 0.5 else 0.0
}

fun weekSets(s: AppState, m: String): Double {
    val w = weekStart()
    return s.log.filter { it.t >= w }.sumOf { cred(it, m) }
}

fun lastTrained(s: AppState, m: String): Long =
    s.log.filter { cred(it, m) > 0 }.maxOfOrNull { it.t } ?: 0L

fun hoursSince(s: AppState, m: String): Double {
    val t = lastTrained(s, m)
    return if (t == 0L) Double.POSITIVE_INFINITY else (System.currentTimeMillis() - t) / 3_600_000.0
}

fun isReady(s: AppState, m: String): Boolean = hoursSince(s, m) >= s.restH

fun lastDone(s: AppState, id: String): Long = s.log.filter { it.ex == id }.maxOfOrNull { it.t } ?: 0L

fun setMin(id: String): Double = ((EX[id]?.rest ?: 60) + 45) / 60.0

// ---------- planner ----------

fun pickExercise(s: AppState, m: String, force: Boolean): String? {
    val c = EXERCISES.filter { e ->
        m in e.primary && (force || e.primary.all { q -> isReady(s, q) && q !in s.sore })
    }
    return c.minByOrNull { lastDone(s, it.id) }?.id
}

fun makePlan(s: AppState, force: Boolean): List<PlanItem> {
    val budget = s.budget.toDouble()
    var used = 0.0
    val plan = ArrayList<PlanItem>()
    val covered = HashSet<String>()
    val ms = MUSCLES
        .filter { force || (it.key !in s.sore && isReady(s, it.key) && it.target - weekSets(s, it.key) > 0) }
        .sortedWith(compareBy<Muscle> { it.tier }.thenByDescending { it.target - weekSets(s, it.key) })
    for (m in ms) {
        if (m.key in covered) continue
        val id = pickExercise(s, m.key, force) ?: continue
        val per = setMin(id)
        val n = minOf(2, floor((budget - used + 0.01) / per).toInt())
        if (n < 1) continue
        plan.add(PlanItem(id, n, 0))
        used += n * per
        EX[id]!!.primary.forEach { covered.add(it) }
    }
    for (i in plan.indices) {
        val per = setMin(plan[i].ex)
        if (plan[i].sets < 3 && used + per <= budget + 0.01) {
            plan[i] = plan[i].copy(sets = plan[i].sets + 1)
            used += per
        }
    }
    return plan
}

fun newSession(s: AppState, force: Boolean): Session =
    Session(day = dayKey(), plan = makePlan(s, force), forced = force)

fun planMinutes(ss: Session): Int =
    Math.round(ss.plan.sumOf { it.sets * setMin(it.ex) }).toInt()

// ---------- session clock helpers ----------

fun Session.flush(now: Long): Session {
    if (segFrom == 0L) return this
    val acc = exAccMs.toMutableMap()
    acc[segEx] = (acc[segEx] ?: 0L) + (now - segFrom)
    return copy(exAccMs = acc, segFrom = 0L)
}

fun Session.open(now: Long): Session {
    val it = plan.getOrNull(idx)
    return if (start != 0L && pausedAt == 0L && it != null) copy(segEx = it.ex, segFrom = now) else this
}

fun Session.exSec(ex: String, now: Long): Int {
    var ms = exAccMs[ex] ?: 0L
    if (segFrom != 0L && segEx == ex) ms += now - segFrom
    return Math.round(ms / 1000.0).toInt()
}

fun Session.elapsedSec(now: Long): Int {
    if (start == 0L) return 0
    val end = if (pausedAt != 0L) pausedAt else now
    return maxOf(0L, (end - start - pausedMs) / 1000).toInt()
}

fun Session.begin(now: Long, lvl: Map<String, Int>): Session =
    copy(start = now, lvl0 = lvl, exAccMs = emptyMap(), segFrom = 0L).open(now)

// ---------- history helpers ----------

fun lastSessionSets(s: AppState, id: String): List<SetLog> {
    val xs = s.log.filter { it.ex == id && !it.seed }
    if (xs.isEmpty()) return emptyList()
    val k = dayKey(xs.maxOf { it.t })
    return xs.filter { dayKey(it.t) == k }
}

fun targetReps(s: AppState, id: String): Int? {
    val l = lastSessionSets(s, id)
    if (l.isEmpty()) return null
    return minOf(EX[id]!!.hi, l.maxOf { it.reps } + 1)
}

// ---------- feedback ----------

data class SetFeedback(val tag: Int, val msg: String, val credit: Double) // tag 0 good, 1 warn, 2 bad

fun setFeedback(e: Ex, x: SetLog): SetFeedback {
    val cr = hardCredit(x.rir)
    return if (x.reps < e.lo && x.rir <= 1) {
        SetFeedback(2, "Below the " + e.lo + " to " + e.hi + " range at your limit. Next set, use an easier setup or stop 1 rep earlier.", cr)
    } else if (x.rir >= 4) {
        SetFeedback(1, "Too easy, " + x.rir + " or more reps left. Next set: slower lowering, or tap Make harder.", cr)
    } else if (x.rir == 3) {
        SetFeedback(1, "Close but short of a hard set. Take the next set to 1 or 2 reps from failure.", cr)
    } else if (x.reps >= e.hi) {
        SetFeedback(0, "Top of the range with " + x.rir + " left. Repeat this on every set and you level up.", cr)
    } else {
        SetFeedback(0, "Good stimulus. Match " + x.reps + " reps next set, or get one more.", cr)
    }
}

fun evaluateExercise(log: List<SetLog>, lvl: Map<String, Int>, id: String, today: String): Pair<String, Map<String, Int>> {
    val e = EX[id]!!
    val xs = log.filter { it.ex == id && !it.seed && dayKey(it.t) == today }
    if (xs.isEmpty()) return Pair("", lvl)
    val l = lvl[id] ?: 0
    val top = e.ladder.size - 1
    val best = xs.maxOf { it.reps }
    val avgR = xs.sumOf { it.rir }.toDouble() / xs.size
    if (xs.all { it.reps >= e.hi && it.rir <= 2 } || avgR >= 3.5) {
        return if (l < top) {
            Pair("Level up for next time: " + e.ladder[l + 1] + ".", lvl + (id to (l + 1)))
        } else {
            Pair("Top level reached. Add load or reps at the same setup.", lvl)
        }
    }
    if (xs[0].reps < e.lo && xs[0].rir <= 1 && l > 0) {
        return Pair("Eased for next time: " + e.ladder[l - 1] + ". Build reps back up first.", lvl + (id to (l - 1)))
    }
    return Pair("Same setup next time (" + e.ladder[l] + "). Aim for " + minOf(e.hi, best + 1) + " reps.", lvl)
}

fun ceilSec(ms: Long): Int = ceil(ms / 1000.0).toInt()

// ---------- persistence ----------

fun freshState(): AppState {
    val zone = ZoneId.of("Asia/Kolkata")
    fun t(day: Int): Long = ZonedDateTime.of(2026, 11, day, 18, 0, 0, 0, zone).toInstant().toEpochMilli()
    val seeds = listOf("curl" to 15, "hammer" to 15, "conc" to 15, "wrist" to 16, "row" to 16, "diamond" to 16)
        .mapIndexed { i, p -> SetLog("seed$i", t(p.second), p.first, 0, 2, 0.0, 0, true) }
    return AppState(log = seeds)
}

class Store(private val ctx: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val file: File get() = File(ctx.filesDir, "tracker.json")

    fun load(): AppState = try {
        json.decodeFromString<AppState>(file.readText())
    } catch (e: Exception) {
        freshState()
    }

    fun save(s: AppState) {
        try {
            val tmp = File(ctx.filesDir, "tracker.tmp")
            tmp.writeText(json.encodeToString(s))
            tmp.renameTo(file)
        } catch (e: Exception) {
            // keep running; the next save will retry
        }
    }
}
