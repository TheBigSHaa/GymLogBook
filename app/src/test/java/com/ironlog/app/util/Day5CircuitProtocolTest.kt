package com.ironlog.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Day5CircuitProtocolTest {

    private val sections = listOf(
        "WARMUP", "WARMUP", "WARMUP",
        "CIRCUIT", "CIRCUIT", "CIRCUIT", "CIRCUIT", "CIRCUIT",
        "COOLDOWN", "COOLDOWN",
    )

    @Test
    fun `warmup is three minutes then waits for a press`() {
        val intervals = Day5CircuitProtocol.buildIntervals(sections)
        val warmup = intervals.take(3)

        assertTrue(warmup.all { it.phase == Day5CircuitProtocol.Phase.WORK })
        assertTrue(warmup.all { it.durationSeconds == 60 })
        assertEquals(listOf(0, 1, 2), warmup.map { it.exerciseIndex })
        assertFalse(warmup[0].waitForPressAfter)
        assertFalse(warmup[1].waitForPressAfter)
        assertTrue(warmup[2].waitForPressAfter)
    }

    @Test
    fun `each workout exercise finishes all four sets before the next`() {
        val intervals = Day5CircuitProtocol.buildIntervals(sections)
        val circuit = intervals.filter { it.exerciseIndex in 3..7 }
        val works = circuit.filter { it.phase == Day5CircuitProtocol.Phase.WORK }

        assertEquals(5 * 4, works.size)
        assertEquals(
            List(5) { ex -> List(4) { 3 + ex } }.flatten(),
            works.map { it.exerciseIndex },
        )
        assertEquals(
            List(5) { listOf(0, 1, 2, 3) }.flatten(),
            works.map { it.setIndex },
        )
        works.forEachIndexed { i, interval ->
            val lastSetOfExercise = i % 4 == 3
            assertEquals(lastSetOfExercise, interval.waitForPressAfter)
        }
    }

    @Test
    fun `rest is only between sets of the same exercise`() {
        val intervals = Day5CircuitProtocol.buildIntervals(sections)
        val pullUps = intervals.filter { it.exerciseIndex == 3 }

        assertEquals(
            listOf(40, 20, 40, 20, 40, 20, 40),
            pullUps.map { it.durationSeconds },
        )
        assertEquals(
            listOf(
                Day5CircuitProtocol.Phase.WORK,
                Day5CircuitProtocol.Phase.REST,
                Day5CircuitProtocol.Phase.WORK,
                Day5CircuitProtocol.Phase.REST,
                Day5CircuitProtocol.Phase.WORK,
                Day5CircuitProtocol.Phase.REST,
                Day5CircuitProtocol.Phase.WORK,
            ),
            pullUps.map { it.phase },
        )
        val firstRest = pullUps.first { it.phase == Day5CircuitProtocol.Phase.REST }
        assertEquals(3, firstRest.exerciseIndex)
        assertEquals(0, firstRest.setIndex)

        val rests = intervals.filter { it.phase == Day5CircuitProtocol.Phase.REST }
        assertEquals(5 * 3, rests.size)
        assertTrue(rests.none { it.waitForPressAfter })
        rests.forEachIndexed { i, rest ->
            assertEquals(
                "rest belongs to the set that just finished, not the next one",
                i % 3,
                rest.setIndex,
            )
        }
    }

    @Test
    fun `circuit rest duration follows the value from settings`() {
        val intervals = Day5CircuitProtocol.buildIntervals(sections, circuitRestSeconds = 35)
        val rests = intervals.filter { it.phase == Day5CircuitProtocol.Phase.REST }
        assertTrue(rests.isNotEmpty())
        assertTrue(rests.all { it.durationSeconds == 35 })
    }

    @Test
    fun `cooldown is two minutes and does not auto-start after the last workout set`() {
        val intervals = Day5CircuitProtocol.buildIntervals(sections)
        val cooldown = intervals.takeLast(2)
        assertTrue(cooldown.all { it.durationSeconds == 60 })
        assertEquals(listOf(8, 9), cooldown.map { it.exerciseIndex })
        assertTrue(intervals[intervals.size - 3].waitForPressAfter)
        assertTrue(cooldown.last().waitForPressAfter)
    }
}
