package com.ironlog.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ironlog.app.ui.components.foundation.rememberChartProgress

/**
 * Simple rounded bar chart with animated bar growth; the last bar is
 * highlighted in the accent color.
 */
@Composable
fun VolumeBarChart(
    data: List<Float>,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val maxVal = data.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val progress by rememberChartProgress(key = data)

    Canvas(modifier = modifier.fillMaxSize()) {
        val barCount = data.size
        val gapRatio = 0.3f
        val totalGaps = (barCount - 1) * gapRatio
        val barWidth = size.width / (barCount + totalGaps)
        val gap = barWidth * gapRatio

        data.forEachIndexed { index, value ->
            val normalizedHeight = (value / maxVal) * size.height * 0.9f * progress
            if (normalizedHeight <= 0f) return@forEachIndexed
            val x = index * (barWidth + gap)
            val isLast = index == data.lastIndex
            val barColor = if (isLast) accentColor else surfaceHigh

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, size.height - normalizedHeight),
                size = Size(barWidth, normalizedHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            )
        }
    }
}
