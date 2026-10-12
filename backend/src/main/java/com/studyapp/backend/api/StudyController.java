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

    private final StudyService studyService;

    public StudyController(StudyService studyService) {
        this.studyService = studyService;
    }

    @GetMapping("/studies")
    public List<StudyEntryResponse> studies() {
        return this.studyService.studies();
    }

    @GetMapping("/subjects/{subject}/studies")
    public List<StudyEntryResponse> studiesForSubject(@PathVariable String subject) {
        return this.studyService.studiesForSubject(subject);
    }

    @PostMapping("/studies")
    @ResponseStatus(HttpStatus.CREATED)
    public StudyEntryResponse recordStudy(@Valid @RequestBody StudyRequest request,
                                          @RequestParam(defaultValue = "false") boolean isReview) {
        return this.studyService.recordStudy(request, isReview);
    }

    @DeleteMapping("/studies/{studyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStudy(@PathVariable String studyId) {
        this.studyService.deleteStudy(studyId);
    }

    @GetMapping("/studies/scheduled")
    public List<ScheduledStudyResponse> scheduledStudies() {
        return this.studyService.scheduledStudies();
    }

    @PostMapping("/studies/scheduled")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduledStudyResponse schedule(@Valid @RequestBody ScheduledStudyRequest request) {
        return this.studyService.schedule(request);
    }

    @DeleteMapping("/studies/scheduled/{scheduledStudyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelScheduledStudy(@PathVariable String scheduledStudyId) {
        this.studyService.cancelScheduledStudy(scheduledStudyId);
    }

    @GetMapping("/studies/summary")
    public StudyProgressSummary summary() {
        return this.studyService.summary();
    }

    @GetMapping("/reviews")
    public List<ReviewScheduleResponse> reviews(
            @RequestParam(required = false) LocalDate dueOnOrBefore) {
        return this.studyService.reviews(dueOnOrBefore);
    }

    @PutMapping("/reviews/{reviewId}")
    public ReviewScheduleResponse reschedule(@PathVariable String reviewId,
                                              @Valid @RequestBody RescheduleReviewRequest request) {
        return this.studyService.reschedule(reviewId, request);
    }

    @PostMapping("/reviews/{reviewId}/complete")
    @ResponseStatus(HttpStatus.CREATED)
    public void completeReview(@PathVariable String reviewId,
                               @Valid @RequestBody StudyRequest request) {
        this.studyService.completeReview(reviewId, request);
    }

    @PostMapping("/reviews/{reviewId}/flashcards/complete")
    public ReviewScheduleResponse completeFlashcardReview(
            @PathVariable String reviewId, @Valid @RequestBody FlashcardReviewRequest request) {
        return this.studyService.completeFlashcardReview(reviewId, request);
    }
}
