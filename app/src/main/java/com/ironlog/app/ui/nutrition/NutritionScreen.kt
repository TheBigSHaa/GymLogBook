package com.ironlog.app.ui.nutrition

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.EggAlt
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ironlog.app.data.model.MealCompletion
import com.ironlog.app.ui.components.foundation.AccentButton
import com.ironlog.app.ui.components.foundation.AnimatedStatText
import com.ironlog.app.ui.components.foundation.HairlineDivider
import com.ironlog.app.ui.components.foundation.ProYouTopBar
import com.ironlog.app.ui.components.foundation.SectionHeader
import com.ironlog.app.ui.components.foundation.StaggeredEntrance
import com.ironlog.app.ui.components.foundation.SurfaceCard
import com.ironlog.app.ui.components.foundation.pressScale
import com.ironlog.app.ui.components.foundation.rememberChartProgress
import com.ironlog.app.ui.nutrition.NutritionViewModel.MealOptionWithState
import com.ironlog.app.ui.nutrition.NutritionViewModel.MealSlotWithOptions
import com.ironlog.app.ui.nutrition.NutritionViewModel.NutritionUiState
import com.ironlog.app.ui.theme.Dimens
import com.ironlog.app.ui.theme.MotionTokens
import kotlinx.coroutines.flow.distinctUntilChanged

// Hydration blue — two tones so it stays legible on both themes (the bright cyan
// washes out on the light theme's white cards).
private val WaterColorDark = Color(0xFF35B8E8)
private val WaterColorLight = Color(0xFF0277BD)

/**
 * Theme-appropriate hydration accent, chosen from the ACTUAL applied color scheme
 * (not the system setting — the app's theme can be forced independently).
 */
@Composable
private fun waterColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) WaterColorLight else WaterColorDark

/** Max entrance-stagger step so late sections never feel sluggish. */
private const val MaxStaggerIndex = 6

@Composable
fun NutritionScreen(viewModel: NutritionViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPrepDialog by remember { mutableStateOf(false) }

    if (showPrepDialog) {
        AlertDialog(
            onDismissRequest = { showPrepDialog = false },
            title = { Text("Meal Prep Protocol") },
            text = { Text("Full meal prep guide coming soon. Check back in the next update!") },
            confirmButton = {
                TextButton(onClick = { showPrepDialog = false }) { Text("OK") }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ProYouTopBar(overline = "Nutrition", title = "Fuel") },
    ) { innerPadding ->
        val slotCount = uiState.mealSlots.size
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = Dimens.ScreenPaddingH,
                top = 8.dp,
                end = Dimens.ScreenPaddingH,
                bottom = Dimens.ScreenPaddingBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
        ) {
            itemsIndexed(uiState.mealSlots, key = { _, s -> s.slot.id }) { index, slotWithOptions ->
                StaggeredEntrance(index = index.coerceAtMost(MaxStaggerIndex)) {
                    MealSlotCard(
                        slotWithOptions = slotWithOptions,
                        completion = uiState.completions[slotWithOptions.slot.id],
                        ingredientChecks = uiState.ingredientChecks[slotWithOptions.slot.id] ?: emptyMap(),
                        isExpanded = uiState.expandedSlotId == slotWithOptions.slot.id,
                        onToggleExpand = { viewModel.expandSlot(slotWithOptions.slot.id) },
                        onToggleComplete = { optId -> viewModel.toggleMealComplete(slotWithOptions.slot.id, optId) },
                        onSelectOption = { optId -> viewModel.selectOption(slotWithOptions.slot.id, optId) },
                        onToggleIngredient = { ingId -> viewModel.toggleIngredient(slotWithOptions.slot.id, ingId) },
                    )
                }
            }

            item(key = "trackersHeader") {
                StaggeredEntrance(index = slotCount.coerceAtMost(MaxStaggerIndex)) {
                    SectionHeader(
                        title = "Daily Trackers",
                        modifier = Modifier.padding(top = Dimens.SectionSpacing - Dimens.ItemSpacing),
                    )
                }
            }

            item(key = "water") {
                StaggeredEntrance(index = (slotCount + 1).coerceAtMost(MaxStaggerIndex)) {
                    WaterTrackerCard(
                        waterMl = uiState.waterMl,
                        targetMl = uiState.waterTargetMl,
                        onAdd = viewModel::addWater,
                    )
                }
            }

            item(key = "macros") {
                StaggeredEntrance(index = (slotCount + 2).coerceAtMost(MaxStaggerIndex)) {
                    MacroTrackerCard(uiState = uiState)
                }
            }

            item(key = "eggs") {
                StaggeredEntrance(index = (slotCount + 3).coerceAtMost(MaxStaggerIndex)) {
                    EggBudgetCard(eggsUsed = uiState.weeklyEggsUsed, budget = uiState.weeklyEggBudget)
                }
            }

            item(key = "prep") {
                StaggeredEntrance(
                    index = (slotCount + 4).coerceAtMost(MaxStaggerIndex),
                    modifier = Modifier.padding(top = Dimens.SectionSpacing - Dimens.ItemSpacing),
                ) {
                    MealPrepButton(onClick = { showPrepDialog = true })
                }
            }
        }
    }
}

