package com.ironlog.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.ironlog.app.ui.components.foundation.rememberChartProgress

/**
 * Minimal trend line with animated left-to-right draw and an end dot that
 * fades in as the line completes.
 */
@Composable
fun SparklineChart(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    if (data.size < 2) return

    val progress by rememberChartProgress(key = data)

    Canvas(modifier = modifier.fillMaxSize()) {
        val maxVal = data.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val minVal = data.minOrNull() ?: 0f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val stepX = size.width / (data.size - 1).coerceAtLeast(1)
        val path = Path()

        data.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - minVal) / range) * size.height * 0.85f

            if (index == 0) path.moveTo(x, y)
            else path.lineTo(x, y)
        }

        clipRect(right = size.width * progress) {
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        // End dot fades in as the line lands.
        val lastX = (data.size - 1) * stepX
        val lastY = size.height - ((data.last() - minVal) / range) * size.height * 0.85f
        drawCircle(
            color = color.copy(alpha = progress),
            radius = 4.dp.toPx(),
            center = Offset(lastX, lastY),
        )
    }
}
