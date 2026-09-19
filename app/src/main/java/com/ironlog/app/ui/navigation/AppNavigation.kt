package com.ironlog.app.ui.navigation

import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ironlog.app.ui.log.LogScreen
import com.ironlog.app.ui.nutrition.NutritionScreen
import com.ironlog.app.ui.plan.PlanScreen
import com.ironlog.app.ui.progress.ProgressScreen
import com.ironlog.app.ui.settings.SettingsScreen
import com.ironlog.app.ui.theme.Dimens
import com.ironlog.app.ui.theme.MotionTokens
import com.ironlog.app.ui.today.TodayScreen
import com.ironlog.app.ui.workout.ActiveWorkoutScreen
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

private object Routes {
    const val Home = "home"
    const val ActiveWorkout = "activeWorkout"
    const val ActiveWorkoutWithArg = "activeWorkout/{workoutDayId}?sessionId={sessionId}"
    const val Settings = "settings"
}

@Composable
fun AppNavigation(startIntent: Intent? = null) {
    val navController = rememberNavController()

    LaunchedEffect(startIntent) {
        val openActive = startIntent?.getBooleanExtra("open_active_workout", false) == true
        val dayId = startIntent?.getIntExtra("workout_day_id", -1) ?: -1
        if (openActive && dayId > 0) {
            navController.navigate("${Routes.ActiveWorkout}/$dayId") {
                launchSingleTop = true
                popUpTo(Routes.Home) { inclusive = false }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Home,
        enterTransition = { fadeIn(tween(MotionTokens.DurationMedium, easing = MotionTokens.EasingStandard)) },
        exitTransition = { fadeOut(tween(MotionTokens.DurationFast, easing = MotionTokens.EasingEmphasizedAccelerate)) },
    ) {
        composable(
            route = Routes.Home,
            // Home recedes slightly when a detail screen covers it, and scales
            // back up on return — a depth cue rather than a flat crossfade.
            exitTransition = {
                fadeOut(tween(MotionTokens.DurationMedium)) +
                    scaleOut(targetScale = 0.96f, animationSpec = tween(MotionTokens.DurationMedium, easing = MotionTokens.EasingStandard))
            },
            popEnterTransition = {
                fadeIn(tween(MotionTokens.DurationMedium)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(MotionTokens.DurationSlow, easing = MotionTokens.EasingEmphasizedDecelerate))
            },
        ) {
            HomeScreen(
                onStartWorkout = { dayId -> navController.navigate("${Routes.ActiveWorkout}/$dayId") },
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onEditSession = { dayId, sessionId ->
                    navController.navigate("${Routes.ActiveWorkout}/$dayId?sessionId=$sessionId")
                },
            )
        }

        composable(
            route = Routes.ActiveWorkoutWithArg,
            arguments = listOf(
                navArgument("workoutDayId") { type = NavType.IntType },
                navArgument("sessionId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
            // A workout session rises from the bottom like a committed action,
            // and drops away when abandoned/finished.
            enterTransition = {
                slideInVertically(
                    animationSpec = tween(MotionTokens.DurationSlow, easing = MotionTokens.EasingEmphasizedDecelerate),
                    initialOffsetY = { it },
                ) + fadeIn(tween(MotionTokens.DurationMedium))
            },
            popExitTransition = {
                slideOutVertically(
                    animationSpec = tween(MotionTokens.DurationMedium, easing = MotionTokens.EasingEmphasizedAccelerate),
                    targetOffsetY = { it },
                ) + fadeOut(tween(MotionTokens.DurationMedium))
            },
        ) {
            ActiveWorkoutScreen(
                onNavigateBackToToday = {
                    navController.popBackStack(Routes.Home, inclusive = false)
                },
            )
        }

        composable(
            route = Routes.Settings,
            // Settings is lateral navigation — shared-axis slide from the right.
            enterTransition = {
                slideInHorizontally(
                    animationSpec = tween(MotionTokens.DurationSlow, easing = MotionTokens.EasingEmphasizedDecelerate),
                    initialOffsetX = { it / 3 },
                ) + fadeIn(tween(MotionTokens.DurationMedium))
            },
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(MotionTokens.DurationMedium, easing = MotionTokens.EasingEmphasizedAccelerate),
                    targetOffsetX = { it / 3 },
                ) + fadeOut(tween(MotionTokens.DurationFast))
            },
        ) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onStartWorkout: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onEditSession: (workoutDayId: Int, sessionId: Long) -> Unit = { _, _ -> },
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            // Subtle depth parallax: the outgoing page dims and recedes while
            // the incoming one settles to full presence.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val offset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                            .absoluteValue
                            .coerceIn(0f, 1f)
                        alpha = 1f - 0.25f * offset
                        val scale = 1f - 0.04f * offset
                        scaleX = scale
                        scaleY = scale
                    },
            ) {
                when (page) {
                    0 -> TodayScreen(
                        onStartWorkout = onStartWorkout,
                        onOpenSettings = onOpenSettings,
                    )
                    1 -> LogScreen()
                    2 -> NutritionScreen()
                    3 -> PlanScreen(
                        onStartWorkout = onStartWorkout,
                        onEditSession = onEditSession,
                    )
                    4 -> ProgressScreen(onOpenSettings = onOpenSettings)
                }
            }
        }

        FloatingBottomNav(
            currentPage = pagerState.currentPage,
            onNavigate = { targetPage ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(targetPage)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

/**
 * Floating pill navigation bar. The selected item carries a tonal pill that
 * animates in, the icon swaps filled/outlined with a spring scale, and colors
 * cross-fade on the standard motion curve.
 */
@Composable
private fun FloatingBottomNav(
    currentPage: Int,
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        BottomNavItem(0, "TODAY", Icons.Outlined.EventNote, Icons.Filled.EventNote),
        BottomNavItem(1, "LOG", Icons.Outlined.ViewList, Icons.Filled.ViewList),
        BottomNavItem(2, "FUEL", Icons.Outlined.Restaurant, Icons.Filled.Restaurant),
        BottomNavItem(3, "PLAN", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
        BottomNavItem(4, "STATS", Icons.Outlined.Equalizer, Icons.Filled.Equalizer),
    )
    val barShape = RoundedCornerShape(26.dp)

    Row(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .fillMaxWidth()
            .clip(barShape)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.98f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = Dimens.HairlineAlpha),
                barShape,
            )
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val selected = currentPage == item.pageIndex

            val iconScale by animateFloatAsState(
                targetValue = if (selected) 1.12f else 1f,
                animationSpec = MotionTokens.springBouncy(),
                label = "navIconScale",
            )
            val contentColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                },
                animationSpec = MotionTokens.standardSpec(),
                label = "navColor",
            )
            val pillAlpha by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = MotionTokens.standardSpec(),
                label = "navPill",
            )
            val pillPadding by animateDpAsState(
                targetValue = if (selected) 10.dp else 6.dp,
                animationSpec = MotionTokens.standardSpec(),
                label = "navPillPadding",
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .graphicsLayer { /* pill drawn via background alpha below */ }
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f * pillAlpha),
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onNavigate(item.pageIndex) }
                        .padding(horizontal = pillPadding, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                        tint = contentColor,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                    )
                    Text(
                        text = item.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 8.5.sp,
                        ),
                        color = contentColor,
                    )
                }
            }
        }
    }
}

private data class BottomNavItem(
    val pageIndex: Int,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
)
