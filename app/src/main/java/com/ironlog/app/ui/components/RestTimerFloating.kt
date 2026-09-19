package com.ironlog.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironlog.app.ui.components.foundation.pressScale
import kotlin.math.max

@Composable
fun RestTimerFloating(
    visible: Boolean,
    remainingSeconds: Int,
    totalSeconds: Int,
    paused: Boolean,
    accentColor: Color,
    onAdjust: (Int) -> Unit,
    onToggleVisibility: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    statusLabel: String? = null,
    onStart: (() -> Unit)? = null,
    onPause: (() -> Unit)? = null,
) {
    // Audio/haptic cues are owned by ActiveWorkoutService so they fire even when the
    // screen is off, the app is backgrounded, or the phone is on silent.
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Full pill — shown when visible=true
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            FullTimerPill(
                remainingSeconds = remainingSeconds,
                totalSeconds = totalSeconds,
                paused = paused,
                accentColor = accentColor,
                onAdjust = onAdjust,
                onDragHide = onToggleVisibility,
                onReset = onReset,
                statusLabel = statusLabel,
                onStart = onStart,
                onPause = onPause,
            )
        }

        // Mini handle — shown when visible=false
        AnimatedVisibility(
            visible = !visible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            MiniTimerHandle(
                remainingSeconds = remainingSeconds,
                accentColor = accentColor,
                onTap = onToggleVisibility,
            )
        }
    }
}

@Composable
private fun FullTimerPill(
    remainingSeconds: Int,
    totalSeconds: Int,
    paused: Boolean,
    accentColor: Color,
    onAdjust: (Int) -> Unit,
    onDragHide: () -> Unit,
    onReset: () -> Unit,
    statusLabel: String? = null,
    onStart: (() -> Unit)? = null,
    onPause: (() -> Unit)? = null,
) {
    // Capture theme colors before entering Canvas (non-composable scope)
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(
        modifier = Modifier
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = { if (totalDrag > 40f) onDragHide() },
                    onVerticalDrag = { _, dy -> totalDrag += dy },
                )
            }
            .clip(RoundedCornerShape(48.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                RoundedCornerShape(48.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // +15s left button
            TimerAdjustButton(label = "+15s", onClick = { onAdjust(15) })

            // Circular ring with time inside — READY starts, PAUSE freezes remaining time.
            val onCenterClick = when {
                paused && onStart != null -> onStart
                !paused && onPause != null -> onPause
                else -> null
            }
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .then(
                        if (onCenterClick != null) {
                            Modifier
                                .pressScale()
                                .clickable(onClick = onCenterClick)
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val safeTotal = max(1, totalSeconds)
                val safeRemaining = max(0, remainingSeconds)
                val progress = (safeTotal - safeRemaining).toFloat() / safeTotal

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 4.dp.toPx()
                    val diameter = size.minDimension - stroke
                    val topLeft = Offset(
                        x = (size.width - diameter) / 2f,
                        y = (size.height - diameter) / 2f,
                    )
                    // Track ring
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    // Progress arc
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = statusLabel ?: if (paused) "READY" else "PAUSE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            letterSpacing = 1.5.sp,
                        ),
                        color = accentColor,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = formatTimerMmSs(remainingSeconds),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // -15s right button
            TimerAdjustButton(label = "-15s", onClick = { onAdjust(-15) })
        }

        // Reset button — small text button below the ring row
        Text(
            text = "RESET",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .pressScale()
                .clickable(onClick = onReset)
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun TimerAdjustButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 40.dp)
            .pressScale(pressedScale = 0.92f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun MiniTimerHandle(
    remainingSeconds: Int,
    accentColor: Color,
    onTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .pressScale()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f))
            .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onTap)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = formatTimerMmSs(remainingSeconds),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatTimerMmSs(seconds: Int): String {
    val safe = max(0, seconds)
    val m = safe / 60
    val s = safe % 60
    return "%02d:%02d".format(m, s)
}
