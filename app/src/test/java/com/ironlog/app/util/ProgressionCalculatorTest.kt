package com.ironlog.app.util

import com.ironlog.app.data.model.EquipmentType
import com.ironlog.app.data.model.Exercise
import com.ironlog.app.data.model.RepUnit
import com.ironlog.app.data.model.SetLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressionCalculatorTest {

    private fun exercise(
        name: String = "Test",
        equipment: EquipmentType = EquipmentType.BARBELL,
        targetSets: Int = 3,
        repsMin: Int = 5,
        repsMax: Int = 8,
        workoutDayId: Int = 1,
        section: String? = null,
        repUnit: RepUnit = RepUnit.REPS,
    ): Exercise = Exercise(
        id = 1,
        workoutDayId = workoutDayId,
        name = name,
        targetSets = targetSets,
        targetRepsMin = repsMin,
        targetRepsMax = repsMax,
        orderIndex = 0,
        equipmentType = equipment,
        repUnit = repUnit,
        section = section,
    )

    private fun set(weight: Float, reps: Int, completed: Boolean = true, warmup: Boolean = false): SetLog =
        SetLog(
            id = 0L,
            exerciseLogId = 0L,
            setNumber = 1,
            weight = weight,
            reps = reps,
            completed = completed,
            isWarmup = warmup,
        )

    private fun suggest(
        exercise: Exercise,
        lastSets: List<SetLog>,
        priorSets: List<SetLog> = emptyList(),
        availableDumbbells: List<Float> = listOf(6f, 10f, 12f, 17f, 20f),
    ) = ProgressionCalculator.suggestWeightDetailed(
        exercise = exercise,
        lastSets = lastSets,
        currentWeek = 1,
        isCompound = true,
        availableDumbbellsKg = availableDumbbells,
        priorSets = priorSets,
    )

    // ── Empty input ─────────────────────────────────────────────────────────────

    @Test
    fun `no completed sets returns zero weight`() {
        val result = suggest(exercise(), emptyList())
        assertEquals(0f, result.weightKg, 0.001f)
        assertNull(result.suggestedReps)
        assertNull(result.suggestedSets)
    }

    @Test
    fun `warmup-only sets are ignored`() {
        val result = suggest(exercise(), listOf(set(50f, 5, warmup = true)))
        assertEquals(0f, result.weightKg, 0.001f)
    }

    // ── Abs: +1 rep up to 30, then add a set and reset to 20 ────────────────────

    @Test
    fun `abs adds one rep to last achieved`() {
        val ex = exercise(name = "Dead Bugs", equipment = EquipmentType.BODYWEIGHT, targetSets = 3, repsMin = 10, repsMax = 10, repUnit = RepUnit.PER_SIDE)
        val result = suggest(ex, listOf(set(0f, 10), set(0f, 10), set(0f, 10)))
        assertEquals(0f, result.weightKg, 0.001f)
        assertEquals(11, result.suggestedReps)
        assertEquals(3, result.suggestedSets)
    }

    @Test
    fun `abs uses the weakest set as the achieved number`() {
        val ex = exercise(name = "Hanging Leg Raises", equipment = EquipmentType.BODYWEIGHT, targetSets = 3)
        val result = suggest(ex, listOf(set(0f, 18), set(0f, 15), set(0f, 12)))
        assertEquals(13, result.suggestedReps)
        assertEquals(3, result.suggestedSets)
    }

    @Test
    fun `abs caps suggested reps at 30`() {
        val ex = exercise(name = "Barbell Rollouts", targetSets = 3, repsMin = 10, repsMax = 15)
        val result = suggest(ex, listOf(set(20f, 29), set(20f, 29), set(20f, 29)))
        assertEquals(20f, result.weightKg, 0.001f)
        assertEquals(30, result.suggestedReps)
        assertEquals(3, result.suggestedSets)
    }

    @Test
    fun `abs all sets at 30 adds a set and resets to 20`() {
        val ex = exercise(name = "DB Russian Twists", equipment = EquipmentType.DUMBBELL, targetSets = 3, section = "CORE")
        val result = suggest(ex, listOf(set(6f, 30), set(6f, 30), set(6f, 30)))
        assertEquals(6f, result.weightKg, 0.001f)
        assertEquals(20, result.suggestedReps)
        assertEquals(4, result.suggestedSets)
    }

    @Test
    fun `abs graduates the same way after the extra set`() {
        val ex = exercise(name = "Dead Bugs", equipment = EquipmentType.BODYWEIGHT, targetSets = 3, repUnit = RepUnit.PER_SIDE)
        val fourSetsAt20 = List(4) { set(0f, 20) }
        val after20 = suggest(ex, fourSetsAt20)
        assertEquals(21, after20.suggestedReps)
        assertEquals(4, after20.suggestedSets)

        val fourSetsAt30 = List(4) { set(0f, 30) }
        val after30 = suggest(ex, fourSetsAt30)
        assertEquals(20, after30.suggestedReps)
        assertEquals(5, after30.suggestedSets)
    }

    @Test
    fun `abs never suggests more than 5 sets`() {
        val ex = exercise(name = "Dead Bugs", equipment = EquipmentType.BODYWEIGHT, targetSets = 3, repUnit = RepUnit.PER_SIDE)
        val fiveAt30 = List(5) { set(0f, 30) }
        val result = suggest(ex, fiveAt30)
        assertEquals(30, result.suggestedReps)
        assertEquals(5, result.suggestedSets)

        val sixAt25 = List(6) { set(0f, 25) }
        val capped = suggest(ex, sixAt25)
        assertEquals(26, capped.suggestedReps)
        assertEquals(5, capped.suggestedSets)
    }

    @Test
    fun `timed abs holds do not use the 20-30 set protocol`() {
        val ex = exercise(
            name = "Plank",
            equipment = EquipmentType.BODYWEIGHT,
            targetSets = 3,
            repsMin = 45,
            repsMax = 45,
            section = "CORE",
            repUnit = RepUnit.SECONDS,
        )
        val result = suggest(ex, listOf(set(0f, 45), set(0f, 45), set(0f, 45)))
        assertEquals(45, result.suggestedReps)
        assertEquals(3, result.suggestedSets)
    }

    // ── Non-abs: double progression ─────────────────────────────────────────────

    @Test
    fun `all sets at the top of the range add 2_5kg and reset to min reps`() {
        val ex = exercise(repsMin = 8, repsMax = 10, targetSets = 5)
        val sets = List(5) { set(20f, 10) }
        val result = suggest(ex, sets)
        assertEquals(22.5f, result.weightKg, 0.001f)
        assertEquals(8, result.suggestedReps)
        assertEquals(5, result.suggestedSets)
    }

    @Test
    fun `completing all sets at the min reps adds one rep same weight`() {
        val ex = exercise(repsMin = 8, repsMax = 10, targetSets = 5)
        val sets = List(5) { set(22.5f, 8) }
        val result = suggest(ex, sets)
        assertEquals(22.5f, result.weightKg, 0.001f)
        assertEquals(9, result.suggestedReps)
        assertEquals(5, result.suggestedSets)
    }

    @Test
    fun `completing all sets at 9 of 8-10 adds one rep to 10`() {
        val ex = exercise(repsMin = 8, repsMax = 10, targetSets = 5)
        val sets = List(5) { set(22.5f, 9) }
        val result = suggest(ex, sets)
        assertEquals(22.5f, result.weightKg, 0.001f)
        assertEquals(10, result.suggestedReps)
    }

    @Test
    fun `one set short of the max does not add weight`() {
        val ex = exercise(repsMin = 8, repsMax = 10, targetSets = 5)
        val sets = listOf(set(20f, 10), set(20f, 10), set(20f, 10), set(20f, 10), set(20f, 9))
        val result = suggest(ex, sets)
        assertEquals(20f, result.weightKg, 0.001f)
        assertEquals(10, result.suggestedReps)
    }

    @Test
    fun `5x5 success adds 2_5kg and stays at 5 reps`() {
        val ex = exercise(name = "Barbell Bench Press", targetSets = 5, repsMin = 5, repsMax = 5)
        val result = suggest(ex, List(5) { set(60f, 5) })
        assertEquals(62.5f, result.weightKg, 0.001f)
        assertEquals(5, result.suggestedReps)
        assertEquals(5, result.suggestedSets)
    }

    @Test
    fun `5x5 failure repeats the same weight at 5`() {
        val ex = exercise(targetSets = 5, repsMin = 5, repsMax = 5)
        val sets = listOf(set(100f, 5), set(100f, 5), set(100f, 5), set(100f, 5), set(100f, 4))
        val result = suggest(ex, sets)
        assertEquals(100f, result.weightKg, 0.001f)
        assertEquals(5, result.suggestedReps)
    }

    @Test
    fun `never suggests more sets than the plan`() {
        val ex = exercise(targetSets = 4, repsMin = 6, repsMax = 8)
        val result = suggest(ex, List(4) { set(60f, 7) })
        assertEquals(4, result.suggestedSets)
        assertEquals(8, result.suggestedReps)
    }

    @Test
    fun `extra sets the user added are kept and reps add one`() {
        val ex = exercise(targetSets = 4, repsMin = 6, repsMax = 8)
        val result = suggest(ex, List(6) { set(60f, 7) })
        assertEquals(6, result.suggestedSets)
        assertEquals(8, result.suggestedReps)
        assertEquals(60f, result.weightKg, 0.001f)
    }

    @Test
    fun `going above the rep range still jumps weight and resets to min`() {
        val ex = exercise(targetSets = 5, repsMin = 8, repsMax = 10)
        val result = suggest(ex, List(5) { set(20f, 12) })
        assertEquals(22.5f, result.weightKg, 0.001f)
        assertEquals(8, result.suggestedReps)
        assertEquals(5, result.suggestedSets)
    }

    @Test
    fun `never suggests reps above the planned max even if last session went over`() {
        val ex = exercise(name = "Barbell Bench Press", targetSets = 5, repsMin = 5, repsMax = 5)
        val result = suggest(ex, List(5) { set(60f, 6) })
        assertEquals(62.5f, result.weightKg, 0.001f)
        assertEquals(5, result.suggestedReps)
        assertEquals(5, result.suggestedSets)
    }

    @Test
    fun `mixed weights do not jump even if every set hit max`() {
        val ex = exercise(targetSets = 5, repsMin = 5, repsMax = 5)
        val sets = listOf(set(60f, 5), set(60f, 5), set(60f, 5), set(60f, 5), set(57.5f, 5))
        val result = suggest(ex, sets)
        assertEquals(60f, result.weightKg, 0.001f)
        assertEquals(5, result.suggestedReps)
    }

    @Test
    fun `missing a planned set does not jump weight`() {
        val ex = exercise(targetSets = 5, repsMin = 5, repsMax = 5)
        val result = suggest(ex, List(4) { set(60f, 5) })
        assertEquals(60f, result.weightKg, 0.001f)
        assertEquals(5, result.suggestedReps)
        assertEquals(5, result.suggestedSets)
    }

    @Test
    fun `snapToAvailableDumbbell picks the nearest configured weight`() {
        val available = listOf(6f, 10f, 12f, 17f, 20f)
        assertEquals(20f, ProgressionCalculator.snapToAvailableDumbbell(22.5f, available), 0.001f)
        assertEquals(17f, ProgressionCalculator.snapToAvailableDumbbell(17f, available), 0.001f)
        assertEquals(12f, ProgressionCalculator.snapToAvailableDumbbell(13f, available), 0.001f)
    }

    @Test
    fun `dumbbells never invent a weight off the configured list`() {
        val ex = exercise(
            name = "Farmer's Walks",
            equipment = EquipmentType.DUMBBELL,
            targetSets = 3,
            repsMin = 40,
            repsMax = 40,
            repUnit = RepUnit.STEPS,
        )
        val result = suggest(ex, List(3) { set(20f, 40) })
        assertEquals(20f, result.weightKg, 0.001f)
        assertEquals(40, result.suggestedReps)
        assertEquals(3, result.suggestedSets)
    }

    @Test
    fun `dumbbell suggestion snaps 22_5kg down onto the 20kg dumbbell`() {
        val ex = exercise(equipment = EquipmentType.DUMBBELL, targetSets = 3, repsMin = 40, repsMax = 40)
        val result = suggest(ex, List(3) { set(22.5f, 40) })
        assertEquals(20f, result.weightKg, 0.001f)
        assertEquals(40, result.suggestedReps)
    }

    @Test
    fun `dumbbell first session at a load is repeated next time`() {
        val ex = exercise(equipment = EquipmentType.DUMBBELL, targetSets = 4, repsMin = 8, repsMax = 12)
        val result = suggest(ex, List(4) { set(17f, 8) })
        assertEquals(17f, result.weightKg, 0.001f)
        assertEquals(8, result.suggestedReps)
        assertEquals(4, result.suggestedSets)
    }

    @Test
    fun `dumbbell second matching same-type session adds one rep`() {
        val ex = exercise(equipment = EquipmentType.DUMBBELL, targetSets = 4, repsMin = 8, repsMax = 12)
        val last = List(4) { set(17f, 8) }
        val prior = List(4) { set(17f, 8) }
        val result = suggest(ex, lastSets = last, priorSets = prior)
        assertEquals(17f, result.weightKg, 0.001f)
        assertEquals(9, result.suggestedReps)
        assertEquals(4, result.suggestedSets)
    }

    @Test
    fun `dumbbell keeps adding reps past the planned max every two sessions`() {
        val ex = exercise(equipment = EquipmentType.DUMBBELL, targetSets = 4, repsMin = 8, repsMax = 12)
        val last = List(4) { set(17f, 12) }
        val prior = List(4) { set(17f, 12) }
        val result = suggest(ex, lastSets = last, priorSets = prior)
        assertEquals(17f, result.weightKg, 0.001f)
        assertEquals(13, result.suggestedReps)
    }

    @Test
    fun `switching to a heavier dumbbell resets reps to the lower limit`() {
        val ex = exercise(equipment = EquipmentType.DUMBBELL, targetSets = 4, repsMin = 8, repsMax = 12)
        val last = List(4) { set(20f, 12) }
        val prior = List(4) { set(17f, 12) }
        val result = suggest(ex, lastSets = last, priorSets = prior)
        assertEquals(20f, result.weightKg, 0.001f)
        assertEquals(8, result.suggestedReps)
    }

    @Test
    fun `farmer walks stay at 20kg and add a step after two matching sessions`() {
        val ex = exercise(
            name = "Farmer's Walks",
            equipment = EquipmentType.DUMBBELL,
            targetSets = 3,
            repsMin = 40,
            repsMax = 40,
            repUnit = RepUnit.STEPS,
        )
        val last = List(3) { set(20f, 40) }
        val prior = List(3) { set(20f, 40) }
        val result = suggest(ex, lastSets = last, priorSets = prior)
        assertEquals(20f, result.weightKg, 0.001f)
        assertEquals(41, result.suggestedReps)
    }

    @Test
    fun `weighted pull-ups add 1_25kg when all sets hit max at the same weight`() {
        val ex = exercise(
            name = "Weighted Pull-ups",
            equipment = EquipmentType.BODYWEIGHT,
            targetSets = 5,
            repsMin = 5,
            repsMax = 5,
        )
        val result = suggest(ex, List(5) { set(10f, 5) })
        assertEquals(11.25f, result.weightKg, 0.001f)
        assertEquals(5, result.suggestedReps)
        assertEquals(5, result.suggestedSets)
    }

    @Test
    fun `bodyweight chin-ups add 1_25kg when all sets hit max`() {
        val ex = exercise(name = "Chin-ups", equipment = EquipmentType.BODYWEIGHT, targetSets = 4, repsMin = 8, repsMax = 10)
        val result = suggest(ex, List(4) { set(0f, 10) })
        assertEquals(1.25f, result.weightKg, 0.001f)
        assertEquals(8, result.suggestedReps)
    }

    @Test
    fun `pull-ups with mixed weights do not add 1_25kg`() {
        val ex = exercise(
            name = "Weighted Pull-ups",
            equipment = EquipmentType.BODYWEIGHT,
            targetSets = 5,
            repsMin = 5,
            repsMax = 5,
        )
        val sets = listOf(set(10f, 5), set(10f, 5), set(10f, 5), set(10f, 5), set(7.5f, 5))
        val result = suggest(ex, sets)
        assertEquals(10f, result.weightKg, 0.001f)
        assertEquals(5, result.suggestedReps)
    }

    @Test
    fun `band work does not invent a weight jump`() {
        val ex = exercise(name = "Banded Spanish Squats", equipment = EquipmentType.BAND, targetSets = 3, repsMin = 15, repsMax = 20)
        val result = suggest(ex, List(3) { set(0f, 20) })
        assertEquals(0f, result.weightKg, 0.001f)
        assertEquals(15, result.suggestedReps)
    }
}
