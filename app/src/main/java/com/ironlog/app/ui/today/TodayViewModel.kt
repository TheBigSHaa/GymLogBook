package com.ironlog.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironlog.app.data.model.ScheduledWorkout
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class WeekStats(
    val programWeek: Int = 1,
    val workoutsThisWeek: Int = 0,
    val weeklyTarget: Int = 4,
    val trainingDayStreak: Int = 0,
)

enum class DayMarkerKind { COMPLETED, SCHEDULED, REST, NONE }

data class DayMarker(
    val date: LocalDate,
    val dayOfWeek: DayOfWeek,
    val isToday: Boolean,
    val kind: DayMarkerKind,
    val workoutDay: WorkoutDay?,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val workoutRepository: WorkoutRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val today: LocalDate = LocalDate.now()

    private val _selectedDate = MutableStateFlow(today)
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val _currentMonth = MutableStateFlow(java.time.YearMonth.now())
    val currentMonth: StateFlow<java.time.YearMonth> = _currentMonth

    fun updateMonth(yearMonth: java.time.YearMonth) {
        _currentMonth.value = yearMonth
    }

    val workoutDays: StateFlow<List<WorkoutDay>> =
        workoutRepository.workoutDays()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val todaySchedule: StateFlow<ScheduledWorkout?> =
        _selectedDate.flatMapLatest { date ->
            scheduleRepository.scheduledForDate(date)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthSchedules: StateFlow<Map<LocalDate, ScheduledWorkout>> =
        _currentMonth.flatMapLatest { ym ->
            val start = ym.atDay(1)
            val end = ym.atEndOfMonth()
            scheduleRepository.scheduledForRange(start, end)
        }.map { list -> list.associateBy { it.date } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val completedSessions: StateFlow<List<WorkoutSession>> =
        workoutRepository.completedSessions()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val exercises =
        todaySchedule
            .map { it?.workoutDayId }
            .flatMapLatest { dayId ->
                if (dayId == null) {
                    kotlinx.coroutines.flow.flowOf(emptyList())
                } else {
                    workoutRepository.exercisesForDay(dayId)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weekStats: StateFlow<WeekStats> =
        combine(
            completedSessions,
            settingsStore.programStartDate,
            settingsStore.firstDayOfWeek,
        ) { sessions, startOverride, firstDay ->
            val completed = sessions.filter { it.completed }
            val programStart = startOverride ?: completed.minByOrNull { it.date }?.date ?: today
            val programWeek = ((java.time.temporal.ChronoUnit.DAYS.between(programStart, today) / 7) + 1)
                .toInt()
                .coerceIn(1, 12)

            val firstDow = parseDayOfWeek(firstDay)
            val weekStart = today.with(TemporalAdjusters.previousOrSame(firstDow))
            val weekEnd = weekStart.plusDays(6)
            val workoutsThisWeek = completed.count { it.date in weekStart..weekEnd }

            val completedDates = completed.map { it.date }.distinct().sortedDescending()
            var streak = 0
            var cursor = today
            while (completedDates.contains(cursor)) {
                streak += 1
                cursor = cursor.minusDays(1)
            }

            WeekStats(
                programWeek = programWeek,
                workoutsThisWeek = workoutsThisWeek,
                trainingDayStreak = streak,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeekStats())

    private fun parseDayOfWeek(raw: String): DayOfWeek =
        runCatching { DayOfWeek.valueOf(raw) }.getOrDefault(DayOfWeek.SUNDAY)

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _currentMonth.value = YearMonth.from(date)
    }

    fun selectWorkoutDay(dayId: Int) {
        viewModelScope.launch {
            scheduleRepository.upsert(
                ScheduledWorkout(
                    workoutDayId = dayId,
                    date = _selectedDate.value,
                    isRestDay = false,
                ),
            )
        }
    }

    fun markRestDay() {
        viewModelScope.launch {
            scheduleRepository.upsert(
                ScheduledWorkout(
                    workoutDayId = null,
                    date = _selectedDate.value,
                    isRestDay = true,
                ),
            )
        }
    }
}

