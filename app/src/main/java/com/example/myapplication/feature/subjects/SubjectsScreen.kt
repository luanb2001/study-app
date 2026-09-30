package com.example.myapplication.feature.subjects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.feature.study.StudyEntry
import com.example.myapplication.feature.study.StudyRepository
import com.example.myapplication.ui.components.EmptyState
import com.example.myapplication.ui.theme.spacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.flow.collect

private const val PAGE_SIZE = 20

private enum class StudyGrouping {
    DAY,
    WEEK,
    MONTH
}

private data class SubjectListItem(
    val key: String,
    val periodKey: String,
    val periodTitle: String,
    val subject: String,
    val sessionCount: Int,
    val totalMinutes: Int
)

@Composable
fun SubjectsScreen(
    studyRepository: StudyRepository,
    modifier: Modifier = Modifier,
    onOpenSubject: (String) -> Unit,
    onStartStudy: () -> Unit,
    onRegisterStudy: () -> Unit,
    onScheduleStudy: () -> Unit
) {
    var groupingName by rememberSaveable { mutableStateOf(StudyGrouping.DAY.name) }
    val grouping = StudyGrouping.valueOf(groupingName)
    var loadedCount by rememberSaveable { mutableIntStateOf(PAGE_SIZE) }
    val listState = rememberLazyListState()
    val locale = Locale.forLanguageTag("pt-BR")
    val allEntries = studyRepository.all()
    val allItems = remember(allEntries, grouping) {
        allEntries
            .groupBy { periodStart(it.date, grouping) }
            .toSortedMap(compareByDescending { it })
            .flatMap { (periodStart, periodEntries) ->
                periodEntries
                    .groupBy { it.subject }
                    .toSortedMap()
                    .map { (subject, subjectEntries) ->
                        SubjectListItem(
                            key = "${periodStart}_$subject",
                            periodKey = periodStart.toString(),
                            periodTitle = periodTitle(periodStart, grouping, locale),
                            subject = subject,
                            sessionCount = subjectEntries.sumOf { it.sessionCount },
                            totalMinutes = subjectEntries.sumOf { it.durationMinutes }
                        )
                    }
            }
    }
    val visibleItems = allItems.take(loadedCount)
    val visibleGroups = visibleItems.groupBy { it.periodKey }

    LaunchedEffect(groupingName) {
        loadedCount = PAGE_SIZE
        listState.scrollToItem(0)
    }

    LaunchedEffect(listState, loadedCount, allItems.size) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisibleIndex ->
            val totalItems = listState.layoutInfo.totalItemsCount
            if (
                lastVisibleIndex != null &&
                lastVisibleIndex >= totalItems - 2 &&
                loadedCount < allItems.size
            ) {
                loadedCount = (loadedCount + PAGE_SIZE).coerceAtMost(allItems.size)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        item {
            Text(
                text = stringResource(R.string.nav_subjects),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = MaterialTheme.spacing.large)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.medium),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(MaterialTheme.spacing.large)
                ) {
                    Text(
                        text = stringResource(R.string.subjects_study_actions_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                    Text(
                        text = stringResource(R.string.subjects_study_actions_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    Button(
                        onClick = onStartStudy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                        Text(stringResource(R.string.start_study))
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        OutlinedButton(
                            onClick = onRegisterStudy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                            Text(stringResource(R.string.register_study_short), maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onScheduleStudy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                            Text(stringResource(R.string.schedule_study_short), maxLines = 1)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.subjects_history_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = MaterialTheme.spacing.large)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    StudyGrouping.DAY to R.string.group_days,
                    StudyGrouping.WEEK to R.string.group_weeks,
                    StudyGrouping.MONTH to R.string.group_months
                ).forEach { (option, label) ->
                    FilterChip(
                        selected = grouping == option,
                        onClick = { groupingName = option.name },
                        label = { Text(stringResource(label)) }
                    )
                }
            }
        }

        if (visibleItems.isEmpty()) {
            item {
                EmptyState(R.string.subjects_empty)
            }
        } else {
            visibleGroups.forEach { (periodKey, items) ->
                item(key = "header_$periodKey") {
                    Text(
                        text = items.first().periodTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(
                            top = MaterialTheme.spacing.medium,
                            bottom = MaterialTheme.spacing.small
                        )
                    )
                }
                items(items, key = { it.key }) { subjectItem ->
                    SubjectRow(
                        subject = subjectItem.subject,
                        sessionCount = subjectItem.sessionCount,
                        totalMinutes = subjectItem.totalMinutes,
                        onClick = { onOpenSubject(subjectItem.subject) }
                    )
                }
            }

            if (loadedCount < allItems.size) {
                item(key = "loading_more") {
                    Text(
                        text = stringResource(R.string.subjects_scroll_for_more),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = MaterialTheme.spacing.medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        }
    }
}

@Composable
private fun SubjectRow(
    subject: String,
    sessionCount: Int,
    totalMinutes: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.large),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.subject_session_count, sessionCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.calendar_study_duration, totalMinutes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun periodStart(date: LocalDate, grouping: StudyGrouping): LocalDate =
    when (grouping) {
        StudyGrouping.DAY -> date
        StudyGrouping.WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        StudyGrouping.MONTH -> date.withDayOfMonth(1)
    }

private fun periodTitle(date: LocalDate, grouping: StudyGrouping, locale: Locale): String =
    when (grouping) {
        StudyGrouping.DAY -> when (date) {
            LocalDate.now() -> "Hoje"
            LocalDate.now().minusDays(1) -> "Ontem"
            else -> date.format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", locale))
                .replaceFirstChar { it.titlecase(locale) }
        }
        StudyGrouping.WEEK -> {
            val end = date.plusDays(6)
            "${date.format(DateTimeFormatter.ofPattern("dd/MM", locale))} – " +
                end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", locale))
        }
        StudyGrouping.MONTH -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
            .replaceFirstChar { it.titlecase(locale) }
    }
