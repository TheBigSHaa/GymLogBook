package com.ironlog.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironlog.app.ui.components.foundation.SurfaceCard
import com.ironlog.app.ui.theme.Dimens
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PersonalRecordDisplay(
    val exerciseName: String,
    val weight: Float,
    val reps: Int,
    val date: LocalDate,
    val accentColor: Color,
    val categoryLabel: String,
)

/**
 * Vertical stack of PR cards. The screen supplies the section header;
 * this component only lays out the records.
 */
@Composable
fun PersonalRecordsSection(
    records: List<PersonalRecordDisplay>,
    modifier: Modifier = Modifier,
) {
    if (records.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
    ) {
        records.forEach { record ->
            PersonalRecordCard(record = record)
        }
    }
}

@Composable
fun PersonalRecordCard(
    record: PersonalRecordDisplay,
    modifier: Modifier = Modifier,
) {
    val dateFormatted = record.date.format(
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
    ).uppercase()

    val accentColor = record.accentColor

    SurfaceCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // 4dp left accent bar painted over the card background.
                    drawRect(
                        color = accentColor,
                        topLeft = Offset(0f, 0f),
                        size = Size(4.dp.toPx(), size.height),
                    )
                }
                .padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = record.categoryLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                    ),
                    color = accentColor,
                )
                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            Text(
                text = record.exerciseName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${record.weight.toInt()}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "kg × ${record.reps}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}
