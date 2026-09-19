package com.ironlog.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ironlog.app.data.model.ScheduledWorkout
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ScheduledWorkoutDao {
    @Query("SELECT * FROM ScheduledWorkout WHERE date = :date LIMIT 1")
    fun getForDate(date: LocalDate): Flow<ScheduledWorkout?>

    @Query("SELECT * FROM ScheduledWorkout WHERE date >= :start AND date <= :end ORDER BY date ASC")
    fun getForDateRange(start: LocalDate, end: LocalDate): Flow<List<ScheduledWorkout>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ScheduledWorkout): Long

    @Query("DELETE FROM ScheduledWorkout WHERE date = :date")
    suspend fun deleteForDate(date: LocalDate)

    @Query("SELECT * FROM ScheduledWorkout")
    fun getAll(): Flow<List<ScheduledWorkout>>

    @Query("DELETE FROM ScheduledWorkout")
    suspend fun deleteAll()
}

