package com.studyapp.backend.repository;

import com.studyapp.backend.api.ApiModels.ReviewScheduleResponse;
import com.studyapp.backend.api.ApiModels.ScheduledStudyResponse;
import com.studyapp.backend.api.ApiModels.StudyEntryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Repository
public class StudyDataRepository {
    private static final RowMapper<StudyEntryResponse> STUDY_MAPPER = (rs, row) ->
            new StudyEntryResponse(
                    rs.getString("id"), rs.getObject("study_date", LocalDate.class),
                    rs.getString("subject"), rs.getString("description"),
                    rs.getInt("duration_minutes"), rs.getInt("session_count")
            );

    private static final RowMapper<ScheduledStudyResponse> SCHEDULED_MAPPER = (rs, row) ->
            new ScheduledStudyResponse(
                    rs.getString("id"), rs.getObject("study_date", LocalDate.class),
                    rs.getString("subject"), rs.getInt("session_count"),
                    rs.getInt("study_minutes"), rs.getInt("break_minutes")
            );

    private static final RowMapper<ReviewScheduleResponse> REVIEW_MAPPER = (rs, row) ->
            new ReviewScheduleResponse(
                    rs.getString("id"), rs.getString("subject"),
                    rs.getObject("due_date", LocalDate.class), rs.getInt("interval_index")
            );

    private final JdbcTemplate jdbcTemplate;

    public StudyDataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String newId() {
        return UUID.randomUUID().toString();
    }

    public List<StudyEntryResponse> studies(String userId) {
        return this.jdbcTemplate.query("""
                SELECT id, study_date, subject, description, duration_minutes, session_count
                FROM study_entries WHERE user_id = ? ORDER BY study_date DESC, created_at DESC
                """, STUDY_MAPPER, userId);
    }

    public Optional<StudyEntryResponse> study(String id, String userId) {
        return this.jdbcTemplate.query("""
                SELECT id, study_date, subject, description, duration_minutes, session_count
                FROM study_entries WHERE id = ? AND user_id = ?
                """, STUDY_MAPPER, id, userId).stream().findFirst();

    }

    public List<StudyEntryResponse> studiesForSubject(String subject, String userId) {
        return this.jdbcTemplate.query("""
                SELECT id, study_date, subject, description, duration_minutes, session_count
                FROM study_entries WHERE subject_key = ? AND user_id = ?
                ORDER BY study_date DESC, created_at DESC
                """, STUDY_MAPPER, this.key(subject), userId);
    }

    public void insertStudy(String id, String userId, LocalDate date, String subject, String description,
                            int minutes, int sessions) {
        this.jdbcTemplate.update("""
                INSERT INTO study_entries
                (id, user_id, study_date, subject, subject_key, description, duration_minutes, session_count)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, userId, date, subject, this.key(subject), description, minutes, sessions);
    }

    public boolean deleteStudy(String id, String userId) {
        return this.jdbcTemplate.update("DELETE FROM study_entries WHERE id = ? AND user_id = ?", id, userId) > 0;
    }

    public boolean hasStudiesForSubject(String subject, String userId) {
        Integer count = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM study_entries WHERE subject_key = ? AND user_id = ?",
                Integer.class, this.key(subject), userId);
        return count != null && count > 0;
    }

    public List<ScheduledStudyResponse> scheduledStudies(String userId) {
        return this.jdbcTemplate.query("""
                SELECT id, study_date, subject, session_count, study_minutes, break_minutes
                FROM scheduled_studies WHERE user_id = ? ORDER BY study_date, subject
                """, SCHEDULED_MAPPER, userId);
    }

    public void insertScheduledStudy(String id, String userId, LocalDate date, String subject, int sessions,
                                     int studyMinutes, int breakMinutes) {
        this.jdbcTemplate.update("""
                INSERT INTO scheduled_studies
                (id, user_id, study_date, subject, session_count, study_minutes, break_minutes)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, userId, date, subject, sessions, studyMinutes, breakMinutes);
    }

