package com.ironlog.app.data.repository

import com.ironlog.app.data.db.dao.ScheduledWorkoutDao
import com.ironlog.app.data.model.ScheduledWorkout
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val scheduledWorkoutDao: ScheduledWorkoutDao,
) {
    fun scheduledForDate(date: LocalDate): Flow<ScheduledWorkout?> = scheduledWorkoutDao.getForDate(date)

    fun scheduledForRange(start: LocalDate, end: LocalDate): Flow<List<ScheduledWorkout>> =
        scheduledWorkoutDao.getForDateRange(start, end)

    suspend fun upsert(item: ScheduledWorkout): Long = scheduledWorkoutDao.upsert(item)

    suspend fun deleteForDate(date: LocalDate) = scheduledWorkoutDao.deleteForDate(date)
}

