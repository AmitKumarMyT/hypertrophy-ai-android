package com.praveen.hypertrophy

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// Poses are drawn in a 380 x 250 space whose y range starts at -35 (floor is y = 200).

data class Pt(val x: Float, val y: Float)

/**
 * 3D spatial coordinate for real-time camera orbit projections.
 * Lifter root (pelvis) is centered at (x=190, y=104, z=0).
 */
data class Pt3(val x: Float, val y: Float, val z: Float) {
    fun toPt(): Pt = Pt(x, y)
}

fun project3D(
    p: Pt3,
    azimuthDeg: Float,
    elevationDeg: Float,
    originX: Float = 190f,
    originY: Float = 104f,
    focalLength: Float = 450f
): Pt {
    val azRad = (azimuthDeg * PI / 180f).toFloat()
    val elRad = (elevationDeg * PI / 180f).toFloat()

    val dx = p.x - originX
    val dy = p.y - originY
    val dz = p.z

    // Azimuth rotation around Y-axis
    val x1 = dx * cos(azRad) + dz * sin(azRad)
    val y1 = dy
    val z1 = -dx * sin(azRad) + dz * cos(azRad)

    // Elevation tilt around X-axis
    val x2 = x1
    val y2 = y1 * cos(elRad) - z1 * sin(elRad)
    val z2 = y1 * sin(elRad) + z1 * cos(elRad)

    val scale = focalLength / (focalLength + z2).coerceAtLeast(80f)
    return Pt(originX + x2 * scale, originY + y2 * scale)
}

fun Pt.to3D(z: Float = 0f): Pt3 = Pt3(x, y, z)

sealed class Item
data class Ln(val a: Pt, val b: Pt, val hl: Boolean, val w: Float) : Item()
data class Hd(val c: Pt) : Item()
data class Dbl(val c: Pt) : Item()
data class Sc(val a: Pt, val b: Pt) : Item()

class PoseB {
    val items = ArrayList<Item>()
    fun ln(a: Pt, b: Pt, hl: Boolean = false, w: Float = 5f) { items.add(Ln(a, b, hl, w)) }
    fun head(c: Pt) { items.add(Hd(c)) }
    fun db(c: Pt) { items.add(Dbl(c)) }
    fun stand(x: Float) {
        ln(Pt(x, 200f), Pt(x + 12f, 200f))
        ln(Pt(x, 200f), Pt(x, 152f))
        ln(Pt(x, 152f), Pt(x, 104f))
    }
}

private fun pose(block: PoseB.() -> Unit): List<Item> {
    val b = PoseB()
    b.block()
    return b.items
}

private fun rad(d: Float): Float = d * PI.toFloat() / 180f

private fun ik(a: Pt, b: Pt, l1: Float, l2: Float, pr: Pt): Pt {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val r = max(hypot(dx, dy), 0.0001f)
    val d = min(max(r, kotlin.math.abs(l1 - l2) + 0.01f), l1 + l2 - 0.01f)
    val ux = dx / r
    val uy = dy / r
    val q = (d * d + l1 * l1 - l2 * l2) / (2f * d)
    val h = sqrt(max(0f, l1 * l1 - q * q))
    var px = -uy
    var py = ux
    if (px * pr.x + py * pr.y < 0f) { px = -px; py = -py }
    return Pt(a.x + ux * q + px * h, a.y + uy * q + py * h)
}

private fun sc(x1: Float, y1: Float, x2: Float, y2: Float): Item = Sc(Pt(x1, y1), Pt(x2, y2))

// ---------- poses (k runs 0..1 through the working part of the rep) ----------

