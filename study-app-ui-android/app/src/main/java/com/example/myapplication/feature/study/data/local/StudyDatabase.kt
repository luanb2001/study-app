package com.example.myapplication.feature.study.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.myapplication.core.data.APP_PREFERENCES_NAME
import com.example.myapplication.feature.study.data.sync.AcceptedSyncChange
import com.example.myapplication.feature.study.data.sync.DownloadedSyncChange
import com.example.myapplication.feature.study.data.sync.PendingSyncChange
import com.example.myapplication.feature.study.data.sync.PendingSyncConflict
import com.example.myapplication.feature.study.domain.model.ReviewSchedule
import com.example.myapplication.feature.study.domain.model.ScheduledStudy
import com.example.myapplication.feature.study.domain.model.StudyEntry
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class StudyDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    private val appContext = context.applicationContext

    init {
        super.writableDatabase
        this.migrateLegacyPreferences()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE study_entries (
                id TEXT NOT NULL,
                account_id TEXT NOT NULL,
                study_date TEXT NOT NULL,
                subject TEXT NOT NULL,
                description TEXT NOT NULL,
                duration_minutes INTEGER NOT NULL,
                session_count INTEGER NOT NULL,
                PRIMARY KEY (account_id, id)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE scheduled_studies (
                id TEXT NOT NULL,
                account_id TEXT NOT NULL,
                study_date TEXT NOT NULL,
                subject TEXT NOT NULL,
                session_count INTEGER NOT NULL,
                study_minutes INTEGER NOT NULL,
                break_minutes INTEGER NOT NULL,
                PRIMARY KEY (account_id, id)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE review_schedules (
                id TEXT NOT NULL,
                account_id TEXT NOT NULL,
                subject_key TEXT NOT NULL,
                subject TEXT NOT NULL,
                due_date TEXT NOT NULL,
                interval_index INTEGER NOT NULL,
                PRIMARY KEY (account_id, id),
                UNIQUE (account_id, subject_key)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE sync_entity_versions (
                account_id TEXT NOT NULL,
                entity_type TEXT NOT NULL,
                entity_id TEXT NOT NULL,
                server_version INTEGER NOT NULL DEFAULT 0,
                deleted INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (account_id, entity_type, entity_id)
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE pending_changes (
                change_id TEXT PRIMARY KEY,
                account_id TEXT NOT NULL,
                entity_type TEXT NOT NULL,
                entity_id TEXT NOT NULL,
                operation TEXT NOT NULL,
                base_version INTEGER NOT NULL,
                payload TEXT,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE sync_state (
                account_id TEXT PRIMARY KEY,
                cursor INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE sync_conflicts (
                account_id TEXT NOT NULL,
                entity_type TEXT NOT NULL,
                entity_id TEXT NOT NULL,
                submitted_version INTEGER NOT NULL,
                server_version INTEGER NOT NULL,
                server_operation TEXT NOT NULL,
                local_operation TEXT NOT NULL,
                local_payload TEXT,
                server_payload TEXT,
                PRIMARY KEY (account_id, entity_type, entity_id)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_study_account_date ON study_entries(account_id, study_date)")
        db.execSQL("CREATE INDEX idx_scheduled_account_date ON scheduled_studies(account_id, study_date)")
        db.execSQL("CREATE INDEX idx_pending_account_created ON pending_changes(account_id, created_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

        if (oldVersion < 2) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS sync_entity_versions (
                    account_id TEXT NOT NULL,
                    entity_type TEXT NOT NULL,
                    entity_id TEXT NOT NULL,
                    server_version INTEGER NOT NULL DEFAULT 0,
                    deleted INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (account_id, entity_type, entity_id)
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS pending_changes (
                    change_id TEXT PRIMARY KEY,
                    account_id TEXT NOT NULL,
                    entity_type TEXT NOT NULL,
                    entity_id TEXT NOT NULL,
                    operation TEXT NOT NULL,
                    base_version INTEGER NOT NULL,
                    payload TEXT,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS sync_state (
                    account_id TEXT PRIMARY KEY,
                    cursor INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
        }

        if (oldVersion < 3) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS sync_conflicts (
                    account_id TEXT NOT NULL,
                    entity_type TEXT NOT NULL,
                    entity_id TEXT NOT NULL,
                    submitted_version INTEGER NOT NULL,
                    server_version INTEGER NOT NULL,
                    server_operation TEXT NOT NULL,
                    local_operation TEXT NOT NULL,
                    local_payload TEXT,
                    server_payload TEXT,
                    PRIMARY KEY (account_id, entity_type, entity_id)
                )
            """.trimIndent())
        }

    }

    fun loadStudies(accountId: String): List<StudyEntry> =
        super.readableDatabase.rawQuery(
            """
                SELECT id, study_date, subject, description, duration_minutes, session_count
                FROM study_entries WHERE account_id = ? ORDER BY study_date DESC, rowid DESC
            """.trimIndent(),
            arrayOf(accountId)
        ).use { cursor ->
            buildList {

                while (cursor.moveToNext()) {
                    add(
                        StudyEntry(
                            date = LocalDate.parse(cursor.getString(1)),
                            subject = cursor.getString(2),
                            description = cursor.getString(3),
                            durationMinutes = cursor.getInt(4),
                            sessionCount = cursor.getInt(5),
                            id = cursor.getString(0)
                        )
                    )
                }

            }
        }

    fun replaceStudies(accountId: String, studies: List<StudyEntry>) {
        super.writableDatabase.beginTransaction()

        try {
            studies.forEach { study ->
                super.writableDatabase.insertOrThrow(
                    "study_entries",
                    null,
                    ContentValues().apply {
                        put("id", study.id)
                        put("account_id", accountId)
                        put("study_date", study.date.toString())
                        put("subject", study.subject)
                        put("description", study.description)
                        put("duration_minutes", study.durationMinutes)
                        put("session_count", study.sessionCount)
                    }
                )
            }
            super.writableDatabase.setTransactionSuccessful()
        } finally {
            super.writableDatabase.endTransaction()
        }
    }

    fun enqueueStudyUpsert(accountId: String, study: StudyEntry) {
        enqueueChange(accountId, STUDY, study.id, UPSERT, study.toPayload())
    }

    fun enqueueStudyDelete(accountId: String, studyId: String) {
        enqueueChange(accountId, STUDY, studyId, DELETE, null)
    }

    fun deleteStudy(accountId: String, studyId: String) {
        super.writableDatabase.delete(
            "study_entries",
            "account_id = ? AND id = ?",
            arrayOf(accountId, studyId)
        )
    }

    fun loadScheduledStudies(accountId: String): List<ScheduledStudy> =
        super.readableDatabase.rawQuery(
            """
                SELECT id, study_date, subject, session_count, study_minutes, break_minutes
                FROM scheduled_studies WHERE account_id = ? ORDER BY study_date, subject
            """.trimIndent(),
            arrayOf(accountId)
        ).use { cursor ->
            buildList {

                while (cursor.moveToNext()) {
                    add(
                        ScheduledStudy(
                            date = LocalDate.parse(cursor.getString(1)),
                            subject = cursor.getString(2),
                            sessionCount = cursor.getInt(3),
                            studyMinutes = cursor.getInt(4),
                            breakMinutes = cursor.getInt(5),
                            id = cursor.getString(0)
                        )
                    )
                }

            }
        }

    fun replaceScheduledStudies(accountId: String, studies: List<ScheduledStudy>) {
        super.writableDatabase.beginTransaction()

        try {
            studies.forEach { study ->
                super.writableDatabase.insertOrThrow(
                    "scheduled_studies",
                    null,
                    ContentValues().apply {
                        put("id", study.id)
                        put("account_id", accountId)
                        put("study_date", study.date.toString())
                        put("subject", study.subject)
                        put("session_count", study.sessionCount)
                        put("study_minutes", study.studyMinutes)
                        put("break_minutes", study.breakMinutes)
                    }
                )
            }
            super.writableDatabase.setTransactionSuccessful()
        } finally {
            super.writableDatabase.endTransaction()
        }
    }

    fun enqueueScheduledStudyUpsert(accountId: String, study: ScheduledStudy) {
        enqueueChange(accountId, SCHEDULED_STUDY, study.id, UPSERT, study.toPayload())
    }

    fun enqueueScheduledStudyDelete(accountId: String, studyId: String) {
        enqueueChange(accountId, SCHEDULED_STUDY, studyId, DELETE, null)
    }

    fun deleteScheduledStudy(accountId: String, studyId: String) {
        super.writableDatabase.delete(
            "scheduled_studies",
            "account_id = ? AND id = ?",
            arrayOf(accountId, studyId)
        )
    }

    fun loadReviews(accountId: String): List<ReviewSchedule> =
        super.readableDatabase.rawQuery(
            """
                SELECT id, subject, due_date, interval_index
                FROM review_schedules WHERE account_id = ? ORDER BY due_date
            """.trimIndent(),
            arrayOf(accountId)
        ).use { cursor ->
            buildList {

                while (cursor.moveToNext()) {
                    add(
                        ReviewSchedule(
                            id = cursor.getString(0),
                            subject = cursor.getString(1),
                            dueDate = LocalDate.parse(cursor.getString(2)),
                            intervalIndex = cursor.getInt(3)
                        )
                    )
                }

            }
        }

    fun replaceReviews(accountId: String, reviews: List<ReviewSchedule>) {
        super.writableDatabase.beginTransaction()

        try {
            reviews.forEach { review ->
                super.writableDatabase.insertOrThrow(
                    "review_schedules",
                    null,
                    ContentValues().apply {
                        put("id", review.id)
                        put("account_id", accountId)
                        put("subject_key", this@StudyDatabase.subjectKey(review.subject))
                        put("subject", review.subject)
                        put("due_date", review.dueDate.toString())
                        put("interval_index", review.intervalIndex)
                    }
                )
            }
            super.writableDatabase.setTransactionSuccessful()
        } finally {
            super.writableDatabase.endTransaction()
        }
    }

    fun enqueueReviewUpsert(accountId: String, review: ReviewSchedule) {
        enqueueChange(accountId, REVIEW, review.id, UPSERT, review.toPayload())
    }

    fun enqueueReviewDelete(accountId: String, reviewId: String) {
        enqueueChange(accountId, REVIEW, reviewId, DELETE, null)
    }

    fun deleteReviews(accountId: String, reviewIds: List<String>) {
        reviewIds.forEach { reviewId ->
            super.writableDatabase.delete(
                "review_schedules",
                "account_id = ? AND id = ?",
                arrayOf(accountId, reviewId)
            )
        }
    }

    fun enqueueChange(
        accountId: String,
        entityType: String,
        entityId: String,
        operation: String,
        payload: JSONObject?
    ) {
        val db = super.writableDatabase
        val version = db.rawQuery(
            """
                SELECT server_version FROM sync_entity_versions
                WHERE account_id = ? AND entity_type = ? AND entity_id = ?
            """.trimIndent(),
            arrayOf(accountId, entityType, entityId)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
        val pendingCount = db.rawQuery(
            """
                SELECT COUNT(*) FROM pending_changes
                WHERE account_id = ? AND entity_type = ? AND entity_id = ?
            """.trimIndent(),
            arrayOf(accountId, entityType, entityId)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
        val baseVersion = version + pendingCount
        db.insertOrThrow(
            "pending_changes",
            null,
            ContentValues().apply {
                put("change_id", UUID.randomUUID().toString())
                put("account_id", accountId)
                put("entity_type", entityType)
                put("entity_id", entityId)
                put("operation", operation)
                put("base_version", baseVersion)
                put("payload", payload?.toString())
                put("created_at", System.currentTimeMillis())
            }
        )
        db.execSQL(
            """
                UPDATE sync_conflicts SET submitted_version = ?, local_operation = ?, local_payload = ?
                WHERE account_id = ? AND entity_type = ? AND entity_id = ?
            """.trimIndent(),
            arrayOf<Any?>(
                baseVersion,
                operation,
                payload?.toString(),
                accountId,
                entityType,
                entityId
            )
        )
    }

    fun pendingChanges(accountId: String, limit: Int): List<PendingSyncChange> =
        super.readableDatabase.rawQuery(
            """
                SELECT change_id, entity_type, entity_id, operation, base_version, payload
                FROM pending_changes WHERE account_id = ?
                ORDER BY created_at, rowid LIMIT ?
            """.trimIndent(),
            arrayOf(accountId, limit.toString())
        ).use { cursor ->
            buildList {

                while (cursor.moveToNext()) {
                    add(
                        PendingSyncChange(
                            changeId = cursor.getString(0),
                            entityType = cursor.getString(1),
                            entityId = cursor.getString(2),
                            operation = cursor.getString(3),
                            baseVersion = cursor.getLong(4),
                            payload = cursor.getString(5)?.let(::JSONObject)
                        )
                    )
                }

            }
        }

    fun syncCursor(accountId: String): Long =
        super.readableDatabase.rawQuery(
            "SELECT cursor FROM sync_state WHERE account_id = ?",
            arrayOf(accountId)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }

    fun markAccepted(accountId: String, accepted: List<AcceptedSyncChange>) {
        val db = super.writableDatabase
        db.beginTransaction()

        try {
            accepted.forEach { change ->
                val pending = pendingChanges(accountId, Int.MAX_VALUE)
                    .firstOrNull { it.changeId == change.changeId }

                if (pending != null) {
                    this.saveServerVersion(
                        db,
                        accountId,
                        pending.entityType,
                        pending.entityId,
                        change.version,
                        pending.operation == DELETE
                    )
                    db.delete("pending_changes", "change_id = ?", arrayOf(change.changeId))
                }

            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun applyServerChanges(accountId: String, changes: List<DownloadedSyncChange>) {
        val db = super.writableDatabase
        db.beginTransaction()

        try {
            changes.forEach { change ->
                val localChanges = db.rawQuery(
                    """
                        SELECT COUNT(*) FROM pending_changes
                        WHERE account_id = ? AND entity_type = ? AND entity_id = ?
                    """.trimIndent(),
                    arrayOf(accountId, change.entityType, change.entityId)
                ).use { cursor ->
                    cursor.moveToFirst() && cursor.getInt(0) > 0
                }

                if (!localChanges) {
                    this.applyChangeToLocalDatabase(db, accountId, change)
                }

                this.saveServerVersion(
                    db,
                    accountId,
                    change.entityType,
                    change.entityId,
                    change.version,
                    change.operation == DELETE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun saveSyncCursor(accountId: String, cursor: Long) {
        super.writableDatabase.insertWithOnConflict(
            "sync_state",
            null,
            ContentValues().apply {
                put("account_id", accountId)
                put("cursor", cursor)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun recordConflict(accountId: String, conflict: PendingSyncConflict) {
        val pending = pendingChanges(accountId, Int.MAX_VALUE)
            .filter {
                it.entityType == conflict.entityType && it.entityId == conflict.entityId
            }
            .lastOrNull()
            ?: throw IllegalStateException("Conflicting local change is no longer queued.")
        super.writableDatabase.insertWithOnConflict(
            "sync_conflicts",
            null,
            ContentValues().apply {
                put("account_id", accountId)
                put("entity_type", conflict.entityType)
                put("entity_id", conflict.entityId)
                put("submitted_version", conflict.submittedVersion)
                put("server_version", conflict.serverVersion)
                put("server_operation", conflict.serverOperation)
                put("local_operation", pending.operation)
                put("local_payload", pending.payload?.toString())
                put("server_payload", conflict.serverPayload?.toString())
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun pendingConflicts(accountId: String): List<PendingSyncConflict> =
        super.readableDatabase.rawQuery(
            """
                SELECT entity_type, entity_id, submitted_version, server_version,
                    server_operation, local_operation, local_payload, server_payload
                FROM sync_conflicts WHERE account_id = ? ORDER BY entity_type, entity_id
            """.trimIndent(),
            arrayOf(accountId)
        ).use { cursor ->
            buildList {

                while (cursor.moveToNext()) {
                    add(
                        PendingSyncConflict(
                            entityType = cursor.getString(0),
                            entityId = cursor.getString(1),
                            submittedVersion = cursor.getLong(2),
                            serverVersion = cursor.getLong(3),
                            serverOperation = cursor.getString(4),
                            localOperation = cursor.getString(5),
                            localPayload = cursor.getString(6)?.let(::JSONObject),
                            serverPayload = cursor.getString(7)?.let(::JSONObject)
                        )
                    )
                }

            }
        }

    fun resolveConflict(accountId: String, conflict: PendingSyncConflict, keepServer: Boolean) {
        val db = super.writableDatabase
        db.beginTransaction()

        try {
            db.delete(
                "pending_changes",
                "account_id = ? AND entity_type = ? AND entity_id = ?",
                arrayOf(accountId, conflict.entityType, conflict.entityId)
            )

            if (keepServer) {
                this.applyChangeToLocalDatabase(
                    db,
                    accountId,
                    DownloadedSyncChange(
                        sequence = 0,
                        changeId = UUID.randomUUID().toString(),
                        entityType = conflict.entityType,
                        entityId = conflict.entityId,
                        operation = conflict.serverOperation,
                        version = conflict.serverVersion,
                        payload = conflict.serverPayload
                    )
                )
                this.saveServerVersion(
                    db,
                    accountId,
                    conflict.entityType,
                    conflict.entityId,
                    conflict.serverVersion,
                    conflict.serverOperation == DELETE
                )
            } else {
                this.saveServerVersion(
                    db,
                    accountId,
                    conflict.entityType,
                    conflict.entityId,
                    conflict.serverVersion,
                    conflict.serverOperation == DELETE
                )
                enqueueChange(
                    accountId,
                    conflict.entityType,
                    conflict.entityId,
                    conflict.localOperation,
                    conflict.localPayload
                )
            }

            db.delete(
                "sync_conflicts",
                "account_id = ? AND entity_type = ? AND entity_id = ?",
                arrayOf(accountId, conflict.entityType, conflict.entityId)
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun applyChangeToLocalDatabase(
        db: SQLiteDatabase,
        accountId: String,
        change: DownloadedSyncChange
    ) {

        val table = when (change.entityType) {
            STUDY -> "study_entries"
            SCHEDULED_STUDY -> "scheduled_studies"
            REVIEW -> "review_schedules"
            else -> throw IllegalArgumentException("Unknown sync entity: ${change.entityType}")
        }

        if (change.operation == DELETE) {
            db.delete(
                table,
                "account_id = ? AND id = ?",
                arrayOf(accountId, change.entityId)
            )
            return
        }

        val payload = change.payload
            ?: throw IllegalArgumentException("A remote upsert is missing its payload.")

        val values = when (change.entityType) {
            STUDY -> ContentValues().apply {
                put("id", change.entityId)
                put("account_id", accountId)
                put("study_date", payload.getString("date"))
                put("subject", payload.getString("subject"))
                put("description", payload.optString("description"))
                put("duration_minutes", payload.getInt("durationMinutes"))
                put("session_count", payload.getInt("sessionCount"))
            }
            SCHEDULED_STUDY -> ContentValues().apply {
                put("id", change.entityId)
                put("account_id", accountId)
                put("study_date", payload.getString("date"))
                put("subject", payload.getString("subject"))
                put("session_count", payload.getInt("sessionCount"))
                put("study_minutes", payload.getInt("studyMinutes"))
                put("break_minutes", payload.getInt("breakMinutes"))
            }
            REVIEW -> ContentValues().apply {
                put("id", change.entityId)
                put("account_id", accountId)
                put("subject_key", this@StudyDatabase.subjectKey(payload.getString("subject")))
                put("subject", payload.getString("subject"))
                put("due_date", payload.getString("dueDate"))
                put("interval_index", payload.getInt("intervalIndex"))
            }
            else -> throw IllegalArgumentException("Unknown sync entity: ${change.entityType}")
        }

        db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    private fun saveServerVersion(
        db: SQLiteDatabase,
        accountId: String,
        entityType: String,
        entityId: String,
        version: Long,
        deleted: Boolean
    ) {
        db.insertWithOnConflict(
            "sync_entity_versions",
            null,
            ContentValues().apply {
                put("account_id", accountId)
                put("entity_type", entityType)
                put("entity_id", entityId)
                put("server_version", version)
                put("deleted", if (deleted) 1 else 0)
            },
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun claimDeviceData(installationId: String, accountId: String) {
        val db = super.writableDatabase
        db.beginTransaction()

        try {
            val scopes = arrayOf("", installationId)
            val studies = scopes.flatMap(::loadStudies).distinctBy { it.id }
            val schedules = scopes.flatMap(::loadScheduledStudies).distinctBy { it.id }
            val reviews = scopes.flatMap(::loadReviews).distinctBy { it.subjectKey() }
            db.delete("pending_changes", "account_id IN (?, ?)", scopes)
            db.delete("sync_entity_versions", "account_id IN (?, ?)", scopes)
            db.delete("sync_state", "account_id IN (?, ?)", scopes)
            db.delete("sync_conflicts", "account_id IN (?, ?)", scopes)
            db.execSQL(
                "UPDATE study_entries SET account_id = ? WHERE account_id IN (?, ?)",
                arrayOf(accountId, *scopes)
            )
            db.execSQL(
                "UPDATE scheduled_studies SET account_id = ? WHERE account_id IN (?, ?)",
                arrayOf(accountId, *scopes)
            )

            reviews.forEach { review ->
                db.delete(
                    "review_schedules",
                    "account_id IN (?, ?) AND subject_key = ? AND id != ?",
                    arrayOf(*scopes, this@StudyDatabase.subjectKey(review.subject), review.id)
                )
                db.delete(
                    "review_schedules",
                    "account_id = ? AND subject_key = ?",
                    arrayOf(accountId, this@StudyDatabase.subjectKey(review.subject))
                )
                val stableReview = review.copy(id = this.stableReviewId(accountId, review.subject))
                db.update(
                    "review_schedules",
                    ContentValues().apply {
                        put("id", stableReview.id)
                        put("account_id", accountId)
                    },
                    "account_id IN (?, ?) AND id = ?",
                    arrayOf(*scopes, review.id)
                )
            }

            studies.forEach { enqueueChange(accountId, STUDY, it.id, UPSERT, it.toPayload()) }

            schedules.forEach {
                enqueueChange(accountId, SCHEDULED_STUDY, it.id, UPSERT, it.toPayload())
            }

            reviews.forEach { review ->
                val stableReview = review.copy(id = this.stableReviewId(accountId, review.subject))
                enqueueChange(accountId, REVIEW, stableReview.id, UPSERT, stableReview.toPayload())
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun assignLegacyTrialData(installationId: String) {
        val db = super.writableDatabase
        db.beginTransaction()

        try {
            db.execSQL("UPDATE study_entries SET account_id = ? WHERE account_id = ''", arrayOf(installationId))
            db.execSQL(
                "UPDATE scheduled_studies SET account_id = ? WHERE account_id = ''",
                arrayOf(installationId)
            )
            db.execSQL(
                "UPDATE review_schedules SET account_id = ? WHERE account_id = ''",
                arrayOf(installationId)
            )
            db.execSQL("UPDATE pending_changes SET account_id = ? WHERE account_id = ''", arrayOf(installationId))
            db.execSQL(
                "UPDATE sync_entity_versions SET account_id = ? WHERE account_id = ''",
                arrayOf(installationId)
            )
            db.execSQL("UPDATE sync_state SET account_id = ? WHERE account_id = ''", arrayOf(installationId))
            db.execSQL(
                "UPDATE sync_conflicts SET account_id = ? WHERE account_id = ''",
                arrayOf(installationId)
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun clearTrialData(installationId: String) {
        val db = super.writableDatabase
        val scopes = arrayOf("", installationId)
        db.beginTransaction()

        try {
            db.delete("pending_changes", "account_id IN (?, ?)", scopes)
            db.delete("sync_entity_versions", "account_id IN (?, ?)", scopes)
            db.delete("sync_state", "account_id IN (?, ?)", scopes)
            db.delete("sync_conflicts", "account_id IN (?, ?)", scopes)
            db.delete("study_entries", "account_id IN (?, ?)", scopes)
            db.delete("scheduled_studies", "account_id IN (?, ?)", scopes)
            db.delete("review_schedules", "account_id IN (?, ?)", scopes)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        this@StudyDatabase.appContext.getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove("deleted_study_ids")
            .apply()
    }

    private fun migrateLegacyPreferences() {
        val prefs = this@StudyDatabase.appContext.getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE)

        if (prefs.getBoolean(LEGACY_MIGRATION_KEY, false)) return

        val db = super.writableDatabase
        db.beginTransaction()

        try {
            val studyJson = prefs.getString(LEGACY_STUDIES_KEY, "[]") ?: "[]"
            val studies = JSONArray(studyJson)

            for (index in 0 until studies.length()) {
                val item = studies.getJSONObject(index)
                val study = StudyEntry(
                    date = LocalDate.parse(item.getString("date")),
                    subject = item.getString("subject"),
                    description = item.optString("description"),
                    durationMinutes = item.getInt("durationMinutes"),
                    sessionCount = item.optInt("sessionCount", 1),
                    id = item.optString("id").ifBlank { UUID.randomUUID().toString() }
                )
                db.insertWithOnConflict(
                    "study_entries",
                    null,
                    this.studyValues("", study),
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            }

            val scheduledJson = prefs.getString(LEGACY_SCHEDULED_KEY, "[]") ?: "[]"
            val scheduled = JSONArray(scheduledJson)

            for (index in 0 until scheduled.length()) {
                val item = scheduled.getJSONObject(index)
                val study = ScheduledStudy(
                    date = LocalDate.parse(item.getString("date")),
                    subject = item.getString("subject"),
                    sessionCount = item.getInt("sessionCount"),
                    studyMinutes = item.getInt("studyMinutes"),
                    breakMinutes = item.getInt("breakMinutes"),
                    id = item.optString("id").ifBlank { UUID.randomUUID().toString() }
                )
                db.insertWithOnConflict(
                    "scheduled_studies",
                    null,
                    this.scheduledValues("", study),
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            }

            val reviewJson = prefs.getString(LEGACY_REVIEWS_KEY, "[]") ?: "[]"
            val reviews = JSONArray(reviewJson)

            for (index in 0 until reviews.length()) {
                val item = reviews.getJSONObject(index)
                val review = ReviewSchedule(
                    subject = item.getString("subject"),
                    dueDate = LocalDate.parse(item.getString("dueDate")),
                    intervalIndex = item.getInt("intervalIndex"),
                    id = item.optString("id").ifBlank { UUID.randomUUID().toString() }
                )
                db.insertWithOnConflict(
                    "review_schedules",
                    null,
                    this.reviewValues("", review),
                    SQLiteDatabase.CONFLICT_IGNORE
                )
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        prefs.edit()
            .remove(LEGACY_STUDIES_KEY)
            .remove(LEGACY_SCHEDULED_KEY)
            .remove(LEGACY_REVIEWS_KEY)
            .putBoolean(LEGACY_MIGRATION_KEY, true)
            .apply()
    }

    private fun studyValues(accountId: String, study: StudyEntry) = ContentValues().apply {
        put("id", study.id)
        put("account_id", accountId)
        put("study_date", study.date.toString())
        put("subject", study.subject)
        put("description", study.description)
        put("duration_minutes", study.durationMinutes)
        put("session_count", study.sessionCount)
    }

    private fun scheduledValues(accountId: String, study: ScheduledStudy) = ContentValues().apply {
        put("id", study.id)
        put("account_id", accountId)
        put("study_date", study.date.toString())
        put("subject", study.subject)
        put("session_count", study.sessionCount)
        put("study_minutes", study.studyMinutes)
        put("break_minutes", study.breakMinutes)
    }

    private fun reviewValues(accountId: String, review: ReviewSchedule) = ContentValues().apply {
        put("id", review.id)
        put("account_id", accountId)
        put("subject_key", this@StudyDatabase.subjectKey(review.subject))
        put("subject", review.subject)
        put("due_date", review.dueDate.toString())
        put("interval_index", review.intervalIndex)
    }

    private fun subjectKey(subject: String) = subject.trim().lowercase(Locale.ROOT)

    private fun ReviewSchedule.subjectKey() = this@StudyDatabase.subjectKey(this.subject)

    private fun stableReviewId(accountId: String, subject: String): String {
        val key = "$accountId:${this.subjectKey(subject)}"
        return UUID.nameUUIDFromBytes(key.toByteArray(Charsets.UTF_8)).toString()
    }

    private fun StudyEntry.toPayload() = JSONObject()
        .put("date", date.toString())
        .put("subject", subject)
        .put("description", description)
        .put("durationMinutes", durationMinutes)
        .put("sessionCount", sessionCount)

    private fun ScheduledStudy.toPayload() = JSONObject()
        .put("date", date.toString())
        .put("subject", subject)
        .put("sessionCount", sessionCount)
        .put("studyMinutes", studyMinutes)
        .put("breakMinutes", breakMinutes)

    private fun ReviewSchedule.toPayload() = JSONObject()
        .put("id", id)
        .put("subject", subject)
        .put("dueDate", dueDate.toString())
        .put("intervalIndex", intervalIndex)

    companion object {
        const val STUDY = "STUDY"
        const val SCHEDULED_STUDY = "SCHEDULED_STUDY"
        const val REVIEW = "REVIEW"
        const val UPSERT = "UPSERT"
        const val DELETE = "DELETE"
        private const val DATABASE_NAME = "study_app.db"
        private const val DATABASE_VERSION = 3
        private const val LEGACY_MIGRATION_KEY = "legacy_data_migrated_to_sqlite"
        private const val LEGACY_STUDIES_KEY = "user_study_entries"
        private const val LEGACY_SCHEDULED_KEY = "user_scheduled_studies"
        private const val LEGACY_REVIEWS_KEY = "review_schedules"
    }
}
