package com.ironlog.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
object NotificationHelper {
    const val CHANNEL_REMINDERS = "workout_reminders"
    const val CHANNEL_ACTIVE = "active_workout"

    private const val NOTIF_ID_ACTIVE = 1001
    private const val NOTIF_ID_REMINDER = 2001

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            "Workout Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Morning reminders for scheduled workouts"
        }

        val active = NotificationChannel(
            CHANNEL_ACTIVE,
            "Active Workout",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Ongoing workout in progress notification"
            setShowBadge(false)
        }

        nm.createNotificationChannel(reminders)
        nm.createNotificationChannel(active)
    }

    fun showActiveWorkoutNotification(
        context: Context,
        startedAtEpochMillis: Long,
    ) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = androidx.core.app.TaskStackBuilder.create(context).run {
            if (intent != null) addNextIntentWithParentStack(intent)
            getPendingIntent(
                0,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_ACTIVE)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("ProYou — Workout in progress")
            .setContentText("Tap to return to your workout")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setWhen(startedAtEpochMillis)
            .setContentIntent(pending)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_ACTIVE, notif)
    }

    fun cancelActiveWorkoutNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID_ACTIVE)
    }

    fun sendMorningReminder(
        context: Context,
        title: String,
        body: String,
    ) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = androidx.core.app.TaskStackBuilder.create(context).run {
            if (intent != null) addNextIntentWithParentStack(intent)
            getPendingIntent(
                1,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_REMINDER, notif)
    }
}