private val pCurl: (Float) -> List<Item> = { k ->
    pose {
        val sh = Pt(190f, 48f); val el = Pt(193f, 82f)
        val t = rad(10f + 135f * k)
        val hd = Pt(el.x + 34f * sin(t), el.y + 34f * cos(t))
        stand(190f); ln(Pt(190f, 104f), sh, w = 7f); ln(sh, el, true); ln(el, hd, true); db(hd); head(Pt(192f, 32f))
    }
}
private val pConc: (Float) -> List<Item> = { k ->
    pose {
        val hip = Pt(165f, 150f); val kn = Pt(212f, 150f); val ft = Pt(214f, 200f)
        val t = rad(40f)
        val sh = Pt(hip.x + 62f * sin(t), hip.y - 62f * cos(t))
        val el = Pt(210f, 138f)
        val a = rad(10f + 140f * k)
        val hd = Pt(el.x + 36f * sin(a), el.y + 36f * cos(a))
        ln(ft, Pt(226f, 200f)); ln(ft, kn); ln(kn, hip); ln(hip, sh, w = 7f)
        ln(sh, el, true); ln(el, hd, true); db(hd); head(Pt(sh.x + 8f, sh.y - 12f))
    }
}
private val pOht: (Float) -> List<Item> = { k ->
    pose {
        val sh = Pt(190f, 48f); val el = Pt(192f, 18f)
        val p = rad(130f * k)
        val hd = Pt(el.x - 30f * sin(p), el.y - 30f * cos(p))
        stand(190f); ln(Pt(190f, 104f), sh, w = 7f); ln(sh, el, true); ln(el, hd, true); db(hd); head(Pt(188f, 32f))
    }
}
private val pPush: (Float) -> List<Item> = { k ->
    pose {
        val hand = Pt(255f, 196f); val sh = Pt(250f, 122f + 50f * k); val toe = Pt(70f, 196f)
        val el = ik(sh, hand, 38f, 38f, Pt(-1f, 0f))
        val dx = sh.x - toe.x; val dy = sh.y - toe.y; val r = hypot(dx, dy)
        ln(toe, sh, w = 7f); ln(sh, el, true); ln(el, hand, true)
        head(Pt(sh.x + dx / r * 18f, sh.y + dy / r * 18f - 4f))
    }
}
private val pWrist: (Float) -> List<Item> = { k ->
    pose {
        val hip = Pt(150f, 150f); val kn = Pt(200f, 150f); val ft = Pt(203f, 200f)
        val t = rad(40f)
        val sh = Pt(hip.x + 62f * sin(t), hip.y - 62f * cos(t))
        val el = Pt(192f, 138f); val wr = Pt(228f, 138f)
        val a = rad(-20f + 55f * k)
        val hd = Pt(wr.x + 18f * cos(a), wr.y - 18f * sin(a))
        ln(ft, Pt(215f, 200f)); ln(ft, kn); ln(kn, hip); ln(hip, sh, w = 7f)
        ln(sh, el); ln(el, wr, true); ln(wr, hd, true); db(hd); head(Pt(sh.x + 8f, sh.y - 12f))
    }
}
private val pLat: (Float) -> List<Item> = { k ->
    pose {
        val a = rad(5f + 82f * k)
        val l = Pt(170f, 62f); val r = Pt(210f, 62f)
        val hl = Pt(l.x - 62f * sin(a), l.y + 62f * cos(a))
        val hr = Pt(r.x + 62f * sin(a), r.y + 62f * cos(a))
        ln(Pt(176f, 200f), Pt(188f, 132f)); ln(Pt(204f, 200f), Pt(192f, 132f))
        ln(Pt(190f, 132f), Pt(190f, 62f), w = 7f); ln(l, r)
        ln(l, hl, true); ln(r, hr, true); db(hl); db(hr); head(Pt(190f, 40f))
    }
}
private val pDead: (Float) -> List<Item> = { k ->
    pose {
        val sh = Pt(72f, 195f); val hip = Pt(150f, 195f)
        val a = rad(70f * k)
        val dx = sin(a); val dy = -cos(a)
        val kn = Pt(hip.x + 46f * dx, hip.y + 46f * dy)
        val b = rad(90f * (1f - k))
        val sx = dx * cos(b) - dy * sin(b); val sy = dx * sin(b) + dy * cos(b)
        val ft = Pt(kn.x + 46f * sx, kn.y + 46f * sy)
        val m = rad(-80f * k)
        val ma = Pt(sh.x + 62f * sin(m), sh.y - 62f * cos(m))
        ln(sh, hip, w = 7f); ln(sh, Pt(sh.x, sh.y - 62f)); ln(sh, ma, true)
        ln(hip, Pt(hip.x, hip.y - 46f)); ln(Pt(hip.x, hip.y - 46f), Pt(hip.x + 46f, hip.y - 46f))
        ln(hip, kn, true); ln(kn, ft, true); head(Pt(54f, 190f))
    }
}
private val pPlank: (Float) -> List<Item> = { k ->
    pose {
        val toe = Pt(70f, 196f); val el = Pt(235f, 194f); val sh = Pt(235f, 150f - 3f * k)
        val dx = sh.x - toe.x; val dy = sh.y - toe.y; val r = hypot(dx, dy)
        ln(toe, sh, true, 7f); ln(sh, el); ln(el, Pt(272f, 194f))
        head(Pt(sh.x + dx / r * 18f, sh.y + dy / r * 18f - 4f))
    }
}
private val pChair: (Float) -> List<Item> = { k ->
    pose {
        val ft = Pt(200f, 200f); val hip = Pt(185f - 37f * k, 106f + 44f * k)
        val kn = ik(hip, ft, 48f, 48f, Pt(1f, 0f))
        val t = rad(6f + 34f * k)
        val sh = Pt(hip.x + 62f * sin(t), hip.y - 62f * cos(t))
        val hand = Pt(sh.x + 38f, sh.y + 6f)
        ln(ft, Pt(212f, 200f)); ln(ft, kn, true); ln(kn, hip, true); ln(hip, sh, w = 7f); ln(sh, hand)
        head(Pt(sh.x + 6f, sh.y - 14f))
    }
}
private val pBridge: (Float) -> List<Item> = { k ->
    pose {
        val sh = Pt(72f, 195f); val ft = Pt(190f, 196f); val hip = Pt(122f + 10f * k, 195f - 48f * k)
        val kn = ik(hip, ft, 46f, 46f, Pt(0f, -1f))
        val ux = kn.x - hip.x; val uy = kn.y - hip.y; val r = hypot(ux, uy)
        val tip = Pt(hip.x + 92f * ux / r, hip.y + 92f * uy / r)
        ln(sh, hip, true, 7f); ln(hip, kn, true); ln(kn, ft, true); ln(hip, tip)
        ln(sh, Pt(110f, 197f)); ln(ft, Pt(202f, 196f)); head(Pt(54f, 190f))
    }
}
private val pRow: (Float) -> List<Item> = { k ->
    pose {
        val hip = Pt(150f, 120f); val ft = Pt(168f, 200f)
        val kn = ik(hip, ft, 46f, 46f, Pt(1f, 0f))
        val sh = Pt(hip.x + 60f, hip.y - 16f); val ch = Pt(246f, 128f)
        val e2 = ik(sh, ch, 38f, 38f, Pt(0f, 1f))
        val u = rad(55f * k)
        val el = Pt(sh.x - 38f * sin(u), sh.y + 38f * cos(u))
        val hd = Pt(el.x, el.y + 34f)
        ln(ft, Pt(180f, 200f)); ln(ft, kn); ln(kn, hip); ln(hip, sh, w = 7f)
        ln(sh, e2); ln(e2, ch); ln(sh, el, true); ln(el, hd, true); db(hd); head(Pt(sh.x + 14f, sh.y - 4f))
    }
}
private val pSplit: (Float) -> List<Item> = { k ->
    pose {
        val hip = Pt(165f, 108f + 34f * k); val ff = Pt(195f, 200f); val rf = Pt(100f, 158f)
        val kf = ik(hip, ff, 50f, 50f, Pt(1f, 0f)); val kr = ik(hip, rf, 50f, 50f, Pt(0.3f, 1f))
        val sh = Pt(165f, hip.y - 66f); val hd = Pt(168f, sh.y + 62f)
        ln(rf, Pt(112f, 158f)); ln(rf, kr); ln(kr, hip); ln(ff, Pt(207f, 200f))
        ln(ff, kf, true); ln(kf, hip, true); ln(hip, sh, w = 7f); ln(sh, hd); db(hd); head(Pt(167f, sh.y - 14f))
    }
}
private val pRdl: (Float) -> List<Item> = { k ->
    pose {
        val th = rad(70f * k)
        val hip = Pt(172f - 22f * k, 106f + 6f * k); val ft = Pt(172f, 200f)
        val kn = ik(hip, ft, 48f, 48f, Pt(1f, 0f))
        val sh = Pt(hip.x + 62f * sin(th), hip.y - 62f * cos(th))
        val ph = rad(max(70f * k, 14f))
        val ff = Pt(hip.x - 88f * sin(ph), hip.y + 88f * cos(ph))
        val hd = Pt(sh.x, sh.y + 58f)
        ln(ft, Pt(184f, 200f)); ln(ft, kn, true); ln(kn, hip, true); ln(hip, ff, true)
        ln(hip, sh, w = 7f); ln(sh, hd); db(hd); head(Pt(sh.x + 15f * sin(th) + 3f, sh.y - 15f * cos(th)))
    }
}
private val pCalf: (Float) -> List<Item> = { k ->
    pose {
        val h = 14f * k
        val toe = Pt(198f, 200f); val heel = Pt(178f, 200f - h); val an = Pt(183f, 192f - h)
        val kn = Pt(183f, an.y - 44f); val hip = Pt(183f, kn.y - 44f); val sh = Pt(183f, hip.y - 56f)
        val tg = Pt(148f, 152f - h)
        val fk = ik(hip, tg, 44f, 44f, Pt(1f, 0.3f))
        val hand = Pt(228f, 100f)
        val el = ik(sh, hand, 41f, 41f, Pt(0.2f, 1f))
        ln(heel, toe); ln(an, heel, true); ln(an, kn, true); ln(kn, hip); ln(hip, fk); ln(fk, tg)
        ln(hip, sh, w = 7f); ln(sh, el); ln(el, hand); head(Pt(sh.x + 3f, sh.y - 14f))
    }
}
private val pCrunch: (Float) -> List<Item> = { k ->
    pose {
        val sh = Pt(72f, 195f); val hip = Pt(148f, 195f - 16f * k)
        val a = rad(-45f * k)
        val dx = sin(a); val dy = -cos(a)
        val kn = Pt(hip.x + 46f * dx, hip.y + 46f * dy)
        val sdx = -dy; val sdy = dx
        val ft = Pt(kn.x + 46f * sdx, kn.y + 46f * sdy)
        ln(sh, hip, true, 7f); ln(sh, Pt(128f, 197f)); ln(hip, kn); ln(kn, ft); head(Pt(54f, 190f))
    }
}
private val pSuper: (Float) -> List<Item> = { k ->
    pose {
        val hip = Pt(190f, 193f)
        val f = rad(16f * k); val p = rad(14f * k)
        val ft = Pt(190f - 92f * cos(p), 193f - 92f * sin(p))
        val sh = Pt(190f + 64f * cos(f), 193f - 64f * sin(f))
        val hd = Pt(sh.x + 62f * cos(f), sh.y - 62f * sin(f))
        ln(hip, ft, true); ln(hip, sh, true, 7f); ln(sh, hd, true); head(Pt(sh.x + 6f, sh.y - 14f))
    }
}

