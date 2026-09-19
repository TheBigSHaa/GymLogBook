package com.ironlog.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironlog.app.ui.components.foundation.AnimatedStatText
import com.ironlog.app.ui.components.foundation.SurfaceCard

/**
 * Compact strength-trend card: accent overline, estimated 1RM with animated
 * value, and an animated sparkline of the e1RM history.
 */
@Composable
fun OneRmCard(
    exerciseName: String,
    estimatedOneRm: Float,
    sparklineData: List<Float>,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(modifier = modifier) {
        Text(
            text = "EST. 1RM",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            ),
            color = accentColor,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = exerciseName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedStatText(
                text = if (estimatedOneRm > 0) "${estimatedOneRm.toInt()}" else "—",
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "kg",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        if (sparklineData.size >= 2) {
            SparklineChart(
                data = sparklineData,
                color = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            )
        } else {
            Text(
                text = "Not enough data yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}
