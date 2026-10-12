package com.example.myapplication.feature.study.domain.model

import java.time.LocalDate
import java.util.UUID

const val MAX_TIMER_MINUTES = 1_440

data class StudyEntry(
    val date: LocalDate,
    val subject: String,
    val description: String,
    val durationMinutes: Int,
    val sessionCount: Int = 1,
    val id: String = UUID.randomUUID().toString()
)

data class ScheduledStudy(
    val date: LocalDate,
    val subject: String,
    val sessionCount: Int,
    val studyMinutes: Int,
    val breakMinutes: Int,
    val id: String = UUID.randomUUID().toString()
)

data class ReviewSchedule(
    val subject: String,
    val dueDate: LocalDate,
    val intervalIndex: Int,
    val id: String = UUID.randomUUID().toString()
)

enum class ReviewDifficulty {
    AGAIN,
    HARD,
    EASY
}

data class StudyProgressSummary(
    val streakDays: Int,
    val monthlyStudyHours: Int,
    val subjectCount: Int
)
