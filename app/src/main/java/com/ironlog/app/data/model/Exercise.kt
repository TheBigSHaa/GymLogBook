package com.ironlog.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
        Index(value = ["workoutDayId", "orderIndex"], unique = true),
    ],
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workoutDayId: Int,
    val name: String,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val orderIndex: Int,
    val equipmentType: EquipmentType,
    val repUnit: RepUnit,
    val notes: String? = null,
    val section: String? = null, // WARMUP / CIRCUIT / CORE / COOLDOWN (used by Day 5)
    val circuitRounds: Int? = null, // if non-null, UI should treat as circuit-style work
    val circuitDurationSeconds: Int? = null, // if non-null, UI should show a shared circuit timer
)

