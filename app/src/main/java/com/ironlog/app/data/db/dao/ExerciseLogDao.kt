package com.ironlog.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ironlog.app.data.model.ExerciseLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseLogDao {
    @Query("SELECT * FROM ExerciseLog WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    fun getLogsForSession(sessionId: Long): Flow<List<ExerciseLog>>

    @Query("SELECT * FROM ExerciseLog WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    suspend fun getLogsForSessionOnce(sessionId: Long): List<ExerciseLog>

    @Query(
        """
        SELECT el.id FROM ExerciseLog el
        INNER JOIN WorkoutSession ws ON ws.id = el.sessionId
        WHERE el.exerciseId = :exerciseId
            AND ws.completed = 1
            AND ws.id != :excludeSessionId
        ORDER BY ws.date DESC, ws.id DESC, el.id DESC
        LIMIT :limit
        """,
    )
    suspend fun getRecentLogIdsForExercise(exerciseId: Int, excludeSessionId: Long, limit: Int): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<ExerciseLog>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ExerciseLog): Long

    @Query("SELECT * FROM ExerciseLog")
    fun getAll(): Flow<List<ExerciseLog>>

    @Query("DELETE FROM ExerciseLog")
    suspend fun deleteAll()
}

