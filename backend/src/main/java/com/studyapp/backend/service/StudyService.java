package com.studyapp.backend.service;

import com.studyapp.backend.api.ApiModels.*;
import com.studyapp.backend.repository.StudyDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class StudyService {
    private static final int[] INTERVAL_DAYS = {1, 7, 21, 60};
    private final StudyDataRepository repository;

    public StudyService(StudyDataRepository repository) {
        this.repository = repository;
    }

    public List<StudyEntryResponse> studies() {
        return repository.studies();
    }

    public List<StudyEntryResponse> studiesForSubject(String subject) {
        return repository.studiesForSubject(subject);
    }

    public List<ScheduledStudyResponse> scheduledStudies() {
        return repository.scheduledStudies();
    }

    public List<ReviewScheduleResponse> reviews(LocalDate dueOnOrBefore) {
        return repository.reviews(dueOnOrBefore);
    }

    public StudyProgressSummary summary() {
        LocalDate today = LocalDate.now();
        return new StudyProgressSummary(
                repository.streak(today), repository.monthlyHours(today), repository.subjectCount());
    }

    @Transactional
    public StudyEntryResponse recordStudy(StudyRequest request, boolean isReview) {
        if (request.date().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("A data do estudo deve ser hoje ou anterior.");
        }

        String subject = request.subject().trim();
        ReviewScheduleResponse previous = repository.reviewForSubject(subject).orElse(null);
        int interval = isReview && previous != null
                ? Math.min(previous.intervalIndex() + 1, INTERVAL_DAYS.length - 1) : 0;
        String id = repository.newId();
        String description = request.description() == null ? "" : request.description();
        repository.insertStudy(id, request.date(), subject, description,
                request.durationMinutes(), request.sessionCount());
        repository.saveReview(previous == null ? repository.newId() : previous.id(),
                previous == null ? subject : previous.subject(),
                request.date().plusDays(INTERVAL_DAYS[interval]), interval);
        return new StudyEntryResponse(id, request.date(), subject, description,
                request.durationMinutes(), request.sessionCount());
    }

    @Transactional
    public ScheduledStudyResponse schedule(ScheduledStudyRequest request) {
        if (request.date().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A data do agendamento deve ser hoje ou futura.");
        }

        String id = repository.newId();
        String subject = request.subject().trim();
        repository.insertScheduledStudy(id, request.date(), subject, request.sessionCount(),
                request.studyMinutes(), request.breakMinutes());
        return new ScheduledStudyResponse(id, request.date(), subject, request.sessionCount(),
                request.studyMinutes(), request.breakMinutes());
    }

    @Transactional
    public void deleteStudy(String id) {
        StudyEntryResponse entry = repository.study(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudo não encontrado."));
        repository.deleteStudy(id);
        if (!repository.hasStudiesForSubject(entry.subject())) {
            repository.deleteReviewForSubject(entry.subject());
        }
    }

    @Transactional
    public void cancelScheduledStudy(String id) {
        if (!repository.deleteScheduledStudy(id)) {
            throw new ResourceNotFoundException("Agendamento não encontrado.");
        }
    }

    @Transactional
    public ReviewScheduleResponse reschedule(String id, RescheduleReviewRequest request) {
        if (request.dueDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A revisão deve ser reagendada para hoje ou uma data futura.");
        }
        ReviewScheduleResponse review = repository.review(id)
                .orElseThrow(() -> new ResourceNotFoundException("Revisão não encontrada."));
        repository.rescheduleReview(id, request.dueDate());
        return new ReviewScheduleResponse(id, review.subject(), request.dueDate(), review.intervalIndex());
    }

    @Transactional
    public ReviewScheduleResponse completeFlashcardReview(
            String id, FlashcardReviewRequest request) {
        ReviewScheduleResponse review = repository.review(id)
                .orElseThrow(() -> new ResourceNotFoundException("Revisão não encontrada."));
        int current = review.intervalIndex();
        int next = switch (request.difficulty()) {
            case AGAIN -> 0;
            case HARD -> current;
            case EASY -> Math.min(current + 1, INTERVAL_DAYS.length - 1);
        };
        LocalDate dueDate = LocalDate.now().plusDays(INTERVAL_DAYS[next]);
        repository.saveReview(id, review.subject(), dueDate, next);
        return new ReviewScheduleResponse(id, review.subject(), dueDate, next);
    }

    @Transactional
    public void completeReview(String id, StudyRequest request) {
        ReviewScheduleResponse review = repository.review(id)
                .orElseThrow(() -> new ResourceNotFoundException("Revisão não encontrada."));
        if (!review.subject().equalsIgnoreCase(request.subject().trim())) {
            throw new IllegalArgumentException("O assunto deve corresponder à revisão.");
        }
        recordStudy(request, true);
    }
}