    public boolean deleteScheduledStudy(String id, String userId) {
        return this.jdbcTemplate.update("DELETE FROM scheduled_studies WHERE id = ? AND user_id = ?", id, userId) > 0;
    }

    public List<ReviewScheduleResponse> reviews(LocalDate dueOnOrBefore, String userId) {

        if (dueOnOrBefore == null) {
            return this.jdbcTemplate.query("""
                    SELECT id, subject, due_date, interval_index
                    FROM review_schedules WHERE user_id = ? ORDER BY due_date, subject
                    """, REVIEW_MAPPER, userId);
        }

        return this.jdbcTemplate.query("""
                SELECT id, subject, due_date, interval_index
                FROM review_schedules WHERE due_date <= ? AND user_id = ? ORDER BY due_date, subject
                """, REVIEW_MAPPER, dueOnOrBefore, userId);
    }

    public Optional<ReviewScheduleResponse> review(String id, String userId) {
        return this.jdbcTemplate.query("""
                SELECT id, subject, due_date, interval_index FROM review_schedules
                WHERE id = ? AND user_id = ?
                """, REVIEW_MAPPER, id, userId).stream().findFirst();

    }

    public Optional<ReviewScheduleResponse> reviewForSubject(String subject, String userId) {
        return this.jdbcTemplate.query("""
                SELECT id, subject, due_date, interval_index
                FROM review_schedules WHERE user_id = ? AND subject_key = ?
                """, REVIEW_MAPPER, userId, this.key(userId, subject)).stream().findFirst();

    }

    public void saveReview(String id, String userId, String subject, LocalDate dueDate, int intervalIndex) {
        int changed = this.jdbcTemplate.update("""
                UPDATE review_schedules SET subject = ?, due_date = ?, interval_index = ?
                WHERE user_id = ? AND subject_key = ?
                """, subject, dueDate, intervalIndex, userId, this.key(userId, subject));

        if (changed == 0) {
            this.jdbcTemplate.update("""
                    INSERT INTO review_schedules
                    (id, user_id, subject, subject_key, due_date, interval_index)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, id, userId, subject, this.key(userId, subject), dueDate, intervalIndex);
        }

    }

    public boolean rescheduleReview(String id, String userId, LocalDate dueDate) {
        return this.jdbcTemplate.update(
                "UPDATE review_schedules SET due_date = ? WHERE id = ? AND user_id = ?",
                dueDate, id, userId) > 0;
    }

    public void deleteReviewForSubject(String subject, String userId) {
        this.jdbcTemplate.update("DELETE FROM review_schedules WHERE user_id = ? AND subject_key = ?",
                userId, this.key(userId, subject));
    }

    public int streak(LocalDate today, String userId) {
        var dates = new HashSet<>(this.jdbcTemplate.query(
                "SELECT DISTINCT study_date FROM study_entries WHERE user_id = ?",
                (rs, row) -> rs.getObject("study_date", LocalDate.class), userId));
        LocalDate start = dates.contains(today) ? today
                : dates.contains(today.minusDays(1)) ? today.minusDays(1) : null;

        if (start == null) {
            return 0;
        }

        int count = 0;

        for (LocalDate date = start; dates.contains(date); date = date.minusDays(1)) {
            count++;
        }

        return count;
    }

    public int monthlyHours(LocalDate today, String userId) {
        Integer minutes = this.jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(duration_minutes), 0) FROM study_entries
                WHERE user_id = ? AND YEAR(study_date) = ? AND MONTH(study_date) = ?
                """, Integer.class, userId, today.getYear(), today.getMonthValue());
        return (minutes == null ? 0 : minutes) / 60;
    }

    public int subjectCount(String userId) {
        Integer count = this.jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT subject_key) FROM study_entries WHERE user_id = ?",
                Integer.class, userId);
        return count == null ? 0 : count;
    }

    private String key(String subject) {
        return subject.trim().toLowerCase(Locale.ROOT);
    }

    private String key(String userId, String subject) {
        return userId + ":" + this.key(subject);
    }
}
