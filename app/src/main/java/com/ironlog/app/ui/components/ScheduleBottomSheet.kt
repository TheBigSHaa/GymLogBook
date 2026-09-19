package com.ironlog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironlog.app.data.model.ScheduledWorkout
import com.ironlog.app.ui.components.foundation.SurfaceCard
import com.ironlog.app.ui.theme.Dimens
import com.ironlog.app.ui.theme.dayColorForId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBottomSheet(
    date: LocalDate,
    currentSchedule: ScheduledWorkout?,
    onScheduleWorkout: (Int) -> Unit,
    onScheduleRest: () -> Unit,
    onRemoveSchedule: () -> Unit,
    onAutoFillWeek: (Int) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState,
) {
    val formattedDate = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = Dimens.RadiusXL, topEnd = Dimens.RadiusXL),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.CardPaddingLarge, vertical = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formattedDate.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Schedule Workout",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (currentSchedule != null) {
                Text(
                    text = if (currentSchedule.isRestDay) "Currently: Rest Day"
                           else "Currently: Day ${currentSchedule.workoutDayId} scheduled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(4.dp))

            val dayOptions = listOf(
                1 to "Upper Power",
                2 to "Lower Power",
                3 to "Upper Hypertrophy",
                4 to "Lower Hypertrophy",
                5 to "Calisthenics",
            )

            dayOptions.forEach { (dayId, label) ->
                val color = dayColorForId(dayId)
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = Dimens.RadiusM,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    onClick = { onScheduleWorkout(dayId) },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Day $dayId",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = color,
                            )
                        }
                    }
                }
            }

            SurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                radius = Dimens.RadiusM,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                onClick = onScheduleRest,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline),
                    )
                    Text(
                        text = "Rest Day",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (currentSchedule != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = onRemoveSchedule,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "REMOVE SCHEDULE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
