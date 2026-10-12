package com.example.myapplication.feature.account.data.local

import android.content.Context
import android.provider.Settings
import java.util.UUID

class DeviceIdentityStore(context: Context) {
    private val appContext = context.applicationContext
    private val sharedPreferences = this@DeviceIdentityStore.appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun installationId(): String {
        val androidId = Settings.Secure.getString(
            this@DeviceIdentityStore.appContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        if (!androidId.isNullOrBlank()) return "android:$androidId"

        val existingId = this@DeviceIdentityStore.sharedPreferences.getString(INSTALLATION_ID_KEY, null)
        if (existingId != null) return existingId

        val newId = "installation:${UUID.randomUUID()}"
        this@DeviceIdentityStore.sharedPreferences.edit().putString(INSTALLATION_ID_KEY, newId).apply()
        return newId
    }

    private companion object {
        const val PREFERENCES_NAME = "study_app_installation"
        const val INSTALLATION_ID_KEY = "installation_id"
    }
}
