package com.example.myapplication

import com.example.myapplication.feature.study.SpacedRepetitionSchedule
import com.example.myapplication.feature.study.ReviewDifficulty
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SpacedRepetitionScheduleTest {
    @Test
    fun firstReviewIsBasedOnManuallySelectedStudyDate() {
        val manuallySelectedDate = LocalDate.of(2026, 8, 15)

        val firstReviewDate =
            SpacedRepetitionSchedule.firstReviewDueDate(manuallySelectedDate)

        assertEquals(LocalDate.of(2026, 8, 16), firstReviewDate)
    }

    @Test
    fun schedulesExpandingIntervalsFromEachCompletedReview() {
        val firstStudyDate = LocalDate.of(2026, 9, 1)
        val firstReview = SpacedRepetitionSchedule.dueDate(firstStudyDate, 0)
        val secondReview = SpacedRepetitionSchedule.dueDate(
            firstReview,
            SpacedRepetitionSchedule.nextIntervalIndex(0)
        )
        val thirdReview = SpacedRepetitionSchedule.dueDate(
            secondReview,
            SpacedRepetitionSchedule.nextIntervalIndex(1)
        )
        val fourthReview = SpacedRepetitionSchedule.dueDate(
            thirdReview,
            SpacedRepetitionSchedule.nextIntervalIndex(2)
        )

        assertEquals(LocalDate.of(2026, 9, 2), firstReview)
        assertEquals(LocalDate.of(2026, 9, 9), secondReview)
        assertEquals(LocalDate.of(2026, 9, 30), thirdReview)
        assertEquals(LocalDate.of(2026, 11, 29), fourthReview)
    }

    @Test
    fun keepsUsingSixtyDayIntervalAfterLastStep() {
        assertEquals(
            SpacedRepetitionSchedule.intervalsInDays.lastIndex,
            SpacedRepetitionSchedule.nextIntervalIndex(
                SpacedRepetitionSchedule.intervalsInDays.lastIndex
            )
        )
    }

    @Test
    fun flashcardDifficultyAdjustsTheNextInterval() {
        assertEquals(
            0,
            SpacedRepetitionSchedule.intervalIndexAfterReview(
                currentIndex = 2,
                difficulty = ReviewDifficulty.AGAIN
            )
        )
        assertEquals(
            2,
            SpacedRepetitionSchedule.intervalIndexAfterReview(
                currentIndex = 2,
                difficulty = ReviewDifficulty.HARD
            )
        )
        assertEquals(
            3,
            SpacedRepetitionSchedule.intervalIndexAfterReview(
                currentIndex = 2,
                difficulty = ReviewDifficulty.EASY
            )
        )
    }
}
