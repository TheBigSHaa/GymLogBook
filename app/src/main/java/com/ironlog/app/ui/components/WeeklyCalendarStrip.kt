package com.ironlog.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironlog.app.ui.components.foundation.SurfaceCard
import com.ironlog.app.ui.components.foundation.pressScale
import com.ironlog.app.ui.theme.Dimens
import com.ironlog.app.ui.theme.MotionTokens
import com.ironlog.app.ui.theme.dayColorForId
import java.time.LocalDate

@Composable
fun WeeklyCalendarStrip(
    today: LocalDate = LocalDate.now(),
    selectedDate: LocalDate = today,
    scheduledDays: Map<LocalDate, Int> = emptyMap(), // date → workoutDayId
    yearMonth: java.time.YearMonth? = null,
    onDateSelected: (LocalDate) -> Unit = {},
    firstDayOfWeek: String = "SUNDAY",
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
) {
    val days = androidx.compose.runtime.remember(yearMonth, today) {
        if (yearMonth != null) {
            val length = yearMonth.lengthOfMonth()
            (1..length).map { yearMonth.atDay(it) }
        } else {
            val startDate = today.minusDays(3)
            (0..13).map { startDate.plusDays(it.toLong()) }
        }
    }

    // Scroll to appropriate date on first composition / month change
    LaunchedEffect(days) {
        val targetDate = if (selectedDate in days) selectedDate else if (today in days) today else days.firstOrNull()
        val index = days.indexOf(targetDate)
        if (index >= 0) {
            listState.scrollToItem(index)
        }
    }

    // Scroll to show selectedDate whenever it changes
    LaunchedEffect(selectedDate) {
        val index = days.indexOf(selectedDate)
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    SurfaceCard(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 14.dp),
    ) {
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            items(days) { date ->
                DayCell(
                    date = date,
                    isToday = date == today,
                    isSelected = date == selectedDate,
                    workoutDayId = scheduledDays[date],
                    onClick = { onDateSelected(date) },
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    workoutDayId: Int?,
    onClick: () -> Unit,
) {
    val dotColor = workoutDayId?.let { dayColorForId(it) }
    val shape = RoundedCornerShape(Dimens.RadiusM)

    val fillColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = MotionTokens.standardSpec(),
        label = "dayCellFill",
    )
    val nameColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color.White.copy(alpha = 0.85f)
            isToday -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        },
        animationSpec = MotionTokens.standardSpec(),
        label = "dayCellName",
    )
    val numberColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color.White
            isToday -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        },
        animationSpec = MotionTokens.standardSpec(),
        label = "dayCellNumber",
    )
    val ringColor by animateColorAsState(
        targetValue = when {
            isToday && isSelected -> Color.White.copy(alpha = 0.55f)
            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            else -> Color.Transparent
        },
        animationSpec = MotionTokens.standardSpec(),
        label = "dayCellRing",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .width(48.dp)
            .pressScale()
            .clip(shape)
            .background(fillColor)
            .border(1.5.dp, ringColor, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        // Day name
        Text(
            text = date.dayOfWeek.name.take(3),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = nameColor,
        )

        // Day number
        Text(
            text = "${date.dayOfMonth}",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
            ),
            color = numberColor,
        )

        // Colored dot for scheduled workout
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor ?: Color.Transparent),
        )
    }
}
