package com.example.myapplication.feature.study

import java.time.LocalDate

object StudyProgressCalculator {
    fun currentStreakDays(studyDates: Set<LocalDate>, today: LocalDate): Int {
        val startDate = when {
            today in studyDates -> today
            today.minusDays(1) in studyDates -> today.minusDays(1)
            else -> return 0
        }
        return generateSequence(startDate) { it.minusDays(1) }
            .takeWhile { it in studyDates }
            .count()
    }
}
