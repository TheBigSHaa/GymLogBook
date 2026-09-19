package com.ironlog.app.data.sync

import android.content.Context
import android.provider.Settings
import androidx.room.withTransaction
import com.ironlog.app.data.db.AppDatabase
import com.ironlog.app.data.db.dao.DailyNutritionLogDao
import com.ironlog.app.data.db.dao.ExerciseLogDao
import com.ironlog.app.data.db.dao.IngredientCheckDao
import com.ironlog.app.data.db.dao.MealCompletionDao
import com.ironlog.app.data.db.dao.ScheduledWorkoutDao
import com.ironlog.app.data.db.dao.SetLogDao
import com.ironlog.app.data.db.dao.WorkoutSessionDao
import com.ironlog.app.data.model.DailyNutritionLog
import com.ironlog.app.data.model.ExerciseLog
import com.ironlog.app.data.model.IngredientCheck
import com.ironlog.app.data.model.MealCompletion
import com.ironlog.app.data.model.ScheduledWorkout
import com.ironlog.app.data.model.SetLog
import com.ironlog.app.data.model.WorkoutSession
import com.ironlog.app.util.SettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val driveManager: DriveBackupManager,
    private val settingsStore: SettingsStore,
    private val workoutSessionDao: WorkoutSessionDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val setLogDao: SetLogDao,
    private val scheduledWorkoutDao: ScheduledWorkoutDao,
    private val dailyNutritionLogDao: DailyNutritionLogDao,
    private val mealCompletionDao: MealCompletionDao,
    private val ingredientCheckDao: IngredientCheckDao,
) {
    private val deviceId: String by lazy {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
    }

    suspend fun createBackupSnapshot(): BackupData {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        val programStartDate = settingsStore.programStartDate.first()
        val dumbbellWeights = settingsStore.dumbbellWeightsKg.first()
        val restPowerSeconds = settingsStore.restPowerSeconds.first()
        val restHypertrophySeconds = settingsStore.restHypertrophySeconds.first()
        val restBonusSeconds = settingsStore.restBonusSeconds.first()
        val reminderEnabled = settingsStore.reminderEnabled.first()
        val reminderTime = settingsStore.reminderTime.first()
        val firstDayOfWeek = settingsStore.firstDayOfWeek.first()
        val workoutScreenTimeout = settingsStore.workoutScreenTimeout.first()

        val sessions = workoutSessionDao.getAll().first()
        val exerciseLogs = exerciseLogDao.getAll().first()
        val setLogs = setLogDao.getAll().first()
        val scheduled = scheduledWorkoutDao.getAll().first()
        val nutritionLogs = dailyNutritionLogDao.getAll().first()
        val mealCompletions = mealCompletionDao.getAll().first()
        val ingredientChecks = ingredientCheckDao.getAll().first()

        return BackupData(
            version = BACKUP_VERSION,
            createdAt = now,
            deviceId = deviceId,
            workoutSessions = sessions.map {
                WorkoutSessionBackup(
                    id = it.id,
                    workoutDayId = it.workoutDayId,
                    date = it.date.toString(),
                    startTime = it.startTime?.toString(),
                    endTime = it.endTime?.toString(),
                    notes = it.notes,
                    completed = it.completed,
                )
            },
            exerciseLogs = exerciseLogs.map {
                ExerciseLogBackup(it.id, it.sessionId, it.exerciseId, it.orderIndex)
            },
            setLogs = setLogs.map {
                SetLogBackup(it.id, it.exerciseLogId, it.setNumber, it.weight, it.reps, it.completed, it.isWarmup, it.rpe, it.restSeconds)
            },
            scheduledWorkouts = scheduled.map {
                ScheduledWorkoutBackup(it.id, it.workoutDayId, it.date.toString(), it.isRestDay)
            },
            dailyNutritionLogs = nutritionLogs.map {
                DailyNutritionLogBackup(it.date.toString(), it.waterMl, it.waterTargetMl)
            },
            mealCompletions = mealCompletions.map {
                MealCompletionBackup(it.date.toString(), it.mealSlotId, it.selectedOptionId, it.completed)
            },
            ingredientChecks = ingredientChecks.map {
                IngredientCheckBackup(it.date.toString(), it.mealSlotId, it.ingredientId, it.checked)
            },
            settings = SettingsBackup(
                programStartDate = programStartDate?.toString(),
                availableDbWeights = dumbbellWeights,
                powerRestSeconds = restPowerSeconds,
                hypertrophyRestSeconds = restHypertrophySeconds,
                bonusRestSeconds = restBonusSeconds,
                reminderEnabled = reminderEnabled,
                reminderTime = reminderTime.toString(),
                firstDayOfWeek = firstDayOfWeek,
                workoutScreenTimeout = workoutScreenTimeout.name,
            ),
        )
    }

    /**
     * Replaces local state with the backup. Wrapped in a single DB transaction so that a crash
     * mid-restore can't leave the DB in a half-restored state.
     *
     * NOTE: Restore inserts entities with their original IDs. With OnConflictStrategy.REPLACE
     * this is idempotent but means that if two devices created records with the same auto-gen
     * ID and both pushed to Drive, the later push wins. Multi-device merge is out of scope for
     * this restore flow; callers should warn the user before invoking it.
     *
     * @return a list of warnings that the UI may surface to the user (e.g. "settings could not be parsed").
     */
    suspend fun restoreFromBackup(backup: BackupData): List<String> {
        val warnings = mutableListOf<String>()
        db.withTransaction {
            workoutSessionDao.deleteAll()
            exerciseLogDao.deleteAll()
            setLogDao.deleteAll()
            scheduledWorkoutDao.deleteAll()
            dailyNutritionLogDao.deleteAll()
            mealCompletionDao.deleteAll()
            ingredientCheckDao.deleteAll()

            backup.workoutSessions.forEach { b ->
                val date = parseLocalDate(b.date) ?: run {
                    warnings += "Skipped session ${b.id}: bad date '${b.date}'"
                    return@forEach
                }
                workoutSessionDao.insert(
                    WorkoutSession(
                        id = b.id,
                        workoutDayId = b.workoutDayId,
                        date = date,
                        startTime = b.startTime?.let { parseLocalDateTime(it) },
                        endTime = b.endTime?.let { parseLocalDateTime(it) },
                        notes = b.notes,
                        completed = b.completed,
                    ),
                )
            }
            backup.exerciseLogs.forEach { b ->
                exerciseLogDao.insert(ExerciseLog(id = b.id, sessionId = b.sessionId, exerciseId = b.exerciseId, orderIndex = b.orderIndex))
            }
            backup.setLogs.forEach { b ->
                setLogDao.insert(SetLog(id = b.id, exerciseLogId = b.exerciseLogId, setNumber = b.setNumber, weight = b.weight, reps = b.reps, completed = b.completed, isWarmup = b.isWarmup, rpe = b.rpe, restSeconds = b.restSeconds))
            }
            backup.scheduledWorkouts.forEach { b ->
                val date = parseLocalDate(b.date) ?: run {
                    warnings += "Skipped scheduled workout ${b.id}: bad date '${b.date}'"
                    return@forEach
                }
                scheduledWorkoutDao.upsert(ScheduledWorkout(id = b.id, workoutDayId = b.workoutDayId, date = date, isRestDay = b.isRestDay))
            }
            backup.dailyNutritionLogs.forEach { b ->
                val date = parseLocalDate(b.date) ?: run {
                    warnings += "Skipped nutrition log: bad date '${b.date}'"
                    return@forEach
                }
                dailyNutritionLogDao.upsert(DailyNutritionLog(date = date, waterMl = b.waterMl, waterTargetMl = b.waterTargetMl))
            }
            backup.mealCompletions.forEach { b ->
                val date = parseLocalDate(b.date) ?: return@forEach
                mealCompletionDao.upsert(MealCompletion(date = date, mealSlotId = b.mealSlotId, selectedOptionId = b.selectedOptionId, completed = b.completed))
            }
            backup.ingredientChecks.forEach { b ->
                val date = parseLocalDate(b.date) ?: return@forEach
                ingredientCheckDao.upsert(IngredientCheck(date = date, mealSlotId = b.mealSlotId, ingredientId = b.ingredientId, checked = b.checked))
            }
        }

        // Settings live in DataStore (outside the Room transaction); restore them with
        // best-effort, surfacing any per-field failures to the caller.
        backup.settings.programStartDate?.let { raw ->
            val parsed = runCatching { LocalDate.parse(raw) }.getOrNull()
            if (parsed != null) {
                settingsStore.setProgramStartDate(parsed)
            } else {
                warnings += "Could not restore program start date '$raw'"
            }
        }
        settingsStore.setDumbbellWeightsKg(backup.settings.availableDbWeights)
        settingsStore.setRestDefaults(backup.settings.powerRestSeconds, backup.settings.hypertrophyRestSeconds)
        settingsStore.setBonusRestSeconds(backup.settings.bonusRestSeconds)
        val reminderTime = backup.settings.reminderTime?.let {
            runCatching { java.time.LocalTime.parse(it) }
                .onFailure { warnings += "Could not restore reminder time '${backup.settings.reminderTime}'" }
                .getOrElse { java.time.LocalTime.of(8, 0) }
        } ?: java.time.LocalTime.of(8, 0)
        settingsStore.setReminder(backup.settings.reminderEnabled, reminderTime)
        settingsStore.setFirstDayOfWeek(backup.settings.firstDayOfWeek)
        val timeout = backup.settings.workoutScreenTimeout?.let {
            runCatching { SettingsStore.WorkoutScreenTimeout.valueOf(it) }.getOrNull()
        } ?: SettingsStore.WorkoutScreenTimeout.OFF
        settingsStore.setWorkoutScreenTimeout(timeout)

        settingsStore.setLastBackupTime(System.currentTimeMillis())
        return warnings
    }

    suspend fun sync(): SyncResult {
        return try {
            val lastLocalBackup = settingsStore.lastBackupTime.first()
            val remoteTimestamp = driveManager.getRemoteBackupTimestamp()

            when {
                remoteTimestamp == null -> uploadAndStamp()
                remoteTimestamp > lastLocalBackup -> {
                    when (val outcome = driveManager.downloadBackup()) {
                        is DriveBackupManager.Outcome.Ok -> {
                            restoreFromBackup(outcome.value)
                            SyncResult.RESTORED
                        }
                        DriveBackupManager.Outcome.NotSignedIn -> SyncResult.NO_ACCOUNT
                        DriveBackupManager.Outcome.AuthRequired -> SyncResult.AUTH_REQUIRED
                        DriveBackupManager.Outcome.NotFound -> uploadAndStamp()
                        is DriveBackupManager.Outcome.Failure -> SyncResult.FAILED
                    }
                }
                else -> uploadAndStamp()
            }
        } catch (e: Exception) {
            android.util.Log.e("ProYou", "Sync failed", e)
            SyncResult.FAILED
        }
    }

    private suspend fun uploadAndStamp(): SyncResult {
        val snapshot = createBackupSnapshot()
        return when (val outcome = driveManager.uploadBackup(snapshot)) {
            is DriveBackupManager.Outcome.Ok -> {
                // Stamp BEFORE returning so a crash after this point doesn't re-upload on next sync.
                settingsStore.setLastBackupTime(System.currentTimeMillis())
                SyncResult.UPLOADED
            }
            DriveBackupManager.Outcome.NotSignedIn -> SyncResult.NO_ACCOUNT
            DriveBackupManager.Outcome.AuthRequired -> SyncResult.AUTH_REQUIRED
            DriveBackupManager.Outcome.NotFound,
            is DriveBackupManager.Outcome.Failure -> SyncResult.FAILED
        }
    }

    suspend fun forceBackup(): SyncResult {
        return uploadAndStamp()
    }

    /**
     * Caller MUST confirm with the user first — this overwrites all local workout & nutrition data.
     */
    suspend fun forceRestore(): RestoreOutcome {
        return when (val outcome = driveManager.downloadBackup()) {
            is DriveBackupManager.Outcome.Ok -> {
                val warnings = restoreFromBackup(outcome.value)
                RestoreOutcome.Restored(warnings)
            }
            DriveBackupManager.Outcome.NotSignedIn -> RestoreOutcome.NoAccount
            DriveBackupManager.Outcome.AuthRequired -> RestoreOutcome.AuthRequired
            DriveBackupManager.Outcome.NotFound -> RestoreOutcome.NoBackupFound
            is DriveBackupManager.Outcome.Failure -> RestoreOutcome.Failed(outcome.error.message)
        }
    }

    private fun parseLocalDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value) }.getOrNull()

    private fun parseLocalDateTime(value: String): LocalDateTime? =
        runCatching { LocalDateTime.parse(value) }.getOrNull()

    companion object {
        /** Backup serialization format version. Bump when BackupData shape changes incompatibly. */
        const val BACKUP_VERSION = 1
    }
}

enum class SyncResult { UPLOADED, RESTORED, FAILED, NO_ACCOUNT, AUTH_REQUIRED }

sealed class RestoreOutcome {
    data class Restored(val warnings: List<String>) : RestoreOutcome()
    data object NoAccount : RestoreOutcome()
    data object AuthRequired : RestoreOutcome()
    data object NoBackupFound : RestoreOutcome()
    data class Failed(val message: String?) : RestoreOutcome()
}
