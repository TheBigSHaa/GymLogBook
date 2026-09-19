package com.ironlog.app.util

/**
 * Day 5 timed protocol from the program table:
 * warmup/cooldown 60s work with no rest; each workout exercise is 4 sets of
 * 40s work / 20s rest. The clock auto-chains **within** a block (all warmup,
 * all 4 sets of one exercise, all cooldown) and waits for a press between
 * exercises / sections.
 */
object Day5CircuitProtocol {
    const val WARMUP_WORK_SECONDS = 60
    const val CIRCUIT_WORK_SECONDS = 40
    const val CIRCUIT_REST_SECONDS = 20
    const val CIRCUIT_SETS = 4
    const val CIRCUIT_ROUNDS = CIRCUIT_SETS
    const val COOLDOWN_WORK_SECONDS = 60

    const val SECTION_WARMUP = "WARMUP"
    const val SECTION_CIRCUIT = "CIRCUIT"
    const val SECTION_COOLDOWN = "COOLDOWN"

    enum class Phase { WORK, REST }

    data class Interval(
        val durationSeconds: Int,
        val phase: Phase,
        /** Index into the day's exercise list. For REST, the set that just finished. */
        val exerciseIndex: Int,
        /** Working-set index within that exercise (0-based). For REST, the set that just finished. */
        val setIndex: Int,
        /** After this interval, pause and wait for the user to start the next block. */
        val waitForPressAfter: Boolean = false,
    )

    fun isDay5Section(section: String?): Boolean {
        val normalized = section?.uppercase() ?: return false
        return normalized == SECTION_WARMUP ||
            normalized == SECTION_CIRCUIT ||
            normalized == SECTION_COOLDOWN
    }

    /**
     * Warmup: 60s × each exercise, then wait.
     * Each circuit exercise: 40 / 20 / 40 / 20 / 40 / 20 / 40, then wait.
     * Cooldown: 60s × each exercise.
     */
    fun buildIntervals(
        sections: List<String?>,
        circuitRestSeconds: Int = CIRCUIT_REST_SECONDS,
    ): List<Interval> {
        val restSeconds = circuitRestSeconds.coerceIn(10, 600)
        val warmup = sections.withIndex().filter { it.value.equals(SECTION_WARMUP, ignoreCase = true) }
        val circuit = sections.withIndex().filter { it.value.equals(SECTION_CIRCUIT, ignoreCase = true) }
        val cooldown = sections.withIndex().filter { it.value.equals(SECTION_COOLDOWN, ignoreCase = true) }

        val out = ArrayList<Interval>(
            warmup.size + cooldown.size + circuit.size * (CIRCUIT_SETS * 2 - 1),
        )

        warmup.forEachIndexed { i, indexed ->
            out += Interval(
                durationSeconds = WARMUP_WORK_SECONDS,
                phase = Phase.WORK,
                exerciseIndex = indexed.index,
                setIndex = 0,
                waitForPressAfter = i == warmup.lastIndex,
            )
        }

        circuit.forEach { indexed ->
            repeat(CIRCUIT_SETS) { set ->
                val lastSet = set == CIRCUIT_SETS - 1
                out += Interval(
                    durationSeconds = CIRCUIT_WORK_SECONDS,
                    phase = Phase.WORK,
                    exerciseIndex = indexed.index,
                    setIndex = set,
                    waitForPressAfter = lastSet,
                )
                if (!lastSet) {
                    out += Interval(
                        durationSeconds = restSeconds,
                        phase = Phase.REST,
                        exerciseIndex = indexed.index,
                        setIndex = set,
                    )
                }
            }
        }

        cooldown.forEachIndexed { i, indexed ->
            out += Interval(
                durationSeconds = COOLDOWN_WORK_SECONDS,
                phase = Phase.WORK,
                exerciseIndex = indexed.index,
                setIndex = 0,
                waitForPressAfter = i == cooldown.lastIndex,
            )
        }

        return out
    }
}
