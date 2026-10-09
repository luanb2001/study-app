package com.studyapp.backend.api;

import com.studyapp.backend.api.ApiModels.*;
import com.studyapp.backend.service.StudyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class StudyController {
    private final StudyService service;

    public StudyController(StudyService service) {
        this.service = service;
    }

    @GetMapping("/studies")
    public List<StudyEntryResponse> studies() {
        return service.studies();
    }

    @GetMapping("/subjects/{subject}/studies")
    public List<StudyEntryResponse> studiesForSubject(@PathVariable String subject) {
        return service.studiesForSubject(subject);
    }

    @PostMapping("/studies")
    @ResponseStatus(HttpStatus.CREATED)
    public StudyEntryResponse recordStudy(@Valid @RequestBody StudyRequest request,
                                          @RequestParam(defaultValue = "false") boolean isReview) {
        return service.recordStudy(request, isReview);
    }

    @DeleteMapping("/studies/{studyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudy(@PathVariable String studyId) {
        service.deleteStudy(studyId);
    }

    @GetMapping("/studies/scheduled")
    public List<ScheduledStudyResponse> scheduledStudies() {
        return service.scheduledStudies();
    }

    @PostMapping("/studies/scheduled")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduledStudyResponse schedule(@Valid @RequestBody ScheduledStudyRequest request) {
        return service.schedule(request);
    }

    @DeleteMapping("/studies/scheduled/{scheduledStudyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelScheduledStudy(@PathVariable String scheduledStudyId) {
        service.cancelScheduledStudy(scheduledStudyId);
    }

    @GetMapping("/studies/summary")
    public StudyProgressSummary summary() {
        return service.summary();
    }

    @GetMapping("/reviews")
    public List<ReviewScheduleResponse> reviews(
            @RequestParam(required = false) LocalDate dueOnOrBefore) {
        return service.reviews(dueOnOrBefore);
    }

    @PutMapping("/reviews/{reviewId}")
    public ReviewScheduleResponse reschedule(@PathVariable String reviewId,
                                              @Valid @RequestBody RescheduleReviewRequest request) {
        return service.reschedule(reviewId, request);
    }

    @PostMapping("/reviews/{reviewId}/complete")
    @ResponseStatus(HttpStatus.CREATED)
    public void completeReview(@PathVariable String reviewId,
                               @Valid @RequestBody StudyRequest request) {
        service.completeReview(reviewId, request);
    }

    @PostMapping("/reviews/{reviewId}/flashcards/complete")
    public ReviewScheduleResponse completeFlashcardReview(
            @PathVariable String reviewId, @Valid @RequestBody FlashcardReviewRequest request) {
        return service.completeFlashcardReview(reviewId, request);
    }
}
