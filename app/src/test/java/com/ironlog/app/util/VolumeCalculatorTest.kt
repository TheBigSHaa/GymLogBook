package com.ironlog.app.util

import com.ironlog.app.data.model.SetLog
import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeCalculatorTest {

    private fun set(weight: Float, reps: Int, completed: Boolean = true, warmup: Boolean = false): SetLog =
        SetLog(
            id = 0L,
            exerciseLogId = 0L,
            setNumber = 1,
            weight = weight,
            reps = reps,
            completed = completed,
            isWarmup = warmup,
        )

    @Test
    fun `empty list returns zero`() {
        assertEquals(0f, VolumeCalculator.totalVolume(emptyList()), 0.001f)
    }

    @Test
    fun `completed working sets sum`() {
        val sets = listOf(set(100f, 5), set(100f, 5), set(90f, 8))
        // 500 + 500 + 720 = 1720
        assertEquals(1720f, VolumeCalculator.totalVolume(sets), 0.001f)
    }

    @Test
    fun `warmup sets are excluded`() {
        val sets = listOf(set(40f, 10, warmup = true), set(100f, 5))
        assertEquals(500f, VolumeCalculator.totalVolume(sets), 0.001f)
    }

    @Test
    fun `incomplete sets are excluded`() {
        val sets = listOf(set(100f, 5, completed = false), set(100f, 5))
        assertEquals(500f, VolumeCalculator.totalVolume(sets), 0.001f)
    }
}
