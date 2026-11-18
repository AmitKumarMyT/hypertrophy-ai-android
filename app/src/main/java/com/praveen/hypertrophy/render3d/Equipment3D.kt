package com.praveen.hypertrophy.render3d

import androidx.compose.ui.graphics.Color

/**
 * Procedural 3D gym equipment models.
 */
object Equipment3D {

    private val steelColor = Color(0xFF8E95A5)
    private val plateColor = Color(0xFF1E2024)
    private val benchPadColor = Color(0xFF181A1F)
    private val frameColor = Color(0xFF2D3139)
    private val floorGridColor = Color(0xFF22252C)

    /**
     * Procedural 3D Olympic Barbell with bumper plates and collars.
     */
    fun createOlympicBarbell(center: Vec3, barLength: Float = 170f, plateCount: Int = 2): List<Primitive3D> {
        val list = mutableListOf<Primitive3D>()
        val halfL = barLength / 2f
        val leftEnd = Vec3(center.x, center.y, center.z - halfL)
        val rightEnd = Vec3(center.x, center.y, center.z + halfL)

        // Main steel barbell shaft
        list.add(
            Cylinder3D(
                a = leftEnd,
                b = rightEnd,
                radius = 3.2f,
                color = steelColor,
                isEquipment = true
            )
        )

        // Left bumper plates
        for (i in 0 until plateCount.coerceAtLeast(1)) {
            val plateZ = center.z - halfL + 16f + (i * 9f)
            list.add(
                Disc3D(
                    center = Vec3(center.x, center.y, plateZ),
                    normal = Vec3.FORWARD,
                    radius = 24f,
                    thickness = 7f,
                    color = plateColor
                )
            )
        }

        // Right bumper plates
        for (i in 0 until plateCount.coerceAtLeast(1)) {
            val plateZ = center.z + halfL - 16f - (i * 9f)
            list.add(
                Disc3D(
                    center = Vec3(center.x, center.y, plateZ),
                    normal = Vec3.FORWARD,
                    radius = 24f,
                    thickness = 7f,
                    color = plateColor
                )
            )
        }

        return list
    }

    /**
     * Procedural 3D Hex Dumbbells in left and right hands.
     */
    fun createDumbbells(leftHand: Vec3, rightHand: Vec3): List<Primitive3D> {
        val list = mutableListOf<Primitive3D>()
        val dbs = listOf(leftHand, rightHand)

        for (pos in dbs) {
            // Handle
            list.add(
                Cylinder3D(
                    a = Vec3(pos.x, pos.y, pos.z - 8f),
                    b = Vec3(pos.x, pos.y, pos.z + 8f),
                    radius = 2.8f,
                    color = steelColor,
                    isEquipment = true
                )
            )
            // Left weight head
            list.add(
                Sphere3D(
                    center = Vec3(pos.x, pos.y, pos.z - 10f),
                    radius = 9f,
                    color = plateColor,
                    isEquipment = true
                )
            )
            // Right weight head
            list.add(
                Sphere3D(
                    center = Vec3(pos.x, pos.y, pos.z + 10f),
                    radius = 9f,
                    color = plateColor,
                    isEquipment = true
                )
            )
        }
        return list
    }

