package com.praveen.hypertrophy.render3d

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Volumetric 3D Stylized Mannequin with bilateral limbs and joint kinematics.
 */
object Mannequin3D {

    private val bodySkinColor = Color(0xFF323640)
    private val voltLimeAccent = Color(0xFFE2F952)
    private val jointColor = Color(0xFF1E2026)

    /**
     * Builds volumetric 3D primitives for the lifter with bilateral left/right limb separation.
     */
    fun buildMannequin(
        head: Vec3,
        pelvis: Vec3,
        shL: Vec3, shR: Vec3,
        elL: Vec3, elR: Vec3,
        wrL: Vec3, wrR: Vec3,
        hipL: Vec3, hipR: Vec3,
        knL: Vec3, knR: Vec3,
        anL: Vec3, anR: Vec3,
        primaryTargetMuscles: String = "arms"
    ): List<Primitive3D> {
        val list = mutableListOf<Primitive3D>()

        val isArmsTarget = primaryTargetMuscles.contains("arm", ignoreCase = true) ||
                primaryTargetMuscles.contains("bicep", ignoreCase = true) ||
                primaryTargetMuscles.contains("tricep", ignoreCase = true)
        val isLegsTarget = primaryTargetMuscles.contains("leg", ignoreCase = true) ||
                primaryTargetMuscles.contains("quad", ignoreCase = true) ||
                primaryTargetMuscles.contains("glute", ignoreCase = true)
        val isChestTarget = primaryTargetMuscles.contains("chest", ignoreCase = true) ||
                primaryTargetMuscles.contains("push", ignoreCase = true)
        val isBackTarget = primaryTargetMuscles.contains("back", ignoreCase = true) ||
                primaryTargetMuscles.contains("pull", ignoreCase = true)

        // 1. 3D Head Sphere
        list.add(
            Sphere3D(
                center = head,
                radius = 12f,
                color = bodySkinColor,
                isHighlight = false
            )
        )

        // 2. Neck
        val midShoulders = (shL + shR) * 0.5f
        list.add(
            Cylinder3D(
                a = Vec3(head.x, head.y + 10f, head.z),
                b = midShoulders,
                radius = 4.5f,
                color = bodySkinColor
            )
        )

        // 3. Volumetric Torso (Spine & Ribcage Clavicle)
        list.add(
            Cylinder3D(
                a = shL,
                b = shR,
                radius = 6.0f,
                color = if (isChestTarget || isBackTarget) voltLimeAccent else bodySkinColor,
                isHighlight = isChestTarget || isBackTarget
            )
        )
        list.add(
            Cylinder3D(
                a = midShoulders,
                b = pelvis,
                radius = 9.0f,
                color = if (isChestTarget || isBackTarget) voltLimeAccent else bodySkinColor,
                isHighlight = isChestTarget || isBackTarget
            )
        )

        // Pelvis Crossbeam
        list.add(
            Cylinder3D(
                a = hipL,
                b = hipR,
                radius = 6.5f,
                color = bodySkinColor
            )
        )

        // 4. Bilateral Arms (Left & Right)
        val armHighlight = isArmsTarget || isChestTarget
        val armColor = if (armHighlight) voltLimeAccent else bodySkinColor

        // Left Arm (upper arm, elbow, forearm, wrist)
        list.add(Cylinder3D(shL, elL, 5.8f, armColor, isHighlight = armHighlight))
        list.add(Sphere3D(elL, 5.0f, jointColor))
        list.add(Cylinder3D(elL, wrL, 4.6f, armColor, isHighlight = armHighlight))
        list.add(Sphere3D(wrL, 4.0f, jointColor))

        // Right Arm (upper arm, elbow, forearm, wrist)
        list.add(Cylinder3D(shR, elR, 5.8f, armColor, isHighlight = armHighlight))
        list.add(Sphere3D(elR, 5.0f, jointColor))
        list.add(Cylinder3D(elR, wrR, 4.6f, armColor, isHighlight = armHighlight))
        list.add(Sphere3D(wrR, 4.0f, jointColor))

        // 5. Bilateral Legs (Left & Right)
        val legHighlight = isLegsTarget
        val legColor = if (legHighlight) voltLimeAccent else bodySkinColor

        // Left Leg (thigh, knee, shin, ankle, foot)
        list.add(Cylinder3D(hipL, knL, 7.5f, legColor, isHighlight = legHighlight))
        list.add(Sphere3D(knL, 6.2f, jointColor))
        list.add(Cylinder3D(knL, anL, 5.5f, legColor, isHighlight = legHighlight))
        list.add(Sphere3D(anL, 4.5f, jointColor))
        list.add(Cylinder3D(anL, Vec3(anL.x + 12f, anL.y, anL.z), 4.0f, bodySkinColor))

        // Right Leg (thigh, knee, shin, ankle, foot)
        list.add(Cylinder3D(hipR, knR, 7.5f, legColor, isHighlight = legHighlight))
        list.add(Sphere3D(knR, 6.2f, jointColor))
        list.add(Cylinder3D(knR, anR, 5.5f, legColor, isHighlight = legHighlight))
        list.add(Sphere3D(anR, 4.5f, jointColor))
        list.add(Cylinder3D(anR, Vec3(anR.x + 12f, anR.y, anR.z), 4.0f, bodySkinColor))

        return list
    }

