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

    private final JdbcTemplate jdbc;

    public StudyDataRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String newId() {
        return UUID.randomUUID().toString();
    }

    public List<StudyEntryResponse> studies() {
        return jdbc.query("""
                SELECT id, study_date, subject, description, duration_minutes, session_count
                FROM study_entries ORDER BY study_date DESC, created_at DESC
                """, STUDY_MAPPER);
    }

    public Optional<StudyEntryResponse> study(String id) {
        return jdbc.query("""
                SELECT id, study_date, subject, description, duration_minutes, session_count
                FROM study_entries WHERE id = ?
                """, STUDY_MAPPER, id).stream().findFirst();
    }

    public List<StudyEntryResponse> studiesForSubject(String subject) {
        return jdbc.query("""
                SELECT id, study_date, subject, description, duration_minutes, session_count
                FROM study_entries WHERE subject_key = ?
                ORDER BY study_date DESC, created_at DESC
                """, STUDY_MAPPER, key(subject));
    }

    public void insertStudy(String id, LocalDate date, String subject, String description,
                            int minutes, int sessions) {
        jdbc.update("""
                INSERT INTO study_entries
                (id, study_date, subject, subject_key, description, duration_minutes, session_count)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, date, subject, key(subject), description, minutes, sessions);
    }

    public boolean deleteStudy(String id) {
        return jdbc.update("DELETE FROM study_entries WHERE id = ?", id) > 0;
    }

    public boolean hasStudiesForSubject(String subject) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM study_entries WHERE subject_key = ?",
                Integer.class, key(subject));
        return count != null && count > 0;
    }

    public List<ScheduledStudyResponse> scheduledStudies() {
        return jdbc.query("""
                SELECT id, study_date, subject, session_count, study_minutes, break_minutes
                FROM scheduled_studies ORDER BY study_date, subject
                """, SCHEDULED_MAPPER);
    }

    public void insertScheduledStudy(String id, LocalDate date, String subject, int sessions,
                                     int studyMinutes, int breakMinutes) {
        jdbc.update("""
                INSERT INTO scheduled_studies
                (id, study_date, subject, session_count, study_minutes, break_minutes)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id, date, subject, sessions, studyMinutes, breakMinutes);
    }

    public boolean deleteScheduledStudy(String id) {
        return jdbc.update("DELETE FROM scheduled_studies WHERE id = ?", id) > 0;
    }

    public List<ReviewScheduleResponse> reviews(LocalDate dueOnOrBefore) {
        if (dueOnOrBefore == null) {
            return jdbc.query("""
                    SELECT id, subject, due_date, interval_index
                    FROM review_schedules ORDER BY due_date, subject
                    """, REVIEW_MAPPER);
        }

        return jdbc.query("""
                SELECT id, subject, due_date, interval_index
                FROM review_schedules WHERE due_date <= ? ORDER BY due_date, subject
                """, REVIEW_MAPPER, dueOnOrBefore);
    }

    public Optional<ReviewScheduleResponse> review(String id) {
        return jdbc.query("""
                SELECT id, subject, due_date, interval_index FROM review_schedules WHERE id = ?
                """, REVIEW_MAPPER, id).stream().findFirst();
    }

    public Optional<ReviewScheduleResponse> reviewForSubject(String subject) {
        return jdbc.query("""
                SELECT id, subject, due_date, interval_index
                FROM review_schedules WHERE subject_key = ?
                """, REVIEW_MAPPER, key(subject)).stream().findFirst();
    }

    public void saveReview(String id, String subject, LocalDate dueDate, int intervalIndex) {
        int changed = jdbc.update("""
                UPDATE review_schedules SET subject = ?, due_date = ?, interval_index = ?
                WHERE subject_key = ?
                """, subject, dueDate, intervalIndex, key(subject));
        if (changed == 0) {
            jdbc.update("""
                    INSERT INTO review_schedules (id, subject, subject_key, due_date, interval_index)
                    VALUES (?, ?, ?, ?, ?)
                    """, id, subject, key(subject), dueDate, intervalIndex);
        }
    }

    public boolean rescheduleReview(String id, LocalDate dueDate) {
        return jdbc.update(
                "UPDATE review_schedules SET due_date = ? WHERE id = ?", dueDate, id) > 0;
    }

    public void deleteReviewForSubject(String subject) {
        jdbc.update("DELETE FROM review_schedules WHERE subject_key = ?", key(subject));
    }

    public int streak(LocalDate today) {
        var dates = new HashSet<>(jdbc.query(
                "SELECT DISTINCT study_date FROM study_entries",
                (rs, row) -> rs.getObject("study_date", LocalDate.class)));
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

    public int monthlyHours(LocalDate today) {
        Integer minutes = jdbc.queryForObject("""
                SELECT COALESCE(SUM(duration_minutes), 0) FROM study_entries
                WHERE YEAR(study_date) = ? AND MONTH(study_date) = ?
                """, Integer.class, today.getYear(), today.getMonthValue());
        return (minutes == null ? 0 : minutes) / 60;
    }

    public int subjectCount() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT subject_key) FROM study_entries", Integer.class);
        return count == null ? 0 : count;
    }

    private String key(String subject) {
        return subject.trim().toLowerCase(Locale.ROOT);
    }
}
