package com.ironlog.app.ui.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task: "when user adds a warm-up set it will appear at the beginning and not
 * the end of the exercise". Also guards the positional set numbering that keeps
 * the (exerciseLogId, setNumber) UNIQUE constraint safe on save.
 */
class SetOpsTest {

    private fun working(number: Int, weight: Float = 60f, reps: Int = 5) =
        EditableSet(setNumber = number, weight = weight, reps = reps, completed = false, isWarmup = false, rpe = null)

    @Test
    fun `warmup is inserted before the first working set`() {
        val sets = listOf(working(1), working(2), working(3))

        val result = insertWarmupSet(sets)

        assertEquals(4, result.size)
        assertTrue(result.first().isWarmup)
        assertEquals(listOf(true, false, false, false), result.map { it.isWarmup })
    }

    @Test
    fun `second warmup goes after the first warmup but before working sets`() {
        val once = insertWarmupSet(listOf(working(1), working(2)))
        val twice = insertWarmupSet(once)

        assertEquals(listOf(true, true, false, false), twice.map { it.isWarmup })
    }

    @Test
    fun `set numbers are positional and unique after warmup insert`() {
        val result = insertWarmupSet(insertWarmupSet(listOf(working(1), working(2), working(3))))

        assertEquals((1..result.size).toList(), result.map { it.setNumber })
        assertEquals(result.size, result.map { it.setNumber }.distinct().size)
    }

    @Test
    fun `working set data is preserved when a warmup is inserted`() {
        val sets = listOf(working(1, weight = 80f, reps = 5), working(2, weight = 82.5f, reps = 3))

        val result = insertWarmupSet(sets)
        val workingAfter = result.filter { !it.isWarmup }

        assertEquals(listOf(80f, 82.5f), workingAfter.map { it.weight })
        assertEquals(listOf(5, 3), workingAfter.map { it.reps })
    }

    @Test
    fun `insert into empty list creates a single warmup`() {
        val result = insertWarmupSet(emptyList())
        assertEquals(1, result.size)
        assertTrue(result.first().isWarmup)
        assertEquals(1, result.first().setNumber)
    }

    @Test
    fun `normalize renumbers by position`() {
        val messy = listOf(working(7), working(2), working(9))
        assertEquals(listOf(1, 2, 3), normalizeSetNumbers(messy).map { it.setNumber })
    }
}
