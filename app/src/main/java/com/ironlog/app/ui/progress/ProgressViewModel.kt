package com.ironlog.app.ui.progress

import androidx.compose.ui.graphics.Color
import com.ironlog.app.data.model.DayType
import com.ironlog.app.data.model.Exercise
import com.ironlog.app.data.model.WorkoutDay
import com.ironlog.app.data.model.WorkoutSession
import com.ironlog.app.data.repository.WorkoutRepository
import com.ironlog.app.ui.components.PersonalRecordDisplay
import com.ironlog.app.ui.theme.dayColorForId
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import javax.inject.Inject

// ─── Legacy state (kept for backward compatibility) ─────────────────────────

data class DataPoint(val date: LocalDate, val value: Float)

data class PersonalRecord(
    val exercise: Exercise,
    val weight: Float,
    val reps: Int,
    val date: LocalDate,
    val dayColor: String,
)

data class WeekVolume(
    val weekLabel: String,
    val totalKg: Float,
    val startDate: LocalDate,
)

data class ProgressState(
    val oneRmData: Map<String, List<DataPoint>> = emptyMap(),
    val personalRecords: List<PersonalRecord> = emptyList(),
    val weeklyVolume: List<WeekVolume> = emptyList(),
    val selectedExercise: Exercise? = null,
    val exerciseProgression: List<DataPoint> = emptyList(),
    val allExercises: List<Exercise> = emptyList(),
    val exerciseSessionRows: List<ExerciseSessionRow> = emptyList(),
)

data class ExerciseSessionRow(
    val date: LocalDate,
    val setsCompleted: Int,
    val bestWeight: Float,
    val bestReps: Int,
)

// ─── New UI state ────────────────────────────────────────────────────────────

/** One slice of the training-split donut: a workout day and how often it was trained. */
data class SplitSlice(val dayId: Int, val label: String, val count: Int)

data class ProgressUiState(
    val isLoading: Boolean = true,
    val hasNoData: Boolean = true,
    val loadFailed: Boolean = false,
    val totalWorkouts: Int = 0,
    val totalVolumeKg: Float = 0f,
    val avgDurationMinutes: Int = 0,
    val prCount: Int = 0,
    val featuredExerciseName: String? = null,
    val featuredDayLabel: String? = null,
    val featuredOneRm: Float? = null,
    val featuredGrowthPercent: Float? = null,
    val featuredWeeklyData: List<Float>? = null,
    val featuredAccentColor: Color? = null,
    val benchOneRm: Float? = null,
    val benchSparkline: List<Float>? = null,
    val ohPressOneRm: Float? = null,
    val ohPressSparkline: List<Float>? = null,
    val currentWeekVolume: Float? = null,
    val previousWeekVolume: Float? = null,
    val daysSinceLastRest: Int? = null,
    val personalRecords: List<PersonalRecordDisplay>? = null,
    /** Working-set volume (kg) per training date, last 84 days. */
    val dailyVolumeByDate: Map<LocalDate, Float> = emptyMap(),
    /** Session count per workout day, for the training-split donut. */
    val dayTypeSplit: List<SplitSlice> = emptyList(),
    /** Session date → duration in minutes, chronological. */
    val durationSeries: List<Pair<LocalDate, Int>> = emptyList(),
    /** Week label ("W1".."Wn") → total working-set volume in kg. */
    val weeklyVolumeSeries: List<Pair<String, Float>> = emptyList(),
)

