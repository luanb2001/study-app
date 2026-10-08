package com.example.myapplication.feature.pomodoro

import android.content.Context
import android.os.SystemClock

enum class PomodoroPhase {
    STUDY,
    BREAK,
    COMPLETED
}

data class PomodoroSessionState(
    val subject: String,
    val studyMinutes: Int,
    val breakMinutes: Int,
    val sessionCount: Int,
    val currentSession: Int,
    val phase: PomodoroPhase,
    val remainingSeconds: Int,
    val isRunning: Boolean,
    val deadlineElapsedRealtime: Long,
    val completedStudyMinutes: Int,
    val completedSessionCount: Int,
    val isReview: Boolean,
    val scheduledStudyId: String
) {
    fun currentRemainingSeconds(nowElapsedRealtime: Long = SystemClock.elapsedRealtime()): Int {

        if (!isRunning) return remainingSeconds

        return ((deadlineElapsedRealtime - nowElapsedRealtime + 999L) / 1_000L)
            .toInt()
            .coerceAtLeast(0)
    }

    fun snapshot(nowElapsedRealtime: Long = SystemClock.elapsedRealtime()): PomodoroSessionState =
        copy(
            remainingSeconds = currentRemainingSeconds(nowElapsedRealtime),
            deadlineElapsedRealtime = if (isRunning) deadlineElapsedRealtime else 0L
        )
}

object PomodoroSessionStore {
    private const val PREFERENCES_NAME = "pomodoro_session"
    private const val KEY_SUBJECT = "subject"
    private const val KEY_STUDY_MINUTES = "study_minutes"
    private const val KEY_BREAK_MINUTES = "break_minutes"
    private const val KEY_SESSION_COUNT = "session_count"
    private const val KEY_CURRENT_SESSION = "current_session"
    private const val KEY_PHASE = "phase"
    private const val KEY_REMAINING_SECONDS = "remaining_seconds"
    private const val KEY_IS_RUNNING = "is_running"
    private const val KEY_DEADLINE = "deadline"
    private const val KEY_COMPLETED_MINUTES = "completed_minutes"
    private const val KEY_COMPLETED_SESSIONS = "completed_sessions"
    private const val KEY_IS_REVIEW = "is_review"
    private const val KEY_SCHEDULED_STUDY_ID = "scheduled_study_id"

    fun load(context: Context): PomodoroSessionState? {
        val preferences = preferences(context)
        val subject = preferences.getString(KEY_SUBJECT, null) ?: return null
        val phaseName = preferences.getString(KEY_PHASE, null) ?: return null
        val phase = PomodoroPhase.entries.firstOrNull { it.name == phaseName } ?: return null
        return PomodoroSessionState(
            subject = subject,
            studyMinutes = preferences.getInt(KEY_STUDY_MINUTES, 25),
            breakMinutes = preferences.getInt(KEY_BREAK_MINUTES, 5),
            sessionCount = preferences.getInt(KEY_SESSION_COUNT, 4),
            currentSession = preferences.getInt(KEY_CURRENT_SESSION, 1),
            phase = phase,
            remainingSeconds = preferences.getInt(KEY_REMAINING_SECONDS, 0),
            isRunning = preferences.getBoolean(KEY_IS_RUNNING, false),
            deadlineElapsedRealtime = preferences.getLong(KEY_DEADLINE, 0L),
            completedStudyMinutes = preferences.getInt(KEY_COMPLETED_MINUTES, 0),
            completedSessionCount = preferences.getInt(KEY_COMPLETED_SESSIONS, 0),
            isReview = preferences.getBoolean(KEY_IS_REVIEW, false),
            scheduledStudyId = preferences.getString(KEY_SCHEDULED_STUDY_ID, "").orEmpty()
        ).snapshot()
    }

    fun save(context: Context, state: PomodoroSessionState) {
        val snapshot = state.snapshot()
        preferences(context).edit()
            .putString(KEY_SUBJECT, snapshot.subject)
            .putInt(KEY_STUDY_MINUTES, snapshot.studyMinutes)
            .putInt(KEY_BREAK_MINUTES, snapshot.breakMinutes)
            .putInt(KEY_SESSION_COUNT, snapshot.sessionCount)
            .putInt(KEY_CURRENT_SESSION, snapshot.currentSession)
            .putString(KEY_PHASE, snapshot.phase.name)
            .putInt(KEY_REMAINING_SECONDS, snapshot.remainingSeconds)
            .putBoolean(KEY_IS_RUNNING, snapshot.isRunning)
            .putLong(KEY_DEADLINE, snapshot.deadlineElapsedRealtime)
            .putInt(KEY_COMPLETED_MINUTES, snapshot.completedStudyMinutes)
            .putInt(KEY_COMPLETED_SESSIONS, snapshot.completedSessionCount)
            .putBoolean(KEY_IS_REVIEW, snapshot.isReview)
            .putString(KEY_SCHEDULED_STUDY_ID, snapshot.scheduledStudyId)
            .apply()
    }

    fun clear(context: Context) {
        preferences(context).edit().clear().apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
