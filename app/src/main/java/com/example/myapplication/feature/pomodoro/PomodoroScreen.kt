package com.example.myapplication.feature.pomodoro

import android.os.VibrationEffect
import android.os.Vibrator
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
import com.example.myapplication.ui.components.NumberInputField
import com.example.myapplication.ui.theme.spacing
import kotlinx.coroutines.delay

private enum class PomodoroPhase {
    STUDY,
    BREAK,
    COMPLETED
}

private suspend fun notifyPhaseChange(vibrator: Vibrator?) {
    if (vibrator == null) return

    vibrator.vibrate(
        VibrationEffect.createWaveform(
            longArrayOf(0, 150, 130, 150, 130, 150),
            -1
        )
    )
    delay(710L)
}

@Composable
fun PomodoroScreen(
    subject: String,
    initialStudyMinutes: Int = 25,
    initialBreakMinutes: Int = 5,
    initialSessionCount: Int = 4,
    startImmediately: Boolean = false,
    onCompleted: (durationMinutes: Int, sessions: Int, summary: String) -> Unit = { _, _, _ -> },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember(context) {
        context.getSystemService(Vibrator::class.java)
    }
    var studyMinutes by rememberSaveable { mutableStateOf(initialStudyMinutes) }
    var breakMinutes by rememberSaveable { mutableStateOf(initialBreakMinutes) }
    var sessionCount by rememberSaveable { mutableStateOf(initialSessionCount) }

    var phase by rememberSaveable { mutableStateOf(PomodoroPhase.STUDY) }
    var currentSession by rememberSaveable { mutableIntStateOf(1) }
    var remainingSeconds by rememberSaveable { mutableIntStateOf(initialStudyMinutes * 60) }
    var isRunning by rememberSaveable { mutableStateOf(startImmediately) }
    var completedStudyMinutes by rememberSaveable { mutableIntStateOf(0) }
    var completedSessionCount by rememberSaveable { mutableIntStateOf(0) }
    var summary by rememberSaveable { mutableStateOf("") }
    var studyFinalized by rememberSaveable { mutableStateOf(false) }

    val totalStudySeconds = studyMinutes * 60
    val totalBreakSeconds = breakMinutes * 60

    LaunchedEffect(isRunning, phase) {
        if (!isRunning) {
            return@LaunchedEffect
        }

        while (isRunning && remainingSeconds > 0) {
            delay(1000L)
            if (isRunning) {
                remainingSeconds -= 1
            }
        }

        when {
            remainingSeconds != 0 -> Unit
            phase == PomodoroPhase.STUDY && currentSession >= sessionCount -> {
                completedStudyMinutes += studyMinutes
                completedSessionCount += 1
                phase = PomodoroPhase.COMPLETED
                isRunning = false
            }
            phase == PomodoroPhase.STUDY -> {
                completedStudyMinutes += studyMinutes
                completedSessionCount += 1
                notifyPhaseChange(vibrator)
                phase = PomodoroPhase.BREAK
                currentSession += 1
                remainingSeconds = totalBreakSeconds
            }
            else -> {
                notifyPhaseChange(vibrator)
                phase = PomodoroPhase.STUDY
                remainingSeconds = totalStudySeconds
            }
        }
    }

    fun resetPomodoro() {
        isRunning = false
        phase = PomodoroPhase.STUDY
        currentSession = 1
        remainingSeconds = totalStudySeconds
        completedStudyMinutes = 0
        completedSessionCount = 0
        summary = ""
    }

    val phaseLabel = when (phase) {
        PomodoroPhase.STUDY -> stringResource(R.string.pomodoro_study)
        PomodoroPhase.BREAK -> stringResource(R.string.pomodoro_break)
        PomodoroPhase.COMPLETED -> stringResource(R.string.pomodoro_completed)
    }

    LaunchedEffect(isRunning, phase, currentSession, subject, phaseLabel) {
        if (isRunning) {
            PomodoroNotificationService.update(
                context = context,
                subject = subject,
                phase = phaseLabel,
                remainingSeconds = remainingSeconds,
                currentSession = currentSession,
                sessionCount = sessionCount
            )
        } else {
            PomodoroNotificationService.stop(context)
        }
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
                            phase = PomodoroPhase.STUDY
                            currentSession = 1
                            remainingSeconds = totalStudySeconds
                            isRunning = true
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
                            onClick = { isRunning = !isRunning },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(if (isRunning) R.string.pause else R.string.start))
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
                            onValueChange = { newValue ->
                                val parsed = newValue.toIntOrNull() ?: 0
                                if (parsed > 0) {
                                    studyMinutes = parsed
                                    if (!isRunning && phase == PomodoroPhase.STUDY) {
                                        remainingSeconds = parsed * 60
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            label = stringResource(R.string.break_duration_short),
                            value = breakMinutes.toString(),
                            onValueChange = { newValue ->
                                val parsed = newValue.toIntOrNull() ?: 0
                                if (parsed > 0) {
                                    breakMinutes = parsed
                                    if (!isRunning && phase == PomodoroPhase.BREAK) {
                                        remainingSeconds = parsed * 60
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    NumberInputField(
                        label = stringResource(R.string.number_of_sessions),
                        value = sessionCount.toString(),
                        onValueChange = { newValue ->
                            val parsed = newValue.toIntOrNull() ?: 1
                            sessionCount = if (parsed > 0) parsed else 1
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
