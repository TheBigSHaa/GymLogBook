package com.ironlog.app.ui.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.ironlog.app.data.model.WorkoutDay
import com.ironlog.app.data.model.WorkoutSession
import com.ironlog.app.data.repository.WorkoutRepository
import com.ironlog.app.data.sync.GoogleSignInHelper
import com.ironlog.app.data.sync.SyncRepository
import com.ironlog.app.data.sync.SyncWorker
import com.ironlog.app.ui.log.ExerciseLogWithSetsUi
import com.ironlog.app.ui.log.SessionWithDetails
import com.ironlog.app.ui.log.SetWithPr
import com.ironlog.app.util.CsvExporter
import com.ironlog.app.util.SettingsStore
import com.ironlog.app.util.SettingsStore.ThemeConfig
import com.ironlog.app.worker.WorkoutReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class SettingsState(
    val programStartDate: LocalDate? = null,
    val dumbbellWeights: List<Float> = listOf(6f, 10f, 12f, 17f, 20f),
    val restPowerSeconds: Int = 90,
    val restHypertrophySeconds: Int = 60,
    val restBonusSeconds: Int = 20,
    val countdownVibrationEnabled: Boolean = false,
    val beepVolume: Float = 1.0f,
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime = LocalTime.of(8, 0),
    val reminderNotifyOnRestDay: Boolean = false,
    val firstDayOfWeek: String = "SUNDAY",
    val workoutScreenTimeout: SettingsStore.WorkoutScreenTimeout = SettingsStore.WorkoutScreenTimeout.OFF,
    val exportInProgress: Boolean = false,
    val exportError: String? = null,
    val lastExportFile: File? = null,
    val isSignedIn: Boolean = false,
    val googleEmail: String? = null,
    val lastBackupTimeStr: String? = null,
    val isSyncing: Boolean = false,
    val restoreError: String? = null,
    val syncError: String? = null,
    val restorePendingConfirmation: Boolean = false,
    val restoreWarnings: List<String> = emptyList(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore,
    private val workoutRepository: WorkoutRepository,
    private val csvExporter: CsvExporter,
    private val googleSignInHelper: GoogleSignInHelper,
    private val syncRepository: SyncRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    val themeConfig: StateFlow<ThemeConfig> =
        settingsStore.themeConfig.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeConfig.DARK)

    init {
        viewModelScope.launch {
            val base =
                combine(
                    settingsStore.programStartDate,
                    settingsStore.dumbbellWeightsKg,
                    settingsStore.restPowerSeconds,
                    settingsStore.restHypertrophySeconds,
                ) { start, dbs, p, h ->
                    SettingsState(
                        programStartDate = start,
                        dumbbellWeights = dbs,
                        restPowerSeconds = p,
                        restHypertrophySeconds = h,
                        restBonusSeconds = _state.value.restBonusSeconds,
                        countdownVibrationEnabled = _state.value.countdownVibrationEnabled,
                        reminderEnabled = _state.value.reminderEnabled,
                        reminderTime = _state.value.reminderTime,
                        reminderNotifyOnRestDay = _state.value.reminderNotifyOnRestDay,
                        exportInProgress = _state.value.exportInProgress,
                        lastExportFile = _state.value.lastExportFile,
                        isSignedIn = _state.value.isSignedIn,
                        googleEmail = _state.value.googleEmail,
                        lastBackupTimeStr = _state.value.lastBackupTimeStr,
                        isSyncing = _state.value.isSyncing,
                    )
                }

            val baseWithRestAndFeedback =
                combine(
                    base,
                    settingsStore.restBonusSeconds,
                    settingsStore.countdownVibrationEnabled,
                    settingsStore.beepVolume,
                    settingsStore.reminderEnabled,
                ) { s, b, cv, bv, re ->
                    s.copy(
                        restBonusSeconds = b,
                        countdownVibrationEnabled = cv,
                        beepVolume = bv,
                        reminderEnabled = re,
                    )
                }

            val withReminder = combine(baseWithRestAndFeedback, settingsStore.reminderTime, settingsStore.reminderNotifyOnRestDay) { s, time, rr ->
                s.copy(reminderTime = time, reminderNotifyOnRestDay = rr)
            }
            combine(withReminder, settingsStore.firstDayOfWeek, settingsStore.workoutScreenTimeout) { s, fdow, timeout ->
                s.copy(firstDayOfWeek = fdow, workoutScreenTimeout = timeout)
            }.collect { s -> _state.value = s }
        }

        viewModelScope.launch {
            val existing = settingsStore.programStartDate.first()
            if (existing == null) {
                val completed = workoutRepository.completedSessions().first().filter { it.completed }
                val start = completed.minByOrNull { it.date }?.date ?: LocalDate.now()
                settingsStore.setProgramStartDate(start)
            }
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSignedIn = googleSignInHelper.isSignedIn(),
                    googleEmail = googleSignInHelper.getAccountEmail(),
                )
            }
            settingsStore.lastBackupTime.collect { millis ->
                _state.update { it.copy(lastBackupTimeStr = if (millis == 0L) null else formatBackupTime(millis)) }
            }
        }
    }

    fun setProgramStartDate(date: LocalDate) {
        viewModelScope.launch { settingsStore.setProgramStartDate(date) }
    }

    fun addDumbbellWeight(weightKg: Float) {
        val updated = (_state.value.dumbbellWeights + weightKg).distinct().sorted()
        viewModelScope.launch { settingsStore.setDumbbellWeightsKg(updated) }
    }

    fun removeDumbbellWeight(weightKg: Float) {
        val updated = _state.value.dumbbellWeights.filterNot { it == weightKg }
        viewModelScope.launch { settingsStore.setDumbbellWeightsKg(updated) }
    }

    fun setPowerRestSeconds(seconds: Int) {
        viewModelScope.launch { settingsStore.setPowerRestSeconds(seconds) }
    }

    fun setHypertrophyRestSeconds(seconds: Int) {
        viewModelScope.launch { settingsStore.setHypertrophyRestSeconds(seconds) }
    }

    fun setBonusRestSeconds(seconds: Int) {
        viewModelScope.launch { settingsStore.setBonusRestSeconds(seconds) }
    }

    fun setCountdownVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setCountdownVibrationEnabled(enabled) }
    }

    fun setBeepVolume(volume: Float) {
        viewModelScope.launch { settingsStore.setBeepVolume(volume) }
    }

    fun setReminder(enabled: Boolean, time: LocalTime) {
        viewModelScope.launch {
            settingsStore.setReminder(enabled, time)
            if (enabled) scheduleReminder(time) else cancelReminder()
        }
    }

    fun setNotifyOnRestDay(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setReminderNotifyOnRestDay(enabled) }
    }

    fun setFirstDayOfWeek(day: String) {
        viewModelScope.launch { settingsStore.setFirstDayOfWeek(day) }
    }

    fun setWorkoutScreenTimeout(timeout: SettingsStore.WorkoutScreenTimeout) {
        viewModelScope.launch { settingsStore.setWorkoutScreenTimeout(timeout) }
    }

    fun updateTheme(newTheme: ThemeConfig) {
        viewModelScope.launch { settingsStore.setThemeConfig(newTheme) }
    }

    fun exportToCsv() {
        viewModelScope.launch {
            _state.update { it.copy(exportInProgress = true, exportError = null) }
            val sessions = buildSessionsForExport()
            csvExporter.exportWorkoutHistory(sessions)
                .onSuccess { file ->
                    _state.update { it.copy(exportInProgress = false, lastExportFile = file) }
                }
                .onFailure { e ->
                    android.util.Log.e("ProYou", "CSV export failed", e)
                    _state.update {
                        it.copy(
                            exportInProgress = false,
                            exportError = e.message ?: "Export failed",
                        )
                    }
                }
        }
    }

    fun clearExportError() {
        _state.update { it.copy(exportError = null) }
    }

    fun shareLastExport() {
        val file = _state.value.lastExportFile ?: return
        val uri = csvExporter.fileToShareUri(file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share ProYou export"))
    }

    // ─── Google Sync ────────────────────────────────────────────────────────────

    fun getSignInIntent(): Intent = googleSignInHelper.getSignInIntent()

    fun onSignInSuccess(account: GoogleSignInAccount) {
        _state.update { it.copy(isSignedIn = true, googleEmail = account.email) }
        SyncWorker.schedule(context)
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            syncRepository.sync()
            _state.update { it.copy(isSyncing = false) }
        }
    }

    fun onSignInFailed() {
        _state.update { it.copy(isSignedIn = false, googleEmail = null) }
    }

    fun signOut() {
        viewModelScope.launch {
            googleSignInHelper.signOut()
            SyncWorker.cancel(context)
            _state.update { it.copy(isSignedIn = false, googleEmail = null) }
        }
    }

    fun backupNow() {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, syncError = null) }
            val result = syncRepository.forceBackup()
            val errorMsg = when (result) {
                com.ironlog.app.data.sync.SyncResult.UPLOADED,
                com.ironlog.app.data.sync.SyncResult.RESTORED -> null
                com.ironlog.app.data.sync.SyncResult.NO_ACCOUNT -> "Sign in to Google to back up"
                com.ironlog.app.data.sync.SyncResult.AUTH_REQUIRED -> "Re-authentication needed — sign in again"
                com.ironlog.app.data.sync.SyncResult.FAILED -> "Backup failed — check your connection and retry"
            }
            _state.update { it.copy(isSyncing = false, syncError = errorMsg) }
            if (errorMsg != null) android.util.Log.w("ProYou", "Backup failed: $errorMsg")
        }
    }

    /**
     * Begins the restore confirmation flow. Until the user confirms via [confirmRestore], no
     * destructive action occurs.
     */
    fun requestRestoreConfirmation() {
        _state.update { it.copy(restorePendingConfirmation = true, restoreError = null) }
    }

    fun cancelRestore() {
        _state.update { it.copy(restorePendingConfirmation = false) }
    }

    fun confirmRestore() {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, restorePendingConfirmation = false, restoreError = null) }
            val outcome = syncRepository.forceRestore()
            val (error, warnings) = when (outcome) {
                is com.ironlog.app.data.sync.RestoreOutcome.Restored -> null to outcome.warnings
                com.ironlog.app.data.sync.RestoreOutcome.NoAccount -> "Sign in to Google to restore" to emptyList()
                com.ironlog.app.data.sync.RestoreOutcome.AuthRequired -> "Re-authentication needed — sign in again" to emptyList()
                com.ironlog.app.data.sync.RestoreOutcome.NoBackupFound -> "No backup found on Drive" to emptyList()
                is com.ironlog.app.data.sync.RestoreOutcome.Failed -> (outcome.message ?: "Restore failed") to emptyList()
            }
            _state.update {
                it.copy(
                    isSyncing = false,
                    restoreError = error,
                    restoreWarnings = warnings,
                )
            }
        }
    }

    fun clearRestoreError() {
        _state.update { it.copy(restoreError = null, restoreWarnings = emptyList()) }
    }

    fun clearSyncError() {
        _state.update { it.copy(syncError = null) }
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    private fun formatBackupTime(millis: Long): String {
        val dt = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        return dt.format(DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.ENGLISH))
    }

    /**
     * Schedules the reminder as a chain of OneTimeWorkRequests rather than a Periodic worker,
     * because PeriodicWorkRequest's [setInitialDelay] is *ignored* on first enqueue — the
     * periodic worker fires immediately the first time, regardless of the time the user picked.
     *
     * Each fired worker reschedules itself for the next occurrence; this also recalculates the
     * absolute clock time per-fire, so DST transitions are handled correctly (the next fire is
     * always pinned to wall-clock HH:mm in the user's current timezone).
     */
    private fun scheduleReminder(time: LocalTime) {
        WorkoutReminderWorker.scheduleNext(context, time)
    }

    private fun cancelReminder() {
        WorkoutReminderWorker.cancel(context)
    }

    private suspend fun buildSessionsForExport(): List<SessionWithDetails> {
        val days: List<WorkoutDay> = workoutRepository.workoutDays().first()
        val dayById = days.associateBy { it.id }
        val sessions: List<WorkoutSession> = workoutRepository.completedSessions().first().filter { it.completed }

        return sessions
            .sortedWith(compareByDescending<WorkoutSession> { it.date }.thenByDescending { it.id })
            .mapNotNull { session ->
                val day = dayById[session.workoutDayId] ?: return@mapNotNull null
                val logs = workoutRepository.logsForSessionOnce(session.id)
                val exercises = buildList {
                    logs.forEach { log ->
                        val exercise = workoutRepository.exerciseById(log.exerciseId) ?: return@forEach
                        val sets = workoutRepository.setsForExerciseLogOnce(log.id)
                        add(
                            ExerciseLogWithSetsUi(
                                exercise = exercise,
                                sets = sets.map { SetWithPr(it, isPr = false) },
                            ),
                        )
                    }
                }
                SessionWithDetails(
                    session = session,
                    workoutDay = day,
                    exercises = exercises,
                    durationMinutes = 0,
                    totalVolume = 0,
                )
            }
    }

    companion object {
        const val UNIQUE_REMINDER_WORK = WorkoutReminderWorker.UNIQUE_NAME
    }
}
