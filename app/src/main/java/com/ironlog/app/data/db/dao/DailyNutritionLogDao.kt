package com.ironlog.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ironlog.app.data.model.DailyNutritionLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyNutritionLogDao {
    @Query("SELECT * FROM daily_nutrition_log WHERE date = :date")
    fun getForDate(date: LocalDate): Flow<DailyNutritionLog?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(log: DailyNutritionLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: DailyNutritionLog)

    @Query("UPDATE daily_nutrition_log SET waterMl = waterMl + :amount WHERE date = :date")
    suspend fun addWater(date: LocalDate, amount: Int)

    @Query("SELECT * FROM daily_nutrition_log")
    fun getAll(): Flow<List<DailyNutritionLog>>

    @Query("DELETE FROM daily_nutrition_log")
    suspend fun deleteAll()
}
