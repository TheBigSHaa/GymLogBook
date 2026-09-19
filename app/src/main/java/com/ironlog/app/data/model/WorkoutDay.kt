package com.ironlog.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class WorkoutDay(
    @PrimaryKey val id: Int,
    val name: String,
    val dayType: DayType,
    val colorHex: String,
)

