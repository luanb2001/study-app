package com.example.myapplication.feature.pomodoro

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.feature.study.MAX_TIMER_MINUTES
import com.example.myapplication.ui.components.NumberInputField
import com.example.myapplication.ui.theme.SuccessGreen
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
    val defaultStudyMinutes = (restoredSession?.studyMinutes ?: initialStudyMinutes)
        .coerceIn(1, MAX_TIMER_MINUTES)
    val defaultBreakMinutes = (restoredSession?.breakMinutes ?: initialBreakMinutes)
        .coerceIn(1, MAX_TIMER_MINUTES)
    val defaultSessionCount = (restoredSession?.sessionCount ?: initialSessionCount)
        .coerceAtLeast(1)
    var studyMinutesText by rememberSaveable(subject) {
        mutableStateOf(defaultStudyMinutes.toString())
    }
    var breakMinutesText by rememberSaveable(subject) {
        mutableStateOf(defaultBreakMinutes.toString())
    }
    var sessionCountText by rememberSaveable(subject) {
        mutableStateOf(defaultSessionCount.toString())
    }
    val validStudyMinutes = studyMinutesText.toIntOrNull()
        ?.takeIf { it in 1..MAX_TIMER_MINUTES }
    val validBreakMinutes = breakMinutesText.toIntOrNull()
        ?.takeIf { it in 1..MAX_TIMER_MINUTES }
    val validSessionCount = sessionCountText.toIntOrNull()?.takeIf { it > 0 }
    val configurationValid = validStudyMinutes != null &&
        validBreakMinutes != null &&
        validSessionCount != null
    val durationValidationMessage = stringResource(
        R.string.pomodoro_duration_validation,
        MAX_TIMER_MINUTES
    )
    val studyMinutes = validStudyMinutes ?: defaultStudyMinutes
    val breakMinutes = validBreakMinutes ?: defaultBreakMinutes
    val sessionCount = validSessionCount ?: defaultSessionCount
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.large),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = subject,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (phase != PomodoroPhase.COMPLETED) {
                    Text(
                        text = stringResource(
                            R.string.pomodoro_session_count,
                            currentSession,
                            sessionCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            }
            IconButton(onClick = onBack, enabled = false) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        if (phase == PomodoroPhase.COMPLETED) {
            PomodoroCompletionCard(
                completedStudyMinutes = completedStudyMinutes,
                summary = summary,
                studyFinalized = studyFinalized,
                onSummaryChange = { summary = it },
                onAddMoreSessions = { startSession(session) },
                onFinishStudy = {

                    if (!studyFinalized) {
                        PomodoroNotificationService.reset(context)
                        onCompleted(
                            completedStudyMinutes,
                            completedSessionCount,
                            summary.trim()
                        )
                        studyFinalized = true
                    }

                }
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(MaterialTheme.spacing.extraLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.shapes.large
                            )
                            .padding(4.dp)
                    ) {
                        listOf(
                            PomodoroPhase.STUDY to R.string.pomodoro_study,
                            PomodoroPhase.BREAK to R.string.pomodoro_break

                        ).forEach { (phaseOption, labelResource) ->
                            val selected = phase == phaseOption
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = MaterialTheme.shapes.large
                            ) {
                                Text(
                                    text = stringResource(labelResource),
                                    modifier = Modifier.padding(vertical = MaterialTheme.spacing.small),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

                    val phaseDuration = (if (phase == PomodoroPhase.BREAK) {
                        breakMinutes
                    } else {
                        studyMinutes
                    }) * 60
                    val progress = (phaseDuration - remainingSeconds)
                        .coerceIn(0, phaseDuration)
                        .toFloat() / phaseDuration
                    val ringColor = if (phase == PomodoroPhase.BREAK) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        SuccessGreen
                    }
                    val ringTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    Box(
                        modifier = Modifier.size(232.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 10.dp.toPx()
                            drawCircle(
                                color = ringTrackColor,
                                style = Stroke(width = strokeWidth)
                            )
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = progress * 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(
                                    R.string.pomodoro_time_format,
                                    remainingSeconds / 60,
                                    remainingSeconds % 60
                                ),
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(
                                    R.string.pomodoro_time_total,
                                    phaseDuration / 60,
                                    0
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                    Text(
                        text = stringResource(
                            R.string.pomodoro_session_count,
                            currentSession,
                            sessionCount
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = { resetPomodoro() }) {
                                Icon(
                                    Icons.Default.RestartAlt,
                                    contentDescription = stringResource(R.string.reset)
                                )
                            }
                            Text(
                                stringResource(R.string.reset),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                val currentSessionState = session

                                if (currentSessionState == null) {
                                    if (configurationValid) startSession()
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
                            enabled = session != null || configurationValid,
                            modifier = Modifier.size(68.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = stringResource(
                                    when {
                                        isRunning -> R.string.pause
                                        session != null -> R.string.resume_study
                                        else -> R.string.start
                                    }
                                ),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                            Text(
                                stringResource(R.string.back),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (phase != PomodoroPhase.COMPLETED) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                val nextPhase = if (phase == PomodoroPhase.STUDY) {
                    R.string.pomodoro_break
                } else {
                    R.string.pomodoro_study
                }
                val nextDuration = if (phase == PomodoroPhase.STUDY) {
                    breakMinutes
                } else {
                    studyMinutes
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                        Text(
                            text = stringResource(
                                R.string.pomodoro_next_phase,
                                stringResource(nextPhase),
                                nextDuration
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
            }

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
                            value = studyMinutesText,
                            enabled = !configurationLocked,
                            onValueChange = { studyMinutesText = it },
                            isError = validStudyMinutes == null,
                            supportingText = durationValidationMessage
                                .takeIf { validStudyMinutes == null },
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            label = stringResource(R.string.break_duration_short),
                            value = breakMinutesText,
                            enabled = !configurationLocked,
                            onValueChange = { breakMinutesText = it },
                            isError = validBreakMinutes == null,
                            supportingText = durationValidationMessage
                                .takeIf { validBreakMinutes == null },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    NumberInputField(
                        label = stringResource(R.string.number_of_sessions),
                        value = sessionCountText,
                        enabled = !configurationLocked,
                        onValueChange = { sessionCountText = it },
                        isError = validSessionCount == null,
                        supportingText = if (validSessionCount == null) {
                            stringResource(R.string.pomodoro_sessions_validation)
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

    }
}

@Composable
fun PomodoroPictureInPictureScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var session by remember(context) {
        mutableStateOf(PomodoroSessionStore.load(context))
    }
    LaunchedEffect(context) {
        while (true) {
            session = PomodoroSessionStore.load(context)
            delay(250L)
        }
    }
    val activeSession = session?.takeIf { it.phase != PomodoroPhase.COMPLETED }
    val remainingSeconds = activeSession?.currentRemainingSeconds() ?: 0
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activeSession?.subject.orEmpty(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(

                        if (activeSession?.phase == PomodoroPhase.BREAK) {
                            R.string.pomodoro_break
                        } else {
                            R.string.pomodoro_study
                        }

                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(
                    R.string.pomodoro_time_format,
                    remainingSeconds / 60,
                    remainingSeconds % 60
                ),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PomodoroCompletionCard(
    completedStudyMinutes: Int,
    summary: String,
    studyFinalized: Boolean,
    onSummaryChange: (String) -> Unit,
    onAddMoreSessions: () -> Unit,
    onFinishStudy: () -> Unit
) {
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
                text = stringResource(R.string.study_finished_duration, completedStudyMinutes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
            Button(
                onClick = onAddMoreSessions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.add_more_sessions))
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            OutlinedTextField(
                value = summary,
                onValueChange = onSummaryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.study_summary)) },
                placeholder = { Text(stringResource(R.string.study_summary_hint)) },
                minLines = 3
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            Button(
                onClick = onFinishStudy,
                enabled = !studyFinalized,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(

                        if (studyFinalized) {
                            R.string.study_finalized
                        } else {
                            R.string.finish_study
                        }

                    )
                )
            }
        }
    }
}
