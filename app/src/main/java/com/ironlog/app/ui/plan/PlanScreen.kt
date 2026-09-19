package com.ironlog.app.ui.plan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ironlog.app.ui.components.CalendarLegend
import com.ironlog.app.ui.components.MonthCalendar
import com.ironlog.app.ui.components.ScheduleBottomSheet
import com.ironlog.app.ui.components.foundation.AccentButton
import com.ironlog.app.ui.components.foundation.AnimatedStatText
import com.ironlog.app.ui.components.foundation.HairlineDivider
import com.ironlog.app.ui.components.foundation.ProYouTopBar
import com.ironlog.app.ui.components.foundation.StaggeredEntrance
import com.ironlog.app.ui.components.foundation.SurfaceCard
import com.ironlog.app.ui.components.foundation.TopBarIconButton
import com.ironlog.app.ui.components.foundation.accentGradient
import com.ironlog.app.ui.theme.Dimens
import com.ironlog.app.ui.theme.MotionTokens
import com.ironlog.app.ui.theme.dayColorForId
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    modifier: Modifier = Modifier,
    viewModel: PlanViewModel = hiltViewModel(),
    onStartWorkout: (Int) -> Unit = {},
    onEditSession: (workoutDayId: Int, sessionId: Long) -> Unit = { _, _ -> },
) {
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val scheduledWorkouts by viewModel.scheduledWorkouts.collectAsStateWithLifecycle()
    val completedDates by viewModel.completedDates.collectAsStateWithLifecycle()
    val firstDayOfWeek by viewModel.firstDayOfWeek.collectAsStateWithLifecycle()
    val pastWorkout by viewModel.pastWorkout.collectAsStateWithLifecycle()
    val swipeDistanceThresholdPx = with(LocalDensity.current) { 48.dp.toPx() }

    // Filter completed dates to current month for the stats pills
    val currentMonthCompleted = completedDates.filterKeys {
        it.month == currentMonth.month && it.year == currentMonth.year
    }

    // Consecutive-day streak ending today (or yesterday, so an unfinished
    // day doesn't zero it out mid-run).
    val streak = remember(completedDates) { computeCurrentStreak(completedDates.keys) }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var showPastSheet by remember { mutableStateOf(false) }
    var showAutofillPrompt by remember { mutableStateOf(false) }
    var autoFillStartDate by remember { mutableStateOf<LocalDate?>(null) }

    val sheetState = rememberModalBottomSheetState()
    val pastSheetState = rememberModalBottomSheetState()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ProYouTopBar(overline = "Plan", title = "Training Calendar")
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = Dimens.ScreenPaddingH, end = Dimens.ScreenPaddingH,
                top = 8.dp, bottom = Dimens.ScreenPaddingBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
        ) {
            // ─── Month header: ghost year + animated month + chevrons ───
            item {
                StaggeredEntrance(index = 0) {
                    MonthHeaderSection(
                        currentMonth = currentMonth,
                        onPreviousMonth = { viewModel.navigateMonth(-1) },
                        onNextMonth = { viewModel.navigateMonth(1) },
                    )
                }
            }

            // ─── Stat pills ───
            item {
                StaggeredEntrance(index = 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                    ) {
                        StatPill(
                            label = "Active Days",
                            value = "${currentMonthCompleted.size}",
                            suffix = "/${currentMonth.lengthOfMonth()}",
                            suffixColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        StatPill(
                            label = "Streak",
                            value = "$streak",
                            suffix = " days",
                            suffixColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ─── Calendar grid ───
            item {
                StaggeredEntrance(index = 2) {
                    SurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        radius = Dimens.RadiusXL,
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = Dimens.CardPadding,
                        ),
                    ) {
                        MonthCalendar(
                            yearMonth = currentMonth,
                            scheduledWorkouts = scheduledWorkouts,
                            completedDates = currentMonthCompleted,
                            onDateTap = { date ->
                                selectedDate = date
                                if (completedDates.containsKey(date)) {
                                    // A workout was performed that day — show it, with
                                    // the option to edit or continue.
                                    viewModel.loadPastWorkout(date)
                                    showPastSheet = true
                                } else {
                                    showSheet = true
                                }
                            },
                            firstDayOfWeek = firstDayOfWeek,
                            modifier = Modifier.pointerInput(currentMonth, swipeDistanceThresholdPx) {
                                detectMonthSwipe(
                                    currentMonth = currentMonth,
                                    onMonthChanged = viewModel::setMonth,
                                    swipeDistanceThresholdPx = swipeDistanceThresholdPx,
                                )
                            },
                        )
                    }
                }
            }

            // ─── Legend ───
            item {
                StaggeredEntrance(index = 3) {
                    CalendarLegend()
                }
            }

            // ─── Today's scheduled workout card (if any) ───
            val today = LocalDate.now()
            val todaySchedule = scheduledWorkouts[today]
            val todayDayId = todaySchedule?.workoutDayId
            if (todaySchedule != null && todayDayId != null && !todaySchedule.isRestDay) {
                item {
                    StaggeredEntrance(index = 4) {
                        TodayInsightCard(
                            workoutDayId = todayDayId,
                            onStartSession = { onStartWorkout(todayDayId) },
                        )
                    }
                }
            }
        }
    }

    // ─── Past workout sheet (view / edit / continue) ───
    val pw = pastWorkout
    if (showPastSheet && pw != null) {
        PastWorkoutSheet(
            workout = pw,
            onEditContinue = {
                showPastSheet = false
                viewModel.clearPastWorkout()
                onEditSession(pw.day.id, pw.session.id)
            },
            onAdjustSchedule = {
                showPastSheet = false
                viewModel.clearPastWorkout()
                showSheet = true
            },
            onDismiss = {
                showPastSheet = false
                viewModel.clearPastWorkout()
            },
            sheetState = pastSheetState,
        )
    }

    // ─── Schedule bottom sheet ───
    if (showSheet && selectedDate != null) {
        ScheduleBottomSheet(
            date = selectedDate!!,
            currentSchedule = scheduledWorkouts[selectedDate],
            onScheduleWorkout = { dayId ->
                viewModel.scheduleWorkout(selectedDate!!, dayId)
                if (dayId == 1) {
                    autoFillStartDate = selectedDate
                    showAutofillPrompt = true
                }
                showSheet = false
            },
            onScheduleRest = {
                viewModel.scheduleRestDay(selectedDate!!)
                showSheet = false
            },
            onRemoveSchedule = {
                viewModel.removeSchedule(selectedDate!!)
                showSheet = false
            },
            onAutoFillWeek = { /* handled via autofill dialog below */ },
            onDismiss = { showSheet = false },
            sheetState = sheetState,
        )
    }

    // ─── Auto-fill week prompt ───
    val fillDate = autoFillStartDate
    if (showAutofillPrompt && fillDate != null) {
        AlertDialog(
            onDismissRequest = { showAutofillPrompt = false },
            title = { Text("Auto-fill the week?") },
            text = { Text("Auto-fill Days 2–4 on the next 3 days + a rest day on the 5th day? (Calisthenics day is optional and not auto-scheduled.)") },
            confirmButton = {
                Button(onClick = {
                    showAutofillPrompt = false
                    viewModel.autoFillWeek(fillDate)
                }) { Text("Auto-fill") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAutofillPrompt = false }) { Text("No thanks") }
            },
        )
    }
}