@Composable
private fun MealSlotCard(
    slotWithOptions: MealSlotWithOptions,
    completion: MealCompletion?,
    ingredientChecks: Map<Int, Boolean>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleComplete: (optionId: Int) -> Unit,
    onSelectOption: (optionId: Int) -> Unit,
    onToggleIngredient: (ingredientId: Int) -> Unit,
) {
    val effectiveOptionId = completion?.selectedOptionId
        ?: slotWithOptions.options.firstOrNull()?.option?.id
    val selectedOptionData = slotWithOptions.options
        .find { it.option.id == effectiveOptionId }
        ?: slotWithOptions.options.firstOrNull()
    val displayIngredients = selectedOptionData?.ingredients ?: emptyList()
    val isCompleted = displayIngredients.isNotEmpty() && displayIngredients.all { ingredientChecks[it.id] == true }
    val selectedIsDisabled = selectedOptionData?.isDisabled == true

    SurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isExpanded) Modifier.pressScale() else Modifier),
        radius = Dimens.RadiusXL,
        contentPadding = PaddingValues(0.dp),
    ) {
        // Collapsed header — tapping body expands
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(Dimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slotWithOptions.slot.timeLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = slotWithOptions.slot.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = selectedOptionData?.option?.description ?: "Tap to view options",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Dimens.ItemSpacing))

            // Quick-complete circle
            val circleFill by animateColorAsState(
                targetValue = if (isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = MotionTokens.standardSpec(),
                label = "quickCompleteFill",
            )
            val circleBorder by animateColorAsState(
                targetValue = if (isCompleted) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                animationSpec = MotionTokens.standardSpec(),
                label = "quickCompleteBorder",
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .then(if (!selectedIsDisabled) Modifier.pressScale() else Modifier)
                    .clip(CircleShape)
                    .background(circleFill)
                    .border(width = 1.5.dp, color = circleBorder, shape = CircleShape)
                    .then(
                        if (!selectedIsDisabled) {
                            Modifier.clickable { effectiveOptionId?.let { onToggleComplete(it) } }
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // Qualified: an enclosing RowScope would otherwise capture this call.
                androidx.compose.animation.AnimatedVisibility(
                    visible = isCompleted,
                    enter = scaleIn(MotionTokens.springBouncy()) + fadeIn(tween(MotionTokens.DurationFast)),
                    exit = scaleOut(tween(MotionTokens.DurationFast)) + fadeOut(tween(MotionTokens.DurationFast)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // Expanded content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(MotionTokens.enterSpec()) + fadeIn(tween(MotionTokens.DurationMedium)),
            exit = shrinkVertically(MotionTokens.exitSpec()) + fadeOut(tween(MotionTokens.DurationFast)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Dimens.ItemSpacing,
                        end = Dimens.ItemSpacing,
                        bottom = Dimens.ItemSpacing,
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.RadiusL))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(16.dp),
                ) {
                    MealOptionBodyPager(
                        options = slotWithOptions.options,
                        effectiveOptionId = effectiveOptionId,
                        ingredientChecks = ingredientChecks,
                        onSelectOption = onSelectOption,
                        onToggleIngredient = onToggleIngredient,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MealOptionBodyPager(
    options: List<MealOptionWithState>,
    effectiveOptionId: Int?,
    ingredientChecks: Map<Int, Boolean>,
    onSelectOption: (optionId: Int) -> Unit,
    onToggleIngredient: (ingredientId: Int) -> Unit,
) {
    if (options.isEmpty()) return

    // Ingredients header
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.ListAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "INGREDIENTS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(Modifier.height(Dimens.ItemSpacing))

    val initialIndex = (options.indexOfFirst { it.option.id == effectiveOptionId }).takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { options.size })
    var innerScrollEnabled by remember { mutableStateOf(true) }

    // When user swipes pages, persist selection (unless disabled).
    LaunchedEffect(options.size) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val opt = options.getOrNull(page) ?: return@collect
                if (!opt.isDisabled) onSelectOption(opt.option.id)
            }
    }

    // When user taps an option chip, move pager immediately.
    LaunchedEffect(effectiveOptionId, options.size) {
        val target = (options.indexOfFirst { it.option.id == effectiveOptionId }).takeIf { it >= 0 } ?: 0
        if (target != pagerState.currentPage) pagerState.scrollToPage(target)
    }

    // Option switcher (A/B/C) stays visible while paging ingredients.
    if (options.size > 1) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEachIndexed { index, optWithState ->
                val isSelected = index == pagerState.currentPage
                val isDisabled = optWithState.isDisabled
                val chipBg by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                    animationSpec = MotionTokens.standardSpec(),
                    label = "optionChipBg",
                )
                val chipText by animateColorAsState(
                    targetValue = when {
                        isDisabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        isSelected -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    animationSpec = MotionTokens.standardSpec(),
                    label = "optionChipText",
                )
                Box(
                    modifier = Modifier
                        .then(if (!isDisabled) Modifier.pressScale() else Modifier)
                        .clip(RoundedCornerShape(Dimens.RadiusS))
                        .background(chipBg)
                        .then(
                            if (!isDisabled) {
                                Modifier.clickable { onSelectOption(optWithState.option.id) }
                            } else {
                                Modifier
                            },
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = optWithState.option.shortLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textDecoration = if (isDisabled) TextDecoration.LineThrough else TextDecoration.None,
                        ),
                        color = chipText,
                    )
                }
            }
        }

        options.getOrNull(pagerState.currentPage)?.disabledReason?.let { reason ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(Dimens.ItemSpacing))
    }

    val boundaryHandoff = remember(options.size) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.Drag) return Offset.Zero

                val atStart = pagerState.currentPage == 0 && pagerState.currentPageOffsetFraction == 0f
                val atEnd = pagerState.currentPage == options.lastIndex && pagerState.currentPageOffsetFraction == 0f
                val draggingToPrevious = available.x > 0f
                val draggingToNext = available.x < 0f

                val shouldHandoffToParent =
                    (atStart && draggingToPrevious) || (atEnd && draggingToNext)

                innerScrollEnabled = !shouldHandoffToParent
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                innerScrollEnabled = true
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                innerScrollEnabled = true
                return Velocity.Zero
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        userScrollEnabled = innerScrollEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .nestedScroll(boundaryHandoff),
    ) { page ->
        val opt = options[page]

        // Each page shows the full meal option composition (all ingredients + note).
        Column(modifier = Modifier.fillMaxWidth()) {
            opt.ingredients.forEachIndexed { index, ingredient ->
                val isChecked = ingredientChecks[ingredient.id] == true
                val checkFill by animateColorAsState(
                    targetValue = if (isChecked) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = MotionTokens.standardSpec(),
                    label = "ingredientFill",
                )
                val checkBorder by animateColorAsState(
                    targetValue = if (isChecked) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                    animationSpec = MotionTokens.standardSpec(),
                    label = "ingredientBorder",
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .then(if (opt.isDisabled) Modifier else Modifier.pressScale())
                            .clip(CircleShape)
                            .background(checkFill)
                            .border(width = 1.5.dp, color = checkBorder, shape = CircleShape)
                            .then(
                                if (opt.isDisabled) {
                                    Modifier
                                } else {
                                    Modifier.clickable { onToggleIngredient(ingredient.id) }
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Qualified: an enclosing RowScope would otherwise capture this call.
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isChecked,
                            enter = scaleIn(MotionTokens.springBouncy()) + fadeIn(tween(MotionTokens.DurationFast)),
                            exit = scaleOut(tween(MotionTokens.DurationFast)) + fadeOut(tween(MotionTokens.DurationFast)),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(Dimens.ItemSpacing))
                    Text(
                        text = ingredient.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = ingredient.amount,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (index < opt.ingredients.lastIndex) {
                    HairlineDivider()
                }
            }

            opt.option.note?.let { note ->
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.RadiusS))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }

    if (options.size > 1) {
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(options.size) { i ->
                val selected = i == pagerState.currentPage
                val dotColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    },
                    animationSpec = MotionTokens.standardSpec(),
                    label = "pagerDotColor",
                )
                val dotSize by animateDpAsState(
                    targetValue = if (selected) 7.dp else 5.dp,
                    animationSpec = MotionTokens.standardSpec(),
                    label = "pagerDotSize",
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(dotColor),
                )
            }
        }
    }
}

