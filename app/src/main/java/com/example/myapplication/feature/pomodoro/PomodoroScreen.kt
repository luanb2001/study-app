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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import com.example.myapplication.R
import com.example.myapplication.ui.theme.spacing
import kotlinx.coroutines.delay

private enum class PomodoroPhase {
    STUDY,
    BREAK,
    COMPLETED
}

@Composable
fun PomodoroScreen(subject: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val vibrator = remember(context) {
        context.getSystemService(Vibrator::class.java)
    }
    var studyMinutes by rememberSaveable { mutableStateOf(25) }
    var breakMinutes by rememberSaveable { mutableStateOf(5) }
    var sessionCount by rememberSaveable { mutableStateOf(4) }

    var phase by rememberSaveable { mutableStateOf(PomodoroPhase.STUDY) }
    var currentSession by rememberSaveable { mutableIntStateOf(1) }
    var remainingSeconds by rememberSaveable { mutableIntStateOf(studyMinutes * 60) }
    var isRunning by rememberSaveable { mutableStateOf(false) }

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

        if (remainingSeconds == 0) {
            if (phase == PomodoroPhase.STUDY) {
                if (currentSession >= sessionCount) {
                    phase = PomodoroPhase.COMPLETED
                    isRunning = false
                } else {
                    if (vibrator != null) {
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(
                                longArrayOf(0, 150, 130, 150, 130, 150),
                                -1
                            )
                        )
                        delay(710L)
                    }
                    phase = PomodoroPhase.BREAK
                    currentSession += 1
                    remainingSeconds = totalBreakSeconds
                }
            } else {
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

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = phaseLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

                Text(
                    text = stringResource(
                        R.string.pomodoro_time_format,
                        remainingSeconds / 60,
                        remainingSeconds % 60
                    ),
                    style = MaterialTheme.typography.displayMedium,
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
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    Button(
                        onClick = {
                            if (phase != PomodoroPhase.COMPLETED) {
                                isRunning = !isRunning
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(if (isRunning) R.string.pause else R.string.start))
                    }

                    Button(
                        onClick = { resetPomodoro() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.reset))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        Text(
            text = stringResource(R.string.configuration),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        OutlinedTextField(
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
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.study_duration)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        OutlinedTextField(
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
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.break_duration)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        OutlinedTextField(
            value = sessionCount.toString(),
            onValueChange = { newValue ->
                val parsed = newValue.toIntOrNull() ?: 1
                sessionCount = if (parsed > 0) parsed else 1
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.number_of_sessions)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}
