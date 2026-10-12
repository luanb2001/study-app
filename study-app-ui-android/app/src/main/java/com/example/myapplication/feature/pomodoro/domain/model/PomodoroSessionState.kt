package com.example.myapplication.feature.pomodoro.domain.model

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

        if (!this.isRunning) return this.remainingSeconds

        return ((this.deadlineElapsedRealtime - nowElapsedRealtime + 999L) / 1_000L)
            .toInt()
            .coerceAtLeast(0)
    }

    fun snapshot(nowElapsedRealtime: Long = SystemClock.elapsedRealtime()): PomodoroSessionState =
        copy(
            remainingSeconds = currentRemainingSeconds(nowElapsedRealtime),
            deadlineElapsedRealtime = if (this.isRunning) this.deadlineElapsedRealtime else 0L
        )
}
