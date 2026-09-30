package com.example.myapplication.feature.study

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import java.time.LocalDate

class LocalStudyRepository(
    context: Context,
    private val studyDataStore: LocalStudyDataStore = LocalStudyDataStore(context),
    private val reviewScheduleStore: ReviewScheduleStore = ReviewScheduleStore(context)
) : StudyRepository {
    private val studyEntries = mutableStateListOf(*studyDataStore.loadStudies().toTypedArray())
    private val scheduledStudies =
        mutableStateListOf(*studyDataStore.loadScheduledStudies().toTypedArray())
    private val deletedStudyIds = studyDataStore.loadDeletedStudyIds().toMutableSet()
    private val reviewSchedules = mutableStateListOf<ReviewSchedule>().apply {
        addAll(reviewScheduleStore.load())
    }

    override fun all(): List<StudyEntry> = buildList {
        addAll(studyEntries)
        if (Data.ENABLED) {
            addAll(Data.studyEntries.filterNot { it.id in deletedStudyIds })
        }
    }

    override fun scheduled(): List<ScheduledStudy> = buildList {
        addAll(scheduledStudies)
        if (Data.ENABLED) addAll(Data.scheduledStudies)
    }

    override fun dueReviews(today: LocalDate): List<ReviewSchedule> =
        effectiveReviewSchedules()
            .filter { !it.dueDate.isAfter(today) }
            .sortedBy { it.dueDate }

    override fun progressSummary(): StudyProgressSummary =
        if (Data.ENABLED && studyEntries.isEmpty()) {
            Data.progressSummary
        } else {
            calculateProgressSummary()
        }

    override fun forSubject(subject: String): List<StudyEntry> =
        all().filter { it.subject.equals(subject, ignoreCase = true) }

    override fun recordStudy(entry: StudyEntry, isReview: Boolean) {
        studyEntries.add(0, entry)

        val scheduleIndex = reviewSchedules.indexOfFirst {
            it.subject.equals(entry.subject, ignoreCase = true)
        }
        val existingSchedule = reviewSchedules.getOrNull(scheduleIndex)
            ?: Data.reviewSchedules.firstOrNull {
                it.subject.equals(entry.subject, ignoreCase = true)
            }
        val nextSchedule = createNextReviewSchedule(entry, existingSchedule, isReview)

        if (scheduleIndex == -1) {
            reviewSchedules.add(nextSchedule)
        } else {
            reviewSchedules[scheduleIndex] = nextSchedule
        }
        studyDataStore.saveStudies(studyEntries)
        reviewScheduleStore.save(reviewSchedules)
    }

    override fun schedule(study: ScheduledStudy) {
        scheduledStudies.add(study)
        studyDataStore.saveScheduledStudies(scheduledStudies)
    }

    override fun deleteStudy(study: StudyEntry) {
        studyEntries.removeAll { it.id == study.id }
        deletedStudyIds.add(study.id)
        studyDataStore.saveStudies(studyEntries)
        studyDataStore.saveDeletedStudyIds(deletedStudyIds)
    }

    override fun cancelScheduledStudy(study: ScheduledStudy) {
        val removed = scheduledStudies.removeAll { it.id == study.id }
        if (removed) studyDataStore.saveScheduledStudies(scheduledStudies)
    }

    private fun calculateProgressSummary(): StudyProgressSummary {
        val today = LocalDate.now()
        val studies = all()
        val studyDates = studies.map { it.date }.toSet()
        val streakDays = generateSequence(today) { it.minusDays(1) }
            .takeWhile { it in studyDates }
            .count()
        val monthlyStudyHours = studies
            .filter { it.date.year == today.year && it.date.month == today.month }
            .sumOf { it.durationMinutes } / MINUTES_PER_HOUR
        val subjectCount = studies
            .map { it.subject.lowercase() }
            .distinct()
            .size

        return StudyProgressSummary(
            streakDays = streakDays,
            monthlyStudyHours = monthlyStudyHours,
            subjectCount = subjectCount
        )
    }

    private fun effectiveReviewSchedules(): List<ReviewSchedule> {
        val savedSubjects = reviewSchedules.map { it.subject.lowercase() }.toSet()
        val mockSchedules = if (Data.ENABLED) {
            Data.reviewSchedules.filterNot { it.subject.lowercase() in savedSubjects }
        } else {
            emptyList()
        }
        return reviewSchedules + mockSchedules
    }

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}

private fun createNextReviewSchedule(
    entry: StudyEntry,
    existingSchedule: ReviewSchedule?,
    isReview: Boolean
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
        intervalIndex = nextIntervalIndex
    )
}
