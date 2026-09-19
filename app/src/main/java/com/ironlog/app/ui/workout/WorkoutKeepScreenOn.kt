package com.ironlog.app.ui.workout

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import com.ironlog.app.util.SettingsStore.WorkoutScreenTimeout
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Keeps the screen awake only while the active-workout screen is visible.
 * Timed options reset on user input; leaving the workout restores the device default.
 */
@Composable
fun Modifier.workoutKeepScreenOn(timeout: WorkoutScreenTimeout): Modifier {
    val view = LocalView.current
    val activityFlow = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }

    LaunchedEffect(timeout) {
        val window = view.context.findActivity()?.window ?: return@LaunchedEffect
        try {
            when (timeout) {
                WorkoutScreenTimeout.OFF -> {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                WorkoutScreenTimeout.ALWAYS -> {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    awaitCancellation()
                }
                else -> {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    while (true) {
                        val touchedBeforeTimeout = withTimeoutOrNull(timeout.idleMillis) {
                            activityFlow.first()
                        }
                        if (touchedBeforeTimeout == null) {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            activityFlow.first()
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                }
            }
        } finally {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val trackTouches = timeout != WorkoutScreenTimeout.OFF &&
        timeout != WorkoutScreenTimeout.ALWAYS
    return if (!trackTouches) {
        this
    } else {
        this.pointerInput(timeout) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial)
                    activityFlow.tryEmit(Unit)
                }
            }
        }
    }
}

private val WorkoutScreenTimeout.idleMillis: Long
    get() = when (this) {
        WorkoutScreenTimeout.SEC_15 -> 15_000L
        WorkoutScreenTimeout.SEC_30 -> 30_000L
        WorkoutScreenTimeout.MIN_1 -> 60_000L
        WorkoutScreenTimeout.MIN_2 -> 120_000L
        WorkoutScreenTimeout.OFF, WorkoutScreenTimeout.ALWAYS -> 0L
    }

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
