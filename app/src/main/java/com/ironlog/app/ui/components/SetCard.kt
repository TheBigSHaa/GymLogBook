package com.ironlog.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironlog.app.data.model.EquipmentType
import com.ironlog.app.ui.theme.Dimens
import com.ironlog.app.ui.theme.MotionTokens
import com.ironlog.app.ui.workout.PreviousSetInfo

/**
 * One set row in the active workout: set badge, weight input, reps stepper,
 * completion toggle — plus, when available, what the user accomplished
 * LAST TIME for this same set (weight × reps and the rest actually taken).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetCard(
    setNumber: Int,
    weight: String,
    reps: Int,
    completed: Boolean,
    isWarmup: Boolean,
    accentColor: Color,
    equipmentType: EquipmentType?,
    availableDbWeights: List<Float>,
    onWeightChange: (String) -> Unit,
    onRepsChange: (Int) -> Unit,
    onToggleComplete: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    previous: PreviousSetInfo? = null,
    hideLoadAndReps: Boolean = false,
    emphasized: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val isBand = equipmentType == EquipmentType.BAND
    val isDumbbell = equipmentType == EquipmentType.DUMBBELL
    val dbWeights = availableDbWeights.ifEmpty { listOf(6f, 10f, 12f, 17f, 20f) }

    val borderGray = MaterialTheme.colorScheme.outlineVariant
    val inputUnderline = MaterialTheme.colorScheme.outline
    val focusAccent = MaterialTheme.colorScheme.tertiary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Completed sets get a faint accent wash so progress is scannable at a glance.
    val containerColor by animateColorAsState(
        targetValue = if (completed) {
            accentColor.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = MotionTokens.standardSpec(),
        label = "setCardBg",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusM))
            .background(containerColor)
            .then(
                if (emphasized) {
                    Modifier.border(1.5.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(Dimens.RadiusM))
                } else {
                    Modifier
                },
            )
            .combinedClickable(
                onClick = { /* no-op on card body */ },
                onLongClick = onLongPress,
            )
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentWidth(Alignment.Start),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isWarmup) "W" else "S$setNumber",
                    color = if (isWarmup) MaterialTheme.colorScheme.tertiary else labelColor,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.width(12.dp))

                if (!hideLoadAndReps) {
                Column(
                    modifier = Modifier.width(56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (isBand) "BAND" else "WEIGHT",
                        color = labelColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    when {
                        isBand -> Text(
                            text = "—",
                            color = labelColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        isDumbbell -> DumbbellWeightSelector(
                            weight = weight,
                            options = dbWeights,
                            accentColor = if (completed) accentColor else inputUnderline,
                            onWeightChange = onWeightChange,
                        )
                        else -> SetInputUnderlineField(
                            value = weight,
                            onValueChange = onWeightChange,
                            underlineColor = inputUnderline,
                            focusedUnderlineColor = focusAccent,
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.width(56.dp),
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "REPS",
                        color = labelColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    RepsStepperRow(
                        reps = reps,
                        borderColor = borderGray,
                        onRepsChange = onRepsChange,
                    )
                }
                }
            }

            Spacer(Modifier.width(12.dp))

            SetCompleteButton(
                completed = completed,
                accentColor = accentColor,
                borderColor = borderGray,
                onClick = {
                    focusManager.clearFocus()
                    onToggleComplete()
                },
                onLongPress = onLongPress,
            )
        }

        // ── What was accomplished last time for this same set ──
        if (!hideLoadAndReps && previous != null && previous.completed) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = labelColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "LAST TIME",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontSize = 9.sp,
                    ),
                    color = labelColor.copy(alpha = 0.7f),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = buildString {
                        append("${trimWeight(previous.weight)}kg × ${previous.reps}")
                        previous.restSeconds?.let { rest -> append("  ·  ${rest}s rest") }
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
            }
        }
    }
}

private fun trimWeight(value: Float): String {
    val asLong = value.toLong()
    return if (asLong.toFloat() == value) asLong.toString() else value.toString()
}

