package com.ironlog.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ironlog.app.data.model.WorkoutDay
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDayDao {
    @Query("SELECT * FROM WorkoutDay ORDER BY id ASC")
    fun getAll(): Flow<List<WorkoutDay>>

    @Query("SELECT * FROM WorkoutDay WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): WorkoutDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(days: List<WorkoutDay>)
}