@Composable
private fun WaterTrackerCard(waterMl: Int, targetMl: Int, onAdd: () -> Unit) {
    val water = waterColor()
    val progress = (waterMl.toFloat() / targetMl).coerceIn(0f, 1f)
    val fill by animateFloatAsState(
        targetValue = progress,
        animationSpec = MotionTokens.standardSpec(),
        label = "waterFill",
    )
    val entrance by rememberChartProgress()

    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "DAILY HYDRATION",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.ItemSpacing))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Bottom,
            ) {
                AnimatedStatText(text = "$waterMl")
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "/ $targetMl ml",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Box(
                modifier = Modifier
                    .pressScale()
                    .heightIn(min = Dimens.TouchTarget)
                    .clip(RoundedCornerShape(Dimens.RadiusM))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onAdd() }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+200 ml",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = water,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((fill * entrance).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(water),
            )
        }
    }
}

@Composable
private fun MacroTrackerCard(uiState: NutritionUiState) {
    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "DAILY MACROS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MacroRow(
                label = "Protein",
                consumed = uiState.consumedProtein,
                target = uiState.targetProtein,
                color = MaterialTheme.colorScheme.secondary,
            )
            MacroRow(
                label = "Carbs",
                consumed = uiState.consumedCarbs,
                target = uiState.targetCarbs,
                color = MaterialTheme.colorScheme.tertiary,
            )
            MacroRow(
                label = "Fats",
                consumed = uiState.consumedFat,
                target = uiState.targetFat,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun MacroRow(label: String, consumed: Float, target: Float, color: Color) {
    val fill by animateFloatAsState(
        targetValue = (consumed / target).coerceIn(0f, 1f),
        animationSpec = MotionTokens.standardSpec(),
        label = "macroFill",
    )
    val entrance by rememberChartProgress()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${consumed.toInt()}g",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = " / ${target.toInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((fill * entrance).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun EggBudgetCard(eggsUsed: Int, budget: Int) {
    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "WEEKLY EGG BUDGET",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.EggAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                AnimatedStatText(
                    text = "$eggsUsed / $budget",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(budget) { index ->
                val filled = index < eggsUsed
                val pillFill by animateColorAsState(
                    targetValue = if (filled) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = MotionTokens.standardSpec(),
                    label = "eggPillFill",
                )
                val pillBorder by animateColorAsState(
                    targetValue = if (filled) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                    animationSpec = MotionTokens.standardSpec(),
                    label = "eggPillBorder",
                )
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 36.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(pillFill)
                        .border(
                            width = 1.5.dp,
                            color = pillBorder,
                            shape = RoundedCornerShape(percent = 50),
                        ),
                )
            }
        }

        Spacer(Modifier.height(Dimens.ItemSpacing))

        Text(
            text = "Limit whole eggs to 4/week for LDL management.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MealPrepButton(onClick: () -> Unit) {
    AccentButton(
        text = "Meal Prep Protocol",
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}
