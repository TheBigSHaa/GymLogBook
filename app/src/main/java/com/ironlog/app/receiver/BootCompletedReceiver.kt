package com.ironlog.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ironlog.app.data.sync.SyncWorker
import com.ironlog.app.util.SettingsStore
import com.ironlog.app.worker.WorkoutReminderWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-arms WorkManager schedules after the device reboots or the app is updated.
 *
 * WorkManager persists periodic schedules across reboots automatically, but our reminder
 * uses a chain of one-shot OneTimeWorkRequests (so the next fire can re-pin to wall clock,
 * handling DST). A single one-shot is NOT auto-reissued on boot, so we re-arm it here.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {
    @Inject lateinit var settingsStore: SettingsStore

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        // goAsync gives us ~10s to perform work off the main thread before the OS reaps us.
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                if (settingsStore.reminderEnabled.first()) {
                    val time = settingsStore.reminderTime.first()
                    WorkoutReminderWorker.scheduleNext(context, time)
                }
                // Re-arm periodic sync; WorkManager dedups by unique name so this is idempotent.
                SyncWorker.schedule(context)
            } catch (e: Exception) {
                android.util.Log.e("ProYou", "BootCompletedReceiver failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}
