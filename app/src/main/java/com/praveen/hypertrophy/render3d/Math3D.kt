package com.praveen.hypertrophy.render3d

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 3D spatial vector with standard vector algebra routines.
 */
data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3): Vec3 = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3): Vec3 = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float): Vec3 = Vec3(x * s, y * s, z * s)
    operator fun div(s: Float): Vec3 = Vec3(x / s, y / s, z / s)

    fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z

    fun cross(o: Vec3): Vec3 = Vec3(
        y * o.z - z * o.y,
        z * o.x - x * o.z,
        x * o.y - y * o.x
    )

    fun length(): Float = sqrt(x * x + y * y + z * z).coerceAtLeast(0.0001f)

    fun normalized(): Vec3 {
        val l = length()
        return Vec3(x / l, y / l, z / l)
    }

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
        val UP = Vec3(0f, -1f, 0f)
        val RIGHT = Vec3(1f, 0f, 0f)
        val FORWARD = Vec3(0f, 0f, 1f)
    }
}

/**
 * 3D Camera system applying Euler orbital rotations (Azimuth around Y, Elevation around X)
 * and perspective projection with focal length and zoom factor.
 */
class Camera3D(
    var azimuthDeg: Float = 90f,
    var elevationDeg: Float = 0f,
    var zoom: Float = 1.0f,
    var target: Vec3 = Vec3(190f, 110f, 0f),
    val focalLength: Float = 420f,
) {
    fun project(p: Vec3, screenWidth: Float, screenHeight: Float): ProjectedPoint {
        val azRad = (azimuthDeg * PI / 180f).toFloat()
        val elRad = (elevationDeg * PI / 180f).toFloat()

        // Translate relative to camera orbit pivot target
        val rel = p - target

        // Rotate around Y-axis (Azimuth)
        val x1 = rel.x * cos(azRad) + rel.z * sin(azRad)
        val y1 = rel.y
        val z1 = -rel.x * sin(azRad) + rel.z * cos(azRad)

        // Rotate around X-axis (Elevation)
        val x2 = x1
        val y2 = y1 * cos(elRad) - z1 * sin(elRad)
        val z2 = y1 * sin(elRad) + z1 * cos(elRad)

        // Perspective division
        val depth = focalLength + z2
        val scale = if (depth > 20f) (focalLength / depth) * zoom else 0.05f

        val sc = screenWidth / 380f
        val sx = (screenWidth / 2f) + x2 * scale * sc
        val sy = (screenHeight / 2f) + y2 * scale * sc

        return ProjectedPoint(Offset(sx, sy), z2, scale * sc)
    }
}

data class ProjectedPoint(val offset: Offset, val zDepth: Float, val scale: Float)
