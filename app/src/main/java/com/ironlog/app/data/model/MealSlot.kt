package com.ironlog.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meal_slot")
data class MealSlot(
    @PrimaryKey val id: Int,
    val name: String,
    val timeLabel: String,
    val orderIndex: Int,
    val mealType: String,
)
