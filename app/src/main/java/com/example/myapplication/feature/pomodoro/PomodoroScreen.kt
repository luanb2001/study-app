package com.example.myapplication.feature.pomodoro

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R
import com.example.myapplication.feature.study.MAX_TIMER_MINUTES
import com.example.myapplication.ui.components.NumberInputField
import com.example.myapplication.ui.theme.spacing
import kotlinx.coroutines.delay

@Composable
fun PomodoroScreen(
    subject: String,
    initialStudyMinutes: Int = 25,
    initialBreakMinutes: Int = 5,
    initialSessionCount: Int = 4,
    isReview: Boolean = false,
    scheduledStudyId: String = "",
    onCompleted: (durationMinutes: Int, sessions: Int, summary: String) -> Unit = { _, _, _ -> },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val restoredSession = remember(context, subject) {
        PomodoroSessionStore.load(context)?.takeIf { it.subject == subject }
    }
    var studyMinutes by rememberSaveable {
        mutableIntStateOf(
            restoredSession?.studyMinutes
                ?: initialStudyMinutes.coerceIn(1, MAX_TIMER_MINUTES)
        )
    }
    var breakMinutes by rememberSaveable {
        mutableIntStateOf(
            restoredSession?.breakMinutes
                ?: initialBreakMinutes.coerceIn(1, MAX_TIMER_MINUTES)
        )
    }
    var sessionCount by rememberSaveable {
        mutableIntStateOf(restoredSession?.sessionCount ?: initialSessionCount.coerceAtLeast(1))
    }
    var session by remember(subject) { mutableStateOf(restoredSession) }
    var summary by rememberSaveable { mutableStateOf("") }
    var studyFinalized by rememberSaveable { mutableStateOf(false) }
    val phase = session?.phase ?: PomodoroPhase.STUDY
    val currentSession = session?.currentSession ?: 1
    val remainingSeconds = session?.currentRemainingSeconds() ?: studyMinutes * 60
    val isRunning = session?.isRunning == true
    val configurationLocked = session != null
    val completedStudyMinutes = session?.completedStudyMinutes ?: 0
    val completedSessionCount = session?.completedSessionCount ?: 0

    LaunchedEffect(context, subject) {
        session?.takeIf { it.phase != PomodoroPhase.COMPLETED }?.let {
            PomodoroNotificationService.start(context, it)
        }
        while (true) {
            delay(250L)
            session = PomodoroSessionStore.load(context)?.takeIf { it.subject == subject }
        }
    }

    fun resetPomodoro() {
        PomodoroNotificationService.reset(context)
        session = null
        summary = ""
        studyFinalized = false
    }

    fun startSession(previous: PomodoroSessionState? = null) {
        val remaining = studyMinutes * 60
        val now = SystemClock.elapsedRealtime()
        val newSession = PomodoroSessionState(
            subject = subject,
            studyMinutes = studyMinutes,
            breakMinutes = breakMinutes,
            sessionCount = sessionCount,
            currentSession = 1,
            phase = PomodoroPhase.STUDY,
            remainingSeconds = remaining,
            isRunning = true,
            deadlineElapsedRealtime = now + remaining * 1_000L,
            completedStudyMinutes = previous?.completedStudyMinutes ?: 0,
            completedSessionCount = previous?.completedSessionCount ?: 0,
            isReview = isReview,
            scheduledStudyId = scheduledStudyId
        )
        session = newSession
        studyFinalized = false
        PomodoroNotificationService.start(context, newSession)
    }

    val phaseLabel = when (phase) {
        PomodoroPhase.STUDY -> stringResource(R.string.pomodoro_study)
        PomodoroPhase.BREAK -> stringResource(R.string.pomodoro_break)
        PomodoroPhase.COMPLETED -> stringResource(R.string.pomodoro_completed)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.large),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }

            Text(
                text = subject,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        if (phase == PomodoroPhase.COMPLETED) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(MaterialTheme.spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.study_finished),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                    Text(
                        text = stringResource(
                            R.string.study_finished_duration,
                            completedStudyMinutes
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                    Button(
                        onClick = {
                            startSession(session)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.add_more_sessions))
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    OutlinedTextField(
                        value = summary,
                        onValueChange = { summary = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.study_summary)) },
                        placeholder = { Text(stringResource(R.string.study_summary_hint)) },
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    Button(
                        onClick = {
                            if (!studyFinalized) {
                                PomodoroNotificationService.reset(context)
                                onCompleted(
                                    completedStudyMinutes,
                                    completedSessionCount,
                                    summary.trim()
                                )
                                studyFinalized = true
                            }
                        },
                        enabled = !studyFinalized,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(
                                if (studyFinalized) R.string.study_finalized else R.string.finish_study
                            )
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(MaterialTheme.spacing.extraLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = if (phase == PomodoroPhase.BREAK) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        },
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = phaseLabel,
                            modifier = Modifier.padding(
                                horizontal = MaterialTheme.spacing.large,
                                vertical = MaterialTheme.spacing.small
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (phase == PomodoroPhase.BREAK) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

                    Text(
                        text = stringResource(
                            R.string.pomodoro_time_format,
                            remainingSeconds / 60,
                            remainingSeconds % 60
                        ),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                    Text(
                        text = stringResource(
                            R.string.pomodoro_session_count,
                            currentSession,
                            sessionCount
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        Button(
                            onClick = {
                                val currentSessionState = session
                                if (currentSessionState == null) {
                                    startSession()
                                } else if (currentSessionState.isRunning) {
                                    val paused = currentSessionState.snapshot().copy(
                                        isRunning = false,
                                        deadlineElapsedRealtime = 0L
                                    )
                                    session = paused
                                    PomodoroNotificationService.pause(context)
                                } else {
                                    val resumed = currentSessionState.copy(
                                        isRunning = true,
                                        deadlineElapsedRealtime = SystemClock.elapsedRealtime() +
                                            currentSessionState.remainingSeconds * 1_000L
                                    )
                                    session = resumed
                                    PomodoroNotificationService.resume(context)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                stringResource(
                                    when {
                                        isRunning -> R.string.pause
                                        session != null -> R.string.resume_study
                                        else -> R.string.start
                                    }
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = { resetPomodoro() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.reset))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
                    Text(
                        text = stringResource(R.string.configuration),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        NumberInputField(
                            label = stringResource(R.string.study_duration_short),
                            value = studyMinutes.toString(),
                            enabled = !configurationLocked,
                            onValueChange = { newValue ->
                                val parsed = newValue.toIntOrNull() ?: 0
                                if (parsed in 1..MAX_TIMER_MINUTES) {
                                    studyMinutes = parsed
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            label = stringResource(R.string.break_duration_short),
                            value = breakMinutes.toString(),
                            enabled = !configurationLocked,
                            onValueChange = { newValue ->
                                val parsed = newValue.toIntOrNull() ?: 0
                                if (parsed in 1..MAX_TIMER_MINUTES) {
                                    breakMinutes = parsed
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    NumberInputField(
                        label = stringResource(R.string.number_of_sessions),
                        value = sessionCount.toString(),
                        enabled = !configurationLocked,
                        onValueChange = { newValue ->
                            val parsed = newValue.toIntOrNull()
                            if (parsed != null && parsed > 0) sessionCount = parsed
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
