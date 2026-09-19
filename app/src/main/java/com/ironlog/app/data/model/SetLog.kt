package com.ironlog.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = ExerciseLog::class,
            parentColumns = ["id"],
            childColumns = ["exerciseLogId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("exerciseLogId"),
        Index(value = ["exerciseLogId", "setNumber"], unique = true),
    ],
)
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val exerciseLogId: Long,
    val setNumber: Int,
    val weight: Float,
    val reps: Int,
    val completed: Boolean,
    val isWarmup: Boolean = false,
    val rpe: Int? = null,
    /** Rest ACTUALLY taken after completing this set, in seconds. Null when not recorded. */
    val restSeconds: Int? = null,
)

