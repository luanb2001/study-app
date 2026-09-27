package com.example.myapplication.feature.study

import androidx.compose.runtime.mutableStateListOf
import java.time.LocalDate

data class StudyEntry(
    val date: LocalDate,
    val subject: String,
    val description: String,
    val durationMinutes: Int
)

object StudyRepository {
    private val studyEntries = mutableStateListOf(
        StudyEntry(
            date = LocalDate.now(),
            subject = "Kotlin",
            description = "Revisei coroutines e os efeitos colaterais no Compose.",
            durationMinutes = 40
        ),
        StudyEntry(
            date = LocalDate.now().minusDays(1),
            subject = "Java Streams",
            description = "Pratiquei map, filter e collectors.",
            durationMinutes = 45
        ),
        StudyEntry(
            date = LocalDate.now().minusDays(3),
            subject = "Spring Transactions",
            description = "Estudei propagação de transações e rollback.",
            durationMinutes = 50
        ),
        StudyEntry(
            date = LocalDate.now().minusDays(3),
            subject = "Kotlin",
            description = "Implementei exercícios com funções de extensão.",
            durationMinutes = 35
        ),
        StudyEntry(
            date = LocalDate.now().minusDays(8),
            subject = "RabbitMQ",
            description = "Revisei filas, exchanges e acknowledgements.",
            durationMinutes = 60
        ),
        StudyEntry(
            date = LocalDate.now().minusDays(35),
            subject = "SQL",
            description = "Pratiquei joins e agregações.",
            durationMinutes = 55
        )
    ).apply {
        val mockSubjects = listOf(
            "Java Streams",
            "RabbitMQ",
            "Spring Transactions",
            "Kotlin",
            "SQL",
            "Compose"
        )
        for (dayOffset in 10L..100L step 2) {
            val subject = mockSubjects[((dayOffset / 2) % mockSubjects.size).toInt()]
            add(
                StudyEntry(
                    date = LocalDate.now().minusDays(dayOffset),
                    subject = subject,
                    description = "Registro de demonstração sobre $subject.",
                    durationMinutes = 25 + ((dayOffset.toInt() % 5) * 10)
                )
            )
        }
        val today = LocalDate.now()
        for (dayOffset in 103L until today.dayOfYear.toLong() step 3) {
            val studyDate = today.minusDays(dayOffset)
            if (studyDate.year == today.year) {
                val sessionCount = when ((dayOffset / 3) % 3) {
                    0L -> 1
                    1L -> 3
                    else -> 5
                }
                repeat(sessionCount) { session ->
                    val subject = mockSubjects[
                        ((dayOffset + session) % mockSubjects.size).toInt()
                    ]
                    add(
                        StudyEntry(
                            date = studyDate,
                            subject = subject,
                            description = "Registro de demonstração sobre $subject.",
                            durationMinutes = 25 + ((dayOffset.toInt() + session) % 5) * 10
                        )
                    )
                }
            }
        }
    }

    fun all(): List<StudyEntry> = studyEntries.toList()

    fun forSubject(subject: String): List<StudyEntry> =
        studyEntries.filter { it.subject.equals(subject, ignoreCase = true) }

    fun add(entry: StudyEntry) {
        studyEntries.add(0, entry)
    }
}
