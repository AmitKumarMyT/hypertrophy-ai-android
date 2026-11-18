package com.praveen.hypertrophy

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun rememberNow(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(250)
    }
}

@Composable
private fun ScreenContent(vm: TrackerVM, ui: Ui, maxWidthDp: androidx.compose.ui.unit.Dp) {
    Column(
        Modifier
            .widthIn(max = maxWidthDp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (ui.tab == 0) TodayScreen(vm) else ProgressScreen(vm)
    }
}

@Composable
fun AppScreen(vm: TrackerVM) {
    val ui by vm.ui.collectAsState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp
        if (isWide) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail {
                    NavigationRailItem(
                        selected = ui.tab == 0, onClick = { vm.setTab(0) },
                        icon = { Icon(Icons.Filled.Home, contentDescription = null) }, label = { Text("Today") },
                    )
                    NavigationRailItem(
                        selected = ui.tab == 1, onClick = { vm.setTab(1) },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }, label = { Text("Progress") },
                    )
                }
                Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    ScreenContent(vm, ui, 900.dp)
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = ui.tab == 0, onClick = { vm.setTab(0) },
                            icon = { Icon(Icons.Filled.Home, contentDescription = null) }, label = { Text("Today") },
                        )
                        NavigationBarItem(
                            selected = ui.tab == 1, onClick = { vm.setTab(1) },
                            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }, label = { Text("Progress") },
                        )
                    }
                },
            ) { inner ->
                Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.TopCenter) {
                    ScreenContent(vm, ui, 600.dp)
                }
            }
        }
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

@Composable
private fun Muted(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ---------------------------------------------------------------- Today

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(vm: TrackerVM) {
    val st by vm.state.collectAsState()
    val ui by vm.ui.collectAsState()
    val ss = st.session ?: return
    val running = ss.start != 0L && ss.pausedAt == 0L && ss.idx < ss.plan.size

    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    val finished = ss.plan.isNotEmpty() && ss.idx >= ss.plan.size
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        AISettingsDialog(
            settings = st.aiSettings,
            onDismiss = { showSettings = false },
            onSave = { key, model, enabled ->
                vm.updateAiSettings(key, model, enabled)
                showSettings = false
            }
        )
    }

    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date()), style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Filled.Settings, contentDescription = "AI Coach Settings")
            }
        }
        Muted("Priority order: arms, core, legs, lower back, then the rest.")
        Text("Time today", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 20, 30).forEach { b ->
                FilterChip(selected = st.budget == b, onClick = { vm.setBudget(b) }, label = { Text("$b min") })
            }
        }
        var open by remember { mutableStateOf(false) }
        TextButton(onClick = { open = !open }) { Text(if (open) "Hide recovery settings" else "Recovery settings") }
        if (open) {
            Text("Still sore today (skipped in the plan)", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MUSCLES.forEach { m ->
                    FilterChip(selected = m.key in st.sore, onClick = { vm.toggleSore(m.key) }, label = { Text(m.name) })
                }
            }
            Text("Rest before training a muscle again: " + st.restH + " h", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { vm.changeRestH(-6) }) { Text("-6 h") }
                FilledTonalButton(onClick = { vm.changeRestH(6) }) { Text("+6 h") }
            }
            Muted("Your preference is 42 hours. Treat it as a guide: if a muscle is still sore, mark it above.")
        }
    }

    if (ss.plan.isEmpty()) {
        Panel {
            Text("Nothing is ready", style = MaterialTheme.typography.titleLarge)
            Muted("Every muscle is either still recovering or already at its weekly target. You can still train.")
            Button(onClick = { vm.rebuild(true) }, modifier = Modifier.fillMaxWidth()) { Text("Train anyway") }
        }
        return
    }

    if (finished) {
        FinishPanel(vm, st, ss, ui)
        return
    }

    if (ss.start != 0L) SessionBar(vm, ss, ui)

    if (ss.start == 0L && ui.fb == null) {
        Panel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Today's plan", style = MaterialTheme.typography.titleLarge)
                Text(planMinutes(ss).toString() + " min", style = MaterialTheme.typography.labelLarge)
            }
            val first = ss.plan[0].ex
            Muted("First up: " + EX[first]!!.name)
            AnimBox(first)
            ExerciseTips(first)
            PlanRows(ss)
            Button(onClick = { vm.startSession() }, modifier = Modifier.fillMaxWidth()) { Text("Start first set") }
            OutlinedButton(onClick = { vm.rebuild(null) }, modifier = Modifier.fillMaxWidth()) { Text("Rebuild plan") }
        }
    } else {
        if (ss.pausedAt != 0L) {
            PausedPanel(vm, ss)
        } else {
            // Live Gemini Hypertrophy Coach Card
            st.lastAdvice?.let { advice ->
                CoachAdvicePanel(advice = advice, onDismiss = { vm.clearAdvice() })
            }
            FeedbackPanel(vm, ui)
            if (ui.restEnd != 0L) RestPanel(vm, ss, ui) else ExercisePanel(vm, st, ss, ui)
        }
        Panel {
            Text("Plan", style = MaterialTheme.typography.titleMedium)
            PlanRows(ss)
        }
    }
}

