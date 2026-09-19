package com.ironlog.app.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironlog.app.data.model.Exercise
import com.ironlog.app.data.model.SetLog
import com.ironlog.app.data.model.WorkoutDay
import com.ironlog.app.data.model.WorkoutSession
import com.ironlog.app.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject

data class ExerciseLogWithSetsUi(
    val exercise: Exercise,
    val sets: List<SetWithPr>,
)

data class SetWithPr(
    val set: SetLog,
    val isPr: Boolean,
)

data class SessionWithDetails(
    val session: WorkoutSession,
    val workoutDay: WorkoutDay,
    val exercises: List<ExerciseLogWithSetsUi>,
    val durationMinutes: Int,
    val totalVolume: Long,
)

@HiltViewModel
class LogViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {
    private val _selectedDayFilter = MutableStateFlow<Int?>(null)
    val selectedDayFilter: StateFlow<Int?> = _selectedDayFilter

    fun setDayFilter(dayId: Int?) {
        _selectedDayFilter.value = dayId
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            workoutRepository.deleteSession(sessionId)
        }
    }

    val workoutDays: StateFlow<List<WorkoutDay>> =
        workoutRepository.workoutDays()
            .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessions: StateFlow<List<SessionWithDetails>> =
        combine(workoutRepository.completedSessions(), workoutDays, selectedDayFilter) { sessions, days, filter ->
            Triple(sessions, days, filter)
        }.mapLatest { (sessions, days, filter) ->
            val dayById = days.associateBy { it.id }
            val filtered = sessions
                .filter { it.completed }
                .let { list -> if (filter == null) list else list.filter { it.workoutDayId == filter } }

            val built = filtered.mapNotNull { session ->
                val day = dayById[session.workoutDayId] ?: return@mapNotNull null

                val logs = workoutRepository.logsForSessionOnce(session.id)
                val exercises = buildList {
                    logs.forEach { log ->
                        val exercise = workoutRepository.exerciseById(log.exerciseId) ?: return@forEach
                        val sets = workoutRepository.setsForExerciseLogOnce(log.id)
                        add(ExerciseLogWithSetsUi(exercise = exercise, sets = sets.map { SetWithPr(it, isPr = false) }))
                    }
                }

                val durationMinutes = computeDurationMinutes(session)
                val volume = computeSessionVolume(exercises)

                SessionWithDetails(
                    session = session,
                    workoutDay = day,
                    exercises = exercises,
                    durationMinutes = durationMinutes,
                    totalVolume = volume,
                )
            }

            // Mark PR sets "at the time" by scanning chronologically.
            markPrsChronologically(built)
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun computeDurationMinutes(session: WorkoutSession): Int {
        val start = session.startTime
        val end = session.endTime
        if (start == null || end == null) return 0
        return Duration.between(start, end).toMinutes().toInt().coerceAtLeast(0)
    }

    private fun computeSessionVolume(exercises: List<ExerciseLogWithSetsUi>): Long {
        val vol = exercises.sumOf { ex ->
            ex.sets
                .asSequence()
                .map { it.set }
                .filter { it.completed && !it.isWarmup }
                .sumOf { (it.weight * it.reps).toDouble() }
        }
        return vol.toLong()
    }

    private fun markPrsChronologically(desc: List<SessionWithDetails>): List<SessionWithDetails> {
        // Input is newest-first; we want oldest-first for PR-at-time detection.
        val asc = desc.sortedWith(compareBy<SessionWithDetails> { it.session.date }.thenBy { it.session.id })
        // Track best (weight, reps, setId) so equal weight+reps falls back to the earlier set,
        // preventing the PR badge from flickering between identical sets.
        val bestByExerciseId = mutableMapOf<Int, Triple<Float, Int, Long>>()

        val updatedAsc = asc.map { s ->
            val updatedExercises = s.exercises.map { ex ->
                val exId = ex.exercise.id
                val priorBest = bestByExerciseId[exId]
                val updatedSets = ex.sets.map { swp ->
                    val set = swp.set
                    val isWorking = set.completed && !set.isWarmup
                    val isPr = if (!isWorking) false else {
                        val pb = priorBest
                        pb == null || compareBest(set.weight, set.reps, set.id, pb.first, pb.second, pb.third) > 0
                    }
                    swp.copy(isPr = isPr)
                }

                // Update best using best set in this session
                ex.sets
                    .asSequence()
                    .map { it.set }
                    .filter { it.completed && !it.isWarmup }
                    .maxWithOrNull { a, b -> compareBest(a.weight, a.reps, a.id, b.weight, b.reps, b.id) }
                    ?.let { best ->
                        val pb = bestByExerciseId[exId]
                        if (pb == null || compareBest(best.weight, best.reps, best.id, pb.first, pb.second, pb.third) > 0) {
                            bestByExerciseId[exId] = Triple(best.weight, best.reps, best.id)
                        }
                    }

                ex.copy(sets = updatedSets)
            }
            s.copy(exercises = updatedExercises)
        }

        // return back to newest-first order
        return updatedAsc.sortedWith(compareByDescending<SessionWithDetails> { it.session.date }.thenByDescending { it.session.id })
    }

    private fun compareBest(wA: Float, rA: Int, idA: Long, wB: Float, rB: Int, idB: Long): Int {
        val w = wA.compareTo(wB)
        if (w != 0) return w
        val r = rA.compareTo(rB)
        if (r != 0) return r
        // Lower id is older; older with same weight+reps wins so the PR badge stays on the
        // first occurrence rather than the most recent equal one.
        return idB.compareTo(idA)
    }
}

