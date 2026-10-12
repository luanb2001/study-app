package com.example.myapplication.feature.account.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.myapplication.feature.account.domain.model.AccountSession
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

class AccountSessionStore(context: Context) {
    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): AccountSession? {
        val ciphertext = this@AccountSessionStore.sharedPreferences.getString(CIPHERTEXT_KEY, null) ?: return null
        val encodedIv = this@AccountSessionStore.sharedPreferences.getString(IV_KEY, null)
            ?: throw IllegalStateException("Stored account session is incomplete.")
        val iv = Base64.decode(encodedIv, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, this.getKey(), GCMParameterSpec(128, iv))
        val json = String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), Charsets.UTF_8)
        val values = JSONObject(json)
        return AccountSession(
            accountId = values.getString(ACCOUNT_ID_KEY),
            email = values.getString(EMAIL_KEY),
            accessToken = values.getString(TOKEN_KEY),
            refreshToken = values.optString(REFRESH_TOKEN_KEY)
        )
    }

    fun save(session: AccountSession) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, this.getKey())
        val values = JSONObject()
            .put(ACCOUNT_ID_KEY, session.accountId)
            .put(EMAIL_KEY, session.email)
            .put(TOKEN_KEY, session.accessToken)
            .put(REFRESH_TOKEN_KEY, session.refreshToken)
        val encrypted = Base64.encodeToString(
            cipher.doFinal(values.toString().toByteArray(Charsets.UTF_8)),
            Base64.NO_WRAP
        )
        val encodedIv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        this@AccountSessionStore.sharedPreferences.edit()
            .putString(CIPHERTEXT_KEY, encrypted)
            .putString(IV_KEY, encodedIv)
            .apply()
    }

    fun clear() {
        this@AccountSessionStore.sharedPreferences.edit().clear().apply()
    }

    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES_NAME = "study_app_account_session"
        const val CIPHERTEXT_KEY = "session_ciphertext"
        const val IV_KEY = "session_iv"
        const val ACCOUNT_ID_KEY = "accountId"
        const val EMAIL_KEY = "email"
        const val TOKEN_KEY = "accessToken"
        const val REFRESH_TOKEN_KEY = "refreshToken"
        const val KEY_ALIAS = "study_app_account_session_key"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
