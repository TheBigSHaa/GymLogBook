package com.ironlog.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.ironlog.app.ui.components.foundation.rememberChartProgress

/**
 * Forge line chart: accent line with a soft gradient area beneath, animated
 * left-to-right draw, and an end-point dot that fades in as the line lands.
 *
 * Labels are regular [Text] composables laid out around the Canvas —
 * no draw-text APIs. Callers must constrain the height via [modifier].
 */
@Composable
fun LineAreaChart(
    data: List<Float>,
    accent: Color,
    modifier: Modifier = Modifier,
    showDots: Boolean = false,
    showMinMax: Boolean = false,
    startLabel: String? = null,
    endLabel: String? = null,
    yFormatter: (Float) -> String = { "${it.toInt()}" },
) {
    if (data.size < 2) {
        ChartEmptyCaption(modifier = modifier)
        return
    }

    val progress by rememberChartProgress(key = data)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val minValue = remember(data) { data.min() }
    val maxValue = remember(data) { data.max() }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val range = (maxValue - minValue).coerceAtLeast(1f)
                val topInset = size.height * 0.08f
                val chartHeight = size.height * 0.84f
                val stepX = size.width / (data.size - 1)

                fun yFor(value: Float): Float =
                    topInset + (1f - (value - minValue) / range) * chartHeight

                val linePath = Path()
                data.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = yFor(value)
                    if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                }
                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }

                clipRect(right = size.width * progress) {
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0f)),
                            startY = topInset,
                            endY = size.height,
                        ),
                    )
                    drawPath(
                        path = linePath,
                        color = accent,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                    if (showDots) {
                        data.forEachIndexed { index, value ->
                            drawCircle(
                                color = accent,
                                radius = 3.dp.toPx(),
                                center = Offset(index * stepX, yFor(value)),
                            )
                        }
                    }
                }

                // End-point dot fades in as the draw completes.
                drawCircle(
                    color = accent.copy(alpha = progress),
                    radius = 4.dp.toPx(),
                    center = Offset(size.width, yFor(data.last())),
                )
            }
            if (showMinMax) {
                Text(
                    text = yFormatter(maxValue),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.TopStart),
                )
                Text(
                    text = yFormatter(minValue),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
        }
        if (startLabel != null || endLabel != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                Text(
                    text = startLabel.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = endLabel.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/** Shared placeholder for charts that need at least two data points. */
@Composable
internal fun ChartEmptyCaption(
    modifier: Modifier = Modifier,
    message: String = "Not enough data yet",
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
