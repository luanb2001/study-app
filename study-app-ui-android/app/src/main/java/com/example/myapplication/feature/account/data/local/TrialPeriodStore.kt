package com.example.myapplication.feature.account.data.local

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class TrialPeriodStore(context: Context) {
    private val appContext = context.applicationContext
    private val sharedPreferences = this@TrialPeriodStore.appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun ensureStarted(): Long {
        val existingStart = this@TrialPeriodStore.sharedPreferences.getLong(TRIAL_START_KEY, 0L)
        if (existingStart > 0L) return existingStart

        val startTime = System.currentTimeMillis()
        this@TrialPeriodStore.sharedPreferences.edit().putLong(TRIAL_START_KEY, startTime).apply()
        return startTime
    }

    fun deadlineMillis(): Long = ensureStarted() + TRIAL_DURATION_MILLIS

    fun remainingDays(): Int {
        val remainingMillis = (deadlineMillis() - System.currentTimeMillis()).coerceAtLeast(0L)
        return ((remainingMillis + MILLIS_PER_DAY - 1) / MILLIS_PER_DAY).toInt()
    }

    fun isExpired(): Boolean = System.currentTimeMillis() >= deadlineMillis()

    fun scheduleExpiry(context: Context, installationId: String) {
        val delayMillis = (deadlineMillis() - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<TrialExpiryWorker>()
            .setInputData(TrialExpiryWorker.inputData(installationId))
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        const val TRIAL_DURATION_MILLIS = 30L * 24 * 60 * 60 * 1000
        const val PREFERENCES_NAME = "study_app_trial"
        const val TRIAL_START_KEY = "trial_started_at"
        private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
        private const val WORK_NAME = "study-trial-expiry"
    }
}
