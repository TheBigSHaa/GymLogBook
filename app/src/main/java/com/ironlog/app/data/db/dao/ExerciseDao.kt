package com.ironlog.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ironlog.app.data.model.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM Exercise WHERE workoutDayId = :dayId ORDER BY orderIndex ASC")
    fun getExercisesForDay(dayId: Int): Flow<List<Exercise>>

    @Query("SELECT * FROM Exercise WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Exercise?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<Exercise>)

    @androidx.room.Update
    suspend fun updateAll(exercises: List<Exercise>)
}

