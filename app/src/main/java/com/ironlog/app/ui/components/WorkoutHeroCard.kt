package com.ironlog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironlog.app.data.model.Exercise
import com.ironlog.app.data.model.RepUnit
import com.ironlog.app.ui.components.foundation.AccentButton
import com.ironlog.app.ui.components.foundation.SectionHeader
import com.ironlog.app.ui.components.foundation.SurfaceCard
import com.ironlog.app.ui.components.foundation.accentGradient
import com.ironlog.app.ui.components.foundation.pressScale
import com.ironlog.app.ui.theme.Dimens
import com.ironlog.app.ui.theme.dayColorForId

@Composable
fun WorkoutHeroCard(
    dayNumber: Int,
    dayName: String,
    accentColor: Color,
    exercises: List<Exercise>,
    durationEstimate: String,
    onStartWorkout: () -> Unit,
    isRestDay: Boolean = false,
    noWorkoutScheduled: Boolean = false,
    onPickWorkout: (Int) -> Unit = {},
    onMarkRest: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    SurfaceCard(
        modifier = modifier.fillMaxWidth(),
        radius = Dimens.RadiusXL,
        contentPadding = PaddingValues(0.dp),
    ) {
        when {
            isRestDay -> RestDayContent()

            noWorkoutScheduled -> NoWorkoutContent(
                onPickWorkout = onPickWorkout,
                onMarkRest = onMarkRest,
            )

            else -> {
                // ── Hero header — accent wash + day identity ─────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                        .background(accentGradient(accentColor))
                        .padding(Dimens.CardPaddingLarge),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Column {
                        Text(
                            text = "DAY $dayNumber",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp,
                            ),
                            color = accentColor,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = dayName.uppercase(),
                            style = MaterialTheme.typography.displayMedium,
                            color = accentColor,
                        )
                    }
                }

                // ── Exercise lineup + CTA ────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Dimens.CardPaddingLarge,
                            end = Dimens.CardPaddingLarge,
                            bottom = Dimens.CardPaddingLarge,
                            top = 4.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.CardPadding),
                ) {
                    SectionHeader(
                        title = "Today's Lineup",
                        action = { DurationChip(text = durationEstimate, accentColor = accentColor) },
                    )

                    Column {
                        exercises.forEachIndexed { idx, exercise ->
                            ExerciseLineItem(
                                name = exercise.name,
                                setsReps = formatSetsReps(exercise),
                                accentColor = accentColor,
                                showDivider = idx < exercises.lastIndex,
                            )
                        }
                    }

                    AccentButton(
                        text = "Start Workout",
                        onClick = onStartWorkout,
                        accent = accentColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Small tonal chip showing the estimated session duration. */
@Composable
private fun DurationChip(
    text: String,
    accentColor: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusS))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
            color = accentColor,
        )
    }
}

/** Calm, centered rest-day treatment. */
@Composable
private fun RestDayContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.CardPaddingLarge, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.SelfImprovement,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "RECOVERY",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Rest Day",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No training today — recovery is where progress happens.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Day picker shown when nothing is scheduled for the selected date. */
@Composable
private fun NoWorkoutContent(
    onPickWorkout: (Int) -> Unit,
    onMarkRest: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.CardPaddingLarge),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "NO WORKOUT SCHEDULED",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Pick a training day",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (1..5).forEach { dayId ->
                val dayColor = dayColorForId(dayId)
                val shape = RoundedCornerShape(Dimens.RadiusM)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .pressScale()
                        .clip(shape)
                        .background(dayColor.copy(alpha = 0.14f))
                        .border(1.dp, dayColor.copy(alpha = 0.45f), shape)
                        .clickable { onPickWorkout(dayId) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "D$dayId",
                        style = MaterialTheme.typography.labelLarge,
                        color = dayColor,
                    )
                }
            }
        }
        OutlinedButton(
            onClick = onMarkRest,
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.TouchTarget)
                .pressScale(),
            shape = RoundedCornerShape(Dimens.RadiusM),
        ) {
            Text(
                text = "MARK AS REST DAY",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 2.sp,
                ),
            )
        }
    }
}

private fun formatSetsReps(exercise: Exercise): String {
    val sets = exercise.targetSets
    val min = exercise.targetRepsMin
    val max = exercise.targetRepsMax
    val unit = if (exercise.repUnit == RepUnit.SECONDS) "s" else ""

    val reps =
        when {
            exercise.section == "CIRCUIT" -> {
                val work = exercise.circuitDurationSeconds ?: min
                return "${sets}×${work}s"
            }
            max <= 0 -> "—"
            min == max -> "$min$unit"
            else -> "$min-$max$unit"
        }

    return "${sets}×${reps}"
}
