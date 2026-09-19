package com.ironlog.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDay::class,
            parentColumns = ["id"],
            childColumns = ["workoutDayId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("workoutDayId"),
        Index(value = ["date"], unique = true),
    ],
)
data class ScheduledWorkout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val workoutDayId: Int? = null,
    val date: LocalDate,
    val isRestDay: Boolean = false,
)

