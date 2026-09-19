package com.ironlog.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ironlog.app.data.model.IngredientCheck
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface IngredientCheckDao {
    @Query("SELECT * FROM ingredient_check WHERE date = :date AND mealSlotId = :slotId")
    fun getChecksForMealOnDate(date: LocalDate, slotId: Int): Flow<List<IngredientCheck>>

    @Query("SELECT * FROM ingredient_check WHERE date = :date")
    fun getAllChecksForDate(date: LocalDate): Flow<List<IngredientCheck>>

    @Query("SELECT * FROM ingredient_check WHERE date = :date AND mealSlotId = :slotId AND ingredientId = :ingredientId")
    suspend fun getCheck(date: LocalDate, slotId: Int, ingredientId: Int): IngredientCheck?

    @Query("SELECT * FROM ingredient_check WHERE date = :date AND mealSlotId = :slotId")
    suspend fun getChecksForMealOnDateOnce(date: LocalDate, slotId: Int): List<IngredientCheck>

    @Query("SELECT * FROM ingredient_check WHERE date BETWEEN :start AND :end")
    fun getAllChecksForWeek(start: LocalDate, end: LocalDate): Flow<List<IngredientCheck>>

    @Query("DELETE FROM ingredient_check WHERE date = :date AND mealSlotId = :slotId")
    suspend fun clearForMealOnDate(date: LocalDate, slotId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(check: IngredientCheck)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(checks: List<IngredientCheck>)

    @Query("SELECT * FROM ingredient_check")
    fun getAll(): Flow<List<IngredientCheck>>

    @Query("DELETE FROM ingredient_check")
    suspend fun deleteAll()
}
