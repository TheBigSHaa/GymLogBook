package com.ironlog.app.data.model

import androidx.room.Entity
import java.time.LocalDate

@Entity(
    tableName = "ingredient_check",
    primaryKeys = ["date", "mealSlotId", "ingredientId"],
)
data class IngredientCheck(
    val date: LocalDate,
    val mealSlotId: Int,
    val ingredientId: Int,
    val checked: Boolean = false,
)
