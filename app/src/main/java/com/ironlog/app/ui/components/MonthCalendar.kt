package com.ironlog.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironlog.app.data.model.ScheduledWorkout
import com.ironlog.app.data.model.WorkoutDay
import com.ironlog.app.ui.theme.dayColorForId
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthCalendar(
    yearMonth: YearMonth,
    scheduledWorkouts: Map<LocalDate, ScheduledWorkout>,
    completedDates: Map<LocalDate, WorkoutDay>,
    onDateTap: (LocalDate) -> Unit,
    firstDayOfWeek: String = "SUNDAY",
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val firstOfMonth = yearMonth.atDay(1)
    // DayOfWeek values: MON=1, TUE=2, WED=3, THU=4, FRI=5, SAT=6, SUN=7
    val firstDayOffset = if (firstDayOfWeek == "SUNDAY") {
        firstOfMonth.dayOfWeek.value % 7  // SUN=0, MON=1, ..., SAT=6
    } else {
        firstOfMonth.dayOfWeek.value - 1  // MON=0, TUE=1, ..., SUN=6
    }
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = firstDayOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    val dayHeaders = if (firstDayOfWeek == "SUNDAY") {
        listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    } else {
        listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Day-of-week headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            dayHeaders.forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 9.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayOfMonth = cellIndex - firstDayOffset + 1

                        if (dayOfMonth in 1..daysInMonth) {
                            val date = yearMonth.atDay(dayOfMonth)
                            val isToday = date == today
                            val scheduled = scheduledWorkouts[date]
                            val completed = completedDates[date]

                            val dotColor = when {
                                completed != null -> dayColorForId(completed.id)
                                scheduled?.workoutDayId != null -> dayColorForId(scheduled.workoutDayId!!)
                                else -> null
                            }
                            val isRestDay = scheduled?.isRestDay == true
                            val isDimmed = dotColor == null && !isRestDay && !isToday

                            CalendarDayCell(
                                day = dayOfMonth,
                                isToday = isToday,
                                dotColor = dotColor,
                                isDimmed = isDimmed,
                                onClick = { onDateTap(date) },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f).height(56.dp))
                        }
                    }
                }
            }
        }
    }
}
