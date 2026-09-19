package com.ironlog.app.util

import com.ironlog.app.data.model.SetLog

object VolumeCalculator {
    fun totalVolume(sets: List<SetLog>): Float {
        return sets
            .asSequence()
            .filter { it.completed && !it.isWarmup }
            .sumOf { (it.weight * it.reps).toDouble() }
            .toFloat()
    }
}

