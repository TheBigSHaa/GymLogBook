package com.ironlog.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ironlog.app.data.model.MealOption
import kotlinx.coroutines.flow.Flow

@Dao
interface MealOptionDao {
    @Query("SELECT * FROM meal_option WHERE mealSlotId = :slotId ORDER BY orderIndex")
    fun getOptionsForSlot(slotId: Int): Flow<List<MealOption>>

    @Query("SELECT * FROM meal_option ORDER BY mealSlotId, orderIndex")
    fun getAll(): Flow<List<MealOption>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(options: List<MealOption>)
}
