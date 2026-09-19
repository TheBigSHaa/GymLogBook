package com.ironlog.app.ui.workout

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironlog.app.data.model.DayType
import com.ironlog.app.data.sync.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ironlog.app.data.model.EquipmentType
import com.ironlog.app.data.model.Exercise
import com.ironlog.app.data.model.ExerciseLog
import com.ironlog.app.data.model.SetLog
import com.ironlog.app.data.model.WorkoutDay
import com.ironlog.app.data.model.WorkoutSession
import com.ironlog.app.data.repository.WorkoutRepository
import com.ironlog.app.service.ActiveWorkoutService
import com.ironlog.app.util.Day5CircuitProtocol
import com.ironlog.app.util.ExerciseClassifier
import com.ironlog.app.util.ProgressionCalculator
import com.ironlog.app.util.RestTimerMath
import com.ironlog.app.util.RestTimerRuntimeStore
import com.ironlog.app.util.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.max

data class ActiveWorkoutState(
    val workoutDay: WorkoutDay? = null,
    val exercises: List<ExerciseWithSets> = emptyList(),
    val availableDbWeights: List<Float> = listOf(6f, 10f, 12f, 17f, 20f),
    val elapsedSeconds: Long = 0L,
    val startedAtEpochMillis: Long = 0L,
    val restTimerSeconds: Int? = null,
    val restTimerTotal: Int = 0,
    val restTimerVisible: Boolean = true,
    val restTimerPaused: Boolean = true,
    /** Bottom-timer status (PAUSE/READY/DONE). Null keeps the default PAUSE/READY. */
    val timerStatusLabel: String? = null,
    val circuitRoundsCompleted: Int = 0,
    val isDay5TimedProtocol: Boolean = false,
    val day5ActiveExerciseIndex: Int? = null,
    val day5ActiveSetIndex: Int? = null,
    val isFinishing: Boolean = false,
    val summary: WorkoutSummary? = null,
    val showNoSetsWarning: Boolean = false,
    val isEditingExistingSession: Boolean = false,
)

data class WorkoutSummary(
    val durationSeconds: Long,
    val totalVolume: Float,
    val exercisesCompleted: Int,
    val exercisesTotal: Int,
    val prExerciseIds: Set<Int> = emptySet(),
)

data class ExerciseWithSets(
    val exercise: Exercise,
    val sets: List<EditableSet>,
    val suggestion: ProgressionCalculator.Suggestion?,
    val lastLine: String?,
    val isExpanded: Boolean,
    /** Working sets from the previous session for this exercise, ordered by set number. */
    val previousSets: List<PreviousSetInfo> = emptyList(),
)

/** What the user accomplished LAST TIME for the same set position. */
data class PreviousSetInfo(
    val weight: Float,
    val reps: Int,
    val restSeconds: Int?,
    /** Whether the set was actually completed last time (only completed sets are shown). */
    val completed: Boolean = true,
)

/** Process-lifetime unique ids so a set keeps its identity across list mutations. */
private object SetUid {
    private val counter = java.util.concurrent.atomic.AtomicLong(1L)
    fun next(): Long = counter.getAndIncrement()
}

data class EditableSet(
    val setNumber: Int,
    val weight: Float,
    val reps: Int,
    val completed: Boolean,
    val isWarmup: Boolean,
    val rpe: Int?,
    /** Rest actually taken after completing this set, in seconds. */
    val restSeconds: Int? = null,
    /** Stable identity for this set within the live editor (survives reorder/insert/delete). */
    val uid: Long = SetUid.next(),
)

