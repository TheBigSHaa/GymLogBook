package com.ironlog.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ironlog.app.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM WorkoutSession WHERE workoutDayId = :dayId ORDER BY date DESC, id DESC")
    fun getSessionsForDay(dayId: Int): Flow<List<WorkoutSession>>

    @Query(
        """
        SELECT * FROM WorkoutSession
        WHERE workoutDayId = :dayId
        ORDER BY date DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestSessionForDay(dayId: Int): WorkoutSession?

    @Query("SELECT * FROM WorkoutSession WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WorkoutSession?

    @Query("SELECT * FROM WorkoutSession WHERE completed = 1 ORDER BY date DESC, id DESC")
    fun getAllCompleted(): Flow<List<WorkoutSession>>

    @Query(
        """
        SELECT * FROM WorkoutSession
        WHERE completed = 1 AND date BETWEEN :start AND :end
        ORDER BY date DESC, id DESC
        """,
    )
    fun getCompletedInRange(start: LocalDate, end: LocalDate): Flow<List<WorkoutSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSession): Long

    @Update
    suspend fun update(session: WorkoutSession)

    @Query("DELETE FROM WorkoutSession WHERE id = :sessionId")
    suspend fun deleteById(sessionId: Long)

    @Query("SELECT * FROM WorkoutSession")
    fun getAll(): Flow<List<WorkoutSession>>

    @Query("DELETE FROM WorkoutSession")
    suspend fun deleteAll()
}

