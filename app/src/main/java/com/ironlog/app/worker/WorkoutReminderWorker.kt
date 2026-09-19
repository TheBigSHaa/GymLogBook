package com.ironlog.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ironlog.app.data.repository.ScheduleRepository
import com.ironlog.app.data.repository.WorkoutRepository
import com.ironlog.app.util.NotificationHelper
import com.ironlog.app.util.SettingsStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

@HiltWorker
class WorkoutReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val workoutRepo: WorkoutRepository,
    private val scheduleRepo: ScheduleRepository,
    private val settings: SettingsStore,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val today = LocalDate.now()
            val scheduled = scheduleRepo.scheduledForDate(today).first()
            val notifyRest = settings.reminderNotifyOnRestDay.first()

            if (scheduled != null) {
                if (scheduled.isRestDay) {
                    if (notifyRest) {
                        NotificationHelper.sendMorningReminder(
                            context = applicationContext,
                            title = "ProYou — Rest day",
                            body = "Rest day — recovery is part of the process.",
                        )
                    }
                } else {
                    val day = scheduled.workoutDayId?.let { workoutRepo.workoutDayById(it) }
                    if (day != null) {
                        val exCount = workoutRepo.exercisesForDay(day.id).first().size
                        NotificationHelper.sendMorningReminder(
                            context = applicationContext,
                            title = "Today: ${day.name}",
                            body = "Today: ${day.name} — $exCount exercises",
                        )
                    }
                }
            }

            // Always reschedule the next fire so the chain self-perpetuates and adjusts to
            // wall-clock time (DST transitions, timezone changes).
            val reminderEnabled = settings.reminderEnabled.first()
            if (reminderEnabled) {
                val time = settings.reminderTime.first()
                scheduleNext(applicationContext, time)
            }

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("ProYou", "WorkoutReminderWorker failed", e)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "proyou.workout_reminder"

        /**
         * Computes the next wall-clock occurrence of [time] and enqueues a one-shot worker.
         * Re-invoke after each fire so the chain re-aligns to wall time across DST changes.
         */
        fun scheduleNext(context: Context, time: LocalTime) {
            val zone = ZoneId.systemDefault()
            val now = ZonedDateTime.now(zone)
            var next = now.withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delayMs = Duration.between(now, next).toMillis().coerceAtLeast(0)

            val request = OneTimeWorkRequestBuilder<WorkoutReminderWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}
