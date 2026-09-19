package com.ironlog.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ironlog.app.data.model.EquipmentType
import com.ironlog.app.data.model.Exercise
import com.ironlog.app.ui.theme.Dimens
import com.ironlog.app.ui.theme.MotionTokens
import com.ironlog.app.ui.theme.SuccessGreen

/**
 * Expandable exercise container on the active-workout screen. Completed
 * exercises carry a subtle success border; the chevron rotates with the
 * expand state and content animates on the standard motion specs.
 */
@Composable
fun ExerciseCard(
    exercise: Exercise,
    isExpanded: Boolean,
    exerciseCompleted: Boolean = false,
    headerRightText: String,
    suggestionLine: String?,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(Dimens.RadiusL)
    val completedBorderColor = SuccessGreen.copy(alpha = 0.55f)

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = MotionTokens.standardSpec(),
        label = "exerciseChevron",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(
                if (exerciseCompleted) {
                    Modifier.border(width = 1.dp, color = completedBorderColor, shape = shape)
                } else {
                    Modifier
                },
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpanded() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = headerRightText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(10.dp))
            EquipmentBadge(exercise.equipmentType)
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = if (exerciseCompleted) {
                    SuccessGreen.copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
            )
        }

        if (!suggestionLine.isNullOrBlank()) {
            Text(
                text = suggestionLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(MotionTokens.enterSpec()) + fadeIn(tween(MotionTokens.DurationMedium)),
            exit = shrinkVertically(MotionTokens.exitSpec()) + fadeOut(tween(MotionTokens.DurationFast)),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun EquipmentBadge(type: EquipmentType) {
    val text = when (type) {
        EquipmentType.BARBELL -> "BB"
        EquipmentType.DUMBBELL -> "DB"
        EquipmentType.BODYWEIGHT -> "BW"
        EquipmentType.KETTLEBELL -> "KB"
        EquipmentType.BAND -> "BAND"
        EquipmentType.NONE -> ""
    }
    if (text.isBlank()) return
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.RadiusS))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
