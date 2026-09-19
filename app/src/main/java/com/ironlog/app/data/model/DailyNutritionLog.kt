package com.ironlog.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_nutrition_log")
data class DailyNutritionLog(
    @PrimaryKey val date: LocalDate,
    val waterMl: Int = 0,
    val waterTargetMl: Int = 2500,
)
