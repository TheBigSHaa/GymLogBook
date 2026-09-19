package com.ironlog.app.util

import com.ironlog.app.data.model.EquipmentType
import com.ironlog.app.data.model.Exercise
import com.ironlog.app.data.model.RepUnit
import com.ironlog.app.data.model.SetLog
import kotlin.math.abs
import kotlin.math.min

object ProgressionCalculator {
    const val ABS_REP_CAP = 30
    const val ABS_RESET_REPS = 20
    const val ABS_MAX_SETS = 5
    const val WEIGHT_INCREMENT_KG = 2.5f
    const val PULLUP_WEIGHT_INCREMENT_KG = 1.25f

    data class Suggestion(
        val weightKg: Float,
        val suggestedReps: Int? = null,
        val suggestedSets: Int? = null,
        val maxDumbbellReached: Boolean = false,
        val note: String? = null,
    )

    /**
     * Convenience wrapper — returns just the weight.
     */
    fun suggestWeight(
        exercise: Exercise,
        lastSets: List<SetLog>,
        currentWeek: Int,
        isCompound: Boolean,
    ): Float = suggestWeightDetailed(exercise, lastSets, currentWeek, isCompound).weightKg

    /**
     * Nearest configured dumbbell. Suggestions never invent a weight that is
     * not on the user's dumbbell list.
     */
    fun snapToAvailableDumbbell(weight: Float, available: List<Float>): Float {
        if (available.isEmpty()) return weight
        return available.minBy { abs(it - weight) }
    }

    /**
     * Suggests the next session from the last session's working sets.
     *
     * Abs (rep-based): +1 rep up to [ABS_REP_CAP]. When every set hits the cap,
     * add a set and reset each to [ABS_RESET_REPS], hard-capped at [ABS_MAX_SETS].
     *
     * Dumbbells (non-abs): never auto-increase weight; snap to [availableDumbbellsKg].
     * Repeat the last load for one same-type session, then +1 rep (including past
     * the planned max). Switching to a heavier dumbbell resets reps to
     * [Exercise.targetRepsMin].
     *
     * Everything else: if every planned set was completed at the same weight
     * and hit [Exercise.targetRepsMax], add [WEIGHT_INCREMENT_KG] (or
     * [PULLUP_WEIGHT_INCREMENT_KG] for pull-ups / chin-ups) and reset reps to
     * [Exercise.targetRepsMin]. Otherwise keep the weight and prescribe one more
     * rep than the last session's weakest set, never above
     * [Exercise.targetRepsMax]. Never add sets beyond the plan unless the user
     * already did extra sets last time.
     */
    fun suggestWeightDetailed(
        exercise: Exercise,
        lastSets: List<SetLog>,
        @Suppress("UNUSED_PARAMETER") currentWeek: Int,
        @Suppress("UNUSED_PARAMETER") isCompound: Boolean,
        availableDumbbellsKg: List<Float> = listOf(6f, 10f, 12f, 17f, 20f),
        priorSets: List<SetLog> = emptyList(),
    ): Suggestion {
        val workingSets = lastSets.filter { it.completed && !it.isWarmup }
        if (workingSets.isEmpty()) return Suggestion(weightKg = 0f)

        val lastWeight = workingSets.maxOf { it.weight }
        val isAbs = ExerciseClassifier.isAbsExercise(exercise.name, exercise.section)

        return when {
            isAbs -> suggestAbs(exercise, workingSets, lastWeight)
            exercise.equipmentType == EquipmentType.DUMBBELL -> suggestDumbbell(
                exercise = exercise,
                workingSets = workingSets,
                lastWeight = lastWeight,
                priorSets = priorSets,
                availableDumbbellsKg = availableDumbbellsKg,
            )
            else -> suggestWeighted(exercise, workingSets, lastWeight)
        }
    }

    private fun suggestAbs(
        exercise: Exercise,
        workingSets: List<SetLog>,
        lastWeight: Float,
    ): Suggestion {
        val lastSetCount = workingSets.size
        // Timed holds keep last duration/set count — the 20/30 protocol is rep-based.
        if (!isRepBasedAbs(exercise)) {
            return Suggestion(
                weightKg = lastWeight,
                suggestedReps = workingSets.minOf { it.reps },
                suggestedSets = persistSetCount(lastSetCount, exercise.targetSets)
                    .coerceAtMost(ABS_MAX_SETS),
            )
        }

        val minReps = workingSets.minOf { it.reps }
        val allHitCap = workingSets.all { it.reps >= ABS_REP_CAP }

        if (allHitCap) {
            return if (lastSetCount < ABS_MAX_SETS) {
                val nextSets = lastSetCount + 1
                Suggestion(
                    weightKg = lastWeight,
                    suggestedReps = ABS_RESET_REPS,
                    suggestedSets = nextSets,
                    note = "All sets hit $ABS_REP_CAP — $nextSets sets × $ABS_RESET_REPS",
                )
            } else {
                Suggestion(
                    weightKg = lastWeight,
                    suggestedReps = ABS_REP_CAP,
                    suggestedSets = ABS_MAX_SETS,
                    note = "Max $ABS_MAX_SETS sets at $ABS_REP_CAP reps",
                )
            }
        }

        return Suggestion(
            weightKg = lastWeight,
            suggestedReps = min(minReps + 1, ABS_REP_CAP),
            suggestedSets = persistSetCount(lastSetCount, exercise.targetSets)
                .coerceAtMost(ABS_MAX_SETS),
        )
    }

