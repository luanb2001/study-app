package com.studyapp.backend.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public final class ApiModels {
    private ApiModels() {
    }

    public record StudyRequest(
            @NotNull LocalDate date,
            @NotBlank @Size(max = 120) String subject,
            @Size(max = 2000) String description,
            @Min(1) @Max(1440) int durationMinutes,
            @Min(1) int sessionCount
    ) {}

    public record StudyEntryResponse(
            String id, LocalDate date, String subject, String description,
            int durationMinutes, int sessionCount
    ) {}

    public record ScheduledStudyRequest(
            @NotNull LocalDate date,
            @NotBlank @Size(max = 120) String subject,
            @Min(1) int sessionCount,
            @Min(1) @Max(1440) int studyMinutes,
            @Min(1) @Max(1440) int breakMinutes
    ) {}

    public record ScheduledStudyResponse(
            String id, LocalDate date, String subject, int sessionCount,
            int studyMinutes, int breakMinutes
    ) {}

    public record ReviewScheduleResponse(
            String id, String subject, LocalDate dueDate, int intervalIndex
    ) {}

    public record RescheduleReviewRequest(@NotNull LocalDate dueDate) {}

    public record FlashcardGenerationRequest(
            @NotBlank @Size(max = 120) String subject,
            @NotNull @Size(max = 100) List<@NotBlank @Size(max = 2000) String> studySummaries
    ) {}

    public record Flashcard(String question, String answer) {}

    public record FlashcardReviewRequest(@NotNull ReviewDifficulty difficulty) {}

    public enum ReviewDifficulty {
        AGAIN, HARD, EASY
    }

    public record StudyProgressSummary(
            int streakDays, int monthlyStudyHours, int subjectCount
    ) {}
}
