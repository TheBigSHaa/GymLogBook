package com.ironlog.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

internal val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ironlog_settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    enum class ThemeConfig {
        DARK,
        LIGHT,
        SYSTEM,
    }

    /** Screen-awake policy while an active workout is on screen. */
    enum class WorkoutScreenTimeout(val label: String) {
        OFF("Off"),
        SEC_15("15 sec"),
        SEC_30("30 sec"),
        MIN_1("1 min"),
        MIN_2("2 min"),
        ALWAYS("Always on"),
    }

    private object Keys {
        val ProgramStartDateIso = stringPreferencesKey("program_start_date_iso")
        val DumbbellWeightsCsv = stringPreferencesKey("dumbbell_weights_csv")
        val RestPowerSeconds = intPreferencesKey("rest_power_seconds")
        val RestHypertrophySeconds = intPreferencesKey("rest_hypertrophy_seconds")
        val RestBonusSeconds = intPreferencesKey("rest_bonus_seconds")
        val CountdownVibrationEnabled = booleanPreferencesKey("countdown_vibration_enabled")
        val BeepVolume = floatPreferencesKey("beep_volume")
        val ReminderEnabled = booleanPreferencesKey("reminder_enabled")
        val ReminderTimeHHmm = stringPreferencesKey("reminder_time_hhmm")
        val ReminderNotifyOnRestDay = booleanPreferencesKey("reminder_notify_on_rest_day")
        val FirstDayOfWeek = stringPreferencesKey("first_day_of_week")
        val LastBackupTime = longPreferencesKey("last_backup_time")
        val ThemeConfig = stringPreferencesKey("theme_config")
        val WorkoutScreenTimeout = stringPreferencesKey("workout_screen_timeout")
    }

    val programStartDate: Flow<LocalDate?> =
        context.settingsDataStore.data.map { prefs ->
            prefs[Keys.ProgramStartDateIso]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        }

    val dumbbellWeightsKg: Flow<List<Float>> =
        context.settingsDataStore.data.map { prefs ->
            val raw = prefs[Keys.DumbbellWeightsCsv] ?: "6,10,12,17,20"
            raw.split(',')
                .mapNotNull { it.trim().takeIf(String::isNotBlank) }
                .mapNotNull { it.toFloatOrNull() }
                .distinct()
                .sorted()
        }

    val restPowerSeconds: Flow<Int> =
        context.settingsDataStore.data.map { it[Keys.RestPowerSeconds] ?: 90 }

    val restHypertrophySeconds: Flow<Int> =
        context.settingsDataStore.data.map { it[Keys.RestHypertrophySeconds] ?: 60 }

    val restBonusSeconds: Flow<Int> =
        context.settingsDataStore.data.map { it[Keys.RestBonusSeconds] ?: 20 }

    val countdownVibrationEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.CountdownVibrationEnabled] ?: false }

    val beepVolume: Flow<Float> =
        context.settingsDataStore.data.map { prefs ->
            (prefs[Keys.BeepVolume] ?: 1.0f).coerceIn(0.0f, 1.0f)
        }

    val reminderEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.ReminderEnabled] ?: false }

    val reminderTime: Flow<LocalTime> =
        context.settingsDataStore.data.map { prefs ->
            val raw = prefs[Keys.ReminderTimeHHmm] ?: "08:00"
            runCatching { LocalTime.parse(raw) }.getOrElse { LocalTime.of(8, 0) }
        }

    val reminderNotifyOnRestDay: Flow<Boolean> =
        context.settingsDataStore.data.map { it[Keys.ReminderNotifyOnRestDay] ?: false }

    val firstDayOfWeek: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.FirstDayOfWeek] ?: "SUNDAY" }

    val lastBackupTime: Flow<Long> =
        context.settingsDataStore.data.map { it[Keys.LastBackupTime] ?: 0L }

    val themeConfig: Flow<ThemeConfig> =
        context.settingsDataStore.data.map { prefs ->
            val raw = prefs[Keys.ThemeConfig] ?: ThemeConfig.DARK.name
            runCatching { ThemeConfig.valueOf(raw) }.getOrElse { ThemeConfig.DARK }
        }

    val workoutScreenTimeout: Flow<WorkoutScreenTimeout> =
        context.settingsDataStore.data.map { prefs ->
            val raw = prefs[Keys.WorkoutScreenTimeout] ?: WorkoutScreenTimeout.OFF.name
            runCatching { WorkoutScreenTimeout.valueOf(raw) }.getOrElse { WorkoutScreenTimeout.OFF }
        }

    suspend fun setProgramStartDate(date: LocalDate) {
        context.settingsDataStore.edit { it[Keys.ProgramStartDateIso] = date.toString() }
    }

    suspend fun setDumbbellWeightsKg(weights: List<Float>) {
        val csv = weights.distinct().sorted().joinToString(",") { w ->
            if (w % 1f == 0f) w.toInt().toString() else w.toString()
        }
        context.settingsDataStore.edit { it[Keys.DumbbellWeightsCsv] = csv }
    }

    suspend fun setRestDefaults(powerSeconds: Int, hypertrophySeconds: Int) {
        context.settingsDataStore.edit {
            it[Keys.RestPowerSeconds] = powerSeconds.coerceIn(REST_SECONDS_MIN, REST_SECONDS_MAX)
            it[Keys.RestHypertrophySeconds] = hypertrophySeconds.coerceIn(REST_SECONDS_MIN, REST_SECONDS_MAX)
        }
    }

    suspend fun setPowerRestSeconds(seconds: Int) {
        context.settingsDataStore.edit {
            it[Keys.RestPowerSeconds] = seconds.coerceIn(REST_SECONDS_MIN, REST_SECONDS_MAX)
        }
    }

    suspend fun setHypertrophyRestSeconds(seconds: Int) {
        context.settingsDataStore.edit {
            it[Keys.RestHypertrophySeconds] = seconds.coerceIn(REST_SECONDS_MIN, REST_SECONDS_MAX)
        }
    }

    suspend fun setBonusRestSeconds(seconds: Int) {
        context.settingsDataStore.edit {
            it[Keys.RestBonusSeconds] = seconds.coerceIn(REST_SECONDS_MIN, REST_SECONDS_MAX)
        }
    }

    suspend fun setCountdownVibrationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.CountdownVibrationEnabled] = enabled }
    }

    suspend fun setBeepVolume(volume: Float) {
        context.settingsDataStore.edit { it[Keys.BeepVolume] = volume.coerceIn(0.0f, 1.0f) }
    }

    suspend fun setReminder(enabled: Boolean, time: LocalTime) {
        context.settingsDataStore.edit {
            it[Keys.ReminderEnabled] = enabled
            it[Keys.ReminderTimeHHmm] = time.toString()
        }
    }

    suspend fun setReminderNotifyOnRestDay(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.ReminderNotifyOnRestDay] = enabled }
    }

    suspend fun setFirstDayOfWeek(day: String) {
        context.settingsDataStore.edit { it[Keys.FirstDayOfWeek] = day }
    }

    suspend fun setLastBackupTime(millis: Long) {
        context.settingsDataStore.edit { it[Keys.LastBackupTime] = millis }
    }

    suspend fun setThemeConfig(value: ThemeConfig) {
        context.settingsDataStore.edit { it[Keys.ThemeConfig] = value.name }
    }

    suspend fun setWorkoutScreenTimeout(value: WorkoutScreenTimeout) {
        context.settingsDataStore.edit { it[Keys.WorkoutScreenTimeout] = value.name }
    }

    companion object {
        const val REST_SECONDS_MIN = 10
        const val REST_SECONDS_MAX = 600
    }
}

