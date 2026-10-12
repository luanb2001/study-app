package com.studyapp.backend.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyapp.backend.api.ApiModels.ReviewScheduleResponse;
import com.studyapp.backend.api.ApiModels.ScheduledStudyRequest;
import com.studyapp.backend.api.ApiModels.StudyRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class SyncDataRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SyncDataRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public long currentVersion(String userId, String entityType, String entityId) {
        List<Long> versions = this.jdbcTemplate.query("""
                SELECT version FROM sync_entity_versions
                WHERE user_id = ? AND entity_type = ? AND entity_id = ?
                """, (rs, row) -> rs.getLong("version"), userId, entityType, entityId);
        return versions.isEmpty() ? 0 : versions.get(0);
    }

    public boolean wasApplied(String userId, String changeId) {
        Integer count = this.jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM applied_sync_changes WHERE user_id = ? AND change_id = ?
                """, Integer.class, userId, changeId);
        return count != null && count > 0;
    }

    public EntityState entityState(String userId, String entityType, String entityId) {
        return this.jdbcTemplate.query("""
                SELECT version, deleted FROM sync_entity_versions
                WHERE user_id = ? AND entity_type = ? AND entity_id = ?
                """, (rs, row) -> new EntityState(
                rs.getLong("version"), rs.getBoolean("deleted")),
                userId, entityType, entityId).stream().findFirst().orElse(new EntityState(0, false));

    }

    public JsonNode currentPayload(String userId, String entityType, String entityId) {
        String table = this.tableFor(entityType);

        return this.jdbcTemplate.query("SELECT * FROM " + table + " WHERE id = ? AND user_id = ?",
                (rs, row) -> this.payloadFor(entityType, rs), entityId, userId)
                .stream().findFirst().orElse(null);

    }

    private JsonNode payloadFor(String entityType, java.sql.ResultSet rs) throws java.sql.SQLException {

        return switch (entityType) {
            case "STUDY" -> this.objectMapper.valueToTree(Map.of(
                    "date", rs.getObject("study_date", LocalDate.class),
                    "subject", rs.getString("subject"),
                    "description", rs.getString("description"),
                    "durationMinutes", rs.getInt("duration_minutes"),
                    "sessionCount", rs.getInt("session_count")));
            case "SCHEDULED_STUDY" -> this.objectMapper.valueToTree(Map.of(
                    "date", rs.getObject("study_date", LocalDate.class),
                    "subject", rs.getString("subject"),
                    "sessionCount", rs.getInt("session_count"),
                    "studyMinutes", rs.getInt("study_minutes"),
                    "breakMinutes", rs.getInt("break_minutes")));
            case "REVIEW" -> this.objectMapper.valueToTree(Map.of(
                    "id", rs.getString("id"),
                    "subject", rs.getString("subject"),
                    "dueDate", rs.getObject("due_date", LocalDate.class),
                    "intervalIndex", rs.getInt("interval_index")));
            default -> throw new IllegalArgumentException("Unknown sync entity: " + entityType);
        };

    }

    public void upsertStudy(String userId, String id, StudyRequest study, String subjectKey) {
        int updated = this.jdbcTemplate.update("""
                UPDATE study_entries SET study_date = ?, subject = ?, subject_key = ?,
                    description = ?, duration_minutes = ?, session_count = ?
                WHERE id = ? AND user_id = ?
                """, study.date(), study.subject().trim(), subjectKey,
                study.description() == null ? "" : study.description(), study.durationMinutes(),
                study.sessionCount(), id, userId);

        if (updated == 0) {
            this.jdbcTemplate.update("""
                    INSERT INTO study_entries
                    (id, user_id, study_date, subject, subject_key, description,
                     duration_minutes, session_count)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, userId, study.date(), study.subject().trim(), subjectKey,
                    study.description() == null ? "" : study.description(),
                    study.durationMinutes(), study.sessionCount());
        }

    }

    public void upsertScheduledStudy(String userId, String id, ScheduledStudyRequest study) {
        int updated = this.jdbcTemplate.update("""
                UPDATE scheduled_studies SET study_date = ?, subject = ?, session_count = ?,
                    study_minutes = ?, break_minutes = ? WHERE id = ? AND user_id = ?
                """, study.date(), study.subject().trim(), study.sessionCount(),
                study.studyMinutes(), study.breakMinutes(), id, userId);

        if (updated == 0) {
            this.jdbcTemplate.update("""
                    INSERT INTO scheduled_studies
                    (id, user_id, study_date, subject, session_count, study_minutes, break_minutes)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, id, userId, study.date(), study.subject().trim(), study.sessionCount(),
                    study.studyMinutes(), study.breakMinutes());
        }

    }

    public void upsertReview(String userId, String id, ReviewScheduleResponse review, String subjectKey) {
        int updated = this.jdbcTemplate.update("""
                UPDATE review_schedules SET subject = ?, subject_key = ?, due_date = ?, interval_index = ?
                WHERE id = ? AND user_id = ?
                """, review.subject().trim(), subjectKey, review.dueDate(), review.intervalIndex(),
                id, userId);

        if (updated == 0) {
            this.jdbcTemplate.update("""
                    INSERT INTO review_schedules
                    (id, user_id, subject, subject_key, due_date, interval_index)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, id, userId, review.subject().trim(), subjectKey,
                    review.dueDate(), review.intervalIndex());
        }

    }

    public void deleteEntity(String userId, String entityType, String entityId) {
        this.jdbcTemplate.update("DELETE FROM " + this.tableFor(entityType) + " WHERE id = ? AND user_id = ?",
                entityId, userId);
    }

    public void saveVersion(
            String userId, String entityType, String entityId, long version, boolean deleted) {
        int updated = this.jdbcTemplate.update("""
                UPDATE sync_entity_versions SET version = ?, deleted = ?
                WHERE user_id = ? AND entity_type = ? AND entity_id = ?
                """, version, deleted, userId, entityType, entityId);

        if (updated == 0) {
            this.jdbcTemplate.update("""
                    INSERT INTO sync_entity_versions
                    (user_id, entity_type, entity_id, version, deleted) VALUES (?, ?, ?, ?, ?)
                    """, userId, entityType, entityId, version, deleted);
        }

    }

    public void markApplied(String userId, String changeId) {
        this.jdbcTemplate.update("INSERT INTO applied_sync_changes (user_id, change_id) VALUES (?, ?)",
                userId, changeId);
    }

    public void appendChange(
            String userId, String changeId, String entityType, String entityId,
            String operation, long version, String payload) {
        this.jdbcTemplate.update("""
                INSERT INTO sync_change_log
                (user_id, change_id, entity_type, entity_id, operation, version, payload)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, userId, changeId, entityType, entityId, operation, version, payload);
    }

    public List<ChangeRow> changesAfter(String userId, long cursor, int limit) {
        return this.jdbcTemplate.query("""
                SELECT change_sequence, change_id, entity_type, entity_id, operation, version, payload
                FROM sync_change_log WHERE user_id = ? AND change_sequence > ?
                ORDER BY change_sequence FETCH FIRST ? ROWS ONLY
                """, (rs, row) -> new ChangeRow(
                rs.getLong("change_sequence"), rs.getString("change_id"),
                rs.getString("entity_type"), rs.getString("entity_id"),
                rs.getString("operation"), rs.getLong("version"),
                this.parsePayload(rs.getString("payload"))), userId, cursor, limit);
    }

    public boolean hasChangesAfter(String userId, long cursor) {
        Integer count = this.jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sync_change_log WHERE user_id = ? AND change_sequence > ?
                """, Integer.class, userId, cursor);
        return count != null && count > 0;
    }

    private JsonNode parsePayload(String payload) {
        if (payload == null) return null;
        try {
            return this.objectMapper.readTree(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored sync payload is invalid.", exception);
        }
    }

    private String tableFor(String entityType) {

        return switch (entityType) {
            case "STUDY" -> "study_entries";
            case "SCHEDULED_STUDY" -> "scheduled_studies";
            case "REVIEW" -> "review_schedules";
            default -> throw new IllegalArgumentException("Unknown sync entity: " + entityType);
        };

    }

    public record EntityState(long version, boolean deleted) {}

    public record ChangeRow(
            long sequence, String changeId, String entityType, String entityId,
            String operation, long version, JsonNode payload) {}
}