/** Consecutive completed days counted back from today (or yesterday). */
private fun computeCurrentStreak(completed: Set<LocalDate>): Int {
    var date = LocalDate.now()
    if (date !in completed) date = date.minusDays(1)
    var streak = 0
    while (date in completed) {
        streak++
        date = date.minusDays(1)
    }
    return streak
}

private suspend fun PointerInputScope.detectMonthSwipe(
    currentMonth: YearMonth,
    onMonthChanged: (YearMonth) -> Unit,
    swipeDistanceThresholdPx: Float,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var totalDx = 0f

        // Wait until gesture becomes a horizontal drag (touch slop).
        var change = awaitHorizontalTouchSlopOrCancellation(down.id) { c, over ->
            c.consumePositionChange()
            totalDx += over
        } ?: return@awaitEachGesture

        // Consume the rest of the horizontal drag so parent pagers ignore it.
        while (!change.changedToUpIgnoreConsumed()) {
            val next = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            val dx = next.positionChange().x
            if (dx != 0f) {
                totalDx += dx
                next.consumePositionChange()
            }
            change = next
        }

        val shouldGoPrev = totalDx > swipeDistanceThresholdPx
        val shouldGoNext = totalDx < -swipeDistanceThresholdPx

        when {
            shouldGoPrev && !shouldGoNext -> onMonthChanged(currentMonth.minusMonths(1))
            shouldGoNext && !shouldGoPrev -> onMonthChanged(currentMonth.plusMonths(1))
            else -> Unit
        }
    }
}

