package com.example.myapplication

import com.example.myapplication.feature.study.StudyProgressCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StudyProgressCalculatorTest {
    private val today = LocalDate.of(2026, 10, 1)

    @Test
    fun streakContinuesFromYesterdayBeforeTodaysStudy() {
        val dates = setOf(today.minusDays(1), today.minusDays(2), today.minusDays(3))

        assertEquals(3, StudyProgressCalculator.currentStreakDays(dates, today))
    }

    @Test
    fun streakStartsTodayWhenStudiedToday() {
        val dates = setOf(today, today.minusDays(1))

        assertEquals(2, StudyProgressCalculator.currentStreakDays(dates, today))
    }

    @Test
    fun streakIsZeroWhenTodayAndYesterdayWereMissed() {
        val dates = setOf(today.minusDays(2))

        assertEquals(0, StudyProgressCalculator.currentStreakDays(dates, today))
    }
}
