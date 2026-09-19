package com.ironlog.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironlog.app.ui.components.foundation.rememberChartProgress

/**
 * Animated sweep donut: slices sweep in clockwise from 12 o'clock using the
 * shared chart progress, with a total label in the hole. The caller renders
 * its own legend beside/below. [slices] pairs a color with a value.
 * Callers should give the donut a square size via [modifier].
 */
@Composable
fun DonutChart(
    slices: List<Pair<Color, Float>>,
    centerValue: String,
    modifier: Modifier = Modifier,
    centerCaption: String? = null,
    strokeWidth: Dp = 16.dp,
) {
    val total = slices.sumOf { it.second.toDouble() }.toFloat()
    if (slices.isEmpty() || total <= 0f) {
        ChartEmptyCaption(modifier = modifier)
        return
    }

    val progress by rememberChartProgress(key = slices)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val arcTopLeft = Offset(inset, inset)
            val gapDegrees = if (slices.size > 1) 2f else 0f
            val availableSweep = 360f - gapDegrees * slices.size
            val animatedEnd = 360f * progress

            var cursor = 0f
            slices.forEach { (color, value) ->
                val fullSweep = value / total * availableSweep
                val visibleSweep = (animatedEnd - cursor).coerceIn(0f, fullSweep)
                if (visibleSweep > 0f) {
                    drawArc(
                        color = color,
                        startAngle = -90f + cursor,
                        sweepAngle = visibleSweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                }
                cursor += fullSweep + gapDegrees
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (centerCaption != null) {
                Text(
                    text = centerCaption.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        letterSpacing = 1.5.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
