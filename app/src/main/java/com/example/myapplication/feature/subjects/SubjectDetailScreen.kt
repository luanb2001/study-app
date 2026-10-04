package com.example.myapplication.feature.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.feature.study.ReviewSchedule
import com.example.myapplication.feature.study.StudyEntry
import com.example.myapplication.feature.study.StudyRepository
import com.example.myapplication.ui.components.ConfirmActionDialog
import com.example.myapplication.ui.components.EmptyState
import com.example.myapplication.ui.theme.spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SubjectDetailScreen(
    subject: String,
    studyRepository: StudyRepository,
    onBack: () -> Unit,
    onStartStudy: () -> Unit,
    onRegisterStudy: () -> Unit,
    onScheduleStudy: () -> Unit,
    onDeleteStudy: (StudyEntry) -> Unit,
    modifier: Modifier = Modifier,
    canStartStudy: Boolean = true
) {
    var studyPendingDeletion by remember { mutableStateOf<StudyEntry?>(null) }
    var selectedTab by remember { mutableIntStateOf(1) }
    val entries = studyRepository.forSubject(subject).sortedByDescending { it.date }
    val review = studyRepository.reviewSchedules()
        .firstOrNull { it.subject.equals(subject, ignoreCase = true) }
    val locale = Locale.forLanguageTag("pt-BR")

    studyPendingDeletion?.let { entry ->
        ConfirmActionDialog(
            titleResource = R.string.delete_study_title,
            messageResource = R.string.delete_study_confirmation,
            confirmResource = R.string.delete,
            onConfirm = {
                onDeleteStudy(entry)
                studyPendingDeletion = null
            },
            onDismiss = { studyPendingDeletion = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.large)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.subject_detail_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        TabRow(selectedTabIndex = selectedTab) {
            listOf(
                R.string.subject_tab_overview,
                R.string.subject_tab_history,
                R.string.subject_tab_reviews
            ).forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(stringResource(label), maxLines = 1) }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = MaterialTheme.spacing.medium)
        ) {
            when (selectedTab) {
                0 -> SubjectOverview(subject, entries, review)
                1 -> {
                    Text(
                        text = stringResource(R.string.subject_history_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                    if (entries.isEmpty()) {
                        EmptyState(R.string.subject_history_empty)
                    } else {
                        entries.forEach { entry ->
                            StudyHistoryRow(
                                subject = subject,
                                entry = entry,
                                locale = locale,
                                onDelete = { studyPendingDeletion = entry }
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                        }
                    }
                }
                else -> ReviewCard(review)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            OutlinedButton(
                onClick = onRegisterStudy,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    stringResource(R.string.register_study_short),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onStartStudy,
                enabled = canStartStudy,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    stringResource(R.string.start_study),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        OutlinedButton(
            onClick = onScheduleStudy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.spacing.small)
        ) {
            Text(stringResource(R.string.schedule_study))
        }
    }
}

@Composable
private fun SubjectOverview(
    subject: String,
    entries: List<StudyEntry>,
    review: ReviewSchedule?
) {
    val totalMinutes = entries.sumOf { it.durationMinutes }
    val totalSessions = entries.sumOf { it.sessionCount }
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("pt-BR"))

    Text(
        text = stringResource(R.string.subject_overview_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        SubjectStat(
            value = totalSessions.toString(),
            label = stringResource(R.string.subject_stat_sessions),
            modifier = Modifier.weight(1f)
        )
        SubjectStat(
            value = stringResource(R.string.calendar_study_duration, totalMinutes),
            label = stringResource(R.string.subject_stat_study_time),
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
            Text(
                text = stringResource(R.string.subject_description_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = entries.firstOrNull()?.description
                    ?.takeIf(String::isNotBlank)
                    ?: stringResource(R.string.subject_no_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    ReviewCard(review)
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    Text(
        text = stringResource(
            R.string.subject_recent_activity,
            subject,
            entries.firstOrNull()?.date?.format(dateFormatter)
                ?: stringResource(R.string.subject_no_activity)
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SubjectStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(MaterialTheme.spacing.medium)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReviewCard(review: ReviewSchedule?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
            Text(
                text = stringResource(R.string.subject_next_review),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            Text(
                text = review?.let {
                    it.dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")))
                } ?: stringResource(R.string.subject_no_review),
                style = MaterialTheme.typography.bodyMedium,
                color = if (review?.dueDate?.isBefore(LocalDate.now()) == true) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun StudyHistoryRow(
    subject: String,
    entry: StudyEntry,
    locale: Locale,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(MaterialTheme.spacing.medium)
            ) {
                Text(
                    text = entry.date.format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", locale)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.study_entry_sessions_and_duration,
                        entry.sessionCount,
                        entry.sessionCount,
                        entry.durationMinutes
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.description.isNotBlank()) {
                    Text(
                        text = entry.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(
                        R.string.delete_study_accessibility,
                        subject
                    ),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
