package com.studyapp.backend.service;

import com.studyapp.backend.api.ApiModels.*;
import com.studyapp.backend.repository.StudyDataRepository;
import com.studyapp.backend.security.AccountPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import com.studyapp.backend.service.SyncService.SyncEntityType;
import com.studyapp.backend.service.SyncService.SyncOperation;

import java.time.LocalDate;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Service
public class StudyService {
    private static final int[] INTERVAL_DAYS = {1, 7, 21, 60};
    private final StudyDataRepository studyDataRepository;
    private final SyncService syncService;

    public StudyService(StudyDataRepository studyDataRepository, SyncService syncService) {
        this.studyDataRepository = studyDataRepository;
        this.syncService = syncService;
    }

    public List<StudyEntryResponse> studies() {
        return this.studyDataRepository.studies(this.accountId());
    }

    public List<StudyEntryResponse> studiesForSubject(String subject) {
        return this.studyDataRepository.studiesForSubject(subject, this.accountId());
    }

    public List<ScheduledStudyResponse> scheduledStudies() {
        return this.studyDataRepository.scheduledStudies(this.accountId());
    }

    public List<ReviewScheduleResponse> reviews(LocalDate dueOnOrBefore) {
        return this.studyDataRepository.reviews(dueOnOrBefore, this.accountId());
    }

    public StudyProgressSummary summary() {
        LocalDate today = LocalDate.now();
        return new StudyProgressSummary(
                this.studyDataRepository.streak(today, this.accountId()), this.studyDataRepository.monthlyHours(today, this.accountId()),
                this.studyDataRepository.subjectCount(this.accountId()));
    }

    @Transactional
    public StudyEntryResponse recordStudy(StudyRequest request, boolean isReview) {

        if (request.date().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("A data do estudo deve ser hoje ou anterior.");
        }

        String subject = request.subject().trim();
        String userId = this.accountId();
        ReviewScheduleResponse previous = this.studyDataRepository.reviewForSubject(subject, userId).orElse(null);
        int interval = isReview && previous != null
                ? Math.min(previous.intervalIndex() + 1, INTERVAL_DAYS.length - 1) : 0;
        String id = this.studyDataRepository.newId();
        String description = request.description() == null ? "" : request.description();
        this.studyDataRepository.insertStudy(id, userId, request.date(), subject, description, request.durationMinutes(), request.sessionCount());
        this.studyDataRepository.saveReview(previous == null ? this.stableReviewId(userId, subject) : previous.id(), userId,
                previous == null ? subject : previous.subject(),
                request.date().plusDays(INTERVAL_DAYS[interval]), interval);
        StudyEntryResponse response = new StudyEntryResponse(id, request.date(), subject, description,
                request.durationMinutes(), request.sessionCount());
        this.syncService.publishDirectChange(
                userId, SyncEntityType.STUDY, id, SyncOperation.UPSERT, response);
        ReviewScheduleResponse nextReview = this.studyDataRepository.reviewForSubject(subject, userId).orElseThrow();
        this.syncService.publishDirectChange(
                userId, SyncEntityType.REVIEW, nextReview.id(), SyncOperation.UPSERT, nextReview);
        return response;
    }

    @Transactional
    public ScheduledStudyResponse schedule(ScheduledStudyRequest request) {

        if (request.date().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A data do agendamento deve ser hoje ou futura.");
        }

        String id = this.studyDataRepository.newId();
        String userId = this.accountId();
        String subject = request.subject().trim();
        this.studyDataRepository.insertScheduledStudy(id, userId, request.date(), subject, request.sessionCount(),
                request.studyMinutes(), request.breakMinutes());
        ScheduledStudyResponse response = new ScheduledStudyResponse(id, request.date(), subject, request.sessionCount(),
                request.studyMinutes(), request.breakMinutes());
        this.syncService.publishDirectChange(
                userId, SyncEntityType.SCHEDULED_STUDY, id, SyncOperation.UPSERT, response);
        return response;
    }

    @Transactional
    public void deleteStudy(String id) {
        String userId = this.accountId();
        StudyEntryResponse entry = this.studyDataRepository.study(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudo não encontrado."));
        this.studyDataRepository.deleteStudy(id, userId);
        this.syncService.publishDirectChange(userId, SyncEntityType.STUDY, id, SyncOperation.DELETE, null);

        if (!this.studyDataRepository.hasStudiesForSubject(entry.subject(), userId)) {
            ReviewScheduleResponse review = this.studyDataRepository.reviewForSubject(entry.subject(), userId).orElse(null);
            this.studyDataRepository.deleteReviewForSubject(entry.subject(), userId);

            if (review != null) {
                this.syncService.publishDirectChange(
                        userId, SyncEntityType.REVIEW, review.id(), SyncOperation.DELETE, null);
            }

        }

    }

    @Transactional
    public void cancelScheduledStudy(String id) {
        String userId = this.accountId();

        if (!this.studyDataRepository.deleteScheduledStudy(id, userId)) {
            throw new ResourceNotFoundException("Agendamento não encontrado.");
        }

        this.syncService.publishDirectChange(
                userId, SyncEntityType.SCHEDULED_STUDY, id, SyncOperation.DELETE, null);
    }

    @Transactional
    public ReviewScheduleResponse reschedule(String id, RescheduleReviewRequest request) {

        if (request.dueDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A revisão deve ser reagendada para hoje ou uma data futura.");
        }

        String userId = this.accountId();
        ReviewScheduleResponse review = this.studyDataRepository.review(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Revisão não encontrada."));
        this.studyDataRepository.rescheduleReview(id, userId, request.dueDate());
        ReviewScheduleResponse response =
                new ReviewScheduleResponse(id, review.subject(), request.dueDate(), review.intervalIndex());
        this.syncService.publishDirectChange(
                userId, SyncEntityType.REVIEW, id, SyncOperation.UPSERT, response);
        return response;
    }

    @Transactional
    public ReviewScheduleResponse completeFlashcardReview(
            String id, FlashcardReviewRequest request) {
        String userId = this.accountId();
        ReviewScheduleResponse review = this.studyDataRepository.review(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Revisão não encontrada."));
        int current = review.intervalIndex();

        int next = switch (request.difficulty()) {
            case AGAIN -> 0;
            case HARD -> current;
            case EASY -> Math.min(current + 1, INTERVAL_DAYS.length - 1);
        };

        LocalDate dueDate = LocalDate.now().plusDays(INTERVAL_DAYS[next]);
        this.studyDataRepository.saveReview(id, userId, review.subject(), dueDate, next);
        ReviewScheduleResponse response = new ReviewScheduleResponse(id, review.subject(), dueDate, next);
        this.syncService.publishDirectChange(
                userId, SyncEntityType.REVIEW, id, SyncOperation.UPSERT, response);
        return response;
    }

    @Transactional
    public void completeReview(String id, StudyRequest request) {
        ReviewScheduleResponse review = this.studyDataRepository.review(id, this.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Revisão não encontrada."));

        if (!review.subject().equalsIgnoreCase(request.subject().trim())) {
            throw new IllegalArgumentException("O assunto deve corresponder à revisão.");
        }

        recordStudy(request, true);
    }

    private String accountId() {
        AccountPrincipal principal = (AccountPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.id();
    }

    private String stableReviewId(String userId, String subject) {
        String key = userId + ":" + subject.trim().toLowerCase(Locale.ROOT);
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
