package com.ironlog.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ironlog.app.data.model.MealIngredient
import kotlinx.coroutines.flow.Flow

@Dao
interface MealIngredientDao {
    @Query("SELECT * FROM meal_ingredient WHERE mealOptionId = :optionId ORDER BY orderIndex")
    fun getIngredientsForOption(optionId: Int): Flow<List<MealIngredient>>

    @Query("SELECT * FROM meal_ingredient WHERE mealOptionId = :optionId ORDER BY orderIndex")
    suspend fun getIngredientsForOptionOnce(optionId: Int): List<MealIngredient>

    @Query("SELECT * FROM meal_ingredient ORDER BY mealOptionId, orderIndex")
    fun getAll(): Flow<List<MealIngredient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingredients: List<MealIngredient>)
}
