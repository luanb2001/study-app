package com.studyapp.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyapp.backend.api.ApiModels.ReviewScheduleResponse;
import com.studyapp.backend.api.ApiModels.ScheduledStudyRequest;
import com.studyapp.backend.api.ApiModels.StudyRequest;
import com.studyapp.backend.repository.SyncDataRepository;
import com.studyapp.backend.security.AccountPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class SyncService {
    private static final int MAX_UPLOAD_CHANGES = 100;
    private static final int MAX_DOWNLOAD_CHANGES = 500;
    private final SyncDataRepository syncDataRepository;
    private final ObjectMapper objectMapper;

    public SyncService(SyncDataRepository syncDataRepository, ObjectMapper objectMapper) {
        this.syncDataRepository = syncDataRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void publishDirectChange(
            String userId, SyncEntityType entityType, String entityId,
            SyncOperation operation, Object payload) {
        long nextVersion = this.syncDataRepository.currentVersion(userId, entityType.name(), entityId) + 1;
        this.syncDataRepository.saveVersion(
                userId, entityType.name(), entityId, nextVersion, operation == SyncOperation.DELETE);
        this.syncDataRepository.appendChange(userId, UUID.randomUUID().toString(), entityType.name(), entityId,
                operation.name(), nextVersion,
                this.serialize(payload == null ? null : this.objectMapper.valueToTree(payload)));
    }

    @Transactional
    public SyncResponse synchronize(SyncRequest request) {

        if (request.cursor() < 0 || request.changes().size() > MAX_UPLOAD_CHANGES) {
            throw new IllegalArgumentException("Invalid cursor or too many changes.");
        }

        String userId = this.accountId();
        var accepted = new java.util.ArrayList<AcceptedChange>();

        for (SyncChange change : request.changes()) {
            this.validateChange(change);

            if (this.syncDataRepository.wasApplied(userId, change.changeId())) {
                accepted.add(new AcceptedChange(change.changeId(),
                        this.syncDataRepository.currentVersion(userId, change.entityType().name(), change.entityId())));
                continue;
            }

            long currentVersion = this.syncDataRepository.currentVersion(
                    userId, change.entityType().name(), change.entityId());

            if (change.baseVersion() != currentVersion) {
                SyncDataRepository.EntityState currentState = this.syncDataRepository.entityState(
                        userId, change.entityType().name(), change.entityId());
                throw new SyncConflictException(change.entityType(), change.entityId(),
                        change.baseVersion(), currentVersion,
                        currentState.deleted() ? SyncOperation.DELETE : SyncOperation.UPSERT,
                        currentState.deleted() ? null : this.syncDataRepository.currentPayload(
                                userId, change.entityType().name(), change.entityId()));
            }

            long nextVersion = currentVersion + 1;
            this.applyChange(userId, change);
            this.syncDataRepository.saveVersion(userId, change.entityType().name(), change.entityId(), nextVersion,
                    change.operation() == SyncOperation.DELETE);
            this.syncDataRepository.markApplied(userId, change.changeId());
            this.syncDataRepository.appendChange(userId, change.changeId(), change.entityType().name(),
                    change.entityId(), change.operation().name(), nextVersion,
                    this.serialize(change.payload()));
            accepted.add(new AcceptedChange(change.changeId(), nextVersion));
        }

        List<SyncChangeResponse> changes = this.syncDataRepository
                .changesAfter(userId, request.cursor(), MAX_DOWNLOAD_CHANGES)
                .stream()
                .map(change -> new SyncChangeResponse(
                        change.sequence(), change.changeId(),
                        SyncEntityType.valueOf(change.entityType()), change.entityId(),
                        SyncOperation.valueOf(change.operation()), change.version(), change.payload()))
                .toList();

        long nextCursor = changes.isEmpty()
                ? request.cursor() : changes.get(changes.size() - 1).sequence();
        boolean hasMore = !changes.isEmpty() && this.syncDataRepository.hasChangesAfter(userId, nextCursor);
        return new SyncResponse(nextCursor, hasMore, accepted, changes);
    }

    private void validateChange(SyncChange change) {
        if (change == null || change.entityType() == null || change.operation() == null
                || change.changeId() == null || change.entityId() == null || change.baseVersion() < 0) {
            throw new IllegalArgumentException("Change identifiers and base version are required.");
        }
        try {
            UUID.fromString(change.changeId());
            UUID.fromString(change.entityId());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Sync identifiers must be UUIDs.");
        }

        if (change.operation() == SyncOperation.UPSERT && change.payload() == null) {
            throw new IllegalArgumentException("An upsert must include a payload.");
        }

        if (change.operation() == SyncOperation.DELETE && change.payload() != null) {
            throw new IllegalArgumentException("A delete must not include a payload.");
        }

    }

    private void applyChange(String userId, SyncChange change) {

        if (change.operation() == SyncOperation.DELETE) {
            this.syncDataRepository.deleteEntity(userId, change.entityType().name(), change.entityId());
            return;
        }

        switch (change.entityType()) {
            case STUDY -> this.upsertStudy(userId, change);
            case SCHEDULED_STUDY -> this.upsertScheduledStudy(userId, change);
            case REVIEW -> this.upsertReview(userId, change);
        }

    }

    private void upsertStudy(String userId, SyncChange change) {
        StudyRequest study = this.readPayload(change.payload(), StudyRequest.class);
        if (study.date() == null || study.date().isAfter(LocalDate.now())
                || study.subject() == null || study.subject().isBlank()
                || study.subject().trim().length() > 120 || study.durationMinutes() < 1
                || study.durationMinutes() > 1_440 || study.sessionCount() < 1
                || (study.description() != null && study.description().length() > 2_000)) {
            throw new IllegalArgumentException("Invalid study payload.");
        }
        this.syncDataRepository.upsertStudy(userId, change.entityId(), study, this.subjectKey(study.subject()));
    }

    private void upsertScheduledStudy(String userId, SyncChange change) {
        ScheduledStudyRequest study = this.readPayload(change.payload(), ScheduledStudyRequest.class);
        if (study.date() == null || study.date().isBefore(LocalDate.now())
                || study.subject() == null || study.subject().isBlank()
                || study.subject().trim().length() > 120 || study.sessionCount() < 1
                || study.studyMinutes() < 1 || study.studyMinutes() > 1_440
                || study.breakMinutes() < 1 || study.breakMinutes() > 1_440) {
            throw new IllegalArgumentException("Invalid scheduled study payload.");
        }
        this.syncDataRepository.upsertScheduledStudy(userId, change.entityId(), study);
    }

    private void upsertReview(String userId, SyncChange change) {
        ReviewScheduleResponse review = this.readPayload(change.payload(), ReviewScheduleResponse.class);
        if (review.id() == null || !review.id().equals(change.entityId())
                || review.subject() == null || review.subject().isBlank()
                || review.dueDate() == null || review.intervalIndex() < 0 || review.intervalIndex() > 3) {
            throw new IllegalArgumentException("Invalid review payload.");
        }
        this.syncDataRepository.upsertReview(userId, change.entityId(), review, this.subjectKey(review.subject()));
    }

    private String subjectKey(String subject) {
        return this.accountId() + ":" + subject.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private <T> T readPayload(JsonNode payload, Class<T> type) {
        try {
            return this.objectMapper.treeToValue(payload, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid sync payload.");
        }
    }

    private String serialize(JsonNode payload) {
        if (payload == null) return null;
        try {
            return this.objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize sync payload.", exception);
        }
    }

    private String accountId() {
        AccountPrincipal principal = (AccountPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return principal.id();
    }

    public enum SyncEntityType {
        STUDY, SCHEDULED_STUDY, REVIEW
    }

    public enum SyncOperation {
        UPSERT, DELETE
    }

    public record SyncRequest(long cursor, List<SyncChange> changes) {
        public SyncRequest {
            changes = changes == null ? List.of() : List.copyOf(changes);
        }
    }

    public record SyncChange(
            String changeId,
            SyncEntityType entityType,
            String entityId,
            SyncOperation operation,
            long baseVersion,
            JsonNode payload) {
    }

    public record AcceptedChange(String changeId, long version) {
    }

    public record SyncChangeResponse(
            long sequence,
            String changeId,
            SyncEntityType entityType,
            String entityId,
            SyncOperation operation,
            long version,
            JsonNode payload) {
    }

    public record SyncResponse(
            long cursor, boolean hasMore, List<AcceptedChange> accepted,
            List<SyncChangeResponse> changes) {
    }

    public static class SyncConflictException extends RuntimeException {
        private final String entityType;
        private final String entityId;
        private final long submittedVersion;
        private final long currentVersion;
        private final SyncOperation serverOperation;
        private final JsonNode serverPayload;

        public SyncConflictException(
                SyncEntityType entityType, String entityId, long submittedVersion,
                long currentVersion, SyncOperation serverOperation, JsonNode serverPayload) {
            super("A newer version of this record already exists.");
            this.entityType = entityType.name();
            this.entityId = entityId;
            this.submittedVersion = submittedVersion;
            this.currentVersion = currentVersion;
            this.serverOperation = serverOperation;
            this.serverPayload = serverPayload;
        }

        public String entityType() { return this.entityType; }
        public String entityId() { return this.entityId; }
        public long submittedVersion() { return this.submittedVersion; }
        public long currentVersion() { return this.currentVersion; }
        public SyncOperation serverOperation() { return this.serverOperation; }
        public JsonNode serverPayload() { return this.serverPayload; }
    }
}
