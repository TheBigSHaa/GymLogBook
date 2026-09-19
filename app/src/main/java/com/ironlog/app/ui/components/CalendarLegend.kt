package com.ironlog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ironlog.app.ui.components.foundation.SectionHeader
import com.ironlog.app.ui.components.foundation.SurfaceCard
import com.ironlog.app.ui.theme.DayBonus
import com.ironlog.app.ui.theme.DayLowerHypertrophy
import com.ironlog.app.ui.theme.DayLowerPower
import com.ironlog.app.ui.theme.DayUpperHypertrophy
import com.ironlog.app.ui.theme.DayUpperPower
import com.ironlog.app.ui.theme.Dimens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarLegend(modifier: Modifier = Modifier) {
    val legendItems = listOf(
        "Upper Power" to DayUpperPower,
        "Lower Power" to DayLowerPower,
        "Upper Hypertrophy" to DayUpperHypertrophy,
        "Lower Hypertrophy" to DayLowerHypertrophy,
        "Calisthenics" to DayBonus,
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        SectionHeader(title = "Legend")

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            legendItems.forEach { (label, color) ->
                SurfaceCard(
                    radius = Dimens.RadiusL,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color),
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
