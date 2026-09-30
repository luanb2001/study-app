package com.example.myapplication.feature.study

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.myapplication.R
import com.example.myapplication.ui.theme.spacing
import java.time.LocalDate

@Composable
fun RegisterStudiedStudyScreen(
    initialSubject: String = "",
    onBack: () -> Unit,
    onRegisterStudy: (StudyEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var subject by rememberSaveable { mutableStateOf(initialSubject) }
    var summary by rememberSaveable { mutableStateOf("") }
    var sessionsText by rememberSaveable { mutableStateOf("1") }
    var durationText by rememberSaveable { mutableStateOf("") }
    val sessions = sessionsText.toIntOrNull()
    val duration = durationText.toIntOrNull()
    val canRegister = subject.isNotBlank() &&
        summary.isNotBlank() &&
        sessions != null && sessions > 0 &&
        duration != null && duration > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.large),
        verticalArrangement = Arrangement.Top
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
            Text(
                text = stringResource(R.string.register_studied_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.study_prompt)) },
            placeholder = { Text(stringResource(R.string.subject_example)) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        OutlinedTextField(
            value = summary,
            onValueChange = { summary = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.study_summary)) },
            placeholder = { Text(stringResource(R.string.register_study_summary_hint)) },
            minLines = 3
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        OutlinedTextField(
            value = sessionsText,
            onValueChange = { if (it.all(Char::isDigit)) sessionsText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.number_of_sessions)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        OutlinedTextField(
            value = durationText,
            onValueChange = { if (it.all(Char::isDigit)) durationText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.total_study_duration)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        Button(
            onClick = {
                val parsedSessions = sessions
                val parsedDuration = duration
                if (
                    subject.isNotBlank() &&
                    summary.isNotBlank() &&
                    parsedSessions != null && parsedSessions > 0 &&
                    parsedDuration != null && parsedDuration > 0
                ) {
                    onRegisterStudy(
                        StudyEntry(
                            date = LocalDate.now(),
                            subject = subject.trim(),
                            description = summary.trim(),
                            durationMinutes = parsedDuration,
                            sessionCount = parsedSessions
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = canRegister
        ) {
            Text(stringResource(R.string.register_study))
        }
    }
}
