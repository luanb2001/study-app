package com.example.myapplication.feature.study.data.local

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.example.myapplication.feature.account.data.local.AccountSessionStore
import com.example.myapplication.feature.account.data.local.DeviceIdentityStore
import com.example.myapplication.feature.study.data.mock.Data
import com.example.myapplication.feature.study.data.sync.StudySyncWorker
import com.example.myapplication.feature.study.domain.SpacedRepetitionSchedule
import com.example.myapplication.feature.study.domain.StudyProgressCalculator
import com.example.myapplication.feature.study.domain.model.ReviewDifficulty
import com.example.myapplication.feature.study.domain.model.ReviewSchedule
import com.example.myapplication.feature.study.domain.model.ScheduledStudy
import com.example.myapplication.feature.study.domain.model.StudyEntry
import com.example.myapplication.feature.study.domain.model.StudyProgressSummary
import com.example.myapplication.feature.study.domain.repository.StudyRepository
import java.time.LocalDate
import java.util.Locale

class LocalStudyRepository(
    private val context: Context,
    private val studyDataStore: LocalStudyDataStore = LocalStudyDataStore(context.applicationContext),
    private val reviewScheduleStore: ReviewScheduleStore =
        ReviewScheduleStore(context.applicationContext)
) : StudyRepository {
    private val accountSessionStore = AccountSessionStore(this@LocalStudyRepository.context).load()
    private val accountId = this@LocalStudyRepository.accountSessionStore?.accountId
        ?: DeviceIdentityStore(this@LocalStudyRepository.context).installationId()
    private val studyEntries = mutableStateListOf(*this@LocalStudyRepository.studyDataStore.loadStudies().toTypedArray())
    private val scheduledStudies =
        mutableStateListOf(*this@LocalStudyRepository.studyDataStore.loadScheduledStudies().toTypedArray())
    private val deletedStudyIds = this@LocalStudyRepository.studyDataStore.loadDeletedStudyIds().toMutableSet()
    private val reviewSchedules = mutableStateListOf<ReviewSchedule>().apply {
        addAll(this@LocalStudyRepository.reviewScheduleStore.load())
    }

    override fun all(): List<StudyEntry> = buildList {
        addAll(this@LocalStudyRepository.studyEntries)

        if (Data.ENABLED) {
            addAll(Data.studyEntries.filterNot { it.id in this@LocalStudyRepository.deletedStudyIds })
        }

    }

    override fun scheduled(): List<ScheduledStudy> = buildList {
        addAll(this@LocalStudyRepository.scheduledStudies)
        if (Data.ENABLED) addAll(Data.scheduledStudies)
    }

    override fun dueReviews(today: LocalDate): List<ReviewSchedule> =
        this@LocalStudyRepository.reviewSchedules()
            .filter { !it.dueDate.isAfter(today) }
            .sortedBy { it.dueDate }

    override fun reviewSchedules(): List<ReviewSchedule> =
        this.effectiveReviewSchedules().sortedBy { it.dueDate }

    override fun progressSummary(): StudyProgressSummary =

        if (Data.ENABLED && this@LocalStudyRepository.studyEntries.isEmpty()) {
            Data.progressSummary
        } else {
            this.calculateProgressSummary()
        }

    override fun forSubject(subject: String): List<StudyEntry> =
        all().filter { it.subject.equals(subject, ignoreCase = true) }

    override fun recordStudy(entry: StudyEntry, isReview: Boolean) {
        this@LocalStudyRepository.studyEntries.add(0, entry)

        val scheduleIndex = this@LocalStudyRepository.reviewSchedules.indexOfFirst {
            it.subject.equals(entry.subject, ignoreCase = true)
        }
        val existingSchedule = this@LocalStudyRepository.reviewSchedules.getOrNull(scheduleIndex)
            ?: Data.reviewSchedules.firstOrNull {
                it.subject.equals(entry.subject, ignoreCase = true)
            }
        val nextSchedule = createNextReviewSchedule(
            entry,
            existingSchedule,
            isReview,
            stableReviewId(this@LocalStudyRepository.accountId, entry.subject)
        )

        if (scheduleIndex == -1) {
            this@LocalStudyRepository.reviewSchedules.add(nextSchedule)
        } else {
            this@LocalStudyRepository.reviewSchedules[scheduleIndex] = nextSchedule
        }

        this@LocalStudyRepository.studyDataStore.enqueueStudyUpsert(entry)
        this@LocalStudyRepository.reviewScheduleStore.enqueueUpsert(nextSchedule)
        this@LocalStudyRepository.studyDataStore.saveStudies(this@LocalStudyRepository.studyEntries)
        this@LocalStudyRepository.reviewScheduleStore.save(this@LocalStudyRepository.reviewSchedules)
        this.scheduleSynchronization()
    }

    override fun completeFlashcardReview(subject: String, difficulty: ReviewDifficulty): ReviewSchedule {
        val existingSchedule = this.effectiveReviewSchedules().firstOrNull {
            it.subject.equals(subject, ignoreCase = true)
        }
        val currentIntervalIndex = existingSchedule?.intervalIndex ?: 0
        val nextIntervalIndex = SpacedRepetitionSchedule.intervalIndexAfterReview(
            currentIntervalIndex,
            difficulty
        )
        val nextSchedule = ReviewSchedule(
            subject = existingSchedule?.subject ?: subject,
            dueDate = SpacedRepetitionSchedule.dueDate(
                LocalDate.now(),
                nextIntervalIndex
            ),
            intervalIndex = nextIntervalIndex,
            id = existingSchedule?.id ?: stableReviewId(this@LocalStudyRepository.accountId, subject)
        )
        val scheduleIndex = this@LocalStudyRepository.reviewSchedules.indexOfFirst {
            it.subject.equals(subject, ignoreCase = true)
        }

        if (scheduleIndex == -1) {
            this@LocalStudyRepository.reviewSchedules.add(nextSchedule)
        } else {
            this@LocalStudyRepository.reviewSchedules[scheduleIndex] = nextSchedule
        }

        this@LocalStudyRepository.reviewScheduleStore.enqueueUpsert(nextSchedule)
        this@LocalStudyRepository.reviewScheduleStore.save(this@LocalStudyRepository.reviewSchedules)
        this.scheduleSynchronization()
        return nextSchedule
    }

    override fun schedule(study: ScheduledStudy) {
        this@LocalStudyRepository.scheduledStudies.add(study)
        this@LocalStudyRepository.studyDataStore.enqueueScheduledStudyUpsert(study)
        this@LocalStudyRepository.studyDataStore.saveScheduledStudies(this@LocalStudyRepository.scheduledStudies)
        this.scheduleSynchronization()
    }

    override fun rescheduleReview(review: ReviewSchedule, newDate: LocalDate) {
        val scheduleIndex = this@LocalStudyRepository.reviewSchedules.indexOfFirst {
            it.subject.equals(review.subject, ignoreCase = true)
        }
        val updatedSchedule = review.copy(dueDate = newDate)

        if (scheduleIndex == -1) {
            this@LocalStudyRepository.reviewSchedules.add(updatedSchedule)
        } else {
            this@LocalStudyRepository.reviewSchedules[scheduleIndex] = updatedSchedule
        }

        this@LocalStudyRepository.reviewScheduleStore.enqueueUpsert(updatedSchedule)
        this@LocalStudyRepository.reviewScheduleStore.save(this@LocalStudyRepository.reviewSchedules)
        this.scheduleSynchronization()
    }

    override fun deleteStudy(study: StudyEntry) {
        this@LocalStudyRepository.studyEntries.removeAll { it.id == study.id }
        this@LocalStudyRepository.studyDataStore.enqueueStudyDelete(study.id)
        this@LocalStudyRepository.studyDataStore.deleteStudy(study.id)
        this@LocalStudyRepository.deletedStudyIds.add(study.id)
        this@LocalStudyRepository.studyDataStore.saveDeletedStudyIds(this@LocalStudyRepository.deletedStudyIds)

        if (all().none { it.subject.equals(study.subject, ignoreCase = true) }) {
            val removedReviews = this@LocalStudyRepository.reviewSchedules.filter {
                it.subject.equals(study.subject, ignoreCase = true)
            }

            if (removedReviews.isNotEmpty()) {
                this@LocalStudyRepository.reviewSchedules.removeAll(removedReviews.toSet())
                removedReviews.forEach { this@LocalStudyRepository.reviewScheduleStore.enqueueDelete(it.id) }
                this@LocalStudyRepository.reviewScheduleStore.deleteReviews(removedReviews)
            }

        }

        this.scheduleSynchronization()

    }

    override fun cancelScheduledStudy(study: ScheduledStudy) {
        val removed = this@LocalStudyRepository.scheduledStudies.any { it.id == study.id }

        if (removed) {
            this@LocalStudyRepository.studyDataStore.enqueueScheduledStudyDelete(study.id)
            this@LocalStudyRepository.studyDataStore.deleteScheduledStudy(study.id)
            this@LocalStudyRepository.scheduledStudies.removeAll { it.id == study.id }
            this.scheduleSynchronization()
        }

    }

    fun refreshFromDatabase() {
        this@LocalStudyRepository.studyEntries.clear()
        this@LocalStudyRepository.studyEntries.addAll(this@LocalStudyRepository.studyDataStore.loadStudies())
        this@LocalStudyRepository.scheduledStudies.clear()
        this@LocalStudyRepository.scheduledStudies.addAll(this@LocalStudyRepository.studyDataStore.loadScheduledStudies())
        this@LocalStudyRepository.reviewSchedules.clear()
        this@LocalStudyRepository.reviewSchedules.addAll(this@LocalStudyRepository.reviewScheduleStore.load())
    }

    private fun scheduleSynchronization() {

        if (this@LocalStudyRepository.accountSessionStore != null) {
            StudySyncWorker.enqueue(this@LocalStudyRepository.context, this@LocalStudyRepository.accountId)
        }

    }

    private fun calculateProgressSummary(): StudyProgressSummary {
        val today = LocalDate.now()
        val studies = all()
        val studyDates = studies.map { it.date }.toSet()
        val streakDays = StudyProgressCalculator.currentStreakDays(studyDates, today)
        val monthlyStudyHours = studies
            .filter { it.date.year == today.year && it.date.month == today.month }
            .sumOf { it.durationMinutes } / MINUTES_PER_HOUR
        val subjectCount = studies
            .map { it.subject.lowercase(Locale.ROOT) }
            .distinct()
            .size

        return StudyProgressSummary(
            streakDays = streakDays,
            monthlyStudyHours = monthlyStudyHours,
            subjectCount = subjectCount
        )
    }

    private fun effectiveReviewSchedules(): List<ReviewSchedule> {
        val savedSubjects = this@LocalStudyRepository.reviewSchedules.map { it.subject.lowercase(Locale.ROOT) }.toSet()
        val mockSchedules = if (Data.ENABLED) {
            Data.reviewSchedules.filterNot {
                it.subject.lowercase(Locale.ROOT) in savedSubjects
            }
        } else {
            emptyList()
        }
        return this@LocalStudyRepository.reviewSchedules + mockSchedules
    }

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}