@Composable
private fun PlanRows(ss: Session) {
    val now by rememberNow()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ss.plan.forEachIndexed { i, it ->
            val e = EX[it.ex]!!
            val current = i == ss.idx && ss.start != 0L
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(e.name, fontWeight = FontWeight.SemiBold)
                        Muted(
                            it.sets.toString() + " sets of " + e.lo + " to " + e.hi + (if (e.isSec) " s" else "") + " · " +
                                e.primary.joinToString(", ") { m -> MUSCLE[m]!!.name },
                        )
                    }
                    Text(
                        if (i < ss.idx) "Done · " + fmt(ss.exSec(it.ex, now)) else it.done.toString() + "/" + it.sets,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionBar(vm: TrackerVM, ss: Session, ui: Ui) {
    val now by rememberNow()
    val el = ss.elapsedSec(now)
    val budgetSec = vm.state.collectAsState().value.budget * 60
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Muted("Session time")
            Text(fmt(el) + " / " + (budgetSec / 60) + ":00", style = MaterialTheme.typography.titleLarge)
        }
        LinearProgressIndicator(progress = { minOf(1f, el / budgetSec.toFloat()) }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (ss.idx < ss.plan.size) {
                FilledTonalButton(onClick = { if (ss.pausedAt != 0L) vm.resume() else vm.pause() }) {
                    Text(if (ss.pausedAt != 0L) "Resume session" else "Pause session")
                }
            }
            OutlinedButton(onClick = { vm.confirmTap("sess") { vm.restartSession() } }) {
                Text(if (ui.confirm == "sess") "Tap again to restart" else "Restart session")
            }
        }
    }
}

@Composable
private fun PausedPanel(vm: TrackerVM, ss: Session) {
    Panel {
        Text("Paused", style = MaterialTheme.typography.headlineSmall)
        Muted("Your session clock" + (if (ss.restLeftMs > 0) " and rest timer are" else " is") + " stopped.")
        if (ss.restLeftMs > 0) {
            Muted("Rest left")
            Text(fmt(ceilSec(ss.restLeftMs)), fontSize = 44.sp, fontWeight = FontWeight.SemiBold)
        }
        Button(onClick = { vm.resume() }, modifier = Modifier.fillMaxWidth()) { Text("Resume session") }
    }
}

