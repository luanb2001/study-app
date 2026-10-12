package com.example.myapplication.feature.pomodoro.data.local

import android.content.Context
import com.example.myapplication.feature.account.data.local.AccountSessionStore
import com.example.myapplication.feature.account.data.local.DeviceIdentityStore
import com.example.myapplication.feature.pomodoro.domain.model.PomodoroPhase
import com.example.myapplication.feature.pomodoro.domain.model.PomodoroSessionState

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

    fun load(context: Context): PomodoroSessionState? = load(context, this.ownerId(context))

    fun load(context: Context, ownerId: String): PomodoroSessionState? {
        this.migrateLegacySession(context)
        val preferences = this.preferences(context)
        val key = { name: String -> this.scopedKey(ownerId, name) }
        val subject = preferences.getString(key(KEY_SUBJECT), null) ?: return null
        val phaseName = preferences.getString(key(KEY_PHASE), null) ?: return null
        val phase = PomodoroPhase.entries.firstOrNull { it.name == phaseName } ?: return null
        return PomodoroSessionState(
            subject = subject,
            studyMinutes = preferences.getInt(key(KEY_STUDY_MINUTES), 25),
            breakMinutes = preferences.getInt(key(KEY_BREAK_MINUTES), 5),
            sessionCount = preferences.getInt(key(KEY_SESSION_COUNT), 4),
            currentSession = preferences.getInt(key(KEY_CURRENT_SESSION), 1),
            phase = phase,
            remainingSeconds = preferences.getInt(key(KEY_REMAINING_SECONDS), 0),
            isRunning = preferences.getBoolean(key(KEY_IS_RUNNING), false),
            deadlineElapsedRealtime = preferences.getLong(key(KEY_DEADLINE), 0L),
            completedStudyMinutes = preferences.getInt(key(KEY_COMPLETED_MINUTES), 0),
            completedSessionCount = preferences.getInt(key(KEY_COMPLETED_SESSIONS), 0),
            isReview = preferences.getBoolean(key(KEY_IS_REVIEW), false),
            scheduledStudyId = preferences.getString(key(KEY_SCHEDULED_STUDY_ID), "").orEmpty()
        ).snapshot()
    }

    fun save(context: Context, state: PomodoroSessionState) =
        save(context, state, this.ownerId(context))

    fun save(context: Context, state: PomodoroSessionState, ownerId: String) {
        this.migrateLegacySession(context)
        val snapshot = state.snapshot()
        val key = { name: String -> this.scopedKey(ownerId, name) }
        this.preferences(context).edit()
            .putString(key(KEY_SUBJECT), snapshot.subject)
            .putInt(key(KEY_STUDY_MINUTES), snapshot.studyMinutes)
            .putInt(key(KEY_BREAK_MINUTES), snapshot.breakMinutes)
            .putInt(key(KEY_SESSION_COUNT), snapshot.sessionCount)
            .putInt(key(KEY_CURRENT_SESSION), snapshot.currentSession)
            .putString(key(KEY_PHASE), snapshot.phase.name)
            .putInt(key(KEY_REMAINING_SECONDS), snapshot.remainingSeconds)
            .putBoolean(key(KEY_IS_RUNNING), snapshot.isRunning)
            .putLong(key(KEY_DEADLINE), snapshot.deadlineElapsedRealtime)
            .putInt(key(KEY_COMPLETED_MINUTES), snapshot.completedStudyMinutes)
            .putInt(key(KEY_COMPLETED_SESSIONS), snapshot.completedSessionCount)
            .putBoolean(key(KEY_IS_REVIEW), snapshot.isReview)
            .putString(key(KEY_SCHEDULED_STUDY_ID), snapshot.scheduledStudyId)
            .apply()
    }

    fun clear(context: Context) = clear(context, this.ownerId(context))

    fun clear(context: Context, ownerId: String) {
        this.migrateLegacySession(context)
        val editor = this.preferences(context).edit()
        SESSION_KEYS.forEach { editor.remove(this.scopedKey(ownerId, it)) }
        editor.apply()
    }

    fun claimDeviceSession(context: Context, installationId: String, accountId: String) {
        val deviceSession = load(context, installationId) ?: return
        save(context, deviceSession, accountId)
        clear(context, installationId)
    }

    private fun ownerId(context: Context): String =
        AccountSessionStore(context).load()?.accountId
            ?: DeviceIdentityStore(context).installationId()

    private fun scopedKey(ownerId: String, key: String) = "$ownerId.$key"

    private fun migrateLegacySession(context: Context) {
        val preferences = this.preferences(context)
        if (preferences.getString(KEY_SUBJECT, null) == null) return
        val currentOwnerId = this.ownerId(context)
        val subjectKey = this.scopedKey(currentOwnerId, KEY_SUBJECT)

        if (preferences.getString(subjectKey, null) == null) {
            val editor = preferences.edit()
            SESSION_KEYS.forEach { key ->

                when (val value = preferences.all[key]) {
                    is String -> editor.putString(this.scopedKey(currentOwnerId, key), value)
                    is Int -> editor.putInt(this.scopedKey(currentOwnerId, key), value)
                    is Boolean -> editor.putBoolean(this.scopedKey(currentOwnerId, key), value)
                    is Long -> editor.putLong(this.scopedKey(currentOwnerId, key), value)
                }

            }
            editor.apply()
        }

        val editor = preferences.edit()
        SESSION_KEYS.forEach(editor::remove)
        editor.apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val SESSION_KEYS = setOf(
        KEY_SUBJECT,
        KEY_STUDY_MINUTES,
        KEY_BREAK_MINUTES,
        KEY_SESSION_COUNT,
        KEY_CURRENT_SESSION,
        KEY_PHASE,
        KEY_REMAINING_SECONDS,
        KEY_IS_RUNNING,
        KEY_DEADLINE,
        KEY_COMPLETED_MINUTES,
        KEY_COMPLETED_SESSIONS,
        KEY_IS_REVIEW,
        KEY_SCHEDULED_STUDY_ID
    )
}
