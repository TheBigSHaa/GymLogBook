package com.ironlog.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironlog.app.ui.components.foundation.AnimatedStatText
import com.ironlog.app.ui.components.foundation.HairlineDivider
import com.ironlog.app.ui.components.foundation.SurfaceCard
import com.ironlog.app.ui.components.foundation.pressScale
import com.ironlog.app.ui.log.SessionWithDetails
import com.ironlog.app.ui.theme.Dimens
import com.ironlog.app.ui.theme.MotionTokens
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutLogCard(
    session: SessionWithDetails,
    accentColor: Color,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedDate = session.session.date
        .format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH))
        .uppercase()

    val volumeFormatted = NumberFormat.getNumberInstance(Locale.US)
        .format(session.totalVolume)

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = MotionTokens.standardSpec(),
        label = "chevronRotation",
    )

    SurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // 4dp accent bar hugging the clipped left edge.
                    drawRect(
                        color = accentColor,
                        topLeft = Offset.Zero,
                        size = Size(4.dp.toPx(), size.height),
                    )
                }
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongPress,
                )
                .padding(Dimens.CardPadding),
        ) {
            // ─── Top row: date + day name + icon badge ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = session.workoutDay.name.uppercase(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = accentColor,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(Dimens.RadiusM))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.FitnessCenter,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ─── Stats row + chevron ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    StatBlock(
                        label = "DURATION",
                        value = if (session.durationMinutes > 0) "${session.durationMinutes} min" else "—",
                    )
                    StatBlock(
                        label = "VOLUME",
                        value = volumeFormatted,
                        suffix = "kg",
                    )
                }
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = chevronRotation },
                )
            }

            // ─── Expanded detail ───
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(MotionTokens.enterSpec()) +
                    fadeIn(tween(MotionTokens.DurationMedium)),
                exit = shrinkVertically(MotionTokens.exitSpec()) +
                    fadeOut(tween(MotionTokens.DurationFast)),
            ) {
                Column(
                    modifier = Modifier.padding(top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HairlineDivider()

                    session.exercises.forEach { exerciseLog ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = exerciseLog.exercise.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val completedSets = exerciseLog.sets.filter { it.set.completed }
                            if (completedSets.isNotEmpty()) {
                                Text(
                                    text = buildAnnotatedString {
                                        // setNumber is positional (warm-ups lead the list), so
                                        // working sets are re-labelled S1..Sn by their own order.
                                        var workingIndex = 0
                                        completedSets.forEachIndexed { idx, swp ->
                                            val s = swp.set
                                            val prefix = if (s.isWarmup) "W" else "S${++workingIndex}"
                                            val weightStr = trimFloat(s.weight)
                                            append("$prefix: ${weightStr}kg × ${s.reps}")
                                            if (swp.isPr) {
                                                append(" ")
                                                withStyle(
                                                    SpanStyle(
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        fontWeight = FontWeight.Bold,
                                                    ),
                                                ) { append("PR") }
                                            }
                                            if (idx != completedSets.lastIndex) append("  ·  ")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (!session.session.notes.isNullOrBlank()) {
                        Text(
                            text = session.session.notes.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Overline label + odometer-animated stat value (with optional unit suffix). */
@Composable
private fun StatBlock(
    label: String,
    value: String,
    suffix: String? = null,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedStatText(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (suffix != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun trimFloat(v: Float): String {
    val i = v.toInt()
    return if (i.toFloat() == v) i.toString() else v.toString()
}
