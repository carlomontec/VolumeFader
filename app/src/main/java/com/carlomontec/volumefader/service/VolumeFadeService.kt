package com.carlomontec.volumefader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.carlomontec.volumefader.MainActivity
import com.carlomontec.volumefader.R
import com.carlomontec.volumefader.audio.AudioVolumeManager
import com.carlomontec.volumefader.audio.FadeCurve
import com.carlomontec.volumefader.audio.MediaPlaybackController
import com.carlomontec.volumefader.model.FadeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VolumeFadeService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var fadeJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private lateinit var audioManager: AudioVolumeManager
    private lateinit var mediaController: MediaPlaybackController
    private lateinit var notificationManager: NotificationManager

    private var initialVolume: Int = 0
    private var totalDurationMs: Long = 0L
    private var elapsedMs: Long = 0L
    private var curve: FadeCurve = FadeCurve.LOGARITHMIC
    private var pauseMediaOnFinish: Boolean = true

    companion object {
        const val CHANNEL_ID = "volume_fade_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.carlomontec.volumefader.ACTION_START"
        const val ACTION_STOP = "com.carlomontec.volumefader.ACTION_STOP"
        const val ACTION_EXTEND_5M = "com.carlomontec.volumefader.ACTION_EXTEND_5M"

        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val EXTRA_CURVE = "extra_curve"
        const val EXTRA_PAUSE_ON_FINISH = "extra_pause_on_finish"

        private val _statusFlow = MutableStateFlow<FadeStatus>(FadeStatus.Idle)
        val statusFlow: StateFlow<FadeStatus> = _statusFlow.asStateFlow()

        fun startIntent(
            context: Context,
            durationMinutes: Int,
            curve: FadeCurve,
            pauseOnFinish: Boolean
        ): Intent {
            return Intent(context, VolumeFadeService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
                putExtra(EXTRA_CURVE, curve.name)
                putExtra(EXTRA_PAUSE_ON_FINISH, pauseOnFinish)
            }
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, VolumeFadeService::class.java).apply {
                action = ACTION_STOP
            }
        }

        fun extendIntent(context: Context): Intent {
            return Intent(context, VolumeFadeService::class.java).apply {
                action = ACTION_EXTEND_5M
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = AudioVolumeManager(this)
        mediaController = MediaPlaybackController(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "VolumeFader::FadeTimerWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 30)
                val curveName = intent.getStringExtra(EXTRA_CURVE) ?: FadeCurve.LOGARITHMIC.name
                curve = runCatching { FadeCurve.valueOf(curveName) }.getOrDefault(FadeCurve.LOGARITHMIC)
                pauseMediaOnFinish = intent.getBooleanExtra(EXTRA_PAUSE_ON_FINISH, true)

                startFade(durationMinutes)
            }
            ACTION_STOP -> {
                stopFade()
            }
            ACTION_EXTEND_5M -> {
                extendDuration(5 * 60 * 1000L)
            }
        }
        return START_NOT_STICKY
    }

    private fun startFade(minutes: Int) {
        fadeJob?.cancel()
        wakeLock?.acquire(minutes * 60 * 1000L + 60_000L)

        initialVolume = audioManager.currentVolume
        totalDurationMs = minutes * 60 * 1000L
        elapsedMs = 0L

        val initialNotification = buildNotification(totalDurationMs)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        fadeJob = serviceScope.launch {
            val updateIntervalMs = 1000L
            while (isActive && elapsedMs < totalDurationMs) {
                delay(updateIntervalMs)
                elapsedMs += updateIntervalMs

                val progress = (elapsedMs.toFloat() / totalDurationMs).coerceIn(0.0f, 1.0f)
                val factor = curve.calculateVolumeFactor(progress)

                audioManager.setVolumeFraction(initialVolume, factor)

                val remainingMs = (totalDurationMs - elapsedMs).coerceAtLeast(0L)
                _statusFlow.value = FadeStatus.Running(
                    totalDurationMs = totalDurationMs,
                    remainingMs = remainingMs,
                    currentVolumeStep = audioManager.currentVolume,
                    maxVolumeStep = audioManager.maxVolume,
                    targetVolumePercent = factor
                )

                notificationManager.notify(NOTIFICATION_ID, buildNotification(remainingMs))
            }

            // Fade completed
            audioManager.setVolume(0)
            if (pauseMediaOnFinish) {
                mediaController.pausePlayback()
            }
            _statusFlow.value = FadeStatus.Completed
            stopSelf()
        }
    }

    private fun extendDuration(extraMs: Long) {
        totalDurationMs += extraMs
        val remainingMs = (totalDurationMs - elapsedMs).coerceAtLeast(0L)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(remainingMs))
    }

    private fun stopFade() {
        fadeJob?.cancel()
        _statusFlow.value = FadeStatus.Idle
        stopSelf()
    }

    private fun buildNotification(remainingMs: Long): Notification {
        val totalSecs = (remainingMs / 1000).coerceAtLeast(0)
        val minutes = totalSecs / 60
        val seconds = totalSecs % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)

        val activityIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent(this),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val extendPendingIntent = PendingIntent.getService(
            this,
            2,
            extendIntent(this),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fading Volume to Sleep")
            .setContentText("Remaining: $timeString • Vol: ${audioManager.currentVolume}/${audioManager.maxVolume}")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(activityIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", stopPendingIntent)
            .addAction(android.R.drawable.ic_input_add, "+5 min", extendPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        fadeJob?.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        _statusFlow.value = FadeStatus.Idle
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