    private fun suggestDumbbell(
        exercise: Exercise,
        workingSets: List<SetLog>,
        lastWeight: Float,
        priorSets: List<SetLog>,
        availableDumbbellsKg: List<Float>,
    ): Suggestion {
        val lastSetCount = workingSets.size
        val suggestedSets = persistSetCount(lastSetCount, exercise.targetSets)
        val lastReps = workingSets.minOf { it.reps }
        val weight = snapToAvailableDumbbell(lastWeight, availableDumbbellsKg)
        val resetReps = if (exercise.targetRepsMin > 0) exercise.targetRepsMin else lastReps

        val priorWorking = priorSets.filter { it.completed && !it.isWarmup }
        val priorWeight = priorWorking.maxOfOrNull { it.weight }

        // User moved to a heavier dumbbell: start again at the bottom of the range.
        if (priorWeight != null && lastWeight > priorWeight) {
            return Suggestion(
                weightKg = weight,
                suggestedReps = resetReps,
                suggestedSets = suggestedSets,
                note = "Heavier DB — ${weight}kg × $resetReps",
            )
        }

        val lastQualified = lastSetCount >= exercise.targetSets &&
            workingSets.all { it.weight == lastWeight }
        val priorQualified = priorWorking.size >= exercise.targetSets &&
            priorWorking.isNotEmpty() &&
            priorWorking.all { it.weight == priorWeight }
        val priorReps = priorWorking.minOfOrNull { it.reps }
        val sameLoad = priorWeight != null &&
            snapToAvailableDumbbell(priorWeight, availableDumbbellsKg) == weight &&
            priorReps == lastReps

        // Two matching same-type sessions at this load → +1 rep, even past the planned max.
        if (lastQualified && priorQualified && sameLoad) {
            return Suggestion(
                weightKg = weight,
                suggestedReps = lastReps + 1,
                suggestedSets = suggestedSets,
            )
        }

        return Suggestion(
            weightKg = weight,
            suggestedReps = lastReps,
            suggestedSets = suggestedSets,
        )
    }

    private fun suggestWeighted(
        exercise: Exercise,
        workingSets: List<SetLog>,
        lastWeight: Float,
    ): Suggestion {
        val lastSetCount = workingSets.size
        val suggestedSets = persistSetCount(lastSetCount, exercise.targetSets)
        val minReps = workingSets.minOf { it.reps }
        val hasRepCeiling = exercise.targetRepsMax > 0
        val allSameWeight = workingSets.all { it.weight == lastWeight }
        val allPlannedSetsDone = lastSetCount >= exercise.targetSets
        val allHitMax = hasRepCeiling &&
            allPlannedSetsDone &&
            allSameWeight &&
            workingSets.all { it.reps >= exercise.targetRepsMax }

        // Every planned set hit the top of the range at the same weight: add a plate, reset to min.
        // Never prescribe reps above the planned max — extra reps last time still count as "hit max".
        if (allHitMax) {
            val nextWeight = nextWeight(exercise, lastWeight)
            return Suggestion(
                weightKg = nextWeight,
                suggestedReps = exercise.targetRepsMin,
                suggestedSets = suggestedSets,
                note = if (nextWeight > lastWeight) {
                    "All sets hit ${exercise.targetRepsMax} — ${nextWeight}kg × ${exercise.targetRepsMin}"
                } else {
                    null
                },
            )
        }

        val nextReps = if (hasRepCeiling) {
            min(minReps + 1, exercise.targetRepsMax)
        } else {
            minReps + 1
        }

        return Suggestion(
            weightKg = lastWeight,
            suggestedReps = nextReps,
            suggestedSets = suggestedSets,
        )
    }

    private fun nextWeight(exercise: Exercise, lastWeight: Float): Float {
        return when (exercise.equipmentType) {
            EquipmentType.BAND, EquipmentType.NONE -> lastWeight
            else -> lastWeight + weightIncrementKg(exercise)
        }
    }

    private fun weightIncrementKg(exercise: Exercise): Float =
        if (ExerciseClassifier.isPullUpExercise(exercise.name)) PULLUP_WEIGHT_INCREMENT_KG
        else WEIGHT_INCREMENT_KG

    private fun persistSetCount(lastSetCount: Int, planSets: Int): Int {
        // Never invent extra sets. Keep extras only when the user already logged them.
        return if (lastSetCount > planSets) lastSetCount else planSets
    }

    private fun isRepBasedAbs(exercise: Exercise): Boolean =
        exercise.repUnit == RepUnit.REPS || exercise.repUnit == RepUnit.PER_SIDE
}