@Composable
private fun RepsStepperRow(
    reps: Int,
    borderColor: Color,
    onRepsChange: (Int) -> Unit,
) {
    val displayText = reps.toString()
    Row(
        modifier = Modifier.wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        StepperIconButton(
            icon = Icons.Filled.Remove,
            contentDescription = when (reps) {
                1 -> "Decrease to 0 reps"
                else -> "Decrease to ${(reps - 1).coerceAtLeast(0)} reps"
            },
            enabled = reps > 0,
            borderColor = borderColor,
            onClick = { onRepsChange((reps - 1).coerceAtLeast(0)) },
        )

        Text(
            text = displayText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(min = 32.dp),
            textAlign = TextAlign.Center,
        )

        StepperIconButton(
            icon = Icons.Filled.Add,
            contentDescription = when (reps) {
                0 -> "Increase to 1 rep"
                else -> "Increase to ${reps + 1} reps"
            },
            enabled = true,
            borderColor = borderColor,
            onClick = { onRepsChange(reps + 1) },
        )
    }
}

@Composable
private fun StepperIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    borderColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val iconTint = when {
        !enabled -> onSurfaceVariant.copy(alpha = 0.4f)
        pressed -> onSurface
        else -> onSurfaceVariant
    }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = MotionTokens.springBouncy(),
        label = "stepperScale",
    )

    Box(
        modifier = Modifier
            .size(28.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(Dimens.RadiusS))
            .border(1.dp, borderColor, RoundedCornerShape(Dimens.RadiusS))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun DumbbellWeightSelector(
    weight: String,
    options: List<Float>,
    accentColor: Color,
    onWeightChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = weight.ifEmpty { "—" }

    Box {
        Text(
            text = displayText,
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val strokeWidth = 2.dp.toPx()
                    val y = size.height - strokeWidth / 2f
                    drawLine(
                        color = accentColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth,
                    )
                }
                .clickable { expanded = true }
                .padding(bottom = 2.dp),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { kg ->
                val label = if (kg == kg.toLong().toFloat()) "${kg.toLong()}kg" else "${kg}kg"
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onWeightChange(if (kg == kg.toLong().toFloat()) kg.toLong().toString() else kg.toString())
                    },
                )
            }
        }
    }
}

@Composable
private fun SetInputUnderlineField(
    value: String,
    onValueChange: (String) -> Unit,
    underlineColor: Color,
    focusedUnderlineColor: Color,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length))) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(value, isFocused) {
        if (!isFocused) {
            textFieldValue = TextFieldValue(
                text = value,
                selection = TextRange(value.length),
            )
        }
    }

    fun commitWeight() {
        val raw = textFieldValue.text.trim()
        if (raw.isEmpty()) {
            onValueChange("")
            return
        }
        val parsed = raw.toFloatOrNull()
        if (parsed == null) {
            textFieldValue = TextFieldValue(
                text = value,
                selection = TextRange(value.length),
            )
            return
        }
        val normalized = when {
            parsed == 0f -> ""
            parsed == parsed.toLong().toFloat() -> parsed.toLong().toString()
            else -> parsed.toString()
        }
        onValueChange(normalized)
        textFieldValue = TextFieldValue(
            text = normalized,
            selection = TextRange(normalized.length),
        )
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValue = newValue
            // Push the parsed value to the VM on every keystroke so tapping "Finish"
            // (which doesn't blur the field first) can't save a stale weight. Blur
            // still runs commitWeight() to normalize the displayed text.
            val trimmed = newValue.text.trim()
            when {
                trimmed.isEmpty() -> onValueChange("")
                else -> trimmed.toFloatOrNull()?.let { parsed ->
                    onValueChange(
                        when {
                            parsed == 0f -> ""
                            parsed == parsed.toLong().toFloat() -> parsed.toLong().toString()
                            else -> parsed.toString()
                        },
                    )
                }
            }
        },
        singleLine = true,
        textStyle = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(focusedUnderlineColor),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() },
        ),
        modifier = modifier
            .onFocusChanged { state ->
                if (isFocused && !state.isFocused) {
                    commitWeight()
                }
                isFocused = state.isFocused
            }
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val y = size.height - strokeWidth / 2f
                drawLine(
                    color = if (isFocused) focusedUnderlineColor else underlineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth,
                )
            },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                innerTextField()
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SetCompleteButton(
    completed: Boolean,
    accentColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fillColor by animateColorAsState(
        targetValue = if (completed) accentColor else Color.Transparent,
        animationSpec = MotionTokens.standardSpec(),
        label = "setCompleteFill",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (completed) 1f else 0.72f,
        animationSpec = MotionTokens.springBouncy(),
        label = "setCompleteScale",
    )

    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(fillColor)
            .then(
                if (completed) Modifier else Modifier.border(2.dp, borderColor, CircleShape),
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = if (completed) "Completed" else "Mark complete",
            tint = if (completed) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    scaleX = checkScale
                    scaleY = checkScale
                },
        )
    }
}
