package com.studyapp.backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LegacyDataRepository {
    private final JdbcTemplate jdbcTemplate;

    public LegacyDataRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void removeUnownedRecords() {
        this.jdbcTemplate.update("DELETE FROM study_entries WHERE user_id IS NULL");
        this.jdbcTemplate.update("DELETE FROM scheduled_studies WHERE user_id IS NULL");
        this.jdbcTemplate.update("DELETE FROM review_schedules WHERE user_id IS NULL");
    }

    public void dropLegacyGlobalReviewSubjectConstraint() {
        this.jdbcTemplate.query("""
                SELECT tc.CONSTRAINT_NAME
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
                  ON tc.CONSTRAINT_CATALOG = kcu.CONSTRAINT_CATALOG
                 AND tc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
                 AND tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
                WHERE tc.TABLE_SCHEMA = CURRENT_SCHEMA()
                  AND tc.TABLE_NAME = 'REVIEW_SCHEDULES'
                  AND tc.CONSTRAINT_TYPE = 'UNIQUE'
                  AND kcu.COLUMN_NAME = 'SUBJECT_KEY'
                """, (rs, row) -> rs.getString(1)).forEach(constraint -> {
            String safeName = constraint.replace("\"", "\"\"");
            this.jdbcTemplate.execute("ALTER TABLE review_schedules DROP CONSTRAINT \"" + safeName + "\"");
        });
    }
}
