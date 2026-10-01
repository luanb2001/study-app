package com.example.myapplication.feature.pomodoro

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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R

class PomodoroNotificationService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = createNotification(intent ?: Intent())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(intent: Intent): Notification {
        val subject = intent.getStringExtra(EXTRA_SUBJECT).orEmpty()
        val phase = intent.getStringExtra(EXTRA_PHASE).orEmpty()
        val remainingSeconds = intent.getIntExtra(EXTRA_REMAINING_SECONDS, 0).coerceAtLeast(0)
        val currentSession = intent.getIntExtra(EXTRA_CURRENT_SESSION, 1)
        val sessionCount = intent.getIntExtra(EXTRA_SESSION_COUNT, 1)
        val openAppIntent = PendingIntent.getActivity(
            this,
            OPEN_APP_REQUEST_CODE,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pomodoro_notification)
            .setContentTitle(getString(R.string.pomodoro_notification_title, phase, subject))
            .setContentText(
                getString(
                    R.string.pomodoro_notification_session,
                    currentSession,
                    sessionCount
                )
            )
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .setWhen(System.currentTimeMillis() + remainingSeconds * 1_000L)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setShowWhen(true)

        val notification = builder.build()
        return if (Build.VERSION.SDK_INT >= 36) {
            Notification.Builder.recoverBuilder(this, notification)
                .setRequestPromotedOngoing(true)
                .build()
        } else {
            notification
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.pomodoro_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.pomodoro_notification_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        private const val ACTION_UPDATE = "com.example.myapplication.pomodoro.UPDATE"
        private const val ACTION_STOP = "com.example.myapplication.pomodoro.STOP"
        private const val CHANNEL_ID = "pomodoro_live_session"
        private const val NOTIFICATION_ID = 8101
        private const val OPEN_APP_REQUEST_CODE = 8102
        private const val EXTRA_SUBJECT = "subject"
        private const val EXTRA_PHASE = "phase"
        private const val EXTRA_REMAINING_SECONDS = "remaining_seconds"
        private const val EXTRA_CURRENT_SESSION = "current_session"
        private const val EXTRA_SESSION_COUNT = "session_count"

        fun update(
            context: Context,
            subject: String,
            phase: String,
            remainingSeconds: Int,
            currentSession: Int,
            sessionCount: Int
        ) {
            val intent = Intent(context, PomodoroNotificationService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_SUBJECT, subject)
                putExtra(EXTRA_PHASE, phase)
                putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
                putExtra(EXTRA_CURRENT_SESSION, currentSession)
                putExtra(EXTRA_SESSION_COUNT, sessionCount)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PomodoroNotificationService::class.java).apply {
                action = ACTION_STOP
            })
        }
    }
}
