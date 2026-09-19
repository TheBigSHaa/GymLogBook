package com.ironlog.app.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.RemoteViews
import android.app.PendingIntent
import com.ironlog.app.MainActivity
import com.ironlog.app.R
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ironlog.app.util.NotificationHelper
import com.ironlog.app.util.RestRuntimeState
import com.ironlog.app.util.RestTimerMath
import com.ironlog.app.util.RestTimerRuntimeStore
import com.ironlog.app.util.SettingsStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Foreground service that keeps an accurate workout timer visible in the notification shade
 * while the active workout screen is running.
 *
 * Also owns the rest-timer sound/vibration cues so they fire even when the screen is off,
 * the app is backgrounded, the phone is locked, or the phone is in silent mode:
 *  - Tones play on STREAM_ALARM, which is independent of the ring/silent switch.
 *  - A short partial wake lock (bounded to the rest duration) keeps the CPU ticking so
 *    the countdown cues stay on time with the screen off.
 *  - Running as a foreground service (with POST_NOTIFICATIONS + FOREGROUND_SERVICE +
 *    FOREGROUND_SERVICE_HEALTH already declared) means Android won't suspend these cues
 *    while the app is backgrounded or the device is locked.
 */
class ActiveWorkoutService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    private var startedAtEpochMs: Long = 0L
    private var workoutDayId: Int? = null
    @Volatile private var runtimeRest: RestRuntimeState = RestRuntimeState()

    // ── Cue settings (mirrored from DataStore) ─────────────────────────────
    @Volatile private var beepVolumePercent: Int = 100
    @Volatile private var countdownVibrationEnabled: Boolean = false
    @Volatile private var settingsLoaded: Boolean = false

    // ── Cue state ──────────────────────────────────────────────────────────
    private var toneGenerator: ToneGenerator? = null
    private var toneGeneratorVolume: Int = -1
    private var lastCueSecond: Int = -1
    private var cueArmedForEndMs: Long = -1L
    private var wakeLock: PowerManager.WakeLock? = null

    // Tracks the last (endEpochMs-or-defaultSeconds, isActivelyCountingDown) state we
    // notified for, so we only rebuild the notification on real state changes — the
    // ticking itself is handled natively by the notification's chronometer.
    private var lastNotifiedKeyA: Long = Long.MIN_VALUE
    private var lastNotifiedKeyB: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("ProYou", "ActiveWorkoutService.onCreate()")
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ActiveWorkoutServiceEntryPoint::class.java,
        )
        val runtimeStore = entryPoint.restRuntimeStore()
        val settingsStore = entryPoint.settingsStore()

        scope.launch {
            runtimeStore.state.collect { state ->
                runtimeRest = state
                android.util.Log.d("ProYou", "Rest state updated: $state")
            }
        }
        scope.launch {
            try {
                settingsStore.beepVolume.collect { volume ->
                    beepVolumePercent = (volume * 100f).roundToInt().coerceIn(0, 100)
                    if (!settingsLoaded) {
                        settingsLoaded = true
                        android.util.Log.d("ProYou", "SETTINGS LOADED - Beep volume: $beepVolumePercent%")
                    } else {
                        android.util.Log.d("ProYou", "Beep volume updated: $volume -> $beepVolumePercent%")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProYou", "Error loading beep volume", e)
                beepVolumePercent = 100
                settingsLoaded = true
            }
        }
        scope.launch {
            try {
                settingsStore.countdownVibrationEnabled.collect { enabled ->
                    countdownVibrationEnabled = enabled
                    android.util.Log.d("ProYou", "Vibration enabled: $enabled")
                }
            } catch (e: Exception) {
                android.util.Log.e("ProYou", "Error loading vibration setting", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val runtimeStore = EntryPointAccessors.fromApplication(
            applicationContext,
            ActiveWorkoutServiceEntryPoint::class.java,
        ).restRuntimeStore()

        when (intent?.action) {
            ACTION_START -> {
                startedAtEpochMs =
                    intent.getLongExtra(EXTRA_STARTED_AT_EPOCH_MS, -1L).takeIf { it > 0L }
                        ?: System.currentTimeMillis()
                workoutDayId = intent.getIntExtra(EXTRA_WORKOUT_DAY_ID, -1).takeIf { it > 0 }
                android.util.Log.d("ProYou", "ACTION_START: startedAtEpochMs=$startedAtEpochMs, workoutDayId=$workoutDayId")
            }
            ACTION_START_REST -> {
                val seconds = runtimeRest.restDefaultSeconds
                if (seconds > 0) {
                    val endMs = System.currentTimeMillis() + (seconds * 1000L)
                    runtimeRest = runtimeRest.copy(restEndEpochMs = endMs, restPaused = false)
                    scope.launch { runtimeStore.setRestState(restEndEpochMs = endMs, paused = false) }
                } else {
                    runtimeRest = runtimeRest.copy(restEndEpochMs = null, restPaused = true)
                    scope.launch { runtimeStore.setRestState(restEndEpochMs = null, paused = true) }
                }
            }
            ACTION_UPDATE_REST -> {
                val endMs = intent.getLongExtra(EXTRA_REST_END_EPOCH_MS, -1L).takeIf { it > 0L }
                val paused = intent.getBooleanExtra(EXTRA_REST_PAUSED, false)
                runtimeRest = runtimeRest.copy(restEndEpochMs = endMs, restPaused = paused)
                scope.launch { runtimeStore.setRestState(restEndEpochMs = endMs, paused = paused) }
            }
            ACTION_CLEAR_REST -> {
                runtimeRest = runtimeRest.copy(restEndEpochMs = null, restPaused = true)
                scope.launch { runtimeStore.setRestState(restEndEpochMs = null, paused = true) }
            }
            else -> {
                android.util.Log.d("ProYou", "Unknown action: ${intent?.action}, treating as start")
                // Backwards-compatible: treat missing action as start.
                startedAtEpochMs =
                    intent?.getLongExtra(EXTRA_STARTED_AT_EPOCH_MS, -1L)?.takeIf { it > 0L }
                        ?: startedAtEpochMs.takeIf { it > 0L }
                        ?: System.currentTimeMillis()
                workoutDayId =
                    intent?.getIntExtra(EXTRA_WORKOUT_DAY_ID, -1)?.takeIf { it > 0 }
                        ?: workoutDayId
            }
        }

        val initial = buildNotification()
        try {
            android.util.Log.d("ProYou", "Starting foreground service with notification ID=$NOTIFICATION_ID")
            if (Build.VERSION.SDK_INT >= 34) {
                this.startForeground(NOTIFICATION_ID, initial, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                ServiceCompat.startForeground(this, NOTIFICATION_ID, initial, 0)
            }
            android.util.Log.d("ProYou", "✓ Foreground service started successfully")
        } catch (e: Exception) {
            android.util.Log.e("ProYou", "startForeground failed; stopping service", e)
            stopSelf()
            return START_NOT_STICKY
        }

        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                handleRestCues()
                maybeNotifyTick()

                val rest = runtimeRest
                val end = rest.restEndEpochMs
                if (end != null && !rest.restPaused) {
                    val diffMs = end - System.currentTimeMillis()
                    if (diffMs in 0..11_000) {
                        val msToNextBoundary = diffMs % 1000
                        delay(if (msToNextBoundary == 0L) 1000L else msToNextBoundary)
                        continue
                    }
                }
                delay(500)
            }
        }

        return START_NOT_STICKY
    }

    /**
     * Fires beeps/vibration around the rest countdown:
     *  - One warning beep (plus optional vibration) at 10 seconds remaining.
     *  - A short beep (plus optional vibration) each second at 5..1 seconds remaining.
     *  - A long beep + vibration at 0.
     * Runs off the runtime-store state, so it works regardless of whether the UI is visible,
     * the app is backgrounded, or the phone is locked.
     */
    private fun handleRestCues() {
        val rest = runtimeRest
        val end = rest.restEndEpochMs
        if (end == null || rest.restPaused) {
            lastCueSecond = -1
            cueArmedForEndMs = -1L
            releaseWakeLock()
            return
        }

        // A new end timestamp = a new countdown; re-arm the cues.
        if (end != cueArmedForEndMs) {
            cueArmedForEndMs = end
            lastCueSecond = -1
            android.util.Log.d("ProYou", "Rest countdown armed: end=$end, now=${System.currentTimeMillis()}")
        }

        val now = System.currentTimeMillis()
        val remaining = RestTimerMath.remainingSeconds(end, now)
        if (remaining > 0) acquireWakeLock(end)

        // Note: For debugging, log the remaining seconds and beep triggers
        if (remaining <= 10 && remaining > 0) {
            android.util.Log.d("ProYou", "Rest countdown: $remaining seconds remaining, lastCueSecond=$lastCueSecond")
        }

        when {
            remaining == 10 && lastCueSecond != 10 -> {
                lastCueSecond = 10
                android.util.Log.d("ProYou", "🔊 BEEP: 10 seconds remaining (volume=$beepVolumePercent%, settings loaded=$settingsLoaded)")
                playTone(ToneGenerator.TONE_PROP_BEEP, 120)
                if (countdownVibrationEnabled) vibrate(this, 100)
            }
            remaining in 1..5 && lastCueSecond != remaining -> {
                lastCueSecond = remaining
                android.util.Log.d("ProYou", "🔊 BEEP: $remaining seconds remaining (volume=$beepVolumePercent%, settings loaded=$settingsLoaded)")
                playTone(ToneGenerator.TONE_PROP_BEEP, 120)
                if (countdownVibrationEnabled) vibrate(this, 100)
            }
            remaining == 0 && lastCueSecond != 0 -> {
                lastCueSecond = 0
                android.util.Log.d("ProYou", "🔊 BEEP: Time's up! (volume=$beepVolumePercent%, settings loaded=$settingsLoaded)")
                playTone(ToneGenerator.TONE_DTMF_0, 800)
                vibrate(this, 500)
                releaseWakeLock()
            }
        }
    }

    private fun playTone(tone: Int, durationMs: Int) {
        try {
            val volume = beepVolumePercent
            if (volume <= 0) return

            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val focusRequest = android.media.AudioFocusRequest.Builder(
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
                .setAudioAttributes(attr)
                .build()

            audioManager.requestAudioFocus(focusRequest)

            if (toneGenerator == null || toneGeneratorVolume != volume) {
                toneGenerator?.release()
                toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, volume)
                toneGeneratorVolume = volume
            }

            toneGenerator?.startTone(tone, durationMs)

            scope.launch {
                delay(durationMs.toLong() + 150)
                audioManager.abandonAudioFocusRequest(focusRequest)
            }
        } catch (e: Exception) {
            android.util.Log.e("ProYou", "Rest timer tone playback failed", e)
        }
    }

    private fun acquireWakeLock(endEpochMs: Long) {
        if (wakeLock?.isHeld == true) return
        val remainingMs = endEpochMs - System.currentTimeMillis()
        if (remainingMs <= 0) return
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ProYou:RestTimer").apply {
                setReferenceCounted(false)
                // Auto-expires shortly after the countdown ends — bounded battery cost.
                acquire(remainingMs + 5_000L)
            }
        } catch (e: Exception) {
            android.util.Log.w("ProYou", "Rest timer wake lock failed", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (e: Exception) {
            // ignore
        }
        wakeLock = null
    }

    /**
     * Rebuild the notification only on real state changes (rest started/stopped/adjusted, or
     * the ready-state default duration changing) — while actively resting, the countdown
     * itself ticks natively via the notification's chronometer, so no per-second rebuild
     * is needed to keep it accurate.
     */
    private fun maybeNotifyTick() {
        val rest = runtimeRest
        val end = rest.restEndEpochMs
        val active = end != null && !rest.restPaused
        val keyA = if (active) end!! else -(rest.restDefaultSeconds.toLong()) - 1L
        if (keyA != lastNotifiedKeyA || active != lastNotifiedKeyB) {
            lastNotifiedKeyA = keyA
            lastNotifiedKeyB = active
            val notification = buildNotification()
            safeNotify(notification)
        }
    }

    /**
     * NotificationManagerCompat.notify() requires POST_NOTIFICATIONS on Android 13+; calling
     * it without the permission throws on some OEM builds. Check before posting.
     */
    private fun safeNotify(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                android.util.Log.w("ProYou", "POST_NOTIFICATIONS permission not granted, skipping notification")
                return
            }
            android.util.Log.d("ProYou", "POST_NOTIFICATIONS permission granted")
        }
        android.util.Log.d("ProYou", "Posting notification with ID=$NOTIFICATION_ID")
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
            android.util.Log.d("ProYou", "Notification posted successfully")
        } catch (e: Exception) {
            android.util.Log.e("ProYou", "Failed to post notification", e)
        }
    }

    override fun onDestroy() {
        tickJob?.cancel()
        scope.cancel()
        releaseWakeLock()
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            // ignore
        }
        toneGenerator = null
        super.onDestroy()
    }

    private fun buildNotification(): android.app.Notification {
        val rest = runtimeRest
        val end = rest.restEndEpochMs
        val isCountingDown = end != null && !rest.restPaused

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_ACTIVE_WORKOUT, true)
            workoutDayId?.let { putExtra(EXTRA_WORKOUT_DAY_ID, it) }
            putExtra(EXTRA_STARTED_AT_EPOCH_MS, startedAtEpochMs)
        }
        val pending = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val startRestPendingIntent = PendingIntent.getService(
            this, 1,
            Intent(this, ActiveWorkoutService::class.java).apply { action = ACTION_START_REST },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val views = RemoteViews(packageName, R.layout.notification_rest)
        views.setOnClickPendingIntent(R.id.notif_start_rest_btn, startRestPendingIntent)

        if (isCountingDown) {
            views.setViewVisibility(R.id.notif_chronometer, View.VISIBLE)
            views.setViewVisibility(R.id.notif_time_text, View.GONE)
            val base = SystemClock.elapsedRealtime() + (end!! - System.currentTimeMillis()) + 999L
            views.setChronometer(R.id.notif_chronometer, base, null, true)
            views.setChronometerCountDown(R.id.notif_chronometer, true)
        } else {
            views.setViewVisibility(R.id.notif_chronometer, View.GONE)
            views.setViewVisibility(R.id.notif_time_text, View.VISIBLE)
            val def = rest.restDefaultSeconds
            views.setTextViewText(R.id.notif_time_text, if (def > 0) formatMmSs(def.toLong()) else "00:00")
        }

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ACTIVE)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setCustomContentView(views)
            .setCustomBigContentView(views)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pending)
            .setShowWhen(false)
            .build()
    }

    private fun formatMmSs(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }

    companion object {
        private const val ACTION_START = "com.ironlog.app.service.action.START"
        private const val ACTION_START_REST = "com.ironlog.app.service.action.START_REST"
        private const val ACTION_UPDATE_REST = "com.ironlog.app.service.action.UPDATE_REST"
        private const val ACTION_CLEAR_REST = "com.ironlog.app.service.action.CLEAR_REST"

        private const val EXTRA_STARTED_AT_EPOCH_MS = "started_at_epoch_ms"
        private const val EXTRA_WORKOUT_DAY_ID = "workout_day_id"
        private const val EXTRA_OPEN_ACTIVE_WORKOUT = "open_active_workout"
        private const val EXTRA_REST_END_EPOCH_MS = "rest_end_epoch_ms"
        private const val EXTRA_REST_PAUSED = "rest_paused"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context, workoutDayId: Int, startedAtEpochMillis: Long) {
            // Channels are created once in IronLogApplication.onCreate(); we no longer
            // duplicate that work here on every service start.
            val intent = Intent(context, ActiveWorkoutService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_WORKOUT_DAY_ID, workoutDayId)
                putExtra(EXTRA_STARTED_AT_EPOCH_MS, startedAtEpochMillis)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun updateRestTimer(context: Context, restEndEpochMs: Long, paused: Boolean) {
            val intent = Intent(context, ActiveWorkoutService::class.java).apply {
                action = ACTION_UPDATE_REST
                putExtra(EXTRA_REST_END_EPOCH_MS, restEndEpochMs)
                putExtra(EXTRA_REST_PAUSED, paused)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun clearRestTimer(context: Context) {
            val intent = Intent(context, ActiveWorkoutService::class.java).apply {
                action = ACTION_CLEAR_REST
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ActiveWorkoutService::class.java))
        }
    }
}

private fun vibrate(context: Context, durationMs: Long) {
    try {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() == true) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    } catch (e: Exception) {
        android.util.Log.w("ProYou", "Rest timer vibration failed", e)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ActiveWorkoutServiceEntryPoint {
    fun restRuntimeStore(): RestTimerRuntimeStore
    fun settingsStore(): SettingsStore
}