private val CH_FLOOR = listOf(sc(105f, 156f, 152f, 156f), sc(105f, 156f, 105f, 110f), sc(108f, 156f, 108f, 200f), sc(150f, 156f, 150f, 200f))
private val CH_SEAT = listOf(sc(130f, 154f, 185f, 154f), sc(130f, 154f, 130f, 112f), sc(133f, 154f, 133f, 200f), sc(183f, 154f, 183f, 200f))
private val CH_WR = listOf(sc(118f, 154f, 172f, 154f), sc(121f, 154f, 121f, 200f), sc(170f, 154f, 170f, 200f))
private val CH_ROW = listOf(sc(246f, 128f, 246f, 160f), sc(246f, 160f, 290f, 160f), sc(248f, 160f, 248f, 200f), sc(288f, 160f, 288f, 200f))
private val CH_CALF = listOf(sc(228f, 100f, 228f, 150f), sc(228f, 150f, 268f, 150f), sc(230f, 150f, 230f, 200f), sc(266f, 150f, 266f, 200f))
private val CH_SPLIT = listOf(sc(80f, 160f, 80f, 118f), sc(76f, 160f, 124f, 160f), sc(80f, 160f, 80f, 200f), sc(120f, 160f, 120f, 200f))

class AnimSpec(
    val pose: (Float) -> List<Item>,
    val scene: List<Item>,
    val a: Float,
    val h: Float,
    val c: Float,
    val caps: List<String>,
)