// ─── ViewModel ───────────────────────────────────────────────────────────────

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProgressState())
    val state: StateFlow<ProgressState> = _state

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState

    init {
        viewModelScope.launch {
            try {
                val days = workoutRepository.workoutDays().first()
                val exercises = buildAllExercises(days)
                val sessions = workoutRepository.completedSessions().first().filter { it.completed }

                val sessionDetails = buildSessionDetails(sessions, days, exercises)

                val oneRm = computeMainCompoundOneRm(sessionDetails)
                val prs = computePersonalRecords(sessionDetails)
                val weekly = computeWeeklyVolume(sessionDetails)

                _state.update {
                    it.copy(
                        oneRmData = oneRm,
                        personalRecords = prs,
                        weeklyVolume = weekly,
                        allExercises = exercises.sortedBy { ex -> ex.name },
                        selectedExercise = exercises.sortedBy { ex -> ex.name }.firstOrNull(),
                    )
                }

                _uiState.value = computeProgressUiState(sessionDetails, days, prs).copy(isLoading = false)

                _state.value.selectedExercise?.let { selectExercise(it) }
            } catch (e: Exception) {
                // Distinguish a genuine failure from "no data yet" so the UI can say so.
                _uiState.value = ProgressUiState(isLoading = false, hasNoData = true, loadFailed = true)
            }
        }
    }

    fun selectExercise(exercise: Exercise) {
        viewModelScope.launch {
            try {
                val days = workoutRepository.workoutDays().first()
                val exercises = _state.value.allExercises.ifEmpty { buildAllExercises(days) }
                val sessions = workoutRepository.completedSessions().first().filter { it.completed }
                val details = buildSessionDetails(sessions, days, exercises)

                val progression = details.mapNotNull { s ->
                    val best = s.bestSetByExerciseId[exercise.id] ?: return@mapNotNull null
                    DataPoint(date = s.session.date, value = best.weight)
                }.sortedBy { it.date }

                val rows = details.mapNotNull { s ->
                    val sets = s.setsByExerciseId[exercise.id] ?: return@mapNotNull null
                    val completedWorking = sets.filter { it.completed && !it.isWarmup }
                    if (completedWorking.isEmpty()) return@mapNotNull null
                    val best = completedWorking.maxWithOrNull { a, b ->
                        compareBest(a.weight, a.reps, b.weight, b.reps)
                    } ?: return@mapNotNull null
                    ExerciseSessionRow(
                        date = s.session.date,
                        setsCompleted = completedWorking.size,
                        bestWeight = best.weight,
                        bestReps = best.reps,
                    )
                }.sortedByDescending { it.date }

                _state.update {
                    it.copy(
                        selectedExercise = exercise,
                        exerciseProgression = progression,
                        exerciseSessionRows = rows,
                    )
                }
            } catch (e: Exception) {
                // leave state unchanged
            }
        }
    }

    // ─── Internal data structures ────────────────────────────────────────────

    private data class SetLite(
        val weight: Float,
        val reps: Int,
        val completed: Boolean,
        val isWarmup: Boolean,
    )

    private data class BestLite(
        val weight: Float,
        val reps: Int,
        val epley1Rm: Float,
    )

    private data class SessionDetail(
        val session: WorkoutSession,
        val workoutDay: WorkoutDay,
        val exercisesById: Map<Int, Exercise>,
        val setsByExerciseId: Map<Int, List<SetLite>>,
        val bestSetByExerciseId: Map<Int, BestLite>,
    )

    // ─── Data building ───────────────────────────────────────────────────────

    private suspend fun buildAllExercises(days: List<WorkoutDay>): List<Exercise> {
        return buildList {
            days.sortedBy { it.id }.forEach { day ->
                addAll(workoutRepository.exercisesForDay(day.id).first())
            }
        }.distinctBy { it.id }
    }

    private suspend fun buildSessionDetails(
        sessions: List<WorkoutSession>,
        days: List<WorkoutDay>,
        exercises: List<Exercise>,
    ): List<SessionDetail> {
        val dayById = days.associateBy { it.id }
        val exerciseById = exercises.associateBy { it.id }

        return sessions
            .sortedWith(compareBy<WorkoutSession> { it.date }.thenBy { it.id })
            .mapNotNull { session ->
                try {
                    val day = dayById[session.workoutDayId] ?: return@mapNotNull null
                    val logs = workoutRepository.logsForSessionOnce(session.id)

                    val setsByExerciseId = mutableMapOf<Int, MutableList<SetLite>>()
                    val usedExercisesById = mutableMapOf<Int, Exercise>()
                    logs.forEach { log ->
                        val exId = log.exerciseId
                        val ex = exerciseById[exId] ?: return@forEach
                        usedExercisesById[exId] = ex
                        val sets = workoutRepository.setsForExerciseLogOnce(log.id)
                        sets.forEach { set ->
                            setsByExerciseId.getOrPut(exId) { mutableListOf() }.add(
                                SetLite(
                                    weight = set.weight,
                                    reps = set.reps,
                                    completed = set.completed,
                                    isWarmup = set.isWarmup,
                                ),
                            )
                        }
                    }

                    val bestByExercise = setsByExerciseId.mapValues { (_, sets) ->
                        val best = sets
                            .asSequence()
                            .filter { it.completed && !it.isWarmup }
                            .maxWithOrNull { a, b -> compareBest(a.weight, a.reps, b.weight, b.reps) }
                            ?: return@mapValues null
                        BestLite(
                            weight = best.weight,
                            reps = best.reps,
                            epley1Rm = epley(best.weight, best.reps),
                        )
                    }.mapNotNull { (k, v) -> v?.let { k to it } }.toMap()

                    SessionDetail(
                        session = session,
                        workoutDay = day,
                        exercisesById = usedExercisesById,
                        setsByExerciseId = setsByExerciseId,
                        bestSetByExerciseId = bestByExercise,
                    )
                } catch (e: Exception) {
                    null
                }
            }
    }

    // ─── Legacy computations ─────────────────────────────────────────────────

    private fun computeMainCompoundOneRm(details: List<SessionDetail>): Map<String, List<DataPoint>> {
        val targetNames = setOf(
            "Barbell Bench Press",
            "Back Squat",
            "Barbell OH Press",
            "Weighted Pull-ups",
        )
        val byName = mutableMapOf<String, MutableList<DataPoint>>()
        details.forEach { s ->
            s.bestSetByExerciseId.forEach { (exId, best) ->
                val exName = s.exercisesById[exId]?.name ?: return@forEach
                if (exName !in targetNames) return@forEach
                byName.getOrPut(exName) { mutableListOf() }.add(
                    DataPoint(date = s.session.date, value = best.epley1Rm),
                )
            }
        }
        return byName.mapValues { (_, points) -> points.sortedBy { it.date } }
    }

    private fun computePersonalRecords(details: List<SessionDetail>): List<PersonalRecord> {
        val bestByExerciseId = mutableMapOf<Int, Triple<Float, Int, LocalDate>>()
        val exById = mutableMapOf<Int, Exercise>()
        val colorByDayId = mutableMapOf<Int, String>()

        details.forEach { s ->
            colorByDayId[s.workoutDay.id] = s.workoutDay.colorHex ?: "#E8A848"
            s.exercisesById.forEach { (id, ex) -> exById[id] = ex }
            s.bestSetByExerciseId.forEach { (exId, best) ->
                val candidate = Triple(best.weight, best.reps, s.session.date)
                val current = bestByExerciseId[exId]
                if (current == null) {
                    bestByExerciseId[exId] = candidate
                } else {
                    val cmp = compareBest(candidate.first, candidate.second, current.first, current.second)
                    if (cmp > 0 || (cmp == 0 && candidate.third.isAfter(current.third))) {
                        bestByExerciseId[exId] = candidate
                    }
                }
            }
        }

        return bestByExerciseId.mapNotNull { (exId, triple) ->
            val ex = exById[exId] ?: return@mapNotNull null
            val dayColor = colorByDayId[ex.workoutDayId] ?: "#E8A848"
            PersonalRecord(
                exercise = ex,
                weight = triple.first,
                reps = triple.second,
                date = triple.third,
                dayColor = dayColor,
            )
        }.sortedWith(compareByDescending<PersonalRecord> { it.date }.thenBy { it.exercise.name })
    }

    private fun computeWeeklyVolume(details: List<SessionDetail>): List<WeekVolume> {
        val weekFields = WeekFields.ISO
        val grouped = details.groupBy { d ->
            val week = d.session.date.get(weekFields.weekOfWeekBasedYear())
            val year = d.session.date.get(weekFields.weekBasedYear())
            year to week
        }
        val earliest = details.minByOrNull { it.session.date }?.session?.date

        return grouped.entries
            .sortedWith(compareBy({ it.key.first }, { it.key.second }))
            .mapIndexed { idx, (_, list) ->
                val start = list.minByOrNull { it.session.date }?.session?.date ?: LocalDate.now()
                val total = list.sumOf { s ->
                    s.setsByExerciseId.values.flatten()
                        .asSequence()
                        .filter { it.completed && !it.isWarmup }
                        .sumOf { (it.weight * it.reps).toDouble() }
                }.toFloat()
                val label = if (earliest == null) "W${idx + 1}" else {
                    val weeks = (java.time.temporal.ChronoUnit.DAYS.between(earliest, start) / 7) + 1
                    "W${weeks.toInt()}"
                }
                WeekVolume(weekLabel = label, totalKg = total, startDate = start)
            }
    }

    // ─── New UI state computation ────────────────────────────────────────────

    private fun computeProgressUiState(
        details: List<SessionDetail>,
        days: List<WorkoutDay>,
        prs: List<PersonalRecord>,
    ): ProgressUiState {
        if (details.isEmpty()) return ProgressUiState(hasNoData = true)

        return try {
            val today = LocalDate.now()
            val dayById = days.associateBy { it.id }
            val weekFields = WeekFields.ISO

            // ─── All-time overview totals ───
            val totalWorkouts = details.size
            val totalVolume = details.sumOf { s ->
                s.setsByExerciseId.values.flatten()
                    .asSequence()
                    .filter { it.completed && !it.isWarmup }
                    .sumOf { (it.weight * it.reps).toDouble() }
            }.toFloat()
            val durations = details.mapNotNull { d ->
                val start = d.session.startTime
                val end = d.session.endTime
                if (start != null && end != null) {
                    java.time.Duration.between(start, end).toMinutes().toInt()
                } else {
                    null
                }
            }.filter { it > 0 }
            val avgDurationMinutes = if (durations.isEmpty()) 0 else durations.average().toInt()

            // ─── Build exercise-id → name index ───
            val exerciseIdByName = mutableMapOf<String, Int>()
            details.forEach { s ->
                s.exercisesById.forEach { (id, ex) ->
                    exerciseIdByName.putIfAbsent(ex.name, id)
                }
            }

            // ─── Featured exercise (most-logged compound) ───
            val compoundNames = listOf("Back Squat", "Barbell Bench Press", "Barbell OH Press", "Weighted Pull-ups")
            val sessionCountByCompound = mutableMapOf<String, Int>()
            details.forEach { s ->
                s.exercisesById.values.forEach { ex ->
                    if (ex.name in compoundNames) {
                        sessionCountByCompound[ex.name] = (sessionCountByCompound[ex.name] ?: 0) + 1
                    }
                }
            }
            val featuredExerciseName = sessionCountByCompound.maxByOrNull { it.value }?.key
            val featuredExerciseId = featuredExerciseName?.let { exerciseIdByName[it] }
            val featuredExercise = featuredExerciseId?.let { id ->
                details.firstNotNullOfOrNull { it.exercisesById[id] }
            }
            val featuredDay = featuredExercise?.let { dayById[it.workoutDayId] }
            val featuredDayLabel = featuredDay?.name?.uppercase()
            val featuredAccentColor = featuredExercise?.let { dayColorForId(it.workoutDayId) }

            // ─── Featured weekly 1RM (last 12 weeks) ───
            val featuredWeeklyData: List<Float>
            val featuredOneRm: Float?
            val featuredGrowthPercent: Float?

            if (featuredExerciseId != null) {
                val byWeek = mutableMapOf<Pair<Int, Int>, Float>()
                details.forEach { s ->
                    val best = s.bestSetByExerciseId[featuredExerciseId] ?: return@forEach
                    val year = s.session.date.get(weekFields.weekBasedYear())
                    val week = s.session.date.get(weekFields.weekOfWeekBasedYear())
                    val key = year to week
                    byWeek[key] = maxOf(byWeek[key] ?: 0f, best.epley1Rm)
                }
                featuredWeeklyData = byWeek.entries
                    .sortedWith(compareBy({ it.key.first }, { it.key.second }))
                    .map { it.value }
                    .takeLast(12)
                featuredOneRm = featuredWeeklyData.lastOrNull()

                val currentMonthStart = today.withDayOfMonth(1)
                val prevMonthStart = currentMonthStart.minusMonths(1)
                val currentMonthBest = details
                    .filter { it.session.date >= currentMonthStart }
                    .mapNotNull { it.bestSetByExerciseId[featuredExerciseId]?.epley1Rm }
                    .maxOrNull()
                val prevMonthBest = details
                    .filter { it.session.date >= prevMonthStart && it.session.date < currentMonthStart }
                    .mapNotNull { it.bestSetByExerciseId[featuredExerciseId]?.epley1Rm }
                    .maxOrNull()
                featuredGrowthPercent = if (currentMonthBest != null && prevMonthBest != null && prevMonthBest > 0f) {
                    (currentMonthBest - prevMonthBest) / prevMonthBest * 100f
                } else null
            } else {
                featuredWeeklyData = emptyList()
                featuredOneRm = null
                featuredGrowthPercent = null
            }

            // ─── Sparklines for Bench and OH Press ───
            fun sparklineFor(exerciseName: String): Pair<Float?, List<Float>> {
                val exId = exerciseIdByName[exerciseName] ?: return null to emptyList()
                val data = details
                    .mapNotNull { s ->
                        val best = s.bestSetByExerciseId[exId] ?: return@mapNotNull null
                        s.session.date to best.epley1Rm
                    }
                    .sortedBy { it.first }
                    .map { it.second }
                return data.lastOrNull() to data
            }

            val (benchOneRm, benchSparkline) = sparklineFor("Barbell Bench Press")
            val (ohPressOneRm, ohPressSparkline) = sparklineFor("Barbell OH Press")

            // ─── Weekly volume ───
            val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val prevWeekStart = currentWeekStart.minusWeeks(1)

            fun weekVolume(start: LocalDate, end: LocalDate): Float {
                return details
                    .filter { it.session.date >= start && it.session.date < end }
                    .sumOf { s ->
                        s.setsByExerciseId.values.flatten()
                            .filter { it.completed && !it.isWarmup }
                            .sumOf { (it.weight * it.reps).toDouble() }
                    }.toFloat()
            }

            val currentWeekVolume = weekVolume(currentWeekStart, today.plusDays(1))
            val previousWeekVolume = weekVolume(prevWeekStart, currentWeekStart)

            // ─── Days since last rest ───
            val sessionDates = details.map { it.session.date }.toSet()
            var daysSinceLastRest = 0
            var checkDate = today
            while (checkDate in sessionDates && daysSinceLastRest < 365) {
                daysSinceLastRest++
                checkDate = checkDate.minusDays(1)
            }

            // ─── Daily volume heatmap (last 84 days) ───
            val heatmapStart = today.minusDays(83)
            val dailyVolumeByDate = details
                .filter { it.session.date >= heatmapStart && !it.session.date.isAfter(today) }
                .groupBy { it.session.date }
                .mapValues { (_, sessionsOnDay) ->
                    sessionsOnDay.sumOf { s ->
                        s.setsByExerciseId.values.flatten()
                            .asSequence()
                            .filter { it.completed && !it.isWarmup }
                            .sumOf { (it.weight * it.reps).toDouble() }
                    }.toFloat()
                }

            // ─── Training split by workout day ───
            val dayTypeSplit = details
                .groupBy { it.workoutDay.id }
                .map { (dayId, sessionsForDay) ->
                    SplitSlice(
                        dayId = dayId,
                        label = sessionsForDay.first().workoutDay.name,
                        count = sessionsForDay.size,
                    )
                }
                .sortedBy { it.dayId }

            // ─── Session duration series (chronological) ───
            val durationSeries = details.mapNotNull { d ->
                val start = d.session.startTime
                val end = d.session.endTime
                if (start != null && end != null) {
                    val minutes = java.time.Duration.between(start, end).toMinutes().toInt()
                    if (minutes > 0) d.session.date to minutes else null
                } else {
                    null
                }
            }

            // ─── Weekly volume series (reuses legacy computation) ───
            val weeklyVolumeSeries = computeWeeklyVolume(details).map { it.weekLabel to it.totalKg }

            // ─── Personal records display ───
            val prDisplays = prs.mapNotNull { pr ->
                try {
                    val day = dayById[pr.exercise.workoutDayId]
                    val categoryLabel = when (day?.dayType) {
                        DayType.POWER -> "STRENGTH"
                        DayType.HYPERTROPHY -> "PRECISION"
                        DayType.CONDITIONING -> "ENDURANCE"
                        null -> "STRENGTH"
                    }
                    val accentColor = try {
                        Color(android.graphics.Color.parseColor(pr.dayColor))
                    } catch (e: Exception) {
                        dayColorForId(pr.exercise.workoutDayId)
                    }
                    PersonalRecordDisplay(
                        exerciseName = pr.exercise.name,
                        weight = pr.weight,
                        reps = pr.reps,
                        date = pr.date,
                        accentColor = accentColor,
                        categoryLabel = categoryLabel,
                    )
                } catch (e: Exception) {
                    null
                }
            }

            ProgressUiState(
                hasNoData = false,
                totalWorkouts = totalWorkouts,
                totalVolumeKg = totalVolume,
                avgDurationMinutes = avgDurationMinutes,
                prCount = prs.size,
                featuredExerciseName = featuredExerciseName,
                featuredDayLabel = featuredDayLabel,
                featuredOneRm = featuredOneRm,
                featuredGrowthPercent = featuredGrowthPercent,
                featuredWeeklyData = featuredWeeklyData.ifEmpty { null },
                featuredAccentColor = featuredAccentColor,
                benchOneRm = benchOneRm,
                benchSparkline = benchSparkline,
                ohPressOneRm = ohPressOneRm,
                ohPressSparkline = ohPressSparkline,
                currentWeekVolume = currentWeekVolume,
                previousWeekVolume = previousWeekVolume,
                daysSinceLastRest = daysSinceLastRest,
                personalRecords = prDisplays,
                dailyVolumeByDate = dailyVolumeByDate,
                dayTypeSplit = dayTypeSplit,
                durationSeries = durationSeries,
                weeklyVolumeSeries = weeklyVolumeSeries,
            )
        } catch (e: Exception) {
            ProgressUiState(hasNoData = true)
        }
    }

    // ─── Utilities ───────────────────────────────────────────────────────────

    private fun epley(weight: Float, reps: Int): Float {
        if (weight <= 0f || reps <= 0) return 0f
        return weight * (1f + (reps / 30f))
    }

    private fun compareBest(wA: Float, rA: Int, wB: Float, rB: Int): Int {
        val w = wA.compareTo(wB)
        if (w != 0) return w
        return rA.compareTo(rB)
    }
}
