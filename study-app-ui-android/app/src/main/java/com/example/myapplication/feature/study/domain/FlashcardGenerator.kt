package com.example.myapplication.feature.study.domain

data class FlashcardGenerationRequest(
    val subject: String,
    val studySummaries: List<String>
)

data class Flashcard(
    val question: String,
    val answer: String
)

interface FlashcardGenerator {
    suspend fun generate(request: FlashcardGenerationRequest): Result<List<Flashcard>>
}
