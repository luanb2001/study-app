package com.example.myapplication.feature.study

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

object MockFlashcardGenerator : FlashcardGenerator {
    override suspend fun generate(
        request: FlashcardGenerationRequest
    ): Result<List<Flashcard>> {
        val context = request.studySummaries
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(separator = "\n\n")
            .ifBlank { "Conteúdo de demonstração sobre ${request.subject}." }

        return Result.success(
            listOf(
                Flashcard(
                    question = "Qual foi a ideia principal estudada em ${request.subject}?",
                    answer = context
                ),
                Flashcard(
                    question = "Como você resumiria o que aprendeu sobre ${request.subject}?",
                    answer = context
                ),
                Flashcard(
                    question = "Que pontos importantes você deve lembrar sobre ${request.subject}?",
                    answer = context
                )
            )
        )
    }
}
