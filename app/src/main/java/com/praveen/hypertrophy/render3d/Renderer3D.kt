package com.praveen.hypertrophy.render3d

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * 3D Primitive types rendered with back-to-front Painter's Algorithm depth sorting.
 */
sealed class Primitive3D {
    abstract val centerZ: Float
    abstract fun draw(scope: DrawScope, camera: Camera3D, width: Float, height: Float, isXRay: Boolean)
}

/**
 * Directional lighting helper with top-right light source.
 */
object Lighting3D {
    private val lightDir = Vec3(0.35f, -0.75f, -0.55f).normalized()

    fun shadeColor(baseColor: Color, normal: Vec3, isHighlight: Boolean = false): Color {
        val n = normal.normalized()
        val diff = (n.dot(lightDir)).coerceIn(0f, 1f)
        val ambient = if (isHighlight) 0.55f else 0.35f
        val factor = ambient + (1f - ambient) * diff
        return baseColor.copy(
            red = (baseColor.red * factor).coerceIn(0f, 1f),
            green = (baseColor.green * factor).coerceIn(0f, 1f),
            blue = (baseColor.blue * factor).coerceIn(0f, 1f),
            alpha = baseColor.alpha
        )
    }
}

/**
 * 3D Volumetric Cylinder / Capsule (e.g., limb, barbell shaft, rack post).
 */
data class Cylinder3D(
    val a: Vec3,
    val b: Vec3,
    val radius: Float,
    val color: Color,
    val isHighlight: Boolean = false,
    val isEquipment: Boolean = false,
) : Primitive3D() {
    override val centerZ: Float = (a.z + b.z) / 2f

    override fun draw(scope: DrawScope, camera: Camera3D, width: Float, height: Float, isXRay: Boolean) {
        val pa = camera.project(a, width, height)
        val pb = camera.project(b, width, height)

        val dir = (b - a).normalized()
        val side = dir.cross(Vec3.FORWARD).normalized()
        val normal = if (side.length() > 0.1f) side else Vec3.UP

        val drawColor = if (isEquipment && isXRay) {
            color.copy(alpha = 0.25f)
        } else {
            Lighting3D.shadeColor(color, normal, isHighlight)
        }

        val avgScale = (pa.scale + pb.scale) / 2f
        val strokeW = (radius * 2f * avgScale).coerceAtLeast(1.5f)

        scope.drawLine(
            color = drawColor,
            start = pa.offset,
            end = pb.offset,
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Subtle core highlight line for metallic or volumetric 3D look
        if (!isEquipment && isHighlight) {
            scope.drawLine(
                color = Color.White.copy(alpha = 0.45f),
                start = pa.offset,
                end = pb.offset,
                strokeWidth = (strokeW * 0.25f).coerceAtLeast(1f),
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * 3D Solid Sphere (e.g. head, joints, dumbbell weight ball).
 */
data class Sphere3D(
    val center: Vec3,
    val radius: Float,
    val color: Color,
    val isHighlight: Boolean = false,
    val isEquipment: Boolean = false,
) : Primitive3D() {
    override val centerZ: Float = center.z

    override fun draw(scope: DrawScope, camera: Camera3D, width: Float, height: Float, isXRay: Boolean) {
        val p = camera.project(center, width, height)
        val r = radius * p.scale

        val drawColor = if (isEquipment && isXRay) {
            color.copy(alpha = 0.25f)
        } else {
            Lighting3D.shadeColor(color, Vec3(0f, -0.4f, -0.9f), isHighlight)
        }

        // Base sphere
        scope.drawCircle(
            color = drawColor,
            radius = r,
            center = p.offset,
            style = Fill
        )

        // Specular highlight dot
        if (!isXRay) {
            val highlightCenter = Offset(p.offset.x - r * 0.3f, p.offset.y - r * 0.35f)
            scope.drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = r * 0.35f,
                center = highlightCenter
            )
        }
    }
}

/**
 * 3D Disc / Circular Weight Plate (e.g. 450mm Olympic bumper plates).
 */
data class Disc3D(
    val center: Vec3,
    val normal: Vec3,
    val radius: Float,
    val thickness: Float,
    val color: Color,
    val isEquipment: Boolean = true,
) : Primitive3D() {
    override val centerZ: Float = center.z

    override fun draw(scope: DrawScope, camera: Camera3D, width: Float, height: Float, isXRay: Boolean) {
        val p = camera.project(center, width, height)
        val r = radius * p.scale

        val drawColor = if (isEquipment && isXRay) {
            color.copy(alpha = 0.30f)
        } else {
            Lighting3D.shadeColor(color, normal)
        }

        scope.drawCircle(
            color = drawColor,
            radius = r,
            center = p.offset,
            style = Fill
        )

        // Outer rim border
        scope.drawCircle(
            color = if (isXRay) Color.White.copy(alpha = 0.4f) else Color(0xFF1E2024),
            radius = r,
            center = p.offset,
            style = Stroke(width = 2.5f * p.scale)
        )

        // Inner Olympic collar ring
        scope.drawCircle(
            color = Color(0xFFE2F952).copy(alpha = if (isXRay) 0.3f else 0.8f),
            radius = r * 0.22f,
            center = p.offset,
            style = Stroke(width = 1.5f * p.scale)
        )
    }
}

/**
 * 3D Quad Polygon (e.g. bench backrest pad, bench seat, floor platform).
 */
data class Quad3D(
    val v1: Vec3,
    val v2: Vec3,
    val v3: Vec3,
    val v4: Vec3,
    val color: Color,
    val isEquipment: Boolean = true,
) : Primitive3D() {
    override val centerZ: Float = (v1.z + v2.z + v3.z + v4.z) / 4f

    override fun draw(scope: DrawScope, camera: Camera3D, width: Float, height: Float, isXRay: Boolean) {
        val p1 = camera.project(v1, width, height)
        val p2 = camera.project(v2, width, height)
        val p3 = camera.project(v3, width, height)
        val p4 = camera.project(v4, width, height)

        val e1 = v2 - v1
        val e2 = v3 - v1
        val normal = e1.cross(e2).normalized()

        val drawColor = if (isEquipment && isXRay) {
            color.copy(alpha = 0.25f)
        } else {
            Lighting3D.shadeColor(color, normal)
        }

        val path = Path().apply {
            moveTo(p1.offset.x, p1.offset.y)
            lineTo(p2.offset.x, p2.offset.y)
            lineTo(p3.offset.x, p3.offset.y)
            lineTo(p4.offset.x, p4.offset.y)
            close()
        }

        scope.drawPath(path, color = drawColor, style = Fill)
        scope.drawPath(path, color = Color.White.copy(alpha = if (isXRay) 0.35f else 0.15f), style = Stroke(width = 1f))
    }
}
