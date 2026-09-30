package com.example.myapplication.feature.study

import java.time.LocalDate
import java.util.UUID

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
    val intervalIndex: Int
)

data class StudyProgressSummary(
    val streakDays: Int,
    val monthlyStudyHours: Int,
    val subjectCount: Int
)

/**
 * The app's single boundary for study data. Compose screens receive this contract
 * instead of depending on storage or HTTP details.
 *
 * The current prototype is synchronous and local. Before adding HTTP calls, expose
 * asynchronous operations through ViewModels so Compose never waits on the network.
 */
interface StudyRepository {
    // Backend: GET /api/studies
    fun all(): List<StudyEntry>

    // Backend: GET /api/studies/scheduled
    fun scheduled(): List<ScheduledStudy>

    // Backend: GET /api/reviews?dueOnOrBefore={date}
    fun dueReviews(today: LocalDate = LocalDate.now()): List<ReviewSchedule>

    // Backend: GET /api/studies/summary
    fun progressSummary(): StudyProgressSummary

    // Backend: GET /api/subjects/{subjectId}/studies
    fun forSubject(subject: String): List<StudyEntry>

    // Backend: POST /api/studies or POST /api/reviews/{reviewId}/complete
    fun recordStudy(entry: StudyEntry, isReview: Boolean = false)

    // Backend: POST /api/studies/scheduled
    fun schedule(study: ScheduledStudy)

    // Backend: DELETE /api/studies/{studyId}
    fun deleteStudy(study: StudyEntry)

    // Backend: DELETE /api/studies/scheduled/{scheduledStudyId}
    fun cancelScheduledStudy(study: ScheduledStudy)
}