    /**
     * Procedural 3D Adjustable Workout Bench (Flat 0°, Incline 30°/45°, Seated 85°).
     */
    fun createWorkoutBench(
        centerX: Float = 190f,
        benchTopY: Float = 145f,
        inclineAngleDeg: Float = 0f
    ): List<Primitive3D> {
        val list = mutableListOf<Primitive3D>()
        val halfW = 16f

        // Bench steel frame legs to floor (floor at y = 200)
        list.add(
            Cylinder3D(
                a = Vec3(centerX - 40f, benchTopY + 5f, 0f),
                b = Vec3(centerX - 40f, 200f, 0f),
                radius = 4f,
                color = frameColor,
                isEquipment = true
            )
        )
        list.add(
            Cylinder3D(
                a = Vec3(centerX + 40f, benchTopY + 5f, 0f),
                b = Vec3(centerX + 40f, 200f, 0f),
                radius = 4f,
                color = frameColor,
                isEquipment = true
            )
        )
        // Horizontal spine
        list.add(
            Cylinder3D(
                a = Vec3(centerX - 45f, benchTopY + 6f, 0f),
                b = Vec3(centerX + 45f, benchTopY + 6f, 0f),
                radius = 4.5f,
                color = frameColor,
                isEquipment = true
            )
        )

        // Padded Bench Backrest
        val rad = (inclineAngleDeg * Math.PI / 180.0).toFloat()
        val len = 90f
        val startX = centerX - 35f
        val startY = benchTopY
        val endX = startX + len * kotlin.math.cos(rad)
        val endY = startY - len * kotlin.math.sin(rad)

        list.add(
            Quad3D(
                v1 = Vec3(startX, startY, -halfW),
                v2 = Vec3(endX, endY, -halfW),
                v3 = Vec3(endX, endY, halfW),
                v4 = Vec3(startX, startY, halfW),
                color = benchPadColor,
                isEquipment = true
            )
        )

        return list
    }

    /**
     * Procedural 3D Squat Rack / Power Cage.
     */
    fun createPowerRack(centerX: Float = 190f, floorY: Float = 200f): List<Primitive3D> {
        val list = mutableListOf<Primitive3D>()
        val height = 175f
        val topY = floorY - height
        val rackDepth = 45f
        val rackWidth = 85f

        val xLeft = centerX - rackDepth
        val xRight = centerX + rackDepth
        val zBack = -rackWidth / 2f
        val zFront = rackWidth / 2f

        val corners = listOf(
            Vec3(xLeft, floorY, zBack),
            Vec3(xRight, floorY, zBack),
            Vec3(xLeft, floorY, zFront),
            Vec3(xRight, floorY, zFront)
        )

        // 4 Vertical steel columns
        for (c in corners) {
            list.add(
                Cylinder3D(
                    a = c,
                    b = Vec3(c.x, topY, c.z),
                    radius = 3.8f,
                    color = frameColor,
                    isEquipment = true
                )
            )
        }

        // Top crossbeams
        list.add(Cylinder3D(Vec3(xLeft, topY, zBack), Vec3(xRight, topY, zBack), 3.2f, frameColor, isEquipment = true))
        list.add(Cylinder3D(Vec3(xLeft, topY, zFront), Vec3(xRight, topY, zFront), 3.2f, frameColor, isEquipment = true))
        list.add(Cylinder3D(Vec3(xLeft, topY, zBack), Vec3(xLeft, topY, zFront), 3.2f, frameColor, isEquipment = true))
        list.add(Cylinder3D(Vec3(xRight, topY, zBack), Vec3(xRight, topY, zFront), 3.2f, frameColor, isEquipment = true))

        return list
    }

    /**
     * 3D Gym Floor Grid with subtle perspective lines and shadow mat.
     */
    fun createFloorGrid(centerX: Float = 190f, floorY: Float = 200f): List<Primitive3D> {
        val list = mutableListOf<Primitive3D>()
        val size = 160f
        val step = 40f

        // Central rubber workout tile mat
        list.add(
            Quad3D(
                v1 = Vec3(centerX - 80f, floorY, -60f),
                v2 = Vec3(centerX + 80f, floorY, -60f),
                v3 = Vec3(centerX + 80f, floorY, 60f),
                v4 = Vec3(centerX - 80f, floorY, 60f),
                color = Color(0xFF141519),
                isEquipment = true
            )
        )

        // Perspective grid lines
        var offset = -size / 2f
        while (offset <= size / 2f) {
            // Z-direction lines
            list.add(
                Cylinder3D(
                    a = Vec3(centerX + offset, floorY, -size / 2f),
                    b = Vec3(centerX + offset, floorY, size / 2f),
                    radius = 0.8f,
                    color = floorGridColor,
                    isEquipment = true
                )
            )
            // X-direction lines
            list.add(
                Cylinder3D(
                    a = Vec3(centerX - size / 2f, floorY, offset),
                    b = Vec3(centerX + size / 2f, floorY, offset),
                    radius = 0.8f,
                    color = floorGridColor,
                    isEquipment = true
                )
            )
            offset += step
        }

        return list
    }
}
