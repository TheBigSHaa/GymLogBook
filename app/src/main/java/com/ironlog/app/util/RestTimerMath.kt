package com.ironlog.app.util

/**
 * Pure epoch-based rest-timer math. The countdown has a single source of truth —
 * the end timestamp — so ±15s adjustments move the clock by exactly 15 seconds
 * regardless of tick timing, process death, or DataStore round-trips.
 */
object RestTimerMath {
    /**
     * Seconds left until [endEpochMs], rounded UP so the displayed value is stable:
     * right after starting a 90s timer this returns 90, and adding 15_000ms to the
     * end always increases the result by exactly 15.
     */
    fun remainingSeconds(endEpochMs: Long, nowEpochMs: Long): Int {
        if (endEpochMs <= nowEpochMs) return 0
        return ((endEpochMs - nowEpochMs + 999L) / 1000L).toInt()
    }

    /** Shift the end timestamp by [deltaSeconds], never landing in the past. */
    fun adjustedEndEpochMs(endEpochMs: Long, nowEpochMs: Long, deltaSeconds: Int): Long {
        return (endEpochMs + deltaSeconds * 1000L).coerceAtLeast(nowEpochMs)
    }

    /**
     * Wall-clock rest actually taken between [startedAtEpochMs] and [endedAtEpochMs],
     * rounded to the NEAREST second and clamped to a sane 0..3600 range so a
     * forgotten timer can't record hours of "rest".
     */
    fun elapsedRestSeconds(startedAtEpochMs: Long, endedAtEpochMs: Long): Int {
        val delta = endedAtEpochMs - startedAtEpochMs
        if (delta <= 0L) return 0
        return ((delta + 500L) / 1000L).toInt().coerceIn(0, 3600)
    }
}