@Composable
private fun FeedbackPanel(vm: TrackerVM, ui: Ui) {
    val fb = ui.fb ?: return
    val cs = MaterialTheme.colorScheme
    val bg = when (fb.tag) { 0 -> cs.primaryContainer; 1 -> cs.tertiaryContainer; else -> cs.errorContainer }
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(fb.name, style = MaterialTheme.typography.titleLarge)
            Text(if (fb.credit >= 1) "Hard set +1" else if (fb.credit > 0) "Half set +0.5" else "No credit", style = MaterialTheme.typography.labelLarge)
        }
        Surface(shape = MaterialTheme.shapes.medium, color = bg) { Text(fb.msg, Modifier.padding(12.dp)) }
        if (fb.timeSec > 0) {
            Surface(shape = MaterialTheme.shapes.medium, color = cs.primaryContainer) {
                Text("Exercise time " + fmt(fb.timeSec) + ", including rest between sets.", Modifier.padding(12.dp))
            }
        }
        if (fb.note.isNotEmpty()) {
            Surface(shape = MaterialTheme.shapes.medium, color = cs.secondaryContainer) {
                Text("Next time. " + fb.note, Modifier.padding(12.dp))
            }
        }
        Muted("This week: " + fb.week)
        OutlinedButton(onClick = { vm.undoLast() }) { Text("Undo that set") }
    }
}

@Composable
private fun RestPanel(vm: TrackerVM, ss: Session, ui: Ui) {
    val now by rememberNow()
    val left = maxOf(0, ceilSec(ui.restEnd - now))
    val next = ss.plan.getOrNull(ss.idx)
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Rest", style = MaterialTheme.typography.titleMedium)
            if (next != null) Muted("Next: " + EX[next.ex]!!.name)
        }
        Text(fmt(left), fontSize = 56.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(
            progress = { if (ui.restTotal <= 0) 0f else (1f - left / ui.restTotal.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { vm.adjustRest(15) }) { Text("+15 s") }
            FilledTonalButton(onClick = { vm.adjustRest(-15) }) { Text("-15 s") }
            OutlinedButton(onClick = { vm.adjustRest(0) }) { Text("Skip rest") }
        }
        if (next != null) {
            AnimBox(next.ex)
            ExerciseTips(next.ex)
        }
    }
}

@Composable
private fun ExercisePanel(vm: TrackerVM, st: AppState, ss: Session, ui: Ui) {
    val item = ss.plan.getOrNull(ss.idx) ?: return
    val dr = ui.draft ?: return
    val e = EX[item.ex]!!
    val lv = st.lvl[item.ex] ?: 0
    val tg = targetReps(st, item.ex)
    val now by rememberNow()

    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(e.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Text("Set " + (item.done + 1) + " of " + item.sets + " · " + fmt(ss.exSec(item.ex, now)), style = MaterialTheme.typography.labelLarge)
        }
        Muted(e.cue)
        AnimBox(item.ex)
        ExerciseTips(item.ex)

        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Muted("Setup, level " + (lv + 1) + " of " + e.ladder.size)
                Text(e.ladder[lv], fontWeight = FontWeight.SemiBold)
                val unit = if (e.isSec) " seconds" else " reps"
                Muted("Target: " + (if (tg != null) tg.toString() + unit + " or more" else e.lo.toString() + " to " + e.hi + unit) + ", finishing 1 to 2 reps short of failure.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.bumpLevel(-1) }) { Text("Make easier") }
                    OutlinedButton(onClick = { vm.bumpLevel(1) }) { Text("Make harder") }
                }
            }
        }

        Text(if (e.isSec) "Seconds held" else "Reps completed", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = { vm.repsDelta(-1) }) { Text("-", fontSize = 24.sp) }
            val shown = if (dr.holdStart != 0L) ((now - dr.holdStart) / 1000).toInt() else dr.reps
            Text(shown.toString(), fontSize = 48.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            FilledTonalButton(onClick = { vm.repsDelta(1) }) { Text("+", fontSize = 24.sp) }
        }
        if (e.isSec) {
            OutlinedButton(onClick = { vm.toggleHold() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (dr.holdStart != 0L) "Stop hold" else "Start hold timer")
            }
        }

        Text("Reps left in the tank at the end (RIR)", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..4).forEach { r ->
                FilterChip(selected = dr.rir == r, onClick = { vm.setRir(r) }, label = { Text(if (r == 4) "4+" else r.toString()) })
            }
        }
        Muted("0 = could not do another. 1 to 2 is the sweet spot.")

        if (e.loaded) {
            OutlinedTextField(
                value = dr.kg,
                onValueChange = { vm.setKg(it) },
                label = { Text("Load per hand, kg (blank for bodyweight)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (dr.err.isNotEmpty()) Text(dr.err, color = MaterialTheme.colorScheme.error)

        Button(onClick = { vm.logSet() }, modifier = Modifier.fillMaxWidth()) { Text("Log set") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.addSet() }) { Text("Add a set") }
            OutlinedButton(onClick = { vm.skipExercise() }) { Text("Skip exercise") }
        }
        OutlinedButton(onClick = { vm.confirmTap("ex") { vm.restartExercise() } }) {
            Text(if (ui.confirm == "ex") "Tap again to restart this exercise" else "Restart exercise")
        }
    }
}

