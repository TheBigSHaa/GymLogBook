package com.ironlog.app.ui.plan

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironlog.app.data.model.ScheduledWorkout
import com.ironlog.app.data.sync.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ironlog.app.data.model.Exercise
import com.ironlog.app.data.model.SetLog
import com.ironlog.app.data.model.WorkoutDay
import com.ironlog.app.data.model.WorkoutSession
import com.ironlog.app.data.repository.ScheduleRepository
import com.ironlog.app.data.repository.WorkoutRepository
import com.ironlog.app.util.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class WeekStatus(
    val weekIndex: Int, // 1..12
    val completedCount: Int,
    val isDeload: Boolean,
    val isCurrent: Boolean,
)

data class PastExerciseDetail(
    val exercise: Exercise,
    val sets: List<SetLog>,
)

/** A completed session loaded for the calendar's "view / edit / continue" sheet. */
data class PastWorkoutDetails(
    val session: WorkoutSession,
    val day: WorkoutDay,
    val exercises: List<PastExerciseDetail>,
    val totalVolumeKg: Long,
    val durationMinutes: Int,
)

@HiltViewModel
class PlanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleRepository: ScheduleRepository,
    private val workoutRepository: WorkoutRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    val firstDayOfWeek: StateFlow<String> =
        settingsStore.firstDayOfWeek
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "SUNDAY")

    fun navigateMonth(delta: Int) {
        setMonth(_currentMonth.value.plusMonths(delta.toLong()))
    }

    fun setMonth(yearMonth: YearMonth) {
        _currentMonth.value = yearMonth
    }

    val workoutDays: StateFlow<List<WorkoutDay>> =
        workoutRepository.workoutDays()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val scheduledWorkouts: StateFlow<Map<LocalDate, ScheduledWorkout>> =
        currentMonth.flatMapLatest { ym ->
            val start = ym.atDay(1)
            val end = ym.atEndOfMonth()
            scheduleRepository.scheduledForRange(start, end)
        }.map { list -> list.associateBy { it.date } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val completedDates: StateFlow<Map<LocalDate, WorkoutDay>> =
        combine(workoutRepository.completedSessions(), workoutDays) { sessions, days ->
            val dayById = days.associateBy { it.id }
            sessions
                .filter { it.completed }
                .associate { it.date to (dayById[it.workoutDayId] ?: WorkoutDay(it.workoutDayId, "DAY ${it.workoutDayId}", com.ironlog.app.data.model.DayType.POWER, "#FFFFFF")) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val weekProgress: StateFlow<List<WeekStatus>> =
        workoutRepository.completedSessions()
            .map { sessions ->
                // Day 5 is "bonus" and should not count toward the 4-workouts/week target.
                val completed = sessions.filter { it.completed && it.workoutDayId != 5 }.sortedBy { it.date }
                val today = LocalDate.now()
                val start = completed.firstOrNull()?.date ?: today
                val programStartWeekStart = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val currentWeekIndex =
                    ((java.time.temporal.ChronoUnit.DAYS.between(programStartWeekStart, today) / 7) + 1).toInt().coerceIn(1, 12)

                (1..12).map { week ->
                    val weekStart = programStartWeekStart.plusDays((week - 1L) * 7L)
                    val weekEnd = weekStart.plusDays(6)
                    val count = completed.count { it.date in weekStart..weekEnd }
                    WeekStatus(
                        weekIndex = week,
                        completedCount = count,
                        isDeload = week == 9,
                        isCurrent = week == currentWeekIndex,
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Past-workout details for a tapped calendar day ─────────────────────────
    private val _pastWorkout = MutableStateFlow<PastWorkoutDetails?>(null)
    val pastWorkout: StateFlow<PastWorkoutDetails?> = _pastWorkout

    fun loadPastWorkout(date: LocalDate) {
        viewModelScope.launch {
            val session = workoutRepository.completedSessions().first()
                .filter { it.completed && it.date == date }
                .maxByOrNull { it.id }
            if (session == null) {
                _pastWorkout.value = null
                return@launch
            }
            val day = workoutRepository.workoutDayById(session.workoutDayId) ?: run {
                _pastWorkout.value = null
                return@launch
            }
            val logs = workoutRepository.logsForSessionOnce(session.id)
            val exercises = logs.mapNotNull { log ->
                val exercise = workoutRepository.exerciseById(log.exerciseId) ?: return@mapNotNull null
                PastExerciseDetail(exercise, workoutRepository.setsForExerciseLogOnce(log.id))
            }
            val volume = exercises.sumOf { detail ->
                detail.sets
                    .filter { it.completed && !it.isWarmup }
                    .sumOf { (it.weight * it.reps).toDouble() }
            }.toLong()
            val duration = if (session.startTime != null && session.endTime != null) {
                java.time.Duration.between(session.startTime, session.endTime)
                    .toMinutes().toInt().coerceAtLeast(0)
            } else {
                0
            }
            _pastWorkout.value = PastWorkoutDetails(
                session = session,
                day = day,
                exercises = exercises,
                totalVolumeKg = volume,
                durationMinutes = duration,
            )
        }
    }

    fun clearPastWorkout() {
        _pastWorkout.value = null
    }

    fun scheduleWorkout(date: LocalDate, dayId: Int) {
        viewModelScope.launch {
            scheduleRepository.upsert(ScheduledWorkout(workoutDayId = dayId, date = date, isRestDay = false))
            SyncWorker.syncNow(context)
        }
    }

    fun scheduleRestDay(date: LocalDate) {
        viewModelScope.launch {
            scheduleRepository.upsert(ScheduledWorkout(workoutDayId = null, date = date, isRestDay = true))
            SyncWorker.syncNow(context)
        }
    }

    fun removeSchedule(date: LocalDate) {
        viewModelScope.launch {
            scheduleRepository.deleteForDate(date)
            SyncWorker.syncNow(context)
        }
    }

    fun clearSchedule(date: LocalDate) = removeSchedule(date)

    fun autoFillWeek(startDate: LocalDate) {
        // Only intended for Day 1; fills next 4 days (Day 2, 3, 4, Rest) without overwriting.
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val rangeEnd = startDate.plusDays(4)
            val existing = scheduleRepository.scheduledForRange(startDate, rangeEnd).first().associateBy { it.date }

            val plan = listOf(
                startDate.plusDays(1) to ScheduledWorkout(id = 0, workoutDayId = 2, date = startDate.plusDays(1), isRestDay = false),
                startDate.plusDays(2) to ScheduledWorkout(id = 0, workoutDayId = 3, date = startDate.plusDays(2), isRestDay = false),
                startDate.plusDays(3) to ScheduledWorkout(id = 0, workoutDayId = 4, date = startDate.plusDays(3), isRestDay = false),
                startDate.plusDays(4) to ScheduledWorkout(id = 0, workoutDayId = null, date = startDate.plusDays(4), isRestDay = true),
            )

            plan.forEach { (date, item) ->
                if (existing[date] == null) scheduleRepository.upsert(item)
            }
        }
    }
}

