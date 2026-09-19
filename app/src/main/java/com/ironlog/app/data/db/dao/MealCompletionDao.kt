package com.ironlog.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ironlog.app.data.model.MealCompletion
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MealCompletionDao {
    @Query("SELECT * FROM meal_completion WHERE date = :date")
    fun getCompletionsForDate(date: LocalDate): Flow<List<MealCompletion>>

    @Query("SELECT * FROM meal_completion WHERE date = :date AND mealSlotId = :slotId")
    suspend fun getCompletion(date: LocalDate, slotId: Int): MealCompletion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(completion: MealCompletion)

    @Query("SELECT * FROM meal_completion WHERE date BETWEEN :start AND :end")
    fun getCompletionsForRange(start: LocalDate, end: LocalDate): Flow<List<MealCompletion>>

    @Query("SELECT * FROM meal_completion")
    fun getAll(): Flow<List<MealCompletion>>

    @Query("DELETE FROM meal_completion")
    suspend fun deleteAll()
}
