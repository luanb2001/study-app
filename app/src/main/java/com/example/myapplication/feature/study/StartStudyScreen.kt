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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R
import com.example.myapplication.ui.components.NumberInputField
import com.example.myapplication.ui.theme.spacing

private data class StudyConfiguration(
    val subject: String,
    val studyMinutes: Int,
    val breakMinutes: Int,
    val sessions: Int
)

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
    initialSubject: String = "",
    canStartStudy: Boolean = true
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
    val configuration = when {
        subject.isBlank() -> null
        durationMinutes == null || durationMinutes !in 1..MAX_TIMER_MINUTES -> null
        breakDurationMinutes == null || breakDurationMinutes !in 1..MAX_TIMER_MINUTES -> null
        sessions == null || sessions <= 0 -> null
        else -> StudyConfiguration(
            subject = subject.trim(),
            studyMinutes = durationMinutes,
            breakMinutes = breakDurationMinutes,
            sessions = sessions
        )
    }

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
                    NumberInputField(
                        label = stringResource(R.string.study_duration_short),
                        value = duration,
                        onValueChange = { duration = it },
                        modifier = Modifier.weight(1f)
                    )
                    NumberInputField(
                        label = stringResource(R.string.break_duration_short),
                        value = breakDuration,
                        onValueChange = { breakDuration = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                NumberInputField(
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
                configuration?.let {
                    onStartStudy(
                        it.subject,
                        it.studyMinutes,
                        it.breakMinutes,
                        it.sessions
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = configuration != null && canStartStudy
        ) {
            Text(stringResource(R.string.start_study))
        }
    }
}
