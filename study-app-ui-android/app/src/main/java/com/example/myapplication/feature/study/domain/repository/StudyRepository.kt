package com.example.myapplication.feature.study.domain.repository

import com.example.myapplication.feature.study.domain.model.ReviewDifficulty
import com.example.myapplication.feature.study.domain.model.ReviewSchedule
import com.example.myapplication.feature.study.domain.model.ScheduledStudy
import com.example.myapplication.feature.study.domain.model.StudyEntry
import com.example.myapplication.feature.study.domain.model.StudyProgressSummary
import java.time.LocalDate

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

    fun reviewSchedules(): List<ReviewSchedule>

    // Backend: GET /api/studies/summary
    fun progressSummary(): StudyProgressSummary

    // Backend: GET /api/subjects/{subjectId}/studies
    fun forSubject(subject: String): List<StudyEntry>

    // Backend: POST /api/studies or POST /api/reviews/{reviewId}/complete
    fun recordStudy(entry: StudyEntry, isReview: Boolean = false)

    fun completeFlashcardReview(
        subject: String,
        difficulty: ReviewDifficulty
    ): ReviewSchedule

    // Backend: POST /api/studies/scheduled
    fun schedule(study: ScheduledStudy)

    fun rescheduleReview(review: ReviewSchedule, newDate: LocalDate)

    // Backend: DELETE /api/studies/{studyId}
    fun deleteStudy(study: StudyEntry)

    // Backend: DELETE /api/studies/scheduled/{scheduledStudyId}
    fun cancelScheduledStudy(study: ScheduledStudy)
}