val ANIMS: Map<String, AnimSpec> = mapOf(
    "curl" to AnimSpec(pCurl, emptyList(), .4f, .1f, .4f, listOf("Curl up", "Squeeze", "Lower slowly")),
    "hammer" to AnimSpec(pCurl, emptyList(), .4f, .1f, .4f, listOf("Curl up, thumbs up", "Squeeze", "Lower slowly")),
    "conc" to AnimSpec(pConc, CH_SEAT, .4f, .1f, .4f, listOf("Curl up", "Squeeze", "Lower slowly")),
    "ohtri" to AnimSpec(pOht, emptyList(), .4f, .1f, .4f, listOf("Lower behind your head", "Feel the stretch", "Extend up")),
    "diamond" to AnimSpec(pPush, emptyList(), .45f, .1f, .35f, listOf("Lower slowly", "Pause", "Press up")),
    "pushup" to AnimSpec(pPush, emptyList(), .45f, .1f, .35f, listOf("Lower slowly", "Pause", "Press up")),
    "wrist" to AnimSpec(pWrist, CH_WR, .4f, .1f, .4f, listOf("Lift the back of your hand", "Squeeze", "Lower slowly")),
    "rcrunch" to AnimSpec(pCrunch, emptyList(), .3f, .15f, .4f, listOf("Curl hips up", "Squeeze", "Lower slowly")),
    "deadbug" to AnimSpec(pDead, emptyList(), .35f, .1f, .4f, listOf("Reach opposite arm and leg", "Hold", "Return")),
    "plank" to AnimSpec(pPlank, emptyList(), .1f, .8f, .1f, listOf("Brace", "Hold, keep breathing", "Release")),
    "splitsq" to AnimSpec(pSplit, CH_SPLIT, .45f, .1f, .3f, listOf("Lower slowly", "Pause at the bottom", "Drive up")),
    "chairsq" to AnimSpec(pChair, CH_FLOOR, .4f, .1f, .35f, listOf("Sit back to the chair", "Touch lightly", "Stand up")),
    "rdl" to AnimSpec(pRdl, emptyList(), .45f, .1f, .3f, listOf("Hips back, flat back", "Feel the stretch", "Hips forward")),
    "bridge" to AnimSpec(pBridge, emptyList(), .3f, .2f, .4f, listOf("Drive hips up", "Squeeze", "Lower slowly")),
    "calf" to AnimSpec(pCalf, CH_CALF, .25f, .15f, .45f, listOf("Rise up", "Squeeze at the top", "Lower slowly")),
    "superman" to AnimSpec(pSuper, emptyList(), .25f, .3f, .3f, listOf("Lift", "Hold 2 seconds", "Lower")),
    "row" to AnimSpec(pRow, CH_ROW, .35f, .15f, .4f, listOf("Pull elbow to hip", "Squeeze", "Lower slowly")),
    "latraise" to AnimSpec(pLat, emptyList(), .4f, .1f, .4f, listOf("Raise to shoulder height", "Pause", "Lower slowly")),
)

