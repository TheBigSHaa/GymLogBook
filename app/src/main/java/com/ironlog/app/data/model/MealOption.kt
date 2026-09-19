package com.ironlog.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meal_option",
    foreignKeys = [
        ForeignKey(
            entity = MealSlot::class,
            parentColumns = ["id"],
            childColumns = ["mealSlotId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mealSlotId")],
)
data class MealOption(
    @PrimaryKey val id: Int,
    val mealSlotId: Int,
    val label: String,
    val shortLabel: String,
    val description: String,
    val orderIndex: Int,
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val note: String? = null,
    val usesWholeEggs: Int = 0,
)
