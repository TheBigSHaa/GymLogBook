package com.ironlog.app.ui.today

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ironlog.app.ui.components.StatTile
import com.ironlog.app.ui.components.WeeklyCalendarStrip
import com.ironlog.app.ui.components.WorkoutHeroCard
import com.ironlog.app.ui.components.foundation.ProYouTopBar
import com.ironlog.app.ui.components.foundation.StaggeredEntrance
import com.ironlog.app.ui.components.foundation.TopBarIconButton
import com.ironlog.app.ui.theme.Dimens
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TodayScreen(
    onStartWorkout: (workoutDayId: Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlan: () -> Unit = {}, // TODO(Agent 2): wire to PlanScreen nav later
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val stats by viewModel.weekStats.collectAsStateWithLifecycle()
    val schedule by viewModel.todaySchedule.collectAsStateWithLifecycle()
    val workoutDays by viewModel.workoutDays.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val monthSchedules by viewModel.monthSchedules.collectAsStateWithLifecycle()
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    val swipeDistanceThresholdPx = with(LocalDensity.current) { 48.dp.toPx() }
    val calendarListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val workoutDay = schedule?.workoutDayId?.let { id -> workoutDays.firstOrNull { it.id == id } }
    val accent = workoutDay?.let { parseColorHexOrNull(it.colorHex) } ?: MaterialTheme.colorScheme.primary

    val today = LocalDate.now()
    val scheduledDays =
        monthSchedules.mapNotNull { (date, sch) ->
            if (sch.workoutDayId != null) date to sch.workoutDayId else null
        }.toMap()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ProYouTopBar(
                overline = "ProYou",
                title = "Today",
                navigationIcon = {
                    TopBarIconButton(
                        icon = Icons.Outlined.Person,
                        contentDescription = "Open Settings",
                        onClick = onOpenSettings,
                    )
                },
                actions = {
                    TopBarIconButton(
                        icon = Icons.Outlined.CalendarToday,
                        contentDescription = "Go to today",
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = {
                            val target = LocalDate.now()
                            viewModel.selectDate(target)
                            scope.launch {
                                val index = (target.dayOfMonth - 1).coerceAtLeast(0)
                                calendarListState.animateCenterItem(index)
                            }
                        },
                    )
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            contentPadding =
                PaddingValues(
                    start = Dimens.ScreenPaddingH,
                    end = Dimens.ScreenPaddingH,
                    top = 12.dp,
                    bottom = Dimens.ScreenPaddingBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
        ) {
            item {
                StaggeredEntrance(index = 0) {
                    WeeklyCalendarStrip(
                        today = today,
                        selectedDate = selectedDate,
                        scheduledDays = scheduledDays,
                        yearMonth = currentMonth,
                        onDateSelected = { date ->
                            viewModel.selectDate(date)
                        },
                        listState = calendarListState,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .pointerInput(currentMonth, swipeDistanceThresholdPx) {
                                    detectMonthSwipe(
                                        currentMonth = currentMonth,
                                        onMonthChanged = viewModel::updateMonth,
                                        swipeDistanceThresholdPx = swipeDistanceThresholdPx,
                                    )
                                },
                    )
                }
            }

            item {
                StaggeredEntrance(index = 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                    ) {
                        StatTile(
                            modifier = Modifier.weight(1f),
                            label = "PHASE",
                            value = "${stats.programWeek}/12",
                            sublabel = "WEEK",
                        )
                        StatTile(
                            modifier = Modifier.weight(1f),
                            label = "GOAL",
                            value = "${stats.workoutsThisWeek}/${stats.weeklyTarget}",
                            sublabel = "WORKOUTS",
                        )
                        StatTile(
                            modifier = Modifier.weight(1f),
                            label = "STREAK",
                            value = "${stats.trainingDayStreak}",
                            sublabel = "DAYS",
                        )
                    }
                }
            }

            item {
                StaggeredEntrance(index = 2) {
                    WorkoutHeroCard(
                        dayNumber = workoutDay?.id ?: 0,
                        dayName = workoutDay?.name ?: "—",
                        accentColor = accent,
                        exercises = exercises,
                        durationEstimate = if (workoutDay?.id == 5) "~25 MIN" else "~${exercises.size * 10} MIN",
                        onStartWorkout = { schedule?.workoutDayId?.let(onStartWorkout) },
                        isRestDay = schedule?.isRestDay == true,
                        noWorkoutScheduled = schedule?.workoutDayId == null && schedule?.isRestDay != true,
                        onPickWorkout = viewModel::selectWorkoutDay,
                        onMarkRest = viewModel::markRestDay,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun parseColorHexOrNull(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: IllegalArgumentException) {
        null
    }
}

private suspend fun LazyListState.animateCenterItem(index: Int) {
    // Wait for first layout pass so viewport + items info exist.
    repeat(10) {
        if (layoutInfo.viewportSize.width > 0 && layoutInfo.totalItemsCount > 0) return@repeat
        delay(16)
    }

    // Ensure the item becomes visible (layoutInfo can only report visible items).
    val safeIndex = index.coerceIn(0, (layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
    animateScrollToItem(safeIndex)
    repeat(10) {
        val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == safeIndex }
        if (item != null && layoutInfo.viewportSize.width > 0) {
            val viewportCenter = layoutInfo.viewportStartOffset + (layoutInfo.viewportSize.width / 2)
            val itemCenter = item.offset + (item.size / 2)
            val delta = (itemCenter - viewportCenter).toFloat()
            if (delta != 0f) {
                animateScrollBy(delta)
            }
            return
        }
        delay(16)
    }
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
