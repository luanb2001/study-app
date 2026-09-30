package com.example.myapplication.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R
import com.example.myapplication.ui.theme.spacing

@Composable
fun StartStudyScreen(
    modifier: Modifier = Modifier,
    onStartStudy: (
        subject: String,
        durationMinutes: Int,
        breakMinutes: Int,
        sessions: Int
    ) -> Unit = { _, _, _, _ -> },
    onBack: () -> Unit = {},
    initialSubject: String = ""
) {
    var subject by rememberSaveable {
        mutableStateOf(initialSubject)
    }

    var duration by rememberSaveable { mutableStateOf("25") }
    var breakDuration by rememberSaveable { mutableStateOf("5") }
    var sessionCount by rememberSaveable { mutableStateOf("4") }
    val durationMinutes = duration.toIntOrNull()
    val breakDurationMinutes = breakDuration.toIntOrNull()
    val sessions = sessionCount.toIntOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.large),
        verticalArrangement = Arrangement.Top
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }

            Text(
                text = stringResource(R.string.start_study_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        Text(
            text = stringResource(R.string.start_study_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
                Text(
                    text = stringResource(R.string.study_prompt),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.subject_example)) },
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

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
                    PomodoroSettingField(
                        label = stringResource(R.string.study_duration_short),
                        value = duration,
                        onValueChange = { duration = it },
                        modifier = Modifier.weight(1f)
                    )
                    PomodoroSettingField(
                        label = stringResource(R.string.break_duration_short),
                        value = breakDuration,
                        onValueChange = { breakDuration = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                PomodoroSettingField(
                    label = stringResource(R.string.number_of_sessions),
                    value = sessionCount,
                    onValueChange = { sessionCount = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        Button(
            onClick = {
                val parsedDuration = durationMinutes
                val parsedBreakDuration = breakDurationMinutes
                val parsedSessions = sessions
                if (
                    parsedDuration != null && parsedDuration > 0 &&
                    parsedBreakDuration != null && parsedBreakDuration > 0 &&
                    parsedSessions != null && parsedSessions > 0 &&
                    subject.isNotBlank()
                ) {
                    onStartStudy(
                        subject.trim(),
                        parsedDuration,
                        parsedBreakDuration,
                        parsedSessions
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = subject.isNotBlank() &&
                durationMinutes != null && durationMinutes > 0 &&
                breakDurationMinutes != null && breakDurationMinutes > 0 &&
                sessions != null && sessions > 0
        ) {
            Text(stringResource(R.string.start_study))
        }
    }
}

@Composable
private fun PomodoroSettingField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            if (newValue.all(Char::isDigit)) {
                onValueChange(newValue)
            }
        },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}
