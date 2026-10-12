package com.example.myapplication.feature.account.data.remote

import com.example.myapplication.BuildConfig
import com.example.myapplication.feature.account.domain.model.AccountSession
import com.example.myapplication.feature.study.data.sync.AcceptedSyncChange
import com.example.myapplication.feature.study.data.sync.DownloadedSyncChange
import com.example.myapplication.feature.study.data.sync.PendingSyncChange
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

class StudyApiClient(
    private val baseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/')
) {
    fun authenticate(email: String, password: String): AccountSession =
        this.session(this.post(
            "/api/auth/login",
            JSONObject().put("email", email).put("password", password)
        ))

    fun register(email: String, password: String): String =
        this.post(
            "/api/auth/register",
            JSONObject().put("email", email).put("password", password)
        ).getString("message")

    fun verifyEmail(email: String, code: String): AccountSession =
        this.session(this.post("/api/auth/verify-email", this.codePayload(email, code)))

    fun resendVerification(email: String): String =
        this.post("/api/auth/resend-verification", JSONObject().put("email", email))
            .getString("message")

    fun requestPasswordReset(email: String): String =
        this.post("/api/auth/password/forgot", JSONObject().put("email", email))
            .getString("message")

    fun resetPassword(email: String, code: String, newPassword: String): String =
        this.post(
            "/api/auth/password/reset",
            this.codePayload(email, code).put("newPassword", newPassword)
        ).getString("message")

    fun refresh(refreshToken: String): AccountSession =
        this.session(this.post("/api/auth/refresh", JSONObject().put("refreshToken", refreshToken)))

    fun logout(refreshToken: String) {
        this.post("/api/auth/logout", JSONObject().put("refreshToken", refreshToken))
    }

    private fun session(response: JSONObject) = AccountSession(
        accountId = response.getString("accountId"),
        email = response.getString("email"),
        accessToken = response.getString("accessToken"),
        refreshToken = response.getString("refreshToken")
    )

    private fun codePayload(email: String, code: String) =
        JSONObject().put("email", email).put("code", code)

    fun synchronize(
        accessToken: String,
        cursor: Long,
        pendingChanges: List<PendingSyncChange>
    ): SyncApiResponse {
        val changes = JSONArray().apply {

            pendingChanges.forEach { change ->
                put(
                    JSONObject()
                        .put("changeId", change.changeId)
                        .put("entityType", change.entityType)
                        .put("entityId", change.entityId)
                        .put("operation", change.operation)
                        .put("baseVersion", change.baseVersion)
                        .put("payload", change.payload)
                )
            }

        }
        val response = this.post(
            "/api/sync",
            JSONObject().put("cursor", cursor).put("changes", changes),
            accessToken
        )
        val acceptedArray = response.getJSONArray("accepted")
        val accepted = (0 until acceptedArray.length())
            .map { index ->
                val item = acceptedArray.getJSONObject(index)
                AcceptedSyncChange(item.getString("changeId"), item.getLong("version"))
            }
        val changesArray = response.getJSONArray("changes")
        val downloaded = (0 until changesArray.length())
            .map { index ->
                val item = changesArray.getJSONObject(index)
                DownloadedSyncChange(
                    sequence = item.getLong("sequence"),
                    changeId = item.getString("changeId"),
                    entityType = item.getString("entityType"),
                    entityId = item.getString("entityId"),
                    operation = item.getString("operation"),
                    version = item.getLong("version"),
                    payload = item.optJSONObject("payload")
                )
            }
        return SyncApiResponse(
            cursor = response.getLong("cursor"),
            hasMore = response.getBoolean("hasMore"),
            accepted = accepted,
            changes = downloaded
        )
    }

    private fun post(path: String, payload: JSONObject, accessToken: String? = null): JSONObject {

        if (!BuildConfig.DEBUG && !this@StudyApiClient.baseUrl.startsWith("https://")) {
            throw IOException("The production API URL must use HTTPS.")
        }

        val connection = URL("${this@StudyApiClient.baseUrl}$path").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
        connection.readTimeout = READ_TIMEOUT_MILLIS
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")

        if (accessToken != null) {
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
        }

        try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (status !in 200..299) {
                throw StudyApiException(status, body)
            }

            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 20_000
    }
}

data class SyncApiResponse(
    val cursor: Long,
    val hasMore: Boolean,
    val accepted: List<AcceptedSyncChange>,
    val changes: List<DownloadedSyncChange>
)

class StudyApiException(
    val statusCode: Int,
    val responseBody: String
) : IOException("Study API returned HTTP $statusCode.")
