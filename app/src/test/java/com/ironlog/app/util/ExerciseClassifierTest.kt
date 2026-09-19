package com.ironlog.app.util

import com.ironlog.app.util.Day5CircuitProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task: "abs workout will be 1 min break no matter the workout type".
 * Verifies the abs/core detection that drives the fixed 60s rest.
 */
class ExerciseClassifierTest {

    @Test
    fun `abs rest is one minute`() {
        assertEquals(60, ExerciseClassifier.ABS_REST_SECONDS)
    }

    @Test
    fun `core section is always abs`() {
        assertTrue(ExerciseClassifier.isAbsExercise("Anything At All", section = "CORE"))
        assertTrue(ExerciseClassifier.isAbsExercise("Anything At All", section = "core"))
    }

    @Test
    fun `seeded ab exercises are detected by name`() {
        val absNames = listOf(
            "Dead Bugs",
            "Hanging Leg Raises",
            "Barbell Rollouts",
            "DB Russian Twists",
            "Hollow Body Hold",
            "DB Side Bend",
            "Plank",
        )
        absNames.forEach { name ->
            assertTrue("$name should be abs", ExerciseClassifier.isAbsExercise(name))
        }
    }

    @Test
    fun `non-ab exercises are not detected`() {
        val nonAbs = listOf(
            "Barbell Bench Press",
            "Back Squat",
            "Weighted Pull-ups",
            "Romanian Deadlifts",
            "Single-Leg Calf Raises",
            "Lateral Raises",
            "Barbell Hip Thrusts",
        )
        nonAbs.forEach { name ->
            assertFalse("$name should NOT be abs", ExerciseClassifier.isAbsExercise(name))
        }
    }

    @Test
    fun `warmup section is not abs`() {
        assertFalse(ExerciseClassifier.isAbsExercise("Arm Circles & Torso Twists", section = "WARMUP"))
    }

    @Test
    fun `day 5 circuit times match the program table`() {
        assertEquals(60, Day5CircuitProtocol.WARMUP_WORK_SECONDS)
        assertEquals(40, Day5CircuitProtocol.CIRCUIT_WORK_SECONDS)
        assertEquals(20, Day5CircuitProtocol.CIRCUIT_REST_SECONDS)
        assertEquals(4, Day5CircuitProtocol.CIRCUIT_ROUNDS)
        assertEquals(60, Day5CircuitProtocol.COOLDOWN_WORK_SECONDS)
    }

    @Test
    fun `pull-up family is detected for the 1_25kg increment`() {
        val pullUps = listOf(
            "Weighted Pull-ups",
            "Pull-ups",
            "Pullups",
            "Chin-ups",
            "Chinups",
        )
        pullUps.forEach { name ->
            assertTrue("$name should be a pull-up", ExerciseClassifier.isPullUpExercise(name))
        }
        assertFalse(ExerciseClassifier.isPullUpExercise("Barbell Bench Press"))
        assertFalse(ExerciseClassifier.isPullUpExercise("Nordic Curls"))
    }

    @Test
    fun `seeded plan abs vs the rest`() {
        val abs = listOf(
            "Dead Bugs",
            "Barbell Rollouts",
            "Hanging Leg Raises",
            "DB Russian Twists",
            "Hollow Body Hold",
            "DB Side Bend",
            "Plank",
        )
        val rest = listOf(
            "Barbell Bench Press",
            "Weighted Pull-ups",
            "Barbell OH Press",
            "Barbell Rows",
            "Close Grip Bench Press",
            "Farmer's Walks",
            "Back Squat",
            "Romanian Deadlifts",
            "Goblet Squats",
            "Walking Lunges",
            "DB Calf Raises",
            "Incline Dumbbell Press",
            "DB Rows",
            "DB Shoulder Press",
            "Chin-ups",
            "DB Flyes",
            "Lateral Raises",
            "Barbell Curls",
            "OH DB Extension",
            "Bulgarian Split Squats",
            "Barbell Hip Thrusts",
            "Nordic Curls",
            "Banded Spanish Squats",
            "Single-Leg Calf Raises",
            "Arm Circles & Torso Twists",
            "Bodyweight Squats",
            "Scapular Pull-ups",
            "Parallel Bar Dips",
            "Inverted Rows",
            "Dead Hang",
            "Chest Stretch",
        )
        abs.forEach { name ->
            assertTrue("$name should be abs", ExerciseClassifier.isAbsExercise(name))
        }
        rest.forEach { name ->
            assertFalse("$name should NOT be abs", ExerciseClassifier.isAbsExercise(name))
        }
    }
}
