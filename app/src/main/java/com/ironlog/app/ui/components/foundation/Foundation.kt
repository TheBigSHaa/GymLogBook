package com.ironlog.app.ui.components.foundation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironlog.app.ui.theme.Dimens
import com.ironlog.app.ui.theme.MotionTokens
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════════
// ProYou foundation kit — the shared vocabulary of the design system.
// Every screen composes these instead of hand-rolling cards, headers,
// buttons and entrance animations.
// ═══════════════════════════════════════════════════════════════════

/**
 * Standard screen header: small primary-colored overline + ExtraBold title,
 * left-aligned, with optional leading icon and trailing actions.
 */
@Composable
fun ProYouTopBar(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Dimens.ScreenPaddingH, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (navigationIcon != null) {
            navigationIcon()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            if (overline != null) {
                Text(
                    text = overline.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Circular tonal icon button used in top bars. */
@Composable
fun TopBarIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .pressScale()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Uppercase section label with wide tracking; optional trailing action slot. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        action?.invoke()
    }
}

/**
 * The standard card container. Use radius [Dimens.RadiusXL] for hero surfaces,
 * default [Dimens.RadiusL] elsewhere. Clickable cards get press-scale feedback.
 */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    radius: Dp = Dimens.RadiusL,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    bordered: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(Dimens.CardPadding),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    var base = modifier
    if (onClick != null) base = base.pressScale()
    base = base
        .clip(shape)
        .background(color)
    if (bordered) {
        base = base.border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = Dimens.HairlineAlpha),
            shape,
        )
    }
    if (onClick != null) base = base.clickable(onClick = onClick)
    Column(modifier = base.padding(contentPadding), content = content)
}

/** Primary CTA — tall filled button in the contextual accent color. */
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    height: Dp = Dimens.CtaHeight,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(height)
            .pressScale(),
        shape = RoundedCornerShape(Dimens.RadiusL),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = Color.White,
            disabledContainerColor = accent.copy(alpha = 0.4f),
            disabledContentColor = Color.White.copy(alpha = 0.7f),
        ),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.5.sp,
                fontSize = 15.sp,
            ),
        )
    }
}

/**
 * Stat value that rolls vertically when it changes (odometer-style).
 * Use for any number that updates live (counters, totals, timers are exempt).
 */
@Composable
fun AnimatedStatText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically(MotionTokens.enterSpec()) { it / 2 } + fadeIn(tween(MotionTokens.DurationMedium)))
                .togetherWith(
                    slideOutVertically(MotionTokens.exitSpec()) { -it / 2 } + fadeOut(tween(MotionTokens.DurationFast)),
                )
                .using(SizeTransform(clip = false))
        },
        modifier = modifier,
        label = "animatedStat",
    ) { value ->
        Text(text = value, style = style, color = color)
    }
}

/** Hairline separator at the system alpha. */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Dimens.HairlineAlpha),
        thickness = 1.dp,
    )
}

/** Centered empty-state block: tonal icon circle, title, supporting message. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Staggered entrance for top-level content blocks: fades in while drifting up,
 * delayed by [index] so sections cascade. Plays once per screen instance
 * (survives config changes via rememberSaveable) and never shifts layout —
 * the content always occupies its final space.
 */
@Composable
fun StaggeredEntrance(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            delay(40L + index * 50L)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    MotionTokens.DurationSlow,
                    easing = MotionTokens.EasingEmphasizedDecelerate,
                ),
            )
            played = true
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 24.dp.toPx()
        },
    ) {
        content()
    }
}

/**
 * Animation progress (0→1) for charts: draw fraction, bar heights, arc sweeps.
 * Re-runs when [key] changes.
 */
@Composable
fun rememberChartProgress(key: Any? = Unit): State<Float> {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                MotionTokens.DurationExtraSlow,
                easing = MotionTokens.EasingEmphasizedDecelerate,
            ),
        )
    }
    return progress.asState()
}

/**
 * Press feedback: scales down slightly while touched, springs back on release.
 * Works alongside any clickable — it observes the pointer without consuming it.
 */
fun Modifier.pressScale(pressedScale: Float = 0.965f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = MotionTokens.springBouncy(),
        label = "pressScale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}

/** Vertical wash of the accent color for hero surfaces. */
fun accentGradient(accent: Color): Brush =
    Brush.verticalGradient(
        colors = listOf(accent.copy(alpha = 0.16f), Color.Transparent),
    )
