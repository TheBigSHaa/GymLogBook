package com.ironlog.app.data.repository

import com.ironlog.app.data.db.dao.ExerciseDao
import com.ironlog.app.data.db.dao.ExerciseLogDao
import com.ironlog.app.data.db.dao.SetLogDao
import com.ironlog.app.data.db.dao.WorkoutDayDao
import com.ironlog.app.data.db.dao.WorkoutSessionDao
import com.ironlog.app.data.model.Exercise
import com.ironlog.app.data.model.ExerciseLog
import com.ironlog.app.data.model.SetLog
import com.ironlog.app.data.model.WorkoutDay
import com.ironlog.app.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepository @Inject constructor(
    private val workoutDayDao: WorkoutDayDao,
    private val exerciseDao: ExerciseDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val setLogDao: SetLogDao,
) {
    fun workoutDays(): Flow<List<WorkoutDay>> = workoutDayDao.getAll()

    suspend fun workoutDayById(id: Int): WorkoutDay? = workoutDayDao.getById(id)

    fun exercisesForDay(dayId: Int): Flow<List<Exercise>> = exerciseDao.getExercisesForDay(dayId)

    suspend fun exerciseById(id: Int): Exercise? = exerciseDao.getById(id)

    fun sessionsForDay(dayId: Int): Flow<List<WorkoutSession>> = workoutSessionDao.getSessionsForDay(dayId)

    suspend fun updateExercises(exercises: List<Exercise>) = exerciseDao.updateAll(exercises)

    suspend fun latestSessionForDay(dayId: Int): WorkoutSession? = workoutSessionDao.getLatestSessionForDay(dayId)

    suspend fun sessionById(id: Long): WorkoutSession? = workoutSessionDao.getById(id)

    fun completedSessions(): Flow<List<WorkoutSession>> = workoutSessionDao.getAllCompleted()

    fun completedSessionsInRange(start: LocalDate, end: LocalDate): Flow<List<WorkoutSession>> =
        workoutSessionDao.getCompletedInRange(start, end)

    suspend fun createSession(session: WorkoutSession): Long = workoutSessionDao.insert(session)

    suspend fun updateSession(session: WorkoutSession) = workoutSessionDao.update(session)

    suspend fun deleteSession(sessionId: Long) = workoutSessionDao.deleteById(sessionId)

    fun logsForSession(sessionId: Long): Flow<List<ExerciseLog>> = exerciseLogDao.getLogsForSession(sessionId)

    suspend fun logsForSessionOnce(sessionId: Long): List<ExerciseLog> = exerciseLogDao.getLogsForSessionOnce(sessionId)

    suspend fun createExerciseLog(log: ExerciseLog): Long = exerciseLogDao.insert(log)

    suspend fun createSet(set: SetLog): Long = setLogDao.insert(set)

    fun setsForExerciseLog(exerciseLogId: Long): Flow<List<SetLog>> = setLogDao.getSetsForExerciseLog(exerciseLogId)

    suspend fun setsForExerciseLogOnce(exerciseLogId: Long): List<SetLog> = setLogDao.getSetsForExerciseLogOnce(exerciseLogId)

    suspend fun latestSetsForExercise(exerciseId: Int): List<SetLog> = setLogDao.getLatestSetsForExercise(exerciseId)

    /** Sets from the single most recent completed session for [exerciseId], excluding [excludeSessionId]. */
    suspend fun setsFromLatestSession(exerciseId: Int, excludeSessionId: Long): List<SetLog> =
        setLogDao.getSetsFromLatestSession(exerciseId, excludeSessionId)

    /**
     * Working-set logs from the most recent completed sessions for [exerciseId],
     * newest first. Used by dumbbell progression (compare last two Day-N sessions).
     */
    suspend fun recentSessionSetsForExercise(
        exerciseId: Int,
        excludeSessionId: Long,
        sessionCount: Int = 2,
    ): List<List<SetLog>> {
        val logIds = exerciseLogDao.getRecentLogIdsForExercise(exerciseId, excludeSessionId, sessionCount)
        return logIds.map { setLogDao.getSetsForExerciseLogOnce(it) }
    }

    suspend fun allCompletedSetsForExercise(exerciseId: Int): List<SetLog> =
        setLogDao.getAllCompletedSetsForExercise(exerciseId)
}

