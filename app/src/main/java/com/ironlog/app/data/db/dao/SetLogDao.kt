package com.ironlog.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ironlog.app.data.model.SetLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SetLogDao {
    @Query("SELECT * FROM SetLog WHERE exerciseLogId = :exerciseLogId ORDER BY setNumber ASC")
    fun getSetsForExerciseLog(exerciseLogId: Long): Flow<List<SetLog>>

    @Query("SELECT * FROM SetLog WHERE exerciseLogId = :exerciseLogId ORDER BY setNumber ASC")
    suspend fun getSetsForExerciseLogOnce(exerciseLogId: Long): List<SetLog>

    @Query(
        """
        SELECT s.* FROM SetLog s
        INNER JOIN ExerciseLog el ON el.id = s.exerciseLogId
        INNER JOIN WorkoutSession ws ON ws.id = el.sessionId
        WHERE el.exerciseId = :exerciseId AND ws.completed = 1
        ORDER BY ws.date DESC, ws.id DESC, el.id DESC, s.setNumber ASC
        """,
    )
    suspend fun getLatestSetsForExercise(exerciseId: Int): List<SetLog>

    @Query(
        """
        SELECT s.* FROM SetLog s
        INNER JOIN ExerciseLog el ON el.id = s.exerciseLogId
        INNER JOIN WorkoutSession ws ON ws.id = el.sessionId
        WHERE el.exerciseId = :exerciseId AND ws.completed = 1
        ORDER BY ws.date DESC, ws.id DESC, el.id DESC, s.setNumber ASC
        """,
    )
    suspend fun getAllCompletedSetsForExercise(exerciseId: Int): List<SetLog>

    /**
     * All sets from the SINGLE most recent completed session that logged this exercise,
     * excluding [excludeSessionId] (so editing a saved session compares against the one before it).
     */
    @Query(
        """
        SELECT s.* FROM SetLog s
        WHERE s.exerciseLogId = (
            SELECT el.id FROM ExerciseLog el
            INNER JOIN WorkoutSession ws ON ws.id = el.sessionId
            WHERE el.exerciseId = :exerciseId
                AND ws.completed = 1
                AND ws.id != :excludeSessionId
            ORDER BY ws.date DESC, ws.id DESC, el.id DESC
            LIMIT 1
        )
        ORDER BY s.setNumber ASC
        """,
    )
    suspend fun getSetsFromLatestSession(exerciseId: Int, excludeSessionId: Long): List<SetLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sets: List<SetLog>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: SetLog): Long

    @Query("SELECT * FROM SetLog")
    fun getAll(): Flow<List<SetLog>>

    @Query("DELETE FROM SetLog")
    suspend fun deleteAll()
}

