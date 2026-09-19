package com.ironlog.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Task: "clock is synchronized to the minus and plus 15 secs (that it actually
 * adds and removes 15 sec and not 20)". The countdown derives from an end
 * timestamp, so a ±15s adjustment must shift the displayed value by exactly 15.
 */
class RestTimerMathTest {

    @Test
    fun `remaining is exact right after starting a timer`() {
        val now = 1_000_000L
        val end = now + 90_000L
        assertEquals(90, RestTimerMath.remainingSeconds(end, now))
    }

    @Test
    fun `remaining rounds up mid-second so display is stable`() {
        val now = 1_000_000L
        val end = now + 47_300L // 47.3s left
        assertEquals(48, RestTimerMath.remainingSeconds(end, now))
    }

    @Test
    fun `remaining is zero at or past the end`() {
        val now = 1_000_000L
        assertEquals(0, RestTimerMath.remainingSeconds(now, now))
        assertEquals(0, RestTimerMath.remainingSeconds(now - 5_000L, now))
    }

    @Test
    fun `plus 15 adds exactly 15 displayed seconds`() {
        val now = 1_000_000L
        val end = now + 47_300L
        val before = RestTimerMath.remainingSeconds(end, now)

        val adjusted = RestTimerMath.adjustedEndEpochMs(end, now, +15)
        val after = RestTimerMath.remainingSeconds(adjusted, now)

        assertEquals(before + 15, after)
    }

    @Test
    fun `minus 15 removes exactly 15 displayed seconds`() {
        val now = 1_000_000L
        val end = now + 60_000L
        val before = RestTimerMath.remainingSeconds(end, now)

        val adjusted = RestTimerMath.adjustedEndEpochMs(end, now, -15)
        val after = RestTimerMath.remainingSeconds(adjusted, now)

        assertEquals(before - 15, after)
    }

    @Test
    fun `repeated plus 15 keeps exact multiples`() {
        val now = 1_000_000L
        var end = now + 30_000L
        repeat(4) { end = RestTimerMath.adjustedEndEpochMs(end, now, +15) }
        assertEquals(30 + 60, RestTimerMath.remainingSeconds(end, now))
    }

    @Test
    fun `minus below zero clamps to now`() {
        val now = 1_000_000L
        val end = now + 10_000L
        val adjusted = RestTimerMath.adjustedEndEpochMs(end, now, -15)
        assertEquals(now, adjusted)
        assertEquals(0, RestTimerMath.remainingSeconds(adjusted, now))
    }

    // ── elapsedRestSeconds — rest actually taken after a set ────────────────

    @Test
    fun `elapsed rest of exact seconds is exact`() {
        val start = 1_000_000L
        assertEquals(90, RestTimerMath.elapsedRestSeconds(start, start + 90_000L))
    }

    @Test
    fun `elapsed rest rounds to nearest second`() {
        val start = 1_000_000L
        // 89.4s → 89, 89.5s → 90, 89.6s → 90
        assertEquals(89, RestTimerMath.elapsedRestSeconds(start, start + 89_400L))
        assertEquals(90, RestTimerMath.elapsedRestSeconds(start, start + 89_500L))
        assertEquals(90, RestTimerMath.elapsedRestSeconds(start, start + 89_600L))
    }

    @Test
    fun `elapsed rest sub-half-second rounds down to zero`() {
        val start = 1_000_000L
        assertEquals(0, RestTimerMath.elapsedRestSeconds(start, start + 499L))
        assertEquals(1, RestTimerMath.elapsedRestSeconds(start, start + 500L))
    }

    @Test
    fun `elapsed rest is zero for same instant or clock skew backwards`() {
        val start = 1_000_000L
        assertEquals(0, RestTimerMath.elapsedRestSeconds(start, start))
        assertEquals(0, RestTimerMath.elapsedRestSeconds(start, start - 5_000L))
    }

    @Test
    fun `elapsed rest is capped at one hour`() {
        val start = 1_000_000L
        val fourHoursLater = start + 4L * 60L * 60L * 1000L
        assertEquals(3600, RestTimerMath.elapsedRestSeconds(start, fourHoursLater))
    }
}
