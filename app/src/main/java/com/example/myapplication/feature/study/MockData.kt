package com.example.myapplication.feature.study

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object Data {
    // Set to false to disable all seeded demonstration data in the local repository.
    const val ENABLED = false

    val studyEntries: List<StudyEntry> = createStudyEntries()

    val scheduledStudies: List<ScheduledStudy>
        get() {
            val today = LocalDate.now()
            return listOf(
                ScheduledStudy(
                    date = today.plusDays(1),
                    subject = "Kotlin",
                    sessionCount = 2,
                    studyMinutes = 25,
                    breakMinutes = 5
                ),
                ScheduledStudy(
                    date = today.plusDays(3),
                    subject = "Compose",
                    sessionCount = 3,
                    studyMinutes = 30,
                    breakMinutes = 5
                )
            )
        }

    val reviewSchedules: List<ReviewSchedule>
        get() {
            val today = LocalDate.now()
            return listOf(
                ReviewSchedule("Java Streams", today, intervalIndex = 1),
                ReviewSchedule("RabbitMQ", today, intervalIndex = 0),
                ReviewSchedule(
                    "Spring Transactions",
                    today.minusDays(1),
                    intervalIndex = 2
                ),
                ReviewSchedule("Kotlin", today.plusDays(3), intervalIndex = 3)
            )
        }

    val progressSummary = StudyProgressSummary(
        streakDays = 12,
        monthlyStudyHours = 18,
        subjectCount = 27
    )

    private fun createStudyEntries(today: LocalDate = LocalDate.now()): List<StudyEntry> {
        val subjects = listOf(
            "Java Streams",
            "RabbitMQ",
            "Spring Transactions",
            "Kotlin",
            "SQL",
            "Compose"
        )
        val recentEntries = listOf(
            StudyEntry(
                date = today,
                subject = "Kotlin",
                description = "Revisei coroutines e os efeitos colaterais no Compose.",
                durationMinutes = 40
            ),
            StudyEntry(
                date = today.minusDays(1),
                subject = "Java Streams",
                description = "Pratiquei map, filter e collectors.",
                durationMinutes = 45
            ),
            StudyEntry(
                date = today.minusDays(3),
                subject = "Spring Transactions",
                description = "Estudei propagação de transações e rollback.",
                durationMinutes = 50
            ),
            StudyEntry(
                date = today.minusDays(3),
                subject = "Kotlin",
                description = "Implementei exercícios com funções de extensão.",
                durationMinutes = 35
            ),
            StudyEntry(
                date = today.minusDays(8),
                subject = "RabbitMQ",
                description = "Revisei filas, exchanges e acknowledgements.",
                durationMinutes = 60
            ),
            StudyEntry(
                date = today.minusDays(35),
                subject = "SQL",
                description = "Pratiquei joins e agregações.",
                durationMinutes = 55
            )
        )
        val generatedEntries = (10L..100L step 2).map { daysAgo ->
            val subject = subjects[((daysAgo / 2) % subjects.size).toInt()]
            StudyEntry(
                date = today.minusDays(daysAgo),
                subject = subject,
                description = "Registro de demonstração sobre $subject.",
                durationMinutes = 25 + ((daysAgo.toInt() % 5) * 10)
            )
        }
        val contributionEntries = (103L until today.dayOfYear.toLong() step 3)
            .map(today::minusDays)
            .filter { it.year == today.year }
            .flatMap { studyDate ->
                val daysAgo = ChronoUnit.DAYS.between(studyDate, today)
                val contributionCount = when ((daysAgo / 3) % 3) {
                    0L -> 1
                    1L -> 3
                    else -> 5
                }
                (0 until contributionCount).map { session ->
                    val subject = subjects[
                        ((daysAgo + session) % subjects.size).toInt()
                    ]
                    StudyEntry(
                        date = studyDate,
                        subject = subject,
                        description = "Registro de demonstração sobre $subject.",
                        durationMinutes = 25 + ((daysAgo.toInt() + session) % 5) * 10
                    )
                }
            }

        return recentEntries + generatedEntries + contributionEntries
    }
}
