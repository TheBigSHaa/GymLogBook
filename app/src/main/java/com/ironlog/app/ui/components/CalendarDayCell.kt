package com.ironlog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ironlog.app.ui.components.foundation.pressScale
import com.ironlog.app.ui.theme.Dimens

@Composable
fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    dotColor: Color?,
    isDimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(56.dp)
            .pressScale()
            .clip(RoundedCornerShape(Dimens.RadiusM))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isToday) {
            // Today: primary-tinted ring cell.
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$day",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            Text(
                text = "$day",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = if (isDimmed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(4.dp))

        if (dotColor != null) {
            // Completed / scheduled marker keeps its glow.
            val dotSize = if (isToday) 7.dp else 5.dp
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        ambientColor = dotColor.copy(alpha = 0.6f),
                        spotColor = dotColor.copy(alpha = 0.6f),
                    )
                    .clip(CircleShape)
                    .background(dotColor),
            )
        } else {
            Spacer(Modifier.size(5.dp))
        }
    }
}
