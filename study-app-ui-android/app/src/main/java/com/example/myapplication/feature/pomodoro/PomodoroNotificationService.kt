package com.example.myapplication.feature.pomodoro

import android.annotation.SuppressLint
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
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.ext.SdkExtensions
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PomodoroNotificationService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var timerJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_RESET) {
            timerJob?.cancel()
            PomodoroSessionStore.clear(this)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val state = when (action) {
            ACTION_START -> PomodoroSessionStore.load(this)
            ACTION_PAUSE -> pauseSession()
            ACTION_RESUME -> resumeSession()
            else -> PomodoroSessionStore.load(this)
        }

        if (state == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForegroundCompat(createNotification(state))

        if (state.isRunning && state.phase != PomodoroPhase.COMPLETED) {
            runTimer()
        } else {
            timerJob?.cancel()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        timerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun pauseSession(): PomodoroSessionState? {
        val state = PomodoroSessionStore.load(this) ?: return null
        return state.copy(
            remainingSeconds = state.currentRemainingSeconds(),
            isRunning = false,
            deadlineElapsedRealtime = 0L
        ).also { PomodoroSessionStore.save(this, it) }
    }

    private fun resumeSession(): PomodoroSessionState? {
        val state = PomodoroSessionStore.load(this) ?: return null

        if (state.phase == PomodoroPhase.COMPLETED) return state

        val resumed = state.copy(
            isRunning = true,
            deadlineElapsedRealtime = SystemClock.elapsedRealtime() +
                state.remainingSeconds * 1_000L
        )
        PomodoroSessionStore.save(this, resumed)
        return resumed
    }

    private fun runTimer() {

        if (timerJob?.isActive == true) return

        timerJob = serviceScope.launch {
            while (isActive) {
                delay(250L)
                var state = PomodoroSessionStore.load(this@PomodoroNotificationService)
                    ?: break

                if (!state.isRunning) break

                while (state.isRunning && state.currentRemainingSeconds() == 0) {
                    val next = advancePhase(state)
                    PomodoroSessionStore.save(this@PomodoroNotificationService, next)

                    if (
                        next.phase != state.phase &&
                        next.phase != PomodoroPhase.COMPLETED
                    ) {
                        getSystemService(Vibrator::class.java)?.vibrate(
                            VibrationEffect.createWaveform(
                                longArrayOf(0, 150, 130, 150, 130, 150),
                                -1
                            )
                        )
                    }

                    state = next

                    if (state.phase == PomodoroPhase.COMPLETED) {
                        updateNotification(state)
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@launch
                    }

                    updateNotification(state)
                }
            }
        }
    }

    private fun advancePhase(state: PomodoroSessionState): PomodoroSessionState {
        val nextDeadline = state.deadlineElapsedRealtime
        return when (state.phase) {
            PomodoroPhase.STUDY -> {
                val completed = state.copy(
                    completedStudyMinutes = state.completedStudyMinutes + state.studyMinutes,
                    completedSessionCount = state.completedSessionCount + 1
                )

                if (state.currentSession >= state.sessionCount) {
                    completed.copy(
                        phase = PomodoroPhase.COMPLETED,
                        remainingSeconds = 0,
                        isRunning = false,
                        deadlineElapsedRealtime = 0L
                    )
                } else {
                    completed.copy(
                        phase = PomodoroPhase.BREAK,
                        currentSession = state.currentSession + 1,
                        remainingSeconds = state.breakMinutes * 60,
                        deadlineElapsedRealtime = nextDeadline + state.breakMinutes * 60_000L
                    )
                }

            }
            PomodoroPhase.BREAK -> state.copy(
                phase = PomodoroPhase.STUDY,
                remainingSeconds = state.studyMinutes * 60,
                deadlineElapsedRealtime = nextDeadline + state.studyMinutes * 60_000L
            )
            PomodoroPhase.COMPLETED -> state
        }
    }

    private fun updateNotification(state: PomodoroSessionState) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, createNotification(state))
    }

    private fun startForegroundCompat(notification: Notification) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

    }

    @SuppressLint("NewApi")
    private fun createNotification(state: PomodoroSessionState): Notification {
        val phase = getString(
            when (state.phase) {
                PomodoroPhase.STUDY -> R.string.pomodoro_study
                PomodoroPhase.BREAK -> R.string.pomodoro_break
                PomodoroPhase.COMPLETED -> R.string.pomodoro_completed
            }
        )
        val openAppIntent = PendingIntent.getActivity(
            this,
            OPEN_APP_REQUEST_CODE,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val remainingSeconds = state.currentRemainingSeconds()
        val contentText = if (state.isRunning) {
            getString(
                R.string.pomodoro_notification_session,
                state.currentSession,
                state.sessionCount
            )
        } else {
            getString(
                R.string.pomodoro_notification_paused,
                remainingSeconds / 60,
                remainingSeconds % 60
            )
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pomodoro_notification)
            .setContentTitle(getString(R.string.pomodoro_notification_title, phase, state.subject))
            .setContentText(contentText)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .setWhen(System.currentTimeMillis() + remainingSeconds * 1_000L)
            .setUsesChronometer(state.isRunning)
            .setChronometerCountDown(true)
            .setShowWhen(state.isRunning)
            .build()

        return if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
            SdkExtensions.getExtensionVersion(Build.VERSION_CODES.BAKLAVA) >= 1
        ) {
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
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val ACTION_START = "com.example.myapplication.pomodoro.START"
        private const val ACTION_PAUSE = "com.example.myapplication.pomodoro.PAUSE"
        private const val ACTION_RESUME = "com.example.myapplication.pomodoro.RESUME"
        private const val ACTION_RESET = "com.example.myapplication.pomodoro.RESET"
        private const val CHANNEL_ID = "pomodoro_live_session"
        private const val NOTIFICATION_ID = 8101
        private const val OPEN_APP_REQUEST_CODE = 8102

        fun start(context: Context, state: PomodoroSessionState) {
            PomodoroSessionStore.save(context, state)
            sendCommand(context, ACTION_START)
        }

        fun pause(context: Context) = sendCommand(context, ACTION_PAUSE)

        fun resume(context: Context) = sendCommand(context, ACTION_RESUME)

        fun reset(context: Context) {
            PomodoroSessionStore.clear(context)
            context.stopService(Intent(context, PomodoroNotificationService::class.java))
        }

        private fun sendCommand(context: Context, action: String) {
            val intent = Intent(context, PomodoroNotificationService::class.java).apply {
                this.action = action
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