@Composable
private fun ExerciseTips(id: String) {
    val e = EX[id] ?: return
    val cs = MaterialTheme.colorScheme
    var open by remember(id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = MaterialTheme.shapes.medium, color = cs.primaryContainer) {
            Column(Modifier.padding(12.dp)) {
                Text("Do", fontWeight = FontWeight.SemiBold, color = cs.onPrimaryContainer)
                e.tipsDo.forEach { Text("• $it", color = cs.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        Surface(shape = MaterialTheme.shapes.medium, color = cs.errorContainer) {
            Column(Modifier.padding(12.dp)) {
                Text("Do not", fontWeight = FontWeight.SemiBold, color = cs.onErrorContainer)
                e.tipsDont.forEach { Text("• $it", color = cs.onErrorContainer, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        TextButton(onClick = { open = !open }) { Text(if (open) "Hide rules for maximum growth" else "Rules for maximum growth") }
        if (open) GENERAL_RULES.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun FinishPanel(vm: TrackerVM, st: AppState, ss: Session, ui: Ui) {
    val today = dayKey()
    val xs = st.log.filter { dayKey(it.t) == today && !it.seed }
    val hard = xs.sumOf { hardCredit(it.rir) }
    val vol = xs.sumOf { if (it.kg > 0) it.kg * it.reps else 0.0 }
    val mins = if (ss.start != 0L) maxOf(1, Math.round(ss.elapsedSec(System.currentTimeMillis()) / 60.0).toInt()) else 0
    if (ui.fb != null) FeedbackPanel(vm, ui)
    Panel {
        Text("Session done", style = MaterialTheme.typography.headlineSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("Sets", xs.size.toString())
            Stat("Hard sets", String.format("%.1f", hard).removeSuffix(".0"))
            Stat("Minutes", mins.toString())
            Stat("Load moved", if (vol > 0) Math.round(vol).toString() + " kg" else "bodyweight")
        }
        Muted("Time per exercise, including rest between sets")
        ss.plan.forEach {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(EX[it.ex]!!.name)
                Text(fmt(ss.exSec(it.ex, System.currentTimeMillis())), fontWeight = FontWeight.SemiBold)
            }
        }
        Muted("Eat protein and sleep. Open Progress to see which muscle is ready next.")
        Button(onClick = { vm.newBlock() }, modifier = Modifier.fillMaxWidth()) { Text("Plan another block") }
        OutlinedButton(onClick = { vm.confirmTap("sess") { vm.restartSession() } }) {
            Text(if (ui.confirm == "sess") "Tap again to restart" else "Restart this session")
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Muted(label)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

// ---------------------------------------------------------------- Progress

@Composable
fun ProgressScreen(vm: TrackerVM) {
    val st by vm.state.collectAsState()
    val ui by vm.ui.collectAsState()
    val ctx = LocalContext.current

    Panel {
        Text("This week", style = MaterialTheme.typography.headlineSmall)
        Muted("Hard sets per muscle since Monday. A set 1 to 2 reps from failure counts 1, three reps away counts half. Secondary muscles get half credit.")
    }
    TIER_NAMES.forEach { (tier, name) ->
        Panel {
            Text(name, style = MaterialTheme.typography.titleMedium)
            MUSCLES.filter { it.tier == tier }.forEach { m ->
                val v = weekSets(st, m.key)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(m.name, fontWeight = FontWeight.SemiBold)
                        Text((Math.round(v * 2) / 2.0).toString().removeSuffix(".0") + " / " + m.target)
                    }
                    LinearProgressIndicator(progress = { (v / m.target).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Muted(readyText(st, m.key))
                }
            }
        }
    }

    val ids = st.log.filter { !it.seed }.map { it.ex }.distinct()
    Panel {
        Text("Exercises", style = MaterialTheme.typography.headlineSmall)
        if (ids.isEmpty()) {
            Muted("Log your first set and each exercise shows up here with its next target.")
        } else {
            ids.forEach { id ->
                val e = EX[id]!!
                val l = lastSessionSets(st, id)
                val best = l.maxByOrNull { it.reps }!!
                val avg = l.sumOf { it.rir }.toDouble() / l.size
                val lt = st.times.lastOrNull { it.ex == id }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(e.name, fontWeight = FontWeight.SemiBold)
                    Muted(
                        "Last: " + l.joinToString(", ") { it.reps.toString() } + (if (e.isSec) " s" else " reps") +
                            (if (best.kg > 0) " at " + best.kg.toString().removeSuffix(".0") + " kg" else "") +
                            " · avg RIR " + String.format("%.1f", avg) + (if (lt != null) " · " + fmt(lt.sec) else ""),
                    )
                    Muted("Next: " + e.ladder[st.lvl[id] ?: 0] + " · aim " + (targetReps(st, id) ?: e.lo) + "+")
                }
            }
        }
    }

    Panel {
        Text("Data", style = MaterialTheme.typography.titleMedium)
        Muted("The 15 and 16 Nov sessions came from your handover. Their reps were not recorded, so they only count for recovery and weekly sets.")
        OutlinedButton(onClick = {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, vm.csv())
            }
            ctx.startActivity(Intent.createChooser(send, "Export log"))
        }) { Text("Export log as CSV") }
        OutlinedButton(onClick = { vm.confirmTap("reset") { vm.resetAll() } }) {
            Text(if (ui.confirm == "reset") "Tap again to erase everything" else "Reset all data")
        }
    }
}

// ---------------------------------------------------------------- Gemini AI Coach UI

@Composable
fun CoachAdvicePanel(advice: HypertrophyAdvice, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (advice.fatigueWarning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Coach Gemini Insight",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (advice.fatigueWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                )
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = if (advice.fatigueWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
            Text(
                text = advice.reasoning,
                style = MaterialTheme.typography.bodyMedium,
                color = if (advice.fatigueWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
            )
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        text = "Next: ${advice.action.replace('_', ' ')}" +
                                (if (advice.suggestedWeightDeltaKg != 0.0) " (${if (advice.suggestedWeightDeltaKg > 0) "+" else ""}${advice.suggestedWeightDeltaKg} kg)" else "") +
                                " · Rest: ${advice.suggestedRestSeconds}s",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (advice.coachingCue.isNotBlank()) {
                        Text(
                            text = "Cue: ${advice.coachingCue}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AISettingsDialog(
    settings: AISettings,
    onDismiss: () -> Unit,
    onSave: (apiKey: String, model: String, enabled: Boolean) -> Unit
) {
    var key by remember { mutableStateOf(settings.apiKey) }
    var selectedModel by remember { mutableStateOf(settings.model) }
    var enabled by remember { mutableStateOf(settings.enabled) }

    val models = listOf(
        "gemini-3.8-flash" to "Gemini 3.8 Flash (High Reasoning)",
        "gemini-3.5-flash-lite" to "Gemini 3.5 Flash-Lite (Fastest Response)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gemini AI Coach Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable AI Hypertrophy Coach")
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                Text(
                    "Select Model:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                models.forEach { (mId, mLabel) ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedModel == mId,
                            onClick = { selectedModel = mId }
                        )
                        Text(
                            mLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(key, selectedModel, enabled) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun readyText(st: AppState, m: String): String {
    if (m in st.sore) return "Sore, skipped in the plan"
    val h = hoursSince(st, m)
    if (h == Double.POSITIVE_INFINITY) return "Fresh"
    if (h >= st.restH) return "Ready"
    return "Ready in " + Math.ceil(st.restH - h).toInt() + " h"
}