/** Attribution of a running rest to the set that started it, by stable identity. */
private data class RestAttribution(
    val exerciseId: Int,
    val setUid: Long,
    val startedAtEpochMs: Long,
)

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workoutRepository: WorkoutRepository,
    private val savedStateHandle: SavedStateHandle,
    private val settingsStore: SettingsStore,
    private val restRuntimeStore: RestTimerRuntimeStore,
) : ViewModel() {
    private val _state = MutableStateFlow(ActiveWorkoutState())
    val state: StateFlow<ActiveWorkoutState> = _state

    val workoutScreenTimeout: StateFlow<SettingsStore.WorkoutScreenTimeout> =
        settingsStore.workoutScreenTimeout.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsStore.WorkoutScreenTimeout.OFF,
        )

    private var restJob: Job? = null
    private var elapsedJob: Job? = null

    private var day5Intervals: List<Day5CircuitProtocol.Interval> = emptyList()
    private var day5Index: Int = 0
    /** Bumped on reset so a just-finished interval does not auto-chain into the next. */
    private var day5Generation: Int = 0
    /** End timestamp already advanced, so a second ticker cannot complete the same interval twice. */
    private var day5AdvancedEndMs: Long? = null

    /** Single source of truth for the running rest countdown. */
    private var restEndEpochMs: Long? = null

    /**
     * The set the running rest belongs to, tracked by STABLE identity so reordering,
     * inserting or deleting other sets during the rest can't mis-attribute it.
     * Finalized (elapsed wall-clock rest written into that set) when the next set is
     * completed or the workout finishes.
     */
    private var pendingRest: RestAttribution? = null

    private val workoutDayId: Int = checkNotNull(savedStateHandle["workoutDayId"])

    /** > 0 when opened from the calendar to edit / continue a saved session. */
    private val sessionIdArg: Long = savedStateHandle.get<Long>("sessionId") ?: -1L
    private var editingSession: WorkoutSession? = null

    private var startedAtEpochMillis: Long =
        savedStateHandle.get<Long>("startedAtEpochMillis") ?: run {
            val now = System.currentTimeMillis()
            savedStateHandle["startedAtEpochMillis"] = now
            now
        }

    init {
        _state.update { it.copy(startedAtEpochMillis = startedAtEpochMillis) }
        viewModelScope.launch {
            val day = workoutRepository.workoutDayById(workoutDayId)
            _state.update { it.copy(workoutDay = day) }

            val editing = if (sessionIdArg > 0) workoutRepository.sessionById(sessionIdArg) else null
            editingSession = editing
            if (editing != null) {
                editing.startTime?.let { start ->
                    startedAtEpochMillis = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    savedStateHandle["startedAtEpochMillis"] = startedAtEpochMillis
                }
                _state.update {
                    it.copy(
                        startedAtEpochMillis = startedAtEpochMillis,
                        isEditingExistingSession = true,
                    )
                }
                ActiveWorkoutService.start(context, workoutDayId = workoutDayId, startedAtEpochMillis = startedAtEpochMillis)
            }

            // Keep the notification in sync with the workout day's default rest duration.
            val dayTypeForDefaults = day?.dayType ?: DayType.POWER
            val defaultRest = when (dayTypeForDefaults) {
                DayType.POWER -> settingsStore.restPowerSeconds.first()
                DayType.HYPERTROPHY -> settingsStore.restHypertrophySeconds.first()
                DayType.CONDITIONING -> settingsStore.restBonusSeconds.first()
            }
            restRuntimeStore.setRestDefaultSeconds(defaultRest)

            val dumbbells = settingsStore.dumbbellWeightsKg.first()
            _state.update { it.copy(availableDbWeights = dumbbells) }
            val exercises = workoutRepository.exercisesForDay(workoutDayId).first()

            val savedLogsByExerciseId = if (editing != null) {
                workoutRepository.logsForSessionOnce(editing.id).associateBy { it.exerciseId }
            } else {
                emptyMap()
            }

            val isDay5Timed = workoutDayId == 5 &&
                exercises.any { Day5CircuitProtocol.isDay5Section(it.section) }
            _state.update { it.copy(isDay5TimedProtocol = isDay5Timed) }
            val built = exercises.mapIndexed { index, ex ->
                val recentSessions = workoutRepository.recentSessionSetsForExercise(
                    exerciseId = ex.id,
                    excludeSessionId = editing?.id ?: -1L,
                    sessionCount = 2,
                )
                val lastSessionSets = recentSessions.getOrNull(0).orEmpty()
                val priorSessionSets = recentSessions.getOrNull(1).orEmpty()
                val (lastWeight, lastReps) = extractLastWorkingTopSet(lastSessionSets)

                // Per-set history: what was accomplished LAST TIME, aligned by working-set
                // position (all working sets kept so ordinals line up 1:1 with this session;
                // the UI only surfaces the ones that were actually completed).
                val previousSets = lastSessionSets
                    .filter { !it.isWarmup }
                    .sortedBy { it.setNumber }
                    .map {
                        PreviousSetInfo(
                            weight = it.weight,
                            reps = it.reps,
                            restSeconds = it.restSeconds,
                            completed = it.completed,
                        )
                    }

                val hasHistory = lastSessionSets.any { it.completed && !it.isWarmup }
                val suggestion = if (!isDay5Timed && hasHistory) {
                    ProgressionCalculator.suggestWeightDetailed(
                        exercise = ex,
                        lastSets = lastSessionSets,
                        currentWeek = 1,
                        isCompound = false,
                        availableDumbbellsKg = dumbbells,
                        priorSets = priorSessionSets,
                    )
                } else {
                    null
                }

                val prefillWeight = when {
                    isDay5Timed -> 0f
                    ex.equipmentType == EquipmentType.KETTLEBELL -> suggestion?.weightKg ?: 10f
                    ex.equipmentType == EquipmentType.BAND -> 0f
                    ex.equipmentType == EquipmentType.DUMBBELL -> {
                        val raw = suggestion?.weightKg ?: lastWeight
                        if (raw > 0f) ProgressionCalculator.snapToAvailableDumbbell(raw, dumbbells) else raw
                    }
                    else -> suggestion?.weightKg ?: lastWeight
                }

                val repsPrefill = when {
                    isDay5Timed -> 0
                    else -> suggestion?.suggestedReps ?: if (ex.targetRepsMax == 0) 0 else ex.targetRepsMin
                }
                val setCount = if (isDay5Timed) ex.targetSets else (suggestion?.suggestedSets ?: ex.targetSets)
                val savedLog = savedLogsByExerciseId[ex.id]
                val sets = when {
                    // Editing a saved session: load the logged sets, warm-ups first.
                    savedLog != null -> {
                        val saved = workoutRepository.setsForExerciseLogOnce(savedLog.id)
                        if (isDay5Timed && saved.isEmpty()) {
                            (1..setCount).map { setNum ->
                                EditableSet(
                                    setNumber = setNum,
                                    weight = 0f,
                                    reps = 0,
                                    completed = false,
                                    isWarmup = false,
                                    rpe = null,
                                )
                            }
                        } else {
                            normalizeSetNumbers(
                                saved
                                    .sortedWith(compareBy({ !it.isWarmup }, { it.setNumber }))
                                    .map {
                                        EditableSet(
                                            setNumber = it.setNumber,
                                            weight = it.weight,
                                            reps = it.reps,
                                            completed = it.completed,
                                            isWarmup = it.isWarmup,
                                            rpe = it.rpe,
                                            restSeconds = it.restSeconds,
                                        )
                                    },
                            )
                        }
                    }

                    else -> (1..setCount).map { setNum ->
                        EditableSet(
                            setNumber = setNum,
                            weight = prefillWeight,
                            reps = repsPrefill,
                            completed = false,
                            isWarmup = false,
                            rpe = null,
                        )
                    }
                }

                val lastLine = when {
                    isDay5Timed -> null

                    editing != null && savedLog != null ->
                        "Editing saved workout — adjust sets, then finish to update"

                    suggestion != null && suggestion.note != null -> suggestion.note

                    suggestion != null && (lastWeight > 0f || lastReps > 0) -> {
                        val toWeight = "${suggestion.weightKg}kg"
                        val repsHint = suggestion.suggestedReps?.let { " × $it reps" } ?: ""
                        val setsHint = suggestion.suggestedSets?.let { " × $it sets" } ?: ""
                        "Last: ${lastWeight}kg × ${lastReps} → Suggested: $toWeight$repsHint$setsHint"
                    }

                    lastWeight > 0f || lastReps > 0 -> "Last: ${lastWeight}kg × ${lastReps}"

                    else -> "Enter your starting weight"
                }

                ExerciseWithSets(
                    exercise = ex,
                    sets = sets,
                    suggestion = suggestion,
                    lastLine = lastLine,
                    isExpanded = index == 0,
                    previousSets = previousSets,
                )
            }
            _state.update { it.copy(exercises = built) }
            if (isDay5Timed) {
                armDay5Protocol(
                    sections = built.map { it.exercise.section },
                    circuitRestSeconds = settingsStore.restBonusSeconds.first(),
                )
            } else if (defaultRest > 0) {
                _state.update {
                    it.copy(
                        restTimerSeconds = defaultRest,
                        restTimerTotal = defaultRest,
                        restTimerVisible = true,
                        restTimerPaused = true,
                    )
                }
            }
        }

        startElapsedTimer()
        ActiveWorkoutService.start(context, workoutDayId = workoutDayId, startedAtEpochMillis = startedAtEpochMillis)

        // If the rest timer is started from the notification shade, sync it into the UI state.
        viewModelScope.launch {
            restRuntimeStore.state
                .distinctUntilChanged()
                .collect { runtime ->
                    syncRestFromRuntime(
                        restEndEpochMs = runtime.restEndEpochMs,
                        paused = runtime.restPaused,
                        defaultSeconds = runtime.restDefaultSeconds,
                    )
                }
        }
    }

    private fun syncRestFromRuntime(restEndEpochMs: Long?, paused: Boolean, defaultSeconds: Int) {
        // Day 5's interval clock is owned by this ViewModel. Store/service echoes
        // must not restart the ticker or they can complete the same set twice.
        if (_state.value.isDay5TimedProtocol) return

        // Ignore echoes of our own writes — the end timestamp is the identity of a countdown.
        if (restEndEpochMs == this.restEndEpochMs && paused == _state.value.restTimerPaused) return

        if (restEndEpochMs == null || paused) {
            restJob?.cancel()
            this.restEndEpochMs = null
            _state.update {
                // Keep remaining time after a mid-countdown pause; only fall back to the
                // day's default when the timer wasn't already holding a paused value.
                val keepRemaining = it.restTimerPaused && it.restTimerSeconds != null
                it.copy(
                    restTimerSeconds = if (keepRemaining) it.restTimerSeconds
                        else if (defaultSeconds > 0) defaultSeconds
                        else it.restTimerSeconds,
                    restTimerTotal = if (keepRemaining) it.restTimerTotal
                        else if (defaultSeconds > 0) defaultSeconds
                        else it.restTimerTotal,
                    restTimerPaused = true,
                )
            }
            return
        }

        val remaining = RestTimerMath.remainingSeconds(restEndEpochMs, System.currentTimeMillis())
        val total = if (defaultSeconds > 0) maxOf(defaultSeconds, remaining) else maxOf(remaining, _state.value.restTimerTotal)

        this.restEndEpochMs = restEndEpochMs
        _state.update {
            it.copy(
                restTimerSeconds = remaining,
                restTimerTotal = total,
                restTimerVisible = true,
                restTimerPaused = false,
            )
        }
        startRestTicker()
    }

    override fun onCleared() {
        restJob?.cancel()
        elapsedJob?.cancel()
        ActiveWorkoutService.stop(context)
        super.onCleared()
    }

    private fun startElapsedTimer() {
        elapsedJob?.cancel()
        elapsedJob = viewModelScope.launch {
            while (true) {
                val elapsed = max(0L, (System.currentTimeMillis() - startedAtEpochMillis) / 1000L)
                _state.update { it.copy(elapsedSeconds = elapsed) }
                delay(1_000)
            }
        }
    }

    fun updateSetWeight(exerciseIndex: Int, setIndex: Int, weight: Float) {
        _state.update { s ->
            s.copy(exercises = s.exercises.updateExercise(exerciseIndex) { ex ->
                val current = ex.sets.getOrNull(setIndex) ?: return@updateExercise ex
                val isDumbbell = ex.exercise.equipmentType == EquipmentType.DUMBBELL &&
                    !ExerciseClassifier.isAbsExercise(ex.exercise.name, ex.exercise.section)
                val heavier = isDumbbell && !current.isWarmup && weight > current.weight
                val resetReps = if (ex.exercise.targetRepsMin > 0) ex.exercise.targetRepsMin else current.reps
                ex.copy(
                    sets = ex.sets.updateAt(setIndex) { set ->
                        set.copy(
                            weight = weight,
                            reps = if (heavier) resetReps else set.reps,
                        )
                    },
                )
            })
        }
    }

    fun updateSetReps(exerciseIndex: Int, setIndex: Int, reps: Int) {
        _state.update { s ->
            s.copy(exercises = s.exercises.updateExercise(exerciseIndex) { ex ->
                ex.copy(sets = ex.sets.updateAt(setIndex) { it.copy(reps = reps) })
            })
        }
    }

    fun toggleSetCompleted(exerciseIndex: Int, setIndex: Int) {
        val dayType = _state.value.workoutDay?.dayType ?: DayType.POWER
        val exercise = _state.value.exercises.getOrNull(exerciseIndex)?.exercise

        _state.update { s ->
            s.copy(exercises = s.exercises.updateExercise(exerciseIndex) { ex ->
                ex.copy(sets = ex.sets.updateAt(setIndex) { set ->
                    // Un-completing a set also discards its recorded rest.
                    set.copy(
                        completed = !set.completed,
                        restSeconds = if (set.completed) null else set.restSeconds,
                    )
                })
            })
        }

        val completedSet = _state.value.exercises.getOrNull(exerciseIndex)?.sets?.getOrNull(setIndex)
        if (completedSet != null && completedSet.completed && exercise != null) {
            if (_state.value.isDay5TimedProtocol) {
                finalizePendingRest()
                pendingRest = null
                return
            }
            val section = exercise.section
            if (section == "WARMUP" || section == "COOLDOWN") {
                finalizePendingRest()
                pendingRest = null
                return
            }
            // A new rest is starting: close out the previous one (recording the full
            // wall-clock rest that just elapsed), then attribute the new rest window
            // to the set that was just completed — tracked by stable uid.
            finalizePendingRest()
            pendingRest = RestAttribution(exercise.id, completedSet.uid, System.currentTimeMillis())
            viewModelScope.launch {
                // Abs/core work always rests 60s, regardless of the day's default.
                val total = if (ExerciseClassifier.isAbsExercise(exercise.name, exercise.section)) {
                    ExerciseClassifier.ABS_REST_SECONDS
                } else {
                    when (dayType) {
                        DayType.POWER -> settingsStore.restPowerSeconds.first()
                        DayType.HYPERTROPHY -> settingsStore.restHypertrophySeconds.first()
                        DayType.CONDITIONING -> settingsStore.restBonusSeconds.first()
                    }
                }
                restRuntimeStore.setRestDefaultSeconds(total)
                startRestTimer(total)
            }
        }
    }

    fun updateSetRpe(exerciseIndex: Int, setIndex: Int, rpe: Int?) {
        _state.update { s ->
            s.copy(exercises = s.exercises.updateExercise(exerciseIndex) { ex ->
                ex.copy(sets = ex.sets.updateAt(setIndex) { it.copy(rpe = rpe) })
            })
        }
    }

    fun addSet(exerciseIndex: Int) {
        _state.update { s ->
            s.copy(exercises = s.exercises.updateExercise(exerciseIndex) { ex ->
                val template = ex.sets.lastOrNull { !it.isWarmup } ?: ex.sets.lastOrNull()
                ex.copy(
                    sets = normalizeSetNumbers(
                        ex.sets + EditableSet(
                            setNumber = 0,
                            weight = template?.weight ?: 0f,
                            reps = template?.reps
                                ?: (if (ex.exercise.targetRepsMax == 0) 0 else ex.exercise.targetRepsMin),
                            completed = false,
                            isWarmup = false,
                            rpe = null,
                        ),
                    ),
                )
            })
        }
    }

    fun addWarmupSet(exerciseIndex: Int) {
        _state.update { s ->
            s.copy(exercises = s.exercises.updateExercise(exerciseIndex) { ex ->
                // Warm-ups are inserted BEFORE the working sets, not appended.
                ex.copy(sets = insertWarmupSet(ex.sets))
            })
        }
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        _state.update { s ->
            s.copy(exercises = s.exercises.updateExercise(exerciseIndex) { ex ->
                val updated = ex.sets.toMutableList().also { if (setIndex in it.indices) it.removeAt(setIndex) }
                ex.copy(sets = normalizeSetNumbers(updated))
            })
        }
    }

    fun deleteSetForSwipe(exerciseIndex: Int, setIndex: Int): EditableSet? {
        val current = _state.value.exercises.getOrNull(exerciseIndex) ?: return null
        val deleted = current.sets.getOrNull(setIndex) ?: return null
        removeSet(exerciseIndex, setIndex)
        return deleted
    }

    fun undoDeleteSet(exerciseIndex: Int, insertIndex: Int, set: EditableSet) {
        _state.update { s ->
            s.copy(exercises = s.exercises.updateExercise(exerciseIndex) { ex ->
                val list = ex.sets.toMutableList()
                val idx = insertIndex.coerceIn(0, list.size)
                list.add(idx, set)
                ex.copy(sets = normalizeSetNumbers(list))
            })
        }
    }

    fun toggleExerciseExpanded(exerciseIndex: Int) {
        _state.update { s ->
            s.copy(exercises = s.exercises.mapIndexed { i, ex ->
                ex.copy(isExpanded = if (i == exerciseIndex) !ex.isExpanded else false)
            })
        }
    }

    fun updateExercises(newExercises: List<ExerciseWithSets>) {
        _state.update { it.copy(exercises = newExercises) }
    }

    fun persistExerciseOrder() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // First pass: shift the new indices by -10000 to avoid UNIQUE constraint collisions
            // with existing rows during the sequential SQLite update process.
            val tempExercises = _state.value.exercises.mapIndexed { index, ex ->
                ex.exercise.copy(orderIndex = index - 10000)
            }
            workoutRepository.updateExercises(tempExercises)

            // Second pass: normalize back to 0-based indices
            val finalExercises = _state.value.exercises.mapIndexed { index, ex ->
                ex.exercise.copy(orderIndex = index)
            }
            workoutRepository.updateExercises(finalExercises)
        }
    }

    fun skipRestTimer() {
        // NOTE: don't finalize the pending rest here — dismissing the countdown UI
        // doesn't mean the rest is over. The true rest is captured when the next set
        // is completed (or the workout finishes), so it reflects wall-clock time even
        // when the timer runs out or is dismissed early.
        restJob?.cancel()
        restEndEpochMs = null
        _state.update { it.copy(restTimerSeconds = null, restTimerTotal = 0, restTimerPaused = true) }
        viewModelScope.launch { restRuntimeStore.setRestState(restEndEpochMs = null, paused = true) }
        ActiveWorkoutService.clearRestTimer(context)
    }

    /**
     * Writes the wall-clock rest actually taken into the set that started the current
     * rest window (located by stable uid, so list mutations can't mis-attribute it),
     * then clears the attribution. No-ops when the set no longer exists or was
     * un-completed meanwhile.
     */
    private fun finalizePendingRest() {
        val pending = pendingRest ?: return
        pendingRest = null
        val elapsed = RestTimerMath.elapsedRestSeconds(pending.startedAtEpochMs, System.currentTimeMillis())
        _state.update { s ->
            val exerciseIndex = s.exercises.indexOfFirst { it.exercise.id == pending.exerciseId }
            if (exerciseIndex == -1) return@update s
            val setIndex = s.exercises[exerciseIndex].sets.indexOfFirst { it.uid == pending.setUid }
            if (setIndex == -1) return@update s
            val set = s.exercises[exerciseIndex].sets[setIndex]
            if (!set.completed) return@update s
            s.copy(exercises = s.exercises.updateExercise(exerciseIndex) { ex ->
                ex.copy(sets = ex.sets.updateAt(setIndex) { it.copy(restSeconds = elapsed) })
            })
        }
    }

    fun adjustRestTimerSeconds(deltaSeconds: Int) {
        val now = System.currentTimeMillis()
        val end = restEndEpochMs
        if (end != null && !_state.value.restTimerPaused) {
            // Running countdown: move the END timestamp by exactly ±delta so the
            // displayed remaining time shifts by exactly that many seconds.
            val newEnd = RestTimerMath.adjustedEndEpochMs(end, now, deltaSeconds)
            restEndEpochMs = newEnd
            val remaining = RestTimerMath.remainingSeconds(newEnd, now)
            _state.update {
                it.copy(
                    restTimerSeconds = remaining,
                    restTimerTotal = maxOf(it.restTimerTotal, remaining),
                )
            }
            viewModelScope.launch { restRuntimeStore.setRestState(restEndEpochMs = newEnd, paused = false) }
            ActiveWorkoutService.updateRestTimer(context, newEnd, paused = false)
        } else {
            // Paused / ready state: adjust the displayed duration only.
            _state.update { s ->
                val current = s.restTimerSeconds ?: return@update s
                val updated = (current + deltaSeconds).coerceAtLeast(0)
                s.copy(restTimerSeconds = updated, restTimerTotal = maxOf(updated, s.restTimerTotal))
            }
        }
    }

    fun startTimerIfPaused() {
        if (!_state.value.restTimerPaused) return
        if (_state.value.timerStatusLabel == "DONE") return
        if (_state.value.isDay5TimedProtocol) {
            startDay5TimerIfPaused()
            return
        }
        val remaining = _state.value.restTimerSeconds?.takeIf { it > 0 } ?: return
        val displayTotal = maxOf(_state.value.restTimerTotal, remaining)
        startRestTimer(remaining, displayTotal)
    }

    fun pauseTimer() {
        if (_state.value.restTimerPaused) return
        if (_state.value.timerStatusLabel == "DONE") return
        val remaining = restEndEpochMs?.let { RestTimerMath.remainingSeconds(it, System.currentTimeMillis()) }
            ?: _state.value.restTimerSeconds
            ?: return
        if (remaining <= 0) return
        restJob?.cancel()
        restEndEpochMs = null
        _state.update {
            it.copy(
                restTimerSeconds = remaining,
                restTimerPaused = true,
                timerStatusLabel = if (it.isDay5TimedProtocol) "READY" else it.timerStatusLabel,
            )
        }
        viewModelScope.launch { restRuntimeStore.setRestState(restEndEpochMs = null, paused = true) }
        ActiveWorkoutService.clearRestTimer(context)
    }

    private fun startDay5TimerIfPaused() {
        if (day5Intervals.isEmpty()) return
        if (day5Index >= day5Intervals.size) return
        val interval = day5Intervals[day5Index]
        val remaining = _state.value.restTimerSeconds?.takeIf { it > 0 } ?: interval.durationSeconds
        val displayTotal = maxOf(_state.value.restTimerTotal, remaining, interval.durationSeconds)
        applyDay5Focus(interval, paused = false)
        startRestTimer(remaining, displayTotal)
    }

    fun finishWorkout() {
        // Close the open rest window so the final set's rest is captured too.
        finalizePendingRest()
        val anyCompleted =
            _state.value.exercises.any { ex -> ex.sets.any { it.completed && !it.isWarmup } } ||
                _state.value.circuitRoundsCompleted > 0
        if (!anyCompleted) {
            _state.update { it.copy(showNoSetsWarning = true) }
            return
        }
        val summary = computeSummary(prExerciseIds = emptySet())
        _state.update { it.copy(isFinishing = true, summary = summary) }
        viewModelScope.launch {
            val prs = runCatching { detectPRs() }.getOrDefault(emptySet())
            _state.update { s ->
                val existing = s.summary
                if (existing == null) s else s.copy(summary = existing.copy(prExerciseIds = prs))
            }
        }
    }

    fun dismissNoSetsWarning() {
        _state.update { it.copy(showNoSetsWarning = false) }
    }

    fun dismissFinish() {
        _state.update { it.copy(isFinishing = false) }
    }

    fun saveWorkout(notes: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            val day = _state.value.workoutDay ?: return@launch
            val editing = editingSession
            val start = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(startedAtEpochMillis),
                ZoneId.systemDefault(),
            )
            val end = LocalDateTime.now()

            // Editing an existing session: re-inserting with the same id REPLACEs the old
            // row, and the old ExerciseLogs/SetLogs cascade-delete — the session is
            // rewritten from the current editor state while keeping its original date.
            val sessionId = workoutRepository.createSession(
                WorkoutSession(
                    id = editing?.id ?: 0L,
                    workoutDayId = day.id,
                    date = editing?.date ?: LocalDate.now(),
                    startTime = start,
                    endTime = end,
                    notes = notes ?: editing?.notes,
                    completed = true,
                ),
            )

            _state.value.exercises.forEachIndexed { exIndex, ex ->
                val exerciseLogId = workoutRepository.createExerciseLog(
                    ExerciseLog(
                        sessionId = sessionId,
                        exerciseId = ex.exercise.id,
                        orderIndex = exIndex,
                    ),
                )
                ex.sets.forEach { set ->
                    workoutRepository.createSet(
                        SetLog(
                            exerciseLogId = exerciseLogId,
                            setNumber = set.setNumber,
                            weight = set.weight,
                            reps = set.reps,
                            completed = set.completed,
                            isWarmup = set.isWarmup,
                            rpe = set.rpe,
                            restSeconds = set.restSeconds,
                        ),
                    )
                }
            }

            SyncWorker.syncNow(context)
            onDone()
        }
    }

    fun toggleRestTimerVisibility() {
        _state.update { it.copy(restTimerVisible = !it.restTimerVisible) }
    }

    fun adjustRestTimer(delta: Int) {
        adjustRestTimerSeconds(delta)
    }

    fun resetRestTimer() {
        // Keep the pending rest open (see skipRestTimer) — resetting the countdown is a
        // display action, not the end of the rest.
        if (_state.value.isDay5TimedProtocol) {
            day5Generation++
            day5AdvancedEndMs = null
            if (day5Index >= day5Intervals.size) day5Index = 0
            val interval = day5Intervals.getOrNull(day5Index) ?: return
            pauseDay5At(interval)
            return
        }
        restJob?.cancel()
        restEndEpochMs = null
        _state.update { it.copy(restTimerSeconds = it.restTimerTotal, restTimerPaused = true) }
        viewModelScope.launch { restRuntimeStore.setRestState(restEndEpochMs = null, paused = true) }
        ActiveWorkoutService.clearRestTimer(context)
    }

    private fun startRestTimer(seconds: Int, totalSeconds: Int = seconds) {
        val end = System.currentTimeMillis() + seconds * 1000L
        restEndEpochMs = end
        _state.update {
            it.copy(
                restTimerSeconds = seconds,
                restTimerTotal = maxOf(totalSeconds, seconds),
                restTimerVisible = true,
                restTimerPaused = false,
            )
        }
        viewModelScope.launch { restRuntimeStore.setRestState(restEndEpochMs = end, paused = false) }
        ActiveWorkoutService.updateRestTimer(context, end, paused = false)
        startRestTicker()
    }

    /**
     * Recomputes the remaining time from [restEndEpochMs] on every tick. Because the
     * display derives from the end timestamp (not a decrementing counter), the clock
     * stays exact across ±15s adjustments, backgrounding, and process resume.
     */
    private fun startRestTicker() {
        restJob?.cancel()
        restJob = viewModelScope.launch {
            while (true) {
                val end = restEndEpochMs ?: return@launch
                val remaining = RestTimerMath.remainingSeconds(end, System.currentTimeMillis())
                _state.update { it.copy(restTimerSeconds = remaining) }
                if (remaining > 0) {
                    delay(250)
                    continue
                }

                // Hold 0 for a second so the long end-beep can finish, then either
                // chain the next Day 5 interval or reset to READY like other days.
                delay(1_000)
                if (restEndEpochMs != end) return@launch

                if (_state.value.isDay5TimedProtocol) {
                    if (day5AdvancedEndMs == end) {
                        delay(250)
                        continue
                    }
                    day5AdvancedEndMs = end
                    val gen = day5Generation
                    armNextDay5Interval()
                    if (gen != day5Generation) return@launch
                    if (restEndEpochMs == null) return@launch
                    continue
                }

                restEndEpochMs = null
                _state.update { it.copy(restTimerSeconds = it.restTimerTotal, restTimerPaused = true) }
                restRuntimeStore.setRestState(restEndEpochMs = null, paused = true)
                ActiveWorkoutService.clearRestTimer(context)
                return@launch
            }
        }
    }

    private fun armDay5Protocol(sections: List<String?>, circuitRestSeconds: Int) {
        day5Intervals = Day5CircuitProtocol.buildIntervals(sections, circuitRestSeconds)
        day5Index = 0
        day5Generation++
        day5AdvancedEndMs = null
        val first = day5Intervals.firstOrNull() ?: return
        pauseDay5At(first)
    }

    private fun pauseDay5At(interval: Day5CircuitProtocol.Interval) {
        restJob?.cancel()
        restEndEpochMs = null
        applyDay5Focus(interval, paused = true)
        _state.update {
            it.copy(
                restTimerSeconds = interval.durationSeconds,
                restTimerTotal = interval.durationSeconds,
                restTimerVisible = true,
                restTimerPaused = true,
                timerStatusLabel = "READY",
            )
        }
        viewModelScope.launch { restRuntimeStore.setRestState(restEndEpochMs = null, paused = true) }
        ActiveWorkoutService.clearRestTimer(context)
    }

    private fun applyDay5Focus(interval: Day5CircuitProtocol.Interval, paused: Boolean) {
        val label = when {
            paused -> "READY"
            else -> "PAUSE"
        }
        _state.update { s ->
            s.copy(
                timerStatusLabel = label,
                day5ActiveExerciseIndex = interval.exerciseIndex,
                day5ActiveSetIndex = interval.setIndex,
                exercises = s.exercises.mapIndexed { i, ex ->
                    ex.copy(isExpanded = i == interval.exerciseIndex)
                },
            )
        }
    }

    private fun armNextDay5Interval() {
        val finished = day5Intervals.getOrNull(day5Index)
        completeCurrentDay5Interval()
        val wait = finished?.waitForPressAfter == true
        day5Index++
        val next = day5Intervals.getOrNull(day5Index)
        if (next == null) {
            restEndEpochMs = null
            _state.update {
                it.copy(
                    restTimerSeconds = 0,
                    restTimerPaused = true,
                    timerStatusLabel = "DONE",
                    day5ActiveExerciseIndex = null,
                    day5ActiveSetIndex = null,
                )
            }
            viewModelScope.launch { restRuntimeStore.setRestState(restEndEpochMs = null, paused = true) }
            ActiveWorkoutService.clearRestTimer(context)
            return
        }
        if (wait) {
            pauseDay5At(next)
            return
        }
        applyDay5Focus(next, paused = false)
        val end = System.currentTimeMillis() + next.durationSeconds * 1000L
        restEndEpochMs = end
        _state.update {
            it.copy(
                restTimerSeconds = next.durationSeconds,
                restTimerTotal = next.durationSeconds,
                restTimerVisible = true,
                restTimerPaused = false,
            )
        }
        viewModelScope.launch { restRuntimeStore.setRestState(restEndEpochMs = end, paused = false) }
        ActiveWorkoutService.updateRestTimer(context, end, paused = false)
    }

    private fun completeCurrentDay5Interval() {
        val current = day5Intervals.getOrNull(day5Index) ?: return
        if (current.phase != Day5CircuitProtocol.Phase.WORK) return
        markDay5SetCompleted(current.exerciseIndex, current.setIndex)
        val isCircuit = _state.value.exercises.getOrNull(current.exerciseIndex)
            ?.exercise?.section.equals(Day5CircuitProtocol.SECTION_CIRCUIT, ignoreCase = true)
        if (isCircuit && current.setIndex == Day5CircuitProtocol.CIRCUIT_SETS - 1) {
            _state.update { it.copy(circuitRoundsCompleted = it.circuitRoundsCompleted + 1) }
        }
    }

    private fun markDay5SetCompleted(exerciseIndex: Int, workingSetIndex: Int) {
        _state.update { s ->
            val ex = s.exercises.getOrNull(exerciseIndex) ?: return@update s
            val setIndex = workingSetListIndex(ex, workingSetIndex)
            if (setIndex < 0) return@update s
            val set = ex.sets[setIndex]
            if (set.completed) return@update s
            s.copy(
                exercises = s.exercises.updateExercise(exerciseIndex) { e ->
                    e.copy(sets = e.sets.updateAt(setIndex) { it.copy(completed = true) })
                },
            )
        }
    }

    private fun workingSetListIndex(ex: ExerciseWithSets, workingSetIndex: Int): Int {
        var seen = -1
        ex.sets.forEachIndexed { i, set ->
            if (!set.isWarmup) {
                seen++
                if (seen == workingSetIndex) return i
            }
        }
        return -1
    }

    private fun computeSummary(prExerciseIds: Set<Int>): WorkoutSummary {
        val s = _state.value
        val totalExercises = s.exercises.size
        val completedExercises =
            s.exercises.count { ex -> ex.sets.any { it.completed && !it.isWarmup } } +
                (if (s.circuitRoundsCompleted > 0) 1 else 0)
        val volume = s.exercises.sumOf { ex ->
            ex.sets.filter { it.completed && !it.isWarmup }.sumOf { (it.weight * it.reps).toDouble() }
        }.toFloat()

        return WorkoutSummary(
            durationSeconds = s.elapsedSeconds,
            totalVolume = volume,
            exercisesCompleted = completedExercises,
            exercisesTotal = totalExercises,
            prExerciseIds = prExerciseIds,
        )
    }

    private suspend fun detectPRs(): Set<Int> {
        val prs = mutableSetOf<Int>()
        _state.value.exercises.forEach { ex ->
            val currentBest = bestSet(ex.sets.filter { it.completed && !it.isWarmup }) ?: return@forEach
            val historical = workoutRepository.allCompletedSetsForExercise(ex.exercise.id)
            val historicalBest = bestSet(historical.filter { it.completed && !it.isWarmup }.map {
                EditableSet(
                    setNumber = it.setNumber,
                    weight = it.weight,
                    reps = it.reps,
                    completed = it.completed,
                    isWarmup = it.isWarmup,
                    rpe = it.rpe,
                )
            })
            if (historicalBest == null || compareBest(currentBest, historicalBest) > 0) {
                prs += ex.exercise.id
            }
        }
        return prs
    }

    private fun bestSet(sets: List<EditableSet>): EditableSet? {
        return sets.maxWithOrNull { a, b -> compareBest(a, b) }
    }

    private fun compareBest(a: EditableSet, b: EditableSet): Int {
        val w = a.weight.compareTo(b.weight)
        if (w != 0) return w
        return a.reps.compareTo(b.reps)
    }

    private fun extractLastWorkingTopSet(latest: List<SetLog>): Pair<Float, Int> {
        val working = latest.filter { it.completed && !it.isWarmup }
        if (working.isEmpty()) return 0f to 0

        val topWeight = working.maxOf { it.weight }
        val atTop = working.filter { it.weight == topWeight }
        val lastAttempt = atTop.maxByOrNull { it.setNumber } ?: atTop.last()
        return topWeight to lastAttempt.reps
    }
}

private inline fun <T> List<T>.updateAt(index: Int, transform: (T) -> T): List<T> {
    if (index !in indices) return this
    return mapIndexed { i, item -> if (i == index) transform(item) else item }
}

private inline fun List<ExerciseWithSets>.updateExercise(index: Int, transform: (ExerciseWithSets) -> ExerciseWithSets): List<ExerciseWithSets> {
    if (index !in indices) return this
    return mapIndexed { i, item -> if (i == index) transform(item) else item }
}
