CREATE TABLE IF NOT EXISTS study_entries (
    id VARCHAR(36) PRIMARY KEY,
    study_date DATE NOT NULL,
    subject VARCHAR(120) NOT NULL,
    subject_key VARCHAR(120) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    session_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_study_entries_subject_date
    ON study_entries (subject_key, study_date);
CREATE TABLE IF NOT EXISTS scheduled_studies (
    id VARCHAR(36) PRIMARY KEY,
    study_date DATE NOT NULL,
    subject VARCHAR(120) NOT NULL,
    session_count INTEGER NOT NULL,
    study_minutes INTEGER NOT NULL,
    break_minutes INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS review_schedules (
    id VARCHAR(36) PRIMARY KEY,
    subject VARCHAR(120) NOT NULL,
    subject_key VARCHAR(120) NOT NULL UNIQUE,
    due_date DATE NOT NULL,
    interval_index INTEGER NOT NULL
);
