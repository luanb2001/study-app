package com.example.myapplication.feature.study.data.sync

import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.myapplication.feature.account.data.local.AccountSessionStore
import com.example.myapplication.feature.account.data.remote.StudyApiClient
import com.example.myapplication.feature.account.data.remote.StudyApiException
import com.example.myapplication.feature.study.data.local.StudyDatabase
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

class StudySyncWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val accountId = super.inputData.getString(ACCOUNT_ID_KEY) ?: return Result.failure()
        val session = AccountSessionStore(super.applicationContext).load() ?: return Result.success()

        if (session.accountId != accountId) return Result.success()

        var currentSession = session

        val studyDatabase = StudyDatabase(super.applicationContext)
        val studyApiClient = StudyApiClient()
        var cursor = studyDatabase.syncCursor(accountId)
        var shouldContinue = true
        var pages = 0

        while (shouldContinue && pages < MAX_PAGES_PER_RUN) {
            val pending = studyDatabase.pendingChanges(accountId, UPLOAD_BATCH_SIZE)
            val response = try {
                withContext(Dispatchers.IO) {
                    try {
                        studyApiClient.synchronize(currentSession.accessToken, cursor, pending)
                    } catch (exception: StudyApiException) {

                        if (exception.statusCode != 401 || currentSession.refreshToken.isBlank()) {
                            throw exception
                        }

                        val refreshed = studyApiClient.refresh(currentSession.refreshToken)
                        AccountSessionStore(super.applicationContext).save(refreshed)
                        currentSession = refreshed
                        studyApiClient.synchronize(refreshed.accessToken, cursor, pending)
                    }
                }
            } catch (exception: StudyApiException) {

                if (exception.statusCode == 409) {
                    val conflictBody = JSONObject(exception.responseBody)
                    studyDatabase.recordConflict(
                        accountId,
                        PendingSyncConflict(
                            entityType = conflictBody.getString("entityType"),
                            entityId = conflictBody.getString("entityId"),
                            submittedVersion = conflictBody.getLong("submittedVersion"),
                            serverVersion = conflictBody.getLong("currentVersion"),
                            serverOperation = conflictBody.getString("serverOperation"),
                            localOperation = "",
                            localPayload = null,
                            serverPayload = conflictBody.optJSONObject("serverPayload")
                        )
                    )
                    this.saveFailure(accountId, CONFLICT_STATUS)
                    this.notifySyncStatus()
                    return Result.failure(
                        Data.Builder().putString(ERROR_KEY, CONFLICT_STATUS).build()
                    )
                }

                if (exception.statusCode == 401) {
                    this.saveFailure(accountId, AUTH_REQUIRED_STATUS)
                    this.notifySyncStatus()
                    return Result.failure(
                        Data.Builder().putString(ERROR_KEY, AUTH_REQUIRED_STATUS).build()
                    )
                }

                if (exception.statusCode in 400..499) {
                    this.saveFailure(accountId, exception.userMessage())
                    this.notifySyncStatus()
                    return Result.failure(
                        Data.Builder().putString(ERROR_KEY, exception.userMessage()).build()
                    )
                }

                return Result.retry()
            } catch (exception: IOException) {
                return Result.retry()
            }

            studyDatabase.markAccepted(accountId, response.accepted)
            studyDatabase.applyServerChanges(accountId, response.changes)
            cursor = maxOf(cursor, response.cursor)
            studyDatabase.saveSyncCursor(accountId, cursor)
            super.applicationContext.sendBroadcast(
                Intent(SYNC_COMPLETED_ACTION).setPackage(super.applicationContext.packageName)
            )
            pages++
            shouldContinue = response.hasMore || pending.size == UPLOAD_BATCH_SIZE
        }

        if (shouldContinue) return Result.retry()

        this.clearFailure(accountId)
        this.notifySyncStatus()
        return Result.success()
    }

    private fun saveFailure(accountId: String, message: String) {
        super.applicationContext.getSharedPreferences(SYNC_STATUS_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString("$ERROR_KEY.$accountId", message)
            .apply()
    }

    private fun clearFailure(accountId: String) {
        super.applicationContext.getSharedPreferences(SYNC_STATUS_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .remove("$ERROR_KEY.$accountId")
            .apply()
    }

    private fun StudyApiException.userMessage(): String {

        if (responseBody.isBlank()) return message ?: "Synchronization failed."

        return try {
            JSONObject(responseBody).optString("detail")
                .ifBlank { message ?: "Synchronization failed." }
        } catch (exception: JSONException) {
            message ?: "Synchronization failed."
        }
    }

    private fun notifySyncStatus() {
        super.applicationContext.sendBroadcast(
            Intent(SYNC_COMPLETED_ACTION).setPackage(super.applicationContext.packageName)
        )
    }

    companion object {
        const val SYNC_COMPLETED_ACTION = "com.example.myapplication.SYNC_COMPLETED"
        const val ACCOUNT_ID_KEY = "account_id"
        const val ERROR_KEY = "sync_error"
        const val SYNC_STATUS_PREFERENCES = "study_sync_status"
        const val CONFLICT_STATUS = "SYNC_CONFLICT"
        const val AUTH_REQUIRED_STATUS = "AUTH_REQUIRED"
        private const val WORK_NAME_PREFIX = "study-sync-"
        private const val UPLOAD_BATCH_SIZE = 100
        private const val MAX_PAGES_PER_RUN = 20

        fun enqueue(context: Context, accountId: String) {
            val request = OneTimeWorkRequestBuilder<StudySyncWorker>()
                .setInputData(Data.Builder().putString(ACCOUNT_ID_KEY, accountId).build())
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                WORK_NAME_PREFIX + accountId,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
        }
    }
}
