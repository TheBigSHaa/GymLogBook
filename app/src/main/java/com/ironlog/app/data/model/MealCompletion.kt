package com.ironlog.app.data.model

import androidx.room.Entity
import java.time.LocalDate

@Entity(
    tableName = "meal_completion",
    primaryKeys = ["date", "mealSlotId"],
)
data class MealCompletion(
    val date: LocalDate,
    val mealSlotId: Int,
    val selectedOptionId: Int,
    val completed: Boolean = false,
)
