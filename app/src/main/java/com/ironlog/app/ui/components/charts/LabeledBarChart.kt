package com.ironlog.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironlog.app.ui.components.foundation.rememberChartProgress
import kotlin.math.min

/**
 * Forge bar chart: rounded bars growing from the baseline with the shared
 * chart-progress animation, X labels underneath each bar, and the most
 * recent (last) bar highlighted in the accent color. Callers must
 * constrain the height via [modifier].
 */
@Composable
fun LabeledBarChart(
    values: List<Float>,
    labels: List<String>,
    accent: Color,
    modifier: Modifier = Modifier,
    highlightValueLabel: String? = null,
) {
    if (values.size < 2) {
        ChartEmptyCaption(modifier = modifier)
        return
    }

    val baseColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val progress by rememberChartProgress(key = values)
    val maxValue = values.max().coerceAtLeast(1f)

    Column(modifier = modifier) {
        if (highlightValueLabel != null) {
            Text(
                text = highlightValueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 4.dp),
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val slot = size.width / values.size
            val barWidth = slot * 0.58f
            values.forEachIndexed { index, value ->
                val barHeight = (value / maxValue) * size.height * progress
                if (barHeight <= 0f) return@forEachIndexed
                val x = index * slot + (slot - barWidth) / 2f
                val corner = min(6.dp.toPx(), barHeight / 2f)
                drawRoundRect(
                    color = if (index == values.lastIndex) accent else baseColor,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(corner, corner),
                )
            }
        }
        if (labels.isNotEmpty()) {
            // Thin out crowded label rows: keep first, last and alternates.
            val visibleLabels = List(values.size) { index ->
                val label = labels.getOrNull(index).orEmpty()
                if (labels.size > 8 && index != 0 && index != values.size - 1 && index % 2 == 1) "" else label
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                visibleLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            letterSpacing = 0.5.sp,
                        ),
                        color = labelColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
