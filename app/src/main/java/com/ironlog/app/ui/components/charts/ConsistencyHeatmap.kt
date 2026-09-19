package com.ironlog.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironlog.app.ui.components.foundation.rememberChartProgress
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * GitHub-style consistency grid: the last [weeks] weeks as columns
 * (oldest → newest), Monday→Sunday as rows. Each cell is bucketed by that
 * day's training volume relative to the best day in the window:
 * 0 → surfaceContainerHigh, then accent at alpha 0.25 / 0.5 / 0.75 / 1.0.
 * Cells sweep in left-to-right using the shared chart progress.
 */
@Composable
fun ConsistencyHeatmap(
    dailyVolume: Map<LocalDate, Float>,
    accent: Color,
    modifier: Modifier = Modifier,
    weeks: Int = 12,
) {
    val emptyCellColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val progress by rememberChartProgress(key = dailyVolume)
    val today = remember { LocalDate.now() }
    val gridStart = remember(today, weeks) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(weeks - 1L)
    }
    val maxVolume = remember(dailyVolume) {
        dailyVolume.values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    }
    val dayInitials = remember { listOf("M", "T", "W", "T", "F", "S", "S") }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gap = 3.dp
        val labelWidth = 14.dp
        val labelGap = 8.dp
        val available = maxWidth - labelWidth - labelGap - gap * (weeks - 1)
        val cell = (available / weeks).coerceIn(4.dp, 20.dp)
        val gridWidth = cell * weeks + gap * (weeks - 1)
        val gridHeight = cell * 7 + gap * 6

        Row {
            Column(
                modifier = Modifier
                    .width(labelWidth)
                    .height(gridHeight),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                dayInitials.forEach { initial ->
                    Box(
                        modifier = Modifier.size(width = labelWidth, height = cell),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                letterSpacing = 0.sp,
                            ),
                            color = labelColor.copy(alpha = 0.7f),
                        )
                    }
                }
            }
            Spacer(Modifier.width(labelGap))
            Canvas(modifier = Modifier.size(width = gridWidth, height = gridHeight)) {
                val cellPx = cell.toPx()
                val gapPx = gap.toPx()
                val corner = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                for (col in 0 until weeks) {
                    // Columns reveal left-to-right as progress runs 0→1.
                    val columnAlpha =
                        ((progress - col.toFloat() / weeks * 0.5f) / 0.5f).coerceIn(0f, 1f)
                    if (columnAlpha <= 0f) continue
                    for (row in 0 until 7) {
                        val date = gridStart.plusDays(col * 7L + row)
                        if (date.isAfter(today)) continue
                        val volume = dailyVolume[date] ?: 0f
                        val cellColor = when {
                            volume <= 0f -> emptyCellColor
                            volume <= maxVolume * 0.25f -> accent.copy(alpha = 0.25f)
                            volume <= maxVolume * 0.5f -> accent.copy(alpha = 0.5f)
                            volume <= maxVolume * 0.75f -> accent.copy(alpha = 0.75f)
                            else -> accent
                        }
                        drawRoundRect(
                            color = cellColor.copy(alpha = cellColor.alpha * columnAlpha),
                            topLeft = Offset(col * (cellPx + gapPx), row * (cellPx + gapPx)),
                            size = Size(cellPx, cellPx),
                            cornerRadius = corner,
                        )
                    }
                }
            }
        }
    }
}
