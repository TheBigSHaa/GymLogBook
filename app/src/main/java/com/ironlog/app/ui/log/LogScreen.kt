package com.ironlog.app.ui.log

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ironlog.app.ui.components.DeleteWorkoutDialog
import com.ironlog.app.ui.components.WorkoutLogCard
import com.ironlog.app.ui.components.foundation.EmptyState
import com.ironlog.app.ui.components.foundation.ProYouTopBar
import com.ironlog.app.ui.components.foundation.StaggeredEntrance
import com.ironlog.app.ui.components.foundation.pressScale
import com.ironlog.app.ui.theme.Dimens
import com.ironlog.app.ui.theme.MotionTokens
import com.ironlog.app.ui.theme.dayColorForId

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogScreen(
    modifier: Modifier = Modifier,
    viewModel: LogViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val workoutDays by viewModel.workoutDays.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedDayFilter.collectAsStateWithLifecycle()

    var expandedSessionId by remember { mutableStateOf<Long?>(null) }
    var sessionToDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ProYouTopBar(overline = "History", title = "Activity Log")
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = Dimens.ScreenPaddingH,
                end = Dimens.ScreenPaddingH,
                top = 8.dp,
                bottom = Dimens.ScreenPaddingBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            // ─── Filter chips ───
            item(key = "filters") {
                StaggeredEntrance(index = 0) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            LogFilterChip(
                                label = "ALL",
                                selected = selectedFilter == null,
                                accent = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp,
                                onClick = { viewModel.setDayFilter(null) },
                            )
                        }
                        items(workoutDays, key = { it.id }) { day ->
                            LogFilterChip(
                                label = day.name.uppercase(),
                                selected = selectedFilter == day.id,
                                accent = dayColorForId(day.id),
                                letterSpacing = 1.sp,
                                onClick = { viewModel.setDayFilter(day.id) },
                            )
                        }
                    }
                }
            }

            // ─── Session cards or empty state ───
            if (sessions.isEmpty()) {
                item(key = "empty") {
                    StaggeredEntrance(index = 1) {
                        EmptyState(
                            icon = Icons.Outlined.FitnessCenter,
                            title = "No workouts logged yet",
                            message = "Finish your first session and it will land here.\nTime to hit the gym!",
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = sessions,
                    key = { _, item -> item.session.id },
                ) { index, sessionWithDetails ->
                    val isExpanded = expandedSessionId == sessionWithDetails.session.id
                    StaggeredEntrance(
                        // Cap the stagger so items deep in the list don't lag behind.
                        index = (index + 1).coerceAtMost(6),
                        modifier = Modifier.animateItemPlacement(),
                    ) {
                        WorkoutLogCard(
                            session = sessionWithDetails,
                            accentColor = dayColorForId(sessionWithDetails.workoutDay.id),
                            isExpanded = isExpanded,
                            onClick = {
                                expandedSessionId =
                                    if (isExpanded) null else sessionWithDetails.session.id
                            },
                            onLongPress = {
                                sessionToDelete = sessionWithDetails.session.id
                            },
                        )
                    }
                }
            }
        }
    }

    sessionToDelete?.let { id ->
        DeleteWorkoutDialog(
            onConfirm = {
                viewModel.deleteSession(id)
                if (expandedSessionId == id) expandedSessionId = null
                sessionToDelete = null
            },
            onDismiss = { sessionToDelete = null },
        )
    }
}

/**
 * Pill filter chip: selected state fills with the contextual accent color;
 * container and label colors cross-fade with the standard motion spec.
 */
@Composable
private fun LogFilterChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    letterSpacing: TextUnit = 1.sp,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = MotionTokens.standardSpec(),
        label = "chipContainer",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = MotionTokens.standardSpec(),
        label = "chipLabel",
    )
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.pressScale(),
        shape = RoundedCornerShape(50),
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = containerColor,
            labelColor = labelColor,
            selectedContainerColor = containerColor,
            selectedLabelColor = labelColor,
        ),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = letterSpacing,
                ),
            )
        },
    )
}
