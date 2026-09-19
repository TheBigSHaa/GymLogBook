package com.ironlog.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_ingredient",
    foreignKeys = [
        ForeignKey(
            entity = MealOption::class,
            parentColumns = ["id"],
            childColumns = ["mealOptionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mealOptionId")],
)
data class MealIngredient(
    @PrimaryKey val id: Int,
    val mealOptionId: Int,
    val name: String,
    val amount: String,
    val orderIndex: Int,
    val proteinG: Float = 0f,
    val carbsG: Float = 0f,
    val fatG: Float = 0f,
    val wholeEggCount: Int = 0,
)
