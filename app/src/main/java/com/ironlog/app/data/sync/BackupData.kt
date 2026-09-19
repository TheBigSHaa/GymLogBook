package com.ironlog.app.data.sync

// ═══════════════════════════════════════════════════════════════
// GOOGLE DRIVE SETUP — ONE-TIME MANUAL STEPS:
//
// 1. Go to https://console.cloud.google.com/
// 2. Create a new project (or use existing): "ProYou" (GCP project name)
// 3. Enable the "Google Drive API" under APIs & Services
// 4. Go to Credentials → Create Credentials → OAuth 2.0 Client ID
// 5. Application type: Android
// 6. Package name: com.ironlog.app
// 7. SHA-1 fingerprint: run this in terminal:
//    keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
//    (use your release keystore for production)
// 8. Create the client ID
// 9. Also create a "Web application" OAuth client ID (needed for the Drive API)
// 10. Download the credentials and note the Web Client ID
// 11. Put the Web Client ID in strings.xml as "default_web_client_id"
//
// Without these steps, Google Sign-In will fail silently.
// ═══════════════════════════════════════════════════════════════

/**
 * Complete app data snapshot for backup/restore.
 * Serialized to JSON and uploaded to Google Drive.
 */
data class BackupData(
    val version: Int = 1,
    val createdAt: String,
    val deviceId: String,

    val workoutSessions: List<WorkoutSessionBackup>,
    val exerciseLogs: List<ExerciseLogBackup>,
    val setLogs: List<SetLogBackup>,
    val scheduledWorkouts: List<ScheduledWorkoutBackup>,

    val dailyNutritionLogs: List<DailyNutritionLogBackup>,
    val mealCompletions: List<MealCompletionBackup>,
    val ingredientChecks: List<IngredientCheckBackup>,

    val settings: SettingsBackup,
)

data class WorkoutSessionBackup(
    val id: Long,
    val workoutDayId: Int,
    val date: String,
    val startTime: String?,
    val endTime: String?,
    val notes: String?,
    val completed: Boolean,
)

data class ExerciseLogBackup(
    val id: Long,
    val sessionId: Long,
    val exerciseId: Int,
    val orderIndex: Int,
)

data class SetLogBackup(
    val id: Long,
    val exerciseLogId: Long,
    val setNumber: Int,
    val weight: Float,
    val reps: Int,
    val completed: Boolean,
    val isWarmup: Boolean,
    val rpe: Int?,
    /** Additive nullable field — older backups without it restore as null. */
    val restSeconds: Int? = null,
)

data class ScheduledWorkoutBackup(
    val id: Long,
    val workoutDayId: Int?,
    val date: String,
    val isRestDay: Boolean,
)

data class DailyNutritionLogBackup(
    val date: String,
    val waterMl: Int,
    val waterTargetMl: Int,
)

data class MealCompletionBackup(
    val date: String,
    val mealSlotId: Int,
    val selectedOptionId: Int,
    val completed: Boolean,
)

data class IngredientCheckBackup(
    val date: String,
    val mealSlotId: Int,
    val ingredientId: Int,
    val checked: Boolean,
)

data class SettingsBackup(
    val programStartDate: String?,
    val availableDbWeights: List<Float>,
    val powerRestSeconds: Int,
    val hypertrophyRestSeconds: Int,
    val bonusRestSeconds: Int,
    val reminderEnabled: Boolean,
    val reminderTime: String?,
    val firstDayOfWeek: String,
    /** Absent in older backups; restore falls back to Off. */
    val workoutScreenTimeout: String? = null,
)
