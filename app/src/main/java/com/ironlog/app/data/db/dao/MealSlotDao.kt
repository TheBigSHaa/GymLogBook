package com.ironlog.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ironlog.app.data.model.MealSlot
import kotlinx.coroutines.flow.Flow

@Dao
interface MealSlotDao {
    @Query("SELECT * FROM meal_slot ORDER BY orderIndex")
    fun getAllOrdered(): Flow<List<MealSlot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(slots: List<MealSlot>)
}