private fun createNextReviewSchedule(
    entry: StudyEntry,
    existingSchedule: ReviewSchedule?,
    isReview: Boolean,
    newReviewId: String
): ReviewSchedule {
    val nextIntervalIndex = if (isReview && existingSchedule != null) {
        SpacedRepetitionSchedule.nextIntervalIndex(existingSchedule.intervalIndex)
    } else {
        0
    }
    return ReviewSchedule(
        subject = existingSchedule?.subject ?: entry.subject,
        dueDate = if (nextIntervalIndex == 0) {
            SpacedRepetitionSchedule.firstReviewDueDate(entry.date)
        } else {
            SpacedRepetitionSchedule.dueDate(entry.date, nextIntervalIndex)
        },
        intervalIndex = nextIntervalIndex,
        id = existingSchedule?.id ?: newReviewId
    )
}

private fun stableReviewId(accountId: String, subject: String): String {
    val key = "${accountId}:${subject.trim().lowercase(Locale.ROOT)}"
    return java.util.UUID.nameUUIDFromBytes(key.toByteArray(Charsets.UTF_8)).toString()
}

private fun stableReviewId(subject: String): String {
    val key = subject.trim().lowercase(Locale.ROOT)
    return java.util.UUID.nameUUIDFromBytes(key.toByteArray(Charsets.UTF_8)).toString()
}
