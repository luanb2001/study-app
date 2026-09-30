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
}
