package com.ironlog.app.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ironlog.app.data.model.Exercise
import com.ironlog.app.ui.components.OneRmCard
import com.ironlog.app.ui.components.PersonalRecordsSection
import com.ironlog.app.ui.components.charts.ConsistencyHeatmap
import com.ironlog.app.ui.components.charts.DonutChart
import com.ironlog.app.ui.components.charts.LabeledBarChart
import com.ironlog.app.ui.components.charts.LineAreaChart
import com.ironlog.app.ui.components.foundation.AnimatedStatText
import com.ironlog.app.ui.components.foundation.EmptyState
import com.ironlog.app.ui.components.foundation.HairlineDivider
import com.ironlog.app.ui.components.foundation.ProYouTopBar
import com.ironlog.app.ui.components.foundation.SectionHeader
import com.ironlog.app.ui.components.foundation.StaggeredEntrance
import com.ironlog.app.ui.components.foundation.SurfaceCard
import com.ironlog.app.ui.components.foundation.TopBarIconButton
import com.ironlog.app.ui.components.foundation.accentGradient
import com.ironlog.app.ui.theme.DayBonus
import com.ironlog.app.ui.theme.Dimens
import com.ironlog.app.ui.theme.SuccessGreen
import com.ironlog.app.ui.theme.dayColorForId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = hiltViewModel(),
    onOpenSettings: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val legacy by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ProYouTopBar(
                overline = "Analytics",
                title = "Performance",
                actions = {
                    TopBarIconButton(
                        icon = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        onClick = onOpenSettings,
                    )
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            // Quiet loading state — avoids flashing the empty state on every visit
            // while the (fast) query runs.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                )
            }
        } else if (state.hasNoData) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Dimens.ScreenPaddingH),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Outlined.BarChart,
                    title = if (state.loadFailed) "Couldn't load analytics" else "No analytics yet",
                    message = if (state.loadFailed) {
                        "Something went wrong loading your stats. Reopen this tab to try again."
                    } else {
                        "Complete a few workouts and your graphs will light up here."
                    },
                )
            }
        } else {
            val records = state.personalRecords
            val hasFeatured = state.featuredExerciseName != null

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = Dimens.ScreenPaddingH,
                    end = Dimens.ScreenPaddingH,
                    top = 8.dp,
                    bottom = Dimens.ScreenPaddingBottom,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
            ) {
                item(key = "overview") {
                    StaggeredEntrance(index = 0) { TrainingOverviewSection(state) }
                }
                if (hasFeatured) {
                    item(key = "featured") {
                        StaggeredEntrance(index = 1) { FeaturedLiftSection(state) }
                    }
                }
                item(key = "strength") {
                    StaggeredEntrance(index = 2) { StrengthTrendsSection(state) }
                }
                item(key = "weeklyVolume") {
                    StaggeredEntrance(index = 3) { WeeklyVolumeSection(state) }
                }
                item(key = "consistency") {
                    StaggeredEntrance(index = 4) { ConsistencySection(state) }
                }
                item(key = "split") {
                    StaggeredEntrance(index = 5) { TrainingSplitSection(state) }
                }
                item(key = "duration") {
                    StaggeredEntrance(index = 6) { SessionDurationSection(state) }
                }
                item(key = "explorer") {
                    StaggeredEntrance(index = 7) {
                        ExerciseExplorerSection(
                            exercises = legacy.allExercises,
                            selected = legacy.selectedExercise,
                            onSelect = viewModel::selectExercise,
                            progression = legacy.exerciseProgression,
                            rows = legacy.exerciseSessionRows,
                        )
                    }
                }
                if (!records.isNullOrEmpty()) {
                    item(key = "records") {
                        StaggeredEntrance(index = 8) {
                            Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
                                SectionHeader(title = "Personal records")
                                PersonalRecordsSection(records = records)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// 1. Training overview
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun TrainingOverviewSection(state: ProgressUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
        SectionHeader(title = "Training overview")
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
            StatCell(
                label = "Workouts",
                value = "${state.totalWorkouts}",
                sub = "completed",
                modifier = Modifier.weight(1f),
            )
            StatCell(
                label = "Volume",
                value = formatVolume(state.totalVolumeKg),
                sub = "kg lifted",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
            StatCell(
                label = "Avg session",
                value = if (state.avgDurationMinutes > 0) "${state.avgDurationMinutes}" else "—",
                sub = "minutes",
                modifier = Modifier.weight(1f),
            )
            StatCell(
                label = "Records",
                value = "${state.prCount}",
                sub = "personal bests",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    SurfaceCard(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        AnimatedStatText(text = value, color = valueColor)
        Text(
            text = sub,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// 2. Featured lift (hero)
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun FeaturedLiftSection(state: ProgressUiState) {
    val accent = state.featuredAccentColor ?: MaterialTheme.colorScheme.primary
    val weekly = state.featuredWeeklyData ?: emptyList()

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
        SectionHeader(title = "Featured lift")
        SurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            radius = Dimens.RadiusXL,
            contentPadding = PaddingValues(0.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentGradient(accent)),
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.CardPaddingLarge),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1f)) {
                            val dayLabel = state.featuredDayLabel.orEmpty()
                            if (dayLabel.isNotBlank()) {
                                Text(
                                    text = dayLabel.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.5.sp,
                                    ),
                                    color = accent,
                                )
                                Spacer(Modifier.height(2.dp))
                            }
                            Text(
                                text = state.featuredExerciseName ?: "—",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                AnimatedStatText(
                                    text = "${(state.featuredOneRm ?: 0f).toInt()}",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                    ),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "KG",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                            val growth = state.featuredGrowthPercent
                            if (growth != null && growth != 0f) {
                                val positive = growth > 0f
                                val chipColor = if (positive) SuccessGreen else MaterialTheme.colorScheme.error
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .clip(RoundedCornerShape(Dimens.RadiusS))
                                        .background(chipColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = "${if (positive) "+" else ""}${growth.toInt()}% this month",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = chipColor,
                                    )
                                }
                            }
                        }
                    }

                    LineAreaChart(
                        data = weekly,
                        accent = accent,
                        showMinMax = true,
                        startLabel = "${weekly.size} WKS AGO",
                        endLabel = "NOW",
                        yFormatter = { "${it.toInt()} kg" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// 3. Strength trends
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun StrengthTrendsSection(state: ProgressUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
        SectionHeader(title = "Strength trends")
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
            OneRmCard(
                exerciseName = "Bench Press",
                estimatedOneRm = state.benchOneRm ?: 0f,
                sparklineData = state.benchSparkline ?: emptyList(),
                accentColor = dayColorForId(1),
                modifier = Modifier.weight(1f),
            )
            OneRmCard(
                exerciseName = "OH Press",
                estimatedOneRm = state.ohPressOneRm ?: 0f,
                sparklineData = state.ohPressSparkline ?: emptyList(),
                accentColor = DayBonus,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// 4. Weekly volume
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun WeeklyVolumeSection(state: ProgressUiState) {
    val current = state.currentWeekVolume ?: 0f
    val previous = state.previousWeekVolume ?: 0f
    val delta = if (previous > 0f) (current - previous) / previous * 100f else null

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
        SectionHeader(title = "Weekly volume")
        SurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = "THIS WEEK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimatedStatText(text = formatVolume(current))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }
                if (delta != null) {
                    Text(
                        text = "${if (delta >= 0f) "+" else ""}${delta.toInt()}% vs last week",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (delta >= 0f) SuccessGreen else MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            val series = state.weeklyVolumeSeries.takeLast(12)
            LabeledBarChart(
                values = series.map { it.second },
                labels = series.map { it.first },
                accent = MaterialTheme.colorScheme.primary,
                highlightValueLabel = series.lastOrNull()?.let { "${formatVolume(it.second)} kg" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// 5. Consistency
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun ConsistencySection(state: ProgressUiState) {
    val today = remember { LocalDate.now() }
    val workoutsThisMonth = remember(state.dailyVolumeByDate) {
        state.dailyVolumeByDate.keys.count { it.month == today.month && it.year == today.year }
    }
    val daysSinceRest = state.daysSinceLastRest ?: 0
    val primary = MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
        SectionHeader(title = "Consistency")
        SurfaceCard(modifier = Modifier.fillMaxWidth()) {
            ConsistencyHeatmap(
                dailyVolume = state.dailyVolumeByDate,
                accent = primary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeatLegendLabel(text = "LESS")
                Spacer(Modifier.width(6.dp))
                listOf(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    primary.copy(alpha = 0.25f),
                    primary.copy(alpha = 0.5f),
                    primary.copy(alpha = 0.75f),
                    primary,
                ).forEach { bucketColor ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 1.5.dp)
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(bucketColor),
                    )
                }
                Spacer(Modifier.width(6.dp))
                HeatLegendLabel(text = "MORE")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
            StatCell(
                // This counts consecutive training days ending today — a rest today reads
                // as 0, which is the correct overtraining signal (label reflects that).
                label = "Load",
                value = "$daysSinceRest",
                sub = if (daysSinceRest > 5) "train days — rest up" else "days in a row",
                modifier = Modifier.weight(1f),
                valueColor = if (daysSinceRest > 5) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            StatCell(
                label = "This month",
                value = "$workoutsThisMonth",
                sub = "training days",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeatLegendLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 8.sp,
            letterSpacing = 1.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    )
}

// ═══════════════════════════════════════════════════════════════════
// 6. Training split
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun TrainingSplitSection(state: ProgressUiState) {
    val split = state.dayTypeSplit

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
        SectionHeader(title = "Training split")
        SurfaceCard(modifier = Modifier.fillMaxWidth()) {
            if (split.isEmpty()) {
                Text(
                    text = "Not enough data yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val totalSessions = split.sumOf { it.count }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    DonutChart(
                        slices = split.map { dayColorForId(it.dayId) to it.count.toFloat() },
                        centerValue = "$totalSessions",
                        centerCaption = "sessions",
                        modifier = Modifier.size(132.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        split.forEach { slice ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(dayColorForId(slice.dayId)),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = slice.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "${slice.count}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// 7. Session duration
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun SessionDurationSection(state: ProgressUiState) {
    val series = state.durationSeries
    val dateFormat = remember { DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH) }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
        SectionHeader(title = "Session duration")
        SurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = "AVERAGE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimatedStatText(
                            text = if (state.avgDurationMinutes > 0) "${state.avgDurationMinutes}" else "—",
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }
                series.lastOrNull()?.let { last ->
                    Text(
                        text = "last: ${last.second} min",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LineAreaChart(
                data = series.map { it.second.toFloat() },
                accent = MaterialTheme.colorScheme.secondary,
                showMinMax = true,
                startLabel = series.firstOrNull()?.first?.format(dateFormat)?.uppercase(),
                endLabel = series.lastOrNull()?.first?.format(dateFormat)?.uppercase(),
                yFormatter = { "${it.toInt()}m" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// 8. Exercise explorer
// ═══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseExplorerSection(
    exercises: List<Exercise>,
    selected: Exercise?,
    onSelect: (Exercise) -> Unit,
    progression: List<DataPoint>,
    rows: List<ExerciseSessionRow>,
) {
    if (exercises.isEmpty()) return
    val dateFormat = remember { DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH) }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)) {
        SectionHeader(title = "Exercise explorer")
        SurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    TextField(
                        value = selected?.name ?: "Select an exercise",
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                        shape = RoundedCornerShape(Dimens.RadiusM),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        exercises.forEach { exercise ->
                            DropdownMenuItem(
                                text = { Text(exercise.name) },
                                onClick = {
                                    expanded = false
                                    onSelect(exercise)
                                },
                            )
                        }
                    }
                }

                val accent = selected?.let { dayColorForId(it.workoutDayId) }
                    ?: MaterialTheme.colorScheme.primary

                if (progression.size >= 2) {
                    LineAreaChart(
                        data = progression.map { it.value },
                        accent = accent,
                        showDots = true,
                        showMinMax = true,
                        startLabel = progression.first().date.format(dateFormat).uppercase(),
                        endLabel = progression.last().date.format(dateFormat).uppercase(),
                        yFormatter = { "${it.toInt()} kg" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        ProgressMiniStat(
                            label = "BEST",
                            value = "${trimWeight(progression.maxOf { it.value })} kg",
                        )
                        ProgressMiniStat(
                            label = "LATEST",
                            value = "${trimWeight(progression.last().value)} kg",
                        )
                        ProgressMiniStat(label = "SESSIONS", value = "${rows.size}")
                    }
                } else {
                    Text(
                        text = "Log this exercise in at least two workouts to see a trend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (rows.isNotEmpty()) {
                    Column {
                        HairlineDivider()
                        rows.take(5).forEach { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = row.date.format(dateFormat),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "${row.setsCompleted} sets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "best ${trimWeight(row.bestWeight)}kg × ${row.bestReps}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressMiniStat(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
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

// ═══════════════════════════════════════════════════════════════════
// Helpers
// ═══════════════════════════════════════════════════════════════════

private fun formatVolume(volumeKg: Float): String {
    return if (volumeKg >= 1000f) {
        val thousands = volumeKg / 1000f
        // Locale.US so the decimal point never becomes a comma (e.g. "12,5k").
        if (thousands >= 100f) "${thousands.toInt()}k" else "%.1fk".format(Locale.US, thousands)
    } else {
        "${volumeKg.toInt()}"
    }
}

private fun trimWeight(value: Float): String {
    val asLong = value.toLong()
    return if (asLong.toFloat() == value) asLong.toString() else "%.1f".format(Locale.US, value)
}
