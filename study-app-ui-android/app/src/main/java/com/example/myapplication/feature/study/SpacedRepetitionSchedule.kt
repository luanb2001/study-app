package com.example.myapplication.feature.study

import java.time.LocalDate

object SpacedRepetitionSchedule {
    val intervalsInDays = listOf(1L, 7L, 21L, 60L)

    fun firstReviewDueDate(studyDate: LocalDate): LocalDate =
        dueDate(studyDate, intervalIndex = 0)

    fun dueDate(studyDate: LocalDate, intervalIndex: Int): LocalDate =
        studyDate.plusDays(intervalsInDays[intervalIndex])

    fun nextIntervalIndex(currentIndex: Int): Int =
        (currentIndex + 1).coerceAtMost(intervalsInDays.lastIndex)

    fun intervalIndexAfterReview(
        currentIndex: Int,
        difficulty: ReviewDifficulty
    ): Int = when (difficulty) {
        ReviewDifficulty.AGAIN -> 0
        ReviewDifficulty.HARD -> currentIndex.coerceIn(0, intervalsInDays.lastIndex)
        ReviewDifficulty.EASY -> nextIntervalIndex(currentIndex.coerceAtLeast(-1))
    }
}
