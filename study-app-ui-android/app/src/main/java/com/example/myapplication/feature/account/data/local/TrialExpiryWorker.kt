package com.example.myapplication.feature.account.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.myapplication.feature.pomodoro.data.local.PomodoroSessionStore
import com.example.myapplication.feature.pomodoro.data.service.PomodoroNotificationService
import com.example.myapplication.feature.study.data.local.StudyDatabase

class TrialExpiryWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val installationId = super.inputData.getString(INSTALLATION_ID_KEY) ?: return Result.failure()
        val trial = TrialPeriodStore(super.applicationContext)
        val studyDatabase = StudyDatabase(super.applicationContext)

        val session = AccountSessionStore(super.applicationContext).load()

        if (session != null) {
            studyDatabase.claimDeviceData(installationId, session.accountId)
            PomodoroSessionStore.claimDeviceSession(
                super.applicationContext,
                installationId,
                session.accountId
            )
            return Result.success()
        }

        if (!trial.isExpired()) {
            trial.scheduleExpiry(super.applicationContext, installationId)
            return Result.success()
        }

        studyDatabase.clearTrialData(installationId)
        PomodoroNotificationService.reset(super.applicationContext)
        return Result.success()
    }

    companion object {
        private const val INSTALLATION_ID_KEY = "installation_id"

        fun inputData(installationId: String) =
            Data.Builder().putString(INSTALLATION_ID_KEY, installationId).build()
    }
}
