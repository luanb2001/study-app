package com.example.myapplication.feature.account.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.feature.account.data.local.AccountSessionStore
import com.example.myapplication.feature.account.data.remote.StudyApiClient
import com.example.myapplication.feature.account.data.remote.StudyApiException
import com.example.myapplication.feature.account.domain.model.AccountSession
import com.example.myapplication.feature.pomodoro.data.local.PomodoroSessionStore
import com.example.myapplication.feature.pomodoro.data.service.PomodoroNotificationService
import com.example.myapplication.feature.study.data.local.LocalStudyDataStore
import com.example.myapplication.feature.study.data.local.StudyDatabase
import com.example.myapplication.feature.study.data.sync.StudySyncWorker
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

@Composable
fun AccountScreen(
    installationId: String,
    onAuthenticated: (AccountSession) -> Unit,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiClient = remember { StudyApiClient() }
    val sessionStore = remember { AccountSessionStore(context) }
    remember { StudyDatabase(context) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(AccountFlow.SIGN_IN) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    suspend fun finishAuthentication(session: AccountSession) {
        try {
            withContext(Dispatchers.IO) {
                LocalStudyDataStore(context, session.accountId)
                    .claimDeviceDataForAccount(installationId, session.accountId)
                PomodoroSessionStore.claimDeviceSession(context, installationId, session.accountId)
                sessionStore.save(session)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            throw AccountDataLinkException(exception)
        }
        PomodoroNotificationService.restoreIfActive(context)
        StudySyncWorker.enqueue(context, session.accountId)
        onAuthenticated(session)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.account_auth_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(mode.subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
                statusMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.account_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            enabled = !isSubmitting
        )

        if (mode == AccountFlow.SIGN_IN || mode == AccountFlow.REGISTER || mode == AccountFlow.RESET) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        stringResource(
                            if (mode == AccountFlow.RESET) R.string.account_new_password
                            else R.string.account_password
                        )
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !isSubmitting
            )
        }

        if (mode == AccountFlow.VERIFY || mode == AccountFlow.RESET) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = verificationCode,
                onValueChange = { value ->
                    verificationCode = value.filter(Char::isDigit).take(6)
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.account_email_code)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                enabled = !isSubmitting
            )
        }

        statusMessage?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.primary)
        }
        errorMessage?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                scope.launch {
                    isSubmitting = true
                    errorMessage = null
                    statusMessage = null

                    try {

                        when (mode) {
                            AccountFlow.SIGN_IN -> {
                                val session = withContext(Dispatchers.IO) {
                                    apiClient.authenticate(email.trim(), password)
                                }
                                finishAuthentication(session)
                            }

                            AccountFlow.REGISTER -> {
                                val message = withContext(Dispatchers.IO) {
                                    apiClient.register(email.trim(), password)
                                }
                                mode = AccountFlow.VERIFY
                                statusMessage = message
                            }

                            AccountFlow.VERIFY -> {
                                val session = withContext(Dispatchers.IO) {
                                    apiClient.verifyEmail(email.trim(), verificationCode)
                                }
                                finishAuthentication(session)
                            }

                            AccountFlow.RESEND -> {
                                statusMessage = withContext(Dispatchers.IO) {
                                    apiClient.resendVerification(email.trim())
                                }
                                mode = AccountFlow.VERIFY
                            }

                            AccountFlow.FORGOT -> {
                                statusMessage = withContext(Dispatchers.IO) {
                                    apiClient.requestPasswordReset(email.trim())
                                }
                                mode = AccountFlow.RESET
                            }

                            AccountFlow.RESET -> {
                                statusMessage = withContext(Dispatchers.IO) {
                                    apiClient.resetPassword(
                                        email.trim(),
                                        verificationCode,
                                        password
                                    )
                                }
                                password = ""
                                verificationCode = ""
                                mode = AccountFlow.SIGN_IN
                            }
                        }

                    } catch (exception: IOException) {
                        errorMessage = exception.userMessage()
                    } catch (exception: JSONException) {
                        errorMessage = context.getString(R.string.account_unexpected_response)
                    } catch (exception: AccountDataLinkException) {
                        errorMessage = context.getString(R.string.account_link_failed)
                    } finally {
                        isSubmitting = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting && mode.canSubmit(email, password, verificationCode)
        ) {

            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(mode.action))
            }

        }

        when (mode) {
            AccountFlow.SIGN_IN -> {
                TextButton(
                    onClick = { mode = AccountFlow.REGISTER },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                ) {
                    Text(stringResource(R.string.account_need_account))
                }
                TextButton(
                    onClick = { mode = AccountFlow.FORGOT },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                ) {
                    Text(stringResource(R.string.account_forgot_password))
                }
                TextButton(
                    onClick = { mode = AccountFlow.RESEND },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                ) {
                    Text(stringResource(R.string.account_resend_verification))
                }
            }

            AccountFlow.REGISTER -> TextButton(
                onClick = { mode = AccountFlow.SIGN_IN },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                Text(stringResource(R.string.account_already_have_account))
            }

            AccountFlow.VERIFY -> {
                TextButton(
                    onClick = { mode = AccountFlow.RESEND },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                ) {
                    Text(stringResource(R.string.account_resend_verification))
                }
                TextButton(
                    onClick = { mode = AccountFlow.SIGN_IN },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                ) {
                    Text(stringResource(R.string.account_back_to_sign_in))
                }
            }

            AccountFlow.RESET -> TextButton(
                onClick = { mode = AccountFlow.FORGOT },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                Text(stringResource(R.string.account_send_another_reset_code))
            }

            AccountFlow.RESEND, AccountFlow.FORGOT -> TextButton(
                onClick = { mode = AccountFlow.SIGN_IN },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                Text(stringResource(R.string.account_back_to_sign_in))
            }
        }

        if (onCancel != null) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                Text(stringResource(R.string.account_continue_without_account))
            }
        }

    }
}

private class AccountDataLinkException(cause: Throwable) : RuntimeException(cause)

private enum class AccountFlow(val subtitle: Int, val action: Int) {
    SIGN_IN(R.string.account_auth_subtitle, R.string.account_sign_in),
    REGISTER(R.string.account_register_subtitle, R.string.account_create),
    VERIFY(R.string.account_verify_subtitle, R.string.account_verify_email),
    RESEND(R.string.account_resend_subtitle, R.string.account_send_verification_code),
    FORGOT(R.string.account_forgot_subtitle, R.string.account_send_reset_code),
    RESET(R.string.account_reset_subtitle, R.string.account_reset_password);

    fun canSubmit(email: String, password: String, code: String): Boolean {

        if (email.isBlank()) return false

        return when (this) {
            SIGN_IN, REGISTER -> password.isNotBlank()
            VERIFY -> code.length == 6
            RESEND, FORGOT -> true
            RESET -> code.length == 6 && password.isNotBlank()
        }

    }
}

private fun IOException.userMessage(): String {

    if (this !is StudyApiException || responseBody.isBlank()) return message.orEmpty()

    return try {
        JSONObject(responseBody).optString("detail").ifBlank { message.orEmpty() }
    } catch (exception: JSONException) {
        message.orEmpty()
    }
}
