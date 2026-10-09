package com.example.myapplication.feature.study

import android.content.Context
import com.example.myapplication.APP_PREFERENCES_NAME
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

class LocalStudyDataStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        APP_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun loadStudies(): List<StudyEntry> =
        readArray(STUDY_ENTRIES_KEY).map { study ->
            StudyEntry(
                date = LocalDate.parse(study.getString(DATE_KEY)),
                subject = study.getString(SUBJECT_KEY),
                description = study.getString(DESCRIPTION_KEY),
                durationMinutes = study.getInt(DURATION_KEY),
                sessionCount = study.getInt(SESSION_COUNT_KEY),
                id = study.optString(ID_KEY).ifBlank { UUID.randomUUID().toString() }
            )
        }

    fun saveStudies(studies: List<StudyEntry>) {
        saveArray(STUDY_ENTRIES_KEY, studies.map { study ->
            JSONObject()
                .put(DATE_KEY, study.date.toString())
                .put(SUBJECT_KEY, study.subject)
                .put(DESCRIPTION_KEY, study.description)
                .put(DURATION_KEY, study.durationMinutes)
                .put(SESSION_COUNT_KEY, study.sessionCount)
                .put(ID_KEY, study.id)
        })
    }

    fun loadDeletedStudyIds(): Set<String> =
        preferences.getStringSet(DELETED_STUDY_IDS_KEY, emptySet())
            .orEmpty()
            .toSet()

    fun saveDeletedStudyIds(ids: Set<String>) {
        preferences.edit()
            .putStringSet(DELETED_STUDY_IDS_KEY, ids.toSet())
            .apply()
    }

    fun loadScheduledStudies(): List<ScheduledStudy> =
        readArray(SCHEDULED_STUDIES_KEY).map { study ->
            ScheduledStudy(
                date = LocalDate.parse(study.getString(DATE_KEY)),
                subject = study.getString(SUBJECT_KEY),
                sessionCount = study.getInt(SESSION_COUNT_KEY),
                studyMinutes = study.getInt(STUDY_MINUTES_KEY),
                breakMinutes = study.getInt(BREAK_MINUTES_KEY),
                id = study.optString(ID_KEY).ifBlank { UUID.randomUUID().toString() }
            )
        }

    fun saveScheduledStudies(studies: List<ScheduledStudy>) {
        saveArray(SCHEDULED_STUDIES_KEY, studies.map { study ->
            JSONObject()
                .put(DATE_KEY, study.date.toString())
                .put(SUBJECT_KEY, study.subject)
                .put(SESSION_COUNT_KEY, study.sessionCount)
                .put(STUDY_MINUTES_KEY, study.studyMinutes)
                .put(BREAK_MINUTES_KEY, study.breakMinutes)
                .put(ID_KEY, study.id)
        })
    }

    private fun readArray(key: String): List<JSONObject> {
        val json = preferences.getString(key, "[]") ?: "[]"
        val array = JSONArray(json)
        return (0 until array.length()).map(array::getJSONObject)
    }

    private fun saveArray(key: String, objects: List<JSONObject>) {
        val array = JSONArray().apply {
            objects.forEach(::put)
        }
        preferences.edit().putString(key, array.toString()).apply()
    }

    private companion object {
        const val STUDY_ENTRIES_KEY = "user_study_entries"
        const val SCHEDULED_STUDIES_KEY = "user_scheduled_studies"
        const val DELETED_STUDY_IDS_KEY = "deleted_study_ids"
        const val DATE_KEY = "date"
        const val SUBJECT_KEY = "subject"
        const val DESCRIPTION_KEY = "description"
        const val DURATION_KEY = "durationMinutes"
        const val SESSION_COUNT_KEY = "sessionCount"
        const val STUDY_MINUTES_KEY = "studyMinutes"
        const val BREAK_MINUTES_KEY = "breakMinutes"
        const val ID_KEY = "id"
    }
}