    /**
     * Solves full bilateral 3D poses for standard exercises parameterized by rep phase k (0..1).
     */
    fun solveExercisePose(exerciseId: String, k: Float): Pair<List<Primitive3D>, List<Primitive3D>> {
        val shoulderZ = 20f
        val hipZ = 14f

        val lifterPrimitives = mutableListOf<Primitive3D>()
        val equipmentPrimitives = mutableListOf<Primitive3D>()

        // Floor grid is common to all gym exercises
        equipmentPrimitives.addAll(Equipment3D.createFloorGrid())

        when (exerciseId) {
            "curl", "bicep", "db_curl" -> {
                // Standing Bicep Curl with Dumbbells
                val head = Vec3(190f, 32f, 0f)
                val pelvis = Vec3(190f, 104f, 0f)
                val shL = Vec3(190f, 48f, -shoulderZ)
                val shR = Vec3(190f, 48f, shoulderZ)
                val elL = Vec3(193f, 82f, -shoulderZ)
                val elR = Vec3(193f, 82f, shoulderZ)

                val curlAngle = (10f + 135f * k) * (PI / 180f).toFloat()
                val wrX = elL.x + 34f * sin(curlAngle)
                val wrY = elL.y + 34f * cos(curlAngle)
                val wrL = Vec3(wrX, wrY, -shoulderZ)
                val wrR = Vec3(wrX, wrY, shoulderZ)

                val hipL = Vec3(190f, 104f, -hipZ)
                val hipR = Vec3(190f, 104f, hipZ)
                val knL = Vec3(190f, 152f, -hipZ)
                val knR = Vec3(190f, 152f, hipZ)
                val anL = Vec3(190f, 200f, -hipZ)
                val anR = Vec3(190f, 200f, hipZ)

                lifterPrimitives.addAll(
                    buildMannequin(head, pelvis, shL, shR, elL, elR, wrL, wrR, hipL, hipR, knL, knR, anL, anR, "biceps")
                )
                equipmentPrimitives.addAll(Equipment3D.createDumbbells(wrL, wrR))
            }

            "bench", "pushup", "db_bench" -> {
                // Flat Bench Press with Olympic Barbell & Bench
                val benchTopY = 145f
                val head = Vec3(155f, benchTopY - 8f, 0f)
                val pelvis = Vec3(220f, benchTopY - 6f, 0f)
                val shL = Vec3(175f, benchTopY - 6f, -shoulderZ)
                val shR = Vec3(175f, benchTopY - 6f, shoulderZ)

                val pressDist = 32f * (1f - k) // k=0 locked out, k=1 bottom of chest
                val barY = (benchTopY - 45f) + pressDist
                val barX = 180f

                val wrL = Vec3(barX, barY, -30f)
                val wrR = Vec3(barX, barY, 30f)
                val elL = Vec3(barX - 8f, barY + 18f, -shoulderZ - 6f)
                val elR = Vec3(barX - 8f, barY + 18f, shoulderZ + 6f)

                val hipL = Vec3(220f, benchTopY - 4f, -hipZ)
                val hipR = Vec3(220f, benchTopY - 4f, hipZ)
                val knL = Vec3(240f, benchTopY + 12f, -hipZ - 8f)
                val knR = Vec3(240f, benchTopY + 12f, hipZ + 8f)
                val anL = Vec3(245f, 200f, -hipZ - 10f)
                val anR = Vec3(245f, 200f, hipZ + 10f)

                lifterPrimitives.addAll(
                    buildMannequin(head, pelvis, shL, shR, elL, elR, wrL, wrR, hipL, hipR, knL, knR, anL, anR, "chest")
                )
                equipmentPrimitives.addAll(Equipment3D.createWorkoutBench(centerX = 190f, benchTopY = benchTopY, inclineAngleDeg = 0f))
                equipmentPrimitives.addAll(Equipment3D.createOlympicBarbell(center = Vec3(barX, barY, 0f), barLength = 160f, plateCount = 2))
            }

            "squat", "split_squat" -> {
                // Barbell Back Squat inside Power Rack
                val depth = 45f * k
                val head = Vec3(190f, 40f + depth, 0f)
                val pelvis = Vec3(172f, 104f + depth, 0f)
                val shL = Vec3(188f, 54f + depth, -shoulderZ)
                val shR = Vec3(188f, 54f + depth, shoulderZ)

                val barPos = Vec3(186f, 50f + depth, 0f)
                val wrL = Vec3(186f, 52f + depth, -shoulderZ - 12f)
                val wrR = Vec3(186f, 52f + depth, shoulderZ + 12f)
                val elL = Vec3(180f, 68f + depth, -shoulderZ - 8f)
                val elR = Vec3(180f, 68f + depth, shoulderZ + 8f)

                val hipL = Vec3(172f, 104f + depth, -hipZ)
                val hipR = Vec3(172f, 104f + depth, hipZ)
                val knL = Vec3(210f, 150f + depth * 0.4f, -hipZ - 4f)
                val knR = Vec3(210f, 150f + depth * 0.4f, hipZ + 4f)
                val anL = Vec3(192f, 200f, -hipZ - 6f)
                val anR = Vec3(192f, 200f, hipZ + 6f)

                lifterPrimitives.addAll(
                    buildMannequin(head, pelvis, shL, shR, elL, elR, wrL, wrR, hipL, hipR, knL, knR, anL, anR, "legs")
                )
                equipmentPrimitives.addAll(Equipment3D.createPowerRack(centerX = 190f, floorY = 200f))
                equipmentPrimitives.addAll(Equipment3D.createOlympicBarbell(center = barPos, barLength = 170f, plateCount = 3))
            }

            else -> {
                // Default / Standing Overhead Press with Barbell
                val pressUp = 45f * k
                val head = Vec3(190f, 32f, 0f)
                val pelvis = Vec3(190f, 104f, 0f)
                val shL = Vec3(190f, 48f, -shoulderZ)
                val shR = Vec3(190f, 48f, shoulderZ)

                val barY = 48f - pressUp
                val barPos = Vec3(190f, barY, 0f)
                val wrL = Vec3(190f, barY, -shoulderZ - 10f)
                val wrR = Vec3(190f, barY, shoulderZ + 10f)
                val elL = Vec3(190f, barY + 24f, -shoulderZ - 6f)
                val elR = Vec3(190f, barY + 24f, shoulderZ + 6f)

                val hipL = Vec3(190f, 104f, -hipZ)
                val hipR = Vec3(190f, 104f, hipZ)
                val knL = Vec3(190f, 152f, -hipZ)
                val knR = Vec3(190f, 152f, hipZ)
                val anL = Vec3(190f, 200f, -hipZ)
                val anR = Vec3(190f, 200f, hipZ)

                lifterPrimitives.addAll(
                    buildMannequin(head, pelvis, shL, shR, elL, elR, wrL, wrR, hipL, hipR, knL, knR, anL, anR, "shoulders")
                )
                equipmentPrimitives.addAll(Equipment3D.createOlympicBarbell(center = barPos, barLength = 160f, plateCount = 2))
            }
        }

        return Pair(lifterPrimitives, equipmentPrimitives)
    }
}
