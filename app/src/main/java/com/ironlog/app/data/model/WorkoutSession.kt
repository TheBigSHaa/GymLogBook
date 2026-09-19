package com.ironlog.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

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
        Index(value = ["workoutDayId", "date"]),
    ],
)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val workoutDayId: Int,
    val date: LocalDate,
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val notes: String? = null,
    val completed: Boolean = false,
)

