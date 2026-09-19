package com.ironlog.app.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.burnoutcrew.reorderable.reorderable
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import com.ironlog.app.data.model.RepUnit
import com.ironlog.app.util.Day5CircuitProtocol
import com.ironlog.app.ui.components.DeleteSetDialog
import com.ironlog.app.ui.components.ExerciseCard
import com.ironlog.app.ui.components.RestTimerFloating
import com.ironlog.app.ui.components.SetCard
import com.ironlog.app.ui.components.foundation.AccentButton
import com.ironlog.app.ui.components.foundation.ProYouTopBar
import com.ironlog.app.ui.components.foundation.SectionHeader
import com.ironlog.app.ui.components.foundation.StaggeredEntrance
import com.ironlog.app.ui.components.foundation.SurfaceCard
import com.ironlog.app.ui.components.foundation.TopBarIconButton
import com.ironlog.app.ui.components.foundation.accentGradient
import com.ironlog.app.ui.theme.Dimens

@Composable
fun ActiveWorkoutScreen(
    onNavigateBackToToday: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val screenTimeout by viewModel.workoutScreenTimeout.collectAsStateWithLifecycle()
    var localExercises by remember { mutableStateOf(state.exercises) }
    val workoutDay = state.workoutDay
    val accent = workoutDay?.let { parseColorHexOrNull(it.colorHex) } ?: MaterialTheme.colorScheme.primary
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                focusManager.clearFocus()
                return Offset.Zero
            }
        }
    }

    var showEndConfirm by remember { mutableStateOf(false) }
    // Triple = (exerciseIndex, setIndex, EditableSet snapshot for display)
    var setToDelete by remember { mutableStateOf<Triple<Int, Int, EditableSet>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Derive hero exercise from first expanded exercise
    val heroExerciseName = remember(state.exercises) {
        state.exercises.firstOrNull { it.isExpanded }?.exercise?.name
            ?: state.exercises.firstOrNull()?.exercise?.name
            ?: workoutDay?.name
            ?: "Workout"
    }

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            val fromKey = from.key as? String
            val toKey = to.key as? String
            if (fromKey?.startsWith("exercise_") == true && toKey?.startsWith("exercise_") == true) {
                val fromId = fromKey.removePrefix("exercise_").toIntOrNull()
                val toId = toKey.removePrefix("exercise_").toIntOrNull()
                if (fromId != null && toId != null) {
                    val fromIdx = localExercises.indexOfFirst { it.exercise.id == fromId }
                    val toIdx = localExercises.indexOfFirst { it.exercise.id == toId }
                    if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
                        localExercises = localExercises.toMutableList().apply {
                            add(toIdx, removeAt(fromIdx))
                        }
                    }
                }
            }
        },
        canDragOver = { draggedOver, _ ->
            draggedOver.key is String && (draggedOver.key as String).startsWith("exercise_")
        },
        onDragEnd = { _, _ ->
            viewModel.updateExercises(localExercises)
            viewModel.persistExerciseOrder()
        }
    )

    LaunchedEffect(state.exercises) {
        if (reorderState.draggingItemKey == null) {
            localExercises = state.exercises
        }
    }
    val displayedExercises =
        if (reorderState.draggingItemKey == null && state.exercises.isNotEmpty()) {
            state.exercises
        } else {
            localExercises
        }

    BackHandler { showEndConfirm = true }

    // Back confirmation dialog
    if (showEndConfirm) {
        AlertDialog(
            onDismissRequest = { showEndConfirm = false },
            shape = RoundedCornerShape(Dimens.RadiusL),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("End workout?") },
            text = { Text("End workout without saving?") },
            confirmButton = {
                TextButton(onClick = {
                    showEndConfirm = false
                    onNavigateBackToToday()
                }) { Text("End", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirm = false }) {
                    Text("Keep going", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    // No sets warning
    if (state.showNoSetsWarning) {
        AlertDialog(
            onDismissRequest = viewModel::dismissNoSetsWarning,
            shape = RoundedCornerShape(Dimens.RadiusL),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text("Nothing to save") },
            text = { Text("Complete at least one working set before saving a workout.") },
            confirmButton = { Button(onClick = viewModel::dismissNoSetsWarning) { Text("OK") } },
        )
    }

    // Finish dialog
    val summary = state.summary
    if (state.isFinishing && summary != null) {
        LaunchedEffect(summary.prExerciseIds) {
            if (summary.prExerciseIds.isNotEmpty()) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
        FinishDialog(
            summary = summary,
            isEditing = state.isEditingExistingSession,
            onDismiss = viewModel::dismissFinish,
            onSave = { notes ->
                viewModel.saveWorkout(notes) { onNavigateBackToToday() }
            },
        )
    }

    // Delete set confirmation
    setToDelete?.let { (exIdx, setIdx, _) ->
        DeleteSetDialog(
            onConfirm = {
                viewModel.removeSet(exIdx, setIdx)
                setToDelete = null
            },
            onDismiss = { setToDelete = null },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ProYouTopBar(
                overline = if (state.isEditingExistingSession) "Editing Session" else "Active Session",
                title = workoutDay?.name ?: "Workout",
                navigationIcon = {
                    TopBarIconButton(
                        icon = Icons.Outlined.ArrowBack,
                        contentDescription = "Back",
                        onClick = { showEndConfirm = true },
                        tint = accent,
                    )
                },
                actions = {
                    ElapsedChip(elapsedSeconds = state.elapsedSeconds, accent = accent)
                },
            )
        },
        modifier = modifier
            .fillMaxSize()
            .workoutKeepScreenOn(screenTimeout),
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
        ) {
            LazyColumn(
                state = reorderState.listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
                    .reorderable(reorderState),
                contentPadding = PaddingValues(
                    start = Dimens.ScreenPaddingH,
                    end = Dimens.ScreenPaddingH,
                    top = 8.dp,
                    // Extra clearance while the rest pill is on screen so it never covers the
                    // Finish button; smaller when there's no floating timer.
                    bottom = if (state.restTimerSeconds != null) 260.dp else 48.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                // Hero header
                item {
                    StaggeredEntrance(index = 0) {
                        ExerciseHeroHeader(
                            dayName = workoutDay?.name ?: "",
                            exerciseName = heroExerciseName,
                            accentColor = accent,
                        )
                    }
                }

                // Exercise list with section headers
                var lastSection: String? = null
                displayedExercises.forEachIndexed { exIndex, ex ->
                    val section = ex.exercise.section
                    if (!section.isNullOrBlank() && section != lastSection) {
                        // Key by the exercise the header precedes — a reorder that splits a
                        // section into two runs would otherwise emit a duplicate LazyColumn key.
                        item(key = "section_${section}_${ex.exercise.id}") {
                            WorkoutSectionBlock(section = section)
                        }
                    }

                    item(key = "exercise_${ex.exercise.id}") {
                        ReorderableItem(reorderState, key = "exercise_${ex.exercise.id}") { isDragging ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .detectReorderAfterLongPress(reorderState)
                            ) {
                                ExerciseCard(
                                    exercise = ex.exercise,
                                    isExpanded = ex.isExpanded,
                                    exerciseCompleted = ex.isExerciseCompletedForUi(),
                                    headerRightText = targetText(ex.exercise),
                                    suggestionLine = ex.lastLine,
                                    onToggleExpanded = { viewModel.toggleExerciseExpanded(exIndex) },
                                ) {
                                    val isDay5 = state.isDay5TimedProtocol
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ex.sets.forEachIndexed { setIndex, set ->
                                            val weightStr = if (set.weight == 0f) "" else {
                                                // Show as integer when no decimal part
                                                if (set.weight == set.weight.toLong().toFloat()) {
                                                    set.weight.toLong().toString()
                                                } else {
                                                    set.weight.toString()
                                                }
                                            }
                                            // Warm-ups lead the list; working sets display their
                                            // position among working sets only (S1, S2, ...).
                                            val displaySetNumber = ex.sets.take(setIndex + 1).count { !it.isWarmup }
                                            // What was accomplished LAST TIME for this same set.
                                            val previous = if (set.isWarmup || isDay5) {
                                                null
                                            } else {
                                                ex.previousSets.getOrNull(displaySetNumber - 1)
                                            }
                                            val workingSetIndex = if (set.isWarmup) -1 else displaySetNumber - 1
                                            SetCard(
                                                setNumber = displaySetNumber,
                                                weight = weightStr,
                                                reps = set.reps,
                                                completed = set.completed,
                                                isWarmup = set.isWarmup,
                                                accentColor = accent,
                                                equipmentType = ex.exercise.equipmentType,
                                                availableDbWeights = state.availableDbWeights,
                                                previous = previous,
                                                hideLoadAndReps = isDay5,
                                                emphasized = isDay5 &&
                                                    state.day5ActiveExerciseIndex == exIndex &&
                                                    state.day5ActiveSetIndex == workingSetIndex,
                                                onWeightChange = { str ->
                                                    viewModel.updateSetWeight(exIndex, setIndex, str.toFloatOrNull() ?: 0f)
                                                },
                                                onRepsChange = { reps ->
                                                    viewModel.updateSetReps(exIndex, setIndex, reps)
                                                },
                                                onToggleComplete = {
                                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    viewModel.toggleSetCompleted(exIndex, setIndex)
                                                },
                                                onLongPress = {
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    setToDelete = Triple(exIndex, setIndex, set)
                                                },
                                            )
                                        }

                                        if (!isDay5) {
                                            // Add set buttons
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxWidth(),
                                            ) {
                                                OutlinedButton(
                                                    onClick = { viewModel.addWarmupSet(exIndex) },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(Dimens.RadiusM),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                                    ),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    ),
                                                ) { Text("+ Warmup", style = MaterialTheme.typography.labelMedium) }

                                                OutlinedButton(
                                                    onClick = { viewModel.addSet(exIndex) },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(Dimens.RadiusM),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                                    ),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    ),
                                                ) { Text("+ Add Set", style = MaterialTheme.typography.labelMedium) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    lastSection = section
                }

                // Finish workout button
                item {
                    Spacer(Modifier.height(8.dp))
                    AccentButton(
                        text = if (state.isEditingExistingSession) "Update Workout" else "Finish Workout",
                        onClick = { viewModel.finishWorkout() },
                        accent = accent,
                        height = 64.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Floating rest timer overlay
            val timerSeconds = state.restTimerSeconds
            if (timerSeconds != null) {
                RestTimerFloating(
                    visible = state.restTimerVisible,
                    remainingSeconds = timerSeconds,
                    totalSeconds = state.restTimerTotal,
                    paused = state.restTimerPaused,
                    accentColor = accent,
                    statusLabel = state.timerStatusLabel,
                    onAdjust = { delta -> viewModel.adjustRestTimerSeconds(delta) },
                    onToggleVisibility = { viewModel.toggleRestTimerVisibility() },
                    onReset = { viewModel.resetRestTimer() },
                    onStart = { viewModel.startTimerIfPaused() },
                    onPause = { viewModel.pauseTimer() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp),
                )
            }
        }
    }
}

/** Live session clock pill in the top bar. */
@Composable
private fun ElapsedChip(elapsedSeconds: Long, accent: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusM))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Timer,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = formatElapsed(elapsedSeconds),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ExerciseHeroHeader(
    dayName: String,
    exerciseName: String,
    accentColor: Color,
) {
    SurfaceCard(
        radius = Dimens.RadiusXL,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .background(accentGradient(accentColor))
                .padding(Dimens.CardPaddingLarge),
            contentAlignment = Alignment.BottomStart,
        ) {
            Column {
                if (dayName.isNotBlank()) {
                    Text(
                        text = dayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp,
                        ),
                        color = accentColor,
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun FinishDialog(
    summary: WorkoutSummary,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
) {
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Dimens.RadiusL),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(if (isEditing) "Update workout" else "Finish workout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Duration: ${formatElapsed(summary.durationSeconds)}")
                Text("Total volume: ${summary.totalVolume.toInt()} kg·reps")
                Text("Exercises: ${summary.exercisesCompleted}/${summary.exercisesTotal}")
                if (summary.prExerciseIds.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            text = "New PRs: ${summary.prExerciseIds.size}",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    shape = RoundedCornerShape(Dimens.RadiusM),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(notes.ifBlank { null }) }) {
                Text(if (isEditing) "Update" else "Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun targetText(ex: com.ironlog.app.data.model.Exercise): String {
    when (ex.section?.uppercase()) {
        "WARMUP", "COOLDOWN" -> {
            val seconds = ex.targetRepsMin.takeIf { it > 0 } ?: Day5CircuitProtocol.WARMUP_WORK_SECONDS
            return "${seconds}s"
        }
        "CIRCUIT" -> {
            val work = ex.circuitDurationSeconds ?: ex.targetRepsMin
            return "${ex.targetSets}×${work}s"
        }
    }
    val unit = if (ex.repUnit == RepUnit.SECONDS) "s" else ""
    val reps = when {
        ex.targetRepsMax == 0 -> "AMRAP"
        ex.targetRepsMin == ex.targetRepsMax -> "${ex.targetRepsMin}$unit"
        else -> "${ex.targetRepsMin}-${ex.targetRepsMax}$unit"
    }
    return "${ex.targetSets}×$reps"
}

@Composable
private fun WorkoutSectionBlock(
    section: String,
    modifier: Modifier = Modifier,
) {
    val title = when (section.uppercase()) {
        "WARMUP" -> "Warmup"
        "CORE" -> "Core — 3 rounds"
        "COOLDOWN" -> "Cooldown"
        "CIRCUIT" -> "Workout"
        else -> section
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeader(title = title)
    }
}

private fun formatElapsed(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

private fun parseColorHexOrNull(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: IllegalArgumentException) {
        null
    }
}

/** All working (non-warmup) sets marked complete. */
private fun ExerciseWithSets.isExerciseCompletedForUi(): Boolean {
    val working = sets.filter { !it.isWarmup }
    if (working.isEmpty()) return false
    return working.all { it.completed }
}