@Composable
fun AnimBox(id: String) {
    val spec = ANIMS[id] ?: return
    val ex = EX[id] ?: return
    val context = LocalContext.current
    val feedback = remember { com.praveen.hypertrophy.feedback.FeedbackManager.getInstance(context) }

    // Playback state
    var isPlaying by remember { mutableStateOf(true) }
    var speedMultiplier by remember { mutableFloatStateOf(1.0f) }
    var normalizedPhase by remember { mutableFloatStateOf(0f) }

    // Multi-angle & 3D Orbit Camera State
    var selectedPreset by remember { mutableStateOf("Side") } // "Side", "Front", "Top", "Iso", "Dual"
    var azimuth by remember { mutableFloatStateOf(90f) }
    var elevation by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var isDualView by remember { mutableStateOf(false) }
    var isXRay by remember { mutableStateOf(false) }

    // Update preset camera angles
    fun setCameraPreset(preset: String) {
        selectedPreset = preset
        feedback.playScrubTick()
        when (preset) {
            "Side" -> { azimuth = 90f; elevation = 0f; isDualView = false }
            "Front" -> { azimuth = 0f; elevation = 0f; isDualView = false }
            "Top" -> { azimuth = 0f; elevation = 70f; isDualView = false }
            "Iso" -> { azimuth = 45f; elevation = 25f; isDualView = false }
            "Dual" -> { isDualView = true }
        }
    }

    // Animation clock coroutine
    var lastTickPhase by remember { mutableIntStateOf(-1) }
    LaunchedEffect(isPlaying, speedMultiplier) {
        val baseDurationMs = 5200f
        var lastTimeNanos = 0L
        while (isPlaying) {
            withFrameNanos { frameTimeNanos ->
                if (lastTimeNanos != 0L) {
                    val deltaSec = (frameTimeNanos - lastTimeNanos) / 1_000_000_000f
                    val deltaPhase = (deltaSec * 1000f * speedMultiplier) / baseDurationMs
                    val nextPhase = (normalizedPhase + deltaPhase) % 1.0f
                    normalizedPhase = nextPhase

                    // Check cadence inflection point (inflection at 0.0 top, spec.a + spec.h bottom)
                    val currentPhaseSegment = when {
                        nextPhase < spec.a -> 0
                        nextPhase < spec.a + spec.h -> 1
                        nextPhase < spec.a + spec.h + spec.c -> 2
                        else -> 3
                    }
                    if (currentPhaseSegment != lastTickPhase && (currentPhaseSegment == 1 || currentPhaseSegment == 0)) {
                        lastTickPhase = currentPhaseSegment
                        feedback.playCadenceTick()
                    }
                }
                lastTimeNanos = frameTimeNanos
            }
        }
    }

    val p = normalizedPhase
    val s: Float
    val i: Int
    if (p < spec.a) { s = p / spec.a; i = 0 }
    else if (p < spec.a + spec.h) { s = 1f; i = 1 }
    else if (p < spec.a + spec.h + spec.c) { s = 1f - (p - spec.a - spec.h) / spec.c; i = 2 }
    else { s = 0f; i = 3 }
    val k = s * s * (3f - 2f * s)

    // Volumetric 3D mannequin and equipment primitives
    val (lifterPrimitives, equipmentPrimitives) = remember(id, (k * 150f).roundToInt()) {
        com.praveen.hypertrophy.render3d.Mannequin3D.solveExercisePose(id, k)
    }

    val ink = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surface

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            // Viewport Section (Single 3D Orbit or Dual Synchronized View)
            if (isDualView) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Viewport 1: Side Profile (90°)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(4.dp)
                    ) {
                        ExerciseCanvas3D(
                            lifterPrimitives = lifterPrimitives,
                            equipmentPrimitives = equipmentPrimitives,
                            azimuth = 90f,
                            elevation = 0f,
                            zoom = 1.0f,
                            isXRay = isXRay,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.2f)
                        )
                        Text(
                            "SIDE 3D",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Viewport 2: Front / 3D Orbit Profile
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    azimuth = (azimuth + dragAmount.x * 0.7f) % 360f
                                    elevation = (elevation - dragAmount.y * 0.5f).coerceIn(-40f, 80f)
                                }
                            }
                            .padding(4.dp)
                    ) {
                        ExerciseCanvas3D(
                            lifterPrimitives = lifterPrimitives,
                            equipmentPrimitives = equipmentPrimitives,
                            azimuth = azimuth,
                            elevation = elevation,
                            zoom = zoomScale,
                            isXRay = isXRay,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.2f)
                        )
                        Text(
                            "ORBIT 3D",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            } else {
                // Single 3D Orbit Canvas with touch drag rotation and pinch-to-zoom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                azimuth = (azimuth + dragAmount.x * 0.7f) % 360f
                                elevation = (elevation - dragAmount.y * 0.5f).coerceIn(-40f, 80f)
                                selectedPreset = "Orbit"
                            }
                        }
                ) {
                    ExerciseCanvas3D(
                        lifterPrimitives = lifterPrimitives,
                        equipmentPrimitives = equipmentPrimitives,
                        azimuth = azimuth,
                        elevation = elevation,
                        zoom = zoomScale,
                        isXRay = isXRay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(380f / 240f)
                    )
                    Text(
                        "${selectedPreset.uppercase()} 3D (DRAG TO ROTATE)",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Camera Presets & X-Ray Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("Side", "Front", "Top", "Iso", "Dual").forEach { preset ->
                        val isSel = (preset == "Dual" && isDualView) || (!isDualView && selectedPreset == preset)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) accent else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { setCameraPreset(preset) }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // X-Ray Equipment Ghosting Toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isXRay) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable {
                            isXRay = !isXRay
                            feedback.playScrubTick()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isXRay) "⚡ X-RAY ON" else "X-RAY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isXRay) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Playback Phase Caption
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (i == 3) "Reset" else spec.caps[i],
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                Text(
                    text = "${(p * 100).toInt()}% • ${(speedMultiplier)}x",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Precision Scrubber Timeline Slider
            Slider(
                value = normalizedPhase,
                onValueChange = { newPhase ->
                    isPlaying = false
                    normalizedPhase = newPhase
                    feedback.playScrubTick()
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth().height(36.dp)
            )

            // Speed Chips & Play/Pause Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isPlaying) MaterialTheme.colorScheme.secondaryContainer else accent)
                        .clickable {
                            isPlaying = !isPlaying
                            feedback.playScrubTick()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isPlaying) "⏸ PAUSE" else "▶ PLAY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPlaying) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Speed Stepper Chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                        val isSpeedSel = speedMultiplier == spd
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSpeedSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable {
                                    speedMultiplier = spd
                                    feedback.playScrubTick()
                                }
                                .padding(horizontal = 7.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${spd}x",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSpeedSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSpeedSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseCanvas3D(
    lifterPrimitives: List<com.praveen.hypertrophy.render3d.Primitive3D>,
    equipmentPrimitives: List<com.praveen.hypertrophy.render3d.Primitive3D>,
    azimuth: Float,
    elevation: Float,
    zoom: Float = 1.0f,
    isXRay: Boolean = false,
    modifier: Modifier = Modifier
) {
    val camera = remember(azimuth, elevation, zoom) {
        com.praveen.hypertrophy.render3d.Camera3D(
            azimuthDeg = azimuth,
            elevationDeg = elevation,
            zoom = zoom
        )
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Combine all primitives and depth sort using Painter's algorithm
        val allPrimitives = ArrayList<com.praveen.hypertrophy.render3d.Primitive3D>(
            equipmentPrimitives.size + lifterPrimitives.size
        )
        allPrimitives.addAll(equipmentPrimitives)
        allPrimitives.addAll(lifterPrimitives)

        // Project and depth sort: highest centerZ / distance drawn first
        val azRad = (azimuth * Math.PI / 180.0).toFloat()
        val elRad = (elevation * Math.PI / 180.0).toFloat()

        allPrimitives.sortByDescending { prim ->
            // Projected camera Z depth
            val z = prim.centerZ
            -z * sin(azRad) * cos(elRad)
        }

        for (prim in allPrimitives) {
            prim.draw(this, camera, w, h, isXRay)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ExerciseAnimPreview() {
    HypertrophyTheme {
        Column(Modifier.padding(16.dp)) {
            AnimBox(id = "curl")
        }
    }
}

