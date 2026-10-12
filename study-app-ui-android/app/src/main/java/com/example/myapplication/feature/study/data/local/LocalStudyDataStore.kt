package com.example.myapplication.feature.study.data.local

import android.content.Context
import com.example.myapplication.core.data.APP_PREFERENCES_NAME
import com.example.myapplication.feature.account.data.local.AccountSessionStore
import com.example.myapplication.feature.account.data.local.DeviceIdentityStore
import com.example.myapplication.feature.study.domain.model.ScheduledStudy
import com.example.myapplication.feature.study.domain.model.StudyEntry

class LocalStudyDataStore(
    private val context: Context,
    private val accountId: String =
        AccountSessionStore(context).load()?.accountId
            ?: DeviceIdentityStore(context).installationId(),
    private val studyDatabase: StudyDatabase = StudyDatabase(context)
) {

    fun loadStudies(): List<StudyEntry> = this@LocalStudyDataStore.studyDatabase.loadStudies(this@LocalStudyDataStore.accountId)

    fun saveStudies(studies: List<StudyEntry>) {
        this@LocalStudyDataStore.studyDatabase.replaceStudies(this@LocalStudyDataStore.accountId, studies)
    }

    fun loadDeletedStudyIds(): Set<String> =
        this.contextPreferences().getStringSet(DELETED_STUDY_IDS_KEY, emptySet())
            .orEmpty()
            .toSet()

    fun saveDeletedStudyIds(ids: Set<String>) {
        this.contextPreferences().edit()
            .putStringSet(DELETED_STUDY_IDS_KEY, ids.toSet())
            .apply()
    }

    fun loadScheduledStudies(): List<ScheduledStudy> = this@LocalStudyDataStore.studyDatabase.loadScheduledStudies(this@LocalStudyDataStore.accountId)

    fun saveScheduledStudies(studies: List<ScheduledStudy>) {
        this@LocalStudyDataStore.studyDatabase.replaceScheduledStudies(this@LocalStudyDataStore.accountId, studies)
    }

    fun enqueueStudyUpsert(study: StudyEntry) = this@LocalStudyDataStore.studyDatabase.enqueueStudyUpsert(this@LocalStudyDataStore.accountId, study)

    fun enqueueStudyDelete(studyId: String) = this@LocalStudyDataStore.studyDatabase.enqueueStudyDelete(this@LocalStudyDataStore.accountId, studyId)

    fun deleteStudy(studyId: String) = this@LocalStudyDataStore.studyDatabase.deleteStudy(this@LocalStudyDataStore.accountId, studyId)

    fun enqueueScheduledStudyUpsert(study: ScheduledStudy) =
        this@LocalStudyDataStore.studyDatabase.enqueueScheduledStudyUpsert(this@LocalStudyDataStore.accountId, study)

    fun enqueueScheduledStudyDelete(studyId: String) =
        this@LocalStudyDataStore.studyDatabase.enqueueScheduledStudyDelete(this@LocalStudyDataStore.accountId, studyId)

    fun deleteScheduledStudy(studyId: String) =
        this@LocalStudyDataStore.studyDatabase.deleteScheduledStudy(this@LocalStudyDataStore.accountId, studyId)

    fun claimDeviceDataForAccount(installationId: String, accountId: String) =
        this@LocalStudyDataStore.studyDatabase.claimDeviceData(installationId, this@LocalStudyDataStore.accountId)

    private fun contextPreferences() =
        this@LocalStudyDataStore.context.getSharedPreferences(APP_PREFERENCES_NAME, Context.MODE_PRIVATE)

    private companion object {
        const val DELETED_STUDY_IDS_KEY = "deleted_study_ids"
    }
}