@Composable
private fun MonthHeaderSection(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Ghost year watermark.
        Text(
            text = "${currentMonth.year}",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Month name slides toward the direction of navigation:
            // the previous month is the AnimatedContent initialState.
            AnimatedContent(
                targetState = currentMonth,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val forward = targetState > initialState
                    val enter = slideInHorizontally(MotionTokens.enterSpec()) { full ->
                        if (forward) full / 2 else -full / 2
                    } + fadeIn(tween(MotionTokens.DurationMedium))
                    val exit = slideOutHorizontally(MotionTokens.exitSpec()) { full ->
                        if (forward) -full / 2 else full / 2
                    } + fadeOut(tween(MotionTokens.DurationFast))
                    (enter togetherWith exit).using(SizeTransform(clip = false))
                },
                label = "monthName",
            ) { month ->
                Text(
                    text = month.month.name.uppercase(),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            TopBarIconButton(
                icon = Icons.Default.ChevronLeft,
                contentDescription = "Previous month",
                onClick = onPreviousMonth,
            )
            Spacer(Modifier.width(8.dp))
            TopBarIconButton(
                icon = Icons.Default.ChevronRight,
                contentDescription = "Next month",
                onClick = onNextMonth,
            )
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    suffix: String,
    suffixColor: Color,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(
        modifier = modifier,
        radius = Dimens.RadiusL,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 9.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedStatText(text = value)
            Text(
                text = suffix,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = suffixColor,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastWorkoutSheet(
    workout: PastWorkoutDetails,
    onEditContinue: () -> Unit,
    onAdjustSchedule: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState,
) {
    val accent = dayColorForId(workout.day.id)
    val formattedDate = workout.session.date
        .format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH))
        .uppercase()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = Dimens.RadiusXL, topEnd = Dimens.RadiusXL),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.CardPaddingLarge, vertical = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                    Text(
                        text = workout.day.name.uppercase(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = accent,
                    )
                }
            }

            SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                radius = Dimens.RadiusL,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentPadding = PaddingValues(Dimens.CardPadding),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    PastWorkoutStat(label = "DURATION", value = if (workout.durationMinutes > 0) "${workout.durationMinutes} min" else "—")
                    PastWorkoutStat(label = "VOLUME", value = "${workout.totalVolumeKg} kg")
                    PastWorkoutStat(label = "EXERCISES", value = "${workout.exercises.size}")
                }
            }

            HairlineDivider()

            workout.exercises.forEach { detail ->
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = Dimens.RadiusM,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = detail.exercise.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    val completed = detail.sets.filter { it.completed }
                    Text(
                        text = if (completed.isEmpty()) {
                            "No completed sets"
                        } else {
                            completed.joinToString("  ·  ") { set ->
                                val w = if (set.weight == set.weight.toLong().toFloat()) {
                                    set.weight.toLong().toString()
                                } else {
                                    set.weight.toString()
                                }
                                val prefix = if (set.isWarmup) "W " else ""
                                "$prefix${w}kg × ${set.reps}"
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!workout.session.notes.isNullOrBlank()) {
                Text(
                    text = workout.session.notes.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(4.dp))

            AccentButton(
                text = "Edit / Continue Workout",
                onClick = onEditContinue,
                modifier = Modifier.fillMaxWidth(),
                accent = accent,
            )

            OutlinedButton(
                onClick = onAdjustSchedule,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.RadiusM),
            ) {
                Text(
                    text = "ADJUST SCHEDULE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PastWorkoutStat(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 9.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TodayInsightCard(
    workoutDayId: Int,
    onStartSession: () -> Unit,
) {
    val dayNames = mapOf(
        1 to "Upper Power",
        2 to "Lower Power",
        3 to "Upper Hypertrophy",
        4 to "Lower Hypertrophy",
        5 to "Calisthenics",
    )
    val accentColor = dayColorForId(workoutDayId)
    val dayName = dayNames[workoutDayId] ?: ""

    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        radius = Dimens.RadiusXL,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(accentGradient(accentColor))
                .padding(Dimens.CardPaddingLarge),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "SCHEDULED TODAY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                ),
                color = accentColor,
            )
            Text(
                text = dayName,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            AccentButton(
                text = "Start Session",
                onClick = onStartSession,
                modifier = Modifier.fillMaxWidth(),
                accent = accentColor,
            )
        }
    }
}
