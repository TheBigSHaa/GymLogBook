package com.ironlog.app.ui.workout

/**
 * Insert a new warm-up set BEFORE the first working set — warm-ups always lead
 * the list (after any existing warm-ups), never trail it.
 */
internal fun insertWarmupSet(sets: List<EditableSet>): List<EditableSet> {
    val insertAt = sets.indexOfFirst { !it.isWarmup }.let { if (it == -1) sets.size else it }
    val list = sets.toMutableList()
    list.add(
        insertAt,
        EditableSet(
            setNumber = 0,
            weight = 0f,
            reps = 0,
            completed = false,
            isWarmup = true,
            rpe = null,
        ),
    )
    return normalizeSetNumbers(list)
}

/**
 * Re-number ALL sets by list position (1-based). SetLog has a UNIQUE index on
 * (exerciseLogId, setNumber), so positional numbering guarantees warm-ups and
 * working sets never collide on save and persist in display order.
 */
internal fun normalizeSetNumbers(sets: List<EditableSet>): List<EditableSet> =
    sets.mapIndexed { index, set ->
        if (set.setNumber == index + 1) set else set.copy(setNumber = index + 1)
    }
