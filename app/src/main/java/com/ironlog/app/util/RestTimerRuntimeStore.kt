package com.ironlog.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

internal val Context.restRuntimeDataStore: DataStore<Preferences> by preferencesDataStore(name = "ironlog_rest_runtime")

data class RestRuntimeState(
    val restEndEpochMs: Long? = null,
    val restPaused: Boolean = true,
    val restDefaultSeconds: Int = 0,
)

@Singleton
class RestTimerRuntimeStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val RestEndEpochMs = longPreferencesKey("rest_end_epoch_ms")
        val RestPaused = booleanPreferencesKey("rest_paused")
        val RestDefaultSeconds = intPreferencesKey("rest_default_seconds")
    }

    val restEndEpochMs: Flow<Long?> =
        context.restRuntimeDataStore.data.map { prefs ->
            prefs[Keys.RestEndEpochMs]?.takeIf { it > 0L }
        }

    val restPaused: Flow<Boolean> =
        context.restRuntimeDataStore.data.map { prefs ->
            prefs[Keys.RestPaused] ?: true
        }

    val restDefaultSeconds: Flow<Int> =
        context.restRuntimeDataStore.data.map { prefs ->
            (prefs[Keys.RestDefaultSeconds] ?: 0).coerceIn(0, 600)
        }

    val state: Flow<RestRuntimeState> =
        combine(restEndEpochMs, restPaused, restDefaultSeconds) { end, paused, defaultSeconds ->
            RestRuntimeState(
                restEndEpochMs = end,
                restPaused = paused,
                restDefaultSeconds = defaultSeconds,
            )
        }

    suspend fun setRestDefaultSeconds(seconds: Int) {
        context.restRuntimeDataStore.edit { prefs ->
            prefs[Keys.RestDefaultSeconds] = seconds.coerceIn(0, 600)
        }
    }

    suspend fun setRestState(restEndEpochMs: Long?, paused: Boolean) {
        context.restRuntimeDataStore.edit { prefs ->
            if (restEndEpochMs == null) {
                prefs.remove(Keys.RestEndEpochMs)
            } else {
                prefs[Keys.RestEndEpochMs] = restEndEpochMs
            }
            prefs[Keys.RestPaused] = paused
        }
    }
}

