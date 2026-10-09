package com.studyapp.backend.api;

import com.studyapp.backend.api.ApiModels.Flashcard;
import com.studyapp.backend.api.ApiModels.FlashcardGenerationRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flashcards")
public class FlashcardController {
    @PostMapping("/generate")
    public List<Flashcard> generate(@Valid @RequestBody FlashcardGenerationRequest request) {
        String context = request.studySummaries().stream()
                .map(String::trim)
                .filter(summary -> !summary.isEmpty())
                .distinct()
                .collect(java.util.stream.Collectors.joining("\n\n"));
        if (context.isBlank()) {
            context = "Conteúdo de demonstração sobre " + request.subject().trim() + ".";
        }
        String subject = request.subject().trim();
        return List.of(
                new Flashcard("Qual foi a ideia principal estudada em " + subject + "?", context),
                new Flashcard("Como você resumiria o que aprendeu sobre " + subject + "?", context),
                new Flashcard("Que pontos importantes deve lembrar sobre " + subject + "?", context)
        );
    }
}
