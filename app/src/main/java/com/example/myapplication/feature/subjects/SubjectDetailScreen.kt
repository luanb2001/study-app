package com.example.myapplication.feature.subjects

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.myapplication.R
import com.example.myapplication.feature.study.StudyRepository
import com.example.myapplication.ui.theme.spacing
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SubjectDetailScreen(
    subject: String,
    onBack: () -> Unit,
    onStartStudy: () -> Unit,
    onRegisterStudy: () -> Unit,
    onScheduleStudy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entries = StudyRepository.forSubject(subject).sortedByDescending { it.date }
    val locale = Locale.forLanguageTag("pt-BR")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.large)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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

        Text(
            text = stringResource(R.string.subject_description_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        entries.forEach { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
                    Text(
                        text = entry.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                    Text(
                        text = entry.description.ifBlank {
                            stringResource(R.string.subject_no_description)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                    Text(
                        text = stringResource(
                            R.string.study_entry_sessions_and_duration,
                            entry.sessionCount,
                            entry.durationMinutes
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        Button(
            onClick = onStartStudy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.start_study))
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Button(
            onClick = onRegisterStudy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.register_studied_subject))
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Button(
            onClick = onScheduleStudy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.schedule_study))
        }
    }
}
