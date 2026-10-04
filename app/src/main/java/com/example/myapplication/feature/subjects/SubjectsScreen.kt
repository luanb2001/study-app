package com.example.myapplication.feature.subjects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.feature.study.StudyEntry
import com.example.myapplication.feature.study.StudyRepository
import com.example.myapplication.feature.pomodoro.PomodoroPhase
import com.example.myapplication.feature.pomodoro.PomodoroSessionStore
import com.example.myapplication.ui.components.EmptyState
import com.example.myapplication.ui.theme.AttentionAmber
import com.example.myapplication.ui.theme.SuccessGreen
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

private enum class SubjectFilter {
    ALL,
    ON_TRACK,
    DUE,
    OVERDUE
}

private data class SubjectListItem(
    val key: String,
    val periodKey: String,
    val periodTitle: String,
    val subject: String,
    val sessionCount: Int,
    val totalMinutes: Int,
    val isInProgress: Boolean = false,
    val isRunning: Boolean = false
)

@Composable
fun SubjectsScreen(
    studyRepository: StudyRepository,
    modifier: Modifier = Modifier,
    onOpenSubject: (String) -> Unit,
    onStartStudy: () -> Unit,
    onRegisterStudy: () -> Unit,
    onScheduleStudy: () -> Unit,
    canStartStudy: Boolean = true
) {
    var groupingName by rememberSaveable { mutableStateOf(StudyGrouping.DAY.name) }
    val grouping = StudyGrouping.valueOf(groupingName)
    var subjectFilterName by rememberSaveable { mutableStateOf(SubjectFilter.ALL.name) }
    val subjectFilter = SubjectFilter.valueOf(subjectFilterName)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var loadedCount by rememberSaveable { mutableIntStateOf(PAGE_SIZE) }
    val listState = rememberLazyListState()
    val locale = Locale.forLanguageTag("pt-BR")
    val context = androidx.compose.ui.platform.LocalContext.current
    var activePomodoro by remember(context) {
        mutableStateOf(PomodoroSessionStore.load(context))
    }
    LaunchedEffect(context) {
        while (true) {
            activePomodoro = PomodoroSessionStore.load(context)
            kotlinx.coroutines.delay(1_000L)
        }
    }
    val activeSubject = activePomodoro
        ?.takeIf { it.phase != PomodoroPhase.COMPLETED }
        ?.subject
    val activeRunning = activePomodoro?.isRunning == true
    val allEntries = studyRepository.all()
    val reviewDates = studyRepository.reviewSchedules()
        .associateBy { it.subject.lowercase(Locale.ROOT) }
    val allItems = remember(allEntries, grouping, activeSubject, activeRunning) {
        val items = allEntries
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
            .toMutableList()
        activeSubject?.let { subject ->
            val todayPeriod = periodStart(LocalDate.now(), grouping)
            val periodKey = todayPeriod.toString()
            val existingIndex = items.indexOfFirst {
                it.periodKey == periodKey && it.subject.equals(subject, ignoreCase = true)
            }
            if (existingIndex >= 0) {
                items[existingIndex] = items[existingIndex].copy(
                    isInProgress = true,
                    isRunning = activeRunning
                )
            } else {
                items.add(
                    0,
                    SubjectListItem(
                        key = "${periodKey}_$subject",
                        periodKey = periodKey,
                        periodTitle = periodTitle(todayPeriod, grouping, locale),
                        subject = subject,
                        sessionCount = 0,
                        totalMinutes = 0,
                        isInProgress = true,
                        isRunning = activeRunning
                    )
                )
            }
        }
        items
    }
    val matchingItems = allItems.filter { item ->
        val schedule = reviewDates[item.subject.lowercase(Locale.ROOT)]
        val due = schedule != null && !schedule.dueDate.isAfter(LocalDate.now())
        val overdue = schedule != null && schedule.dueDate.isBefore(LocalDate.now())
        val matchesSearch = item.subject.contains(searchQuery.trim(), ignoreCase = true)
        val matchesFilter = when (subjectFilter) {
            SubjectFilter.ALL -> true
            SubjectFilter.ON_TRACK -> schedule != null && !due
            SubjectFilter.DUE -> due && !overdue
            SubjectFilter.OVERDUE -> overdue
        }
        matchesSearch && matchesFilter
    }
    val visibleItems = matchingItems.take(loadedCount)
    val visibleGroups = visibleItems.groupBy { it.periodKey }

    LaunchedEffect(groupingName, subjectFilterName, searchQuery) {
        loadedCount = PAGE_SIZE
        listState.scrollToItem(0)
    }

    LaunchedEffect(listState, loadedCount, matchingItems.size) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisibleIndex ->
            val totalItems = listState.layoutInfo.totalItemsCount
            if (
                lastVisibleIndex != null &&
                lastVisibleIndex >= totalItems - 2 &&
                loadedCount < matchingItems.size
            ) {
                loadedCount = (loadedCount + PAGE_SIZE).coerceAtMost(matchingItems.size)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.large),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.nav_subjects),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = onRegisterStudy,
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.register_study),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.small),
                placeholder = { Text(stringResource(R.string.subject_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                listOf(
                    SubjectFilter.ALL to R.string.subject_filter_all,
                    SubjectFilter.ON_TRACK to R.string.subject_filter_on_track,
                    SubjectFilter.DUE to R.string.subject_filter_due,
                    SubjectFilter.OVERDUE to R.string.subject_filter_overdue
                ).forEach { (option, label) ->
                    FilterChip(
                        selected = subjectFilter == option,
                        onClick = { subjectFilterName = option.name },
                        label = { Text(stringResource(label)) }
                    )
                }
            }
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
                        isInProgress = subjectItem.isInProgress,
                        isRunning = subjectItem.isRunning,
                        statusColor = when {
                            subjectItem.isInProgress -> SuccessGreen
                            reviewDates[subjectItem.subject.lowercase(Locale.ROOT)]
                                ?.dueDate?.isBefore(LocalDate.now()) == true ->
                                MaterialTheme.colorScheme.tertiary
                            reviewDates[subjectItem.subject.lowercase(Locale.ROOT)]
                                ?.dueDate == LocalDate.now() ->
                                AttentionAmber
                            reviewDates[subjectItem.subject.lowercase(Locale.ROOT)]
                                ?.dueDate?.isAfter(LocalDate.now()) == true ->
                                SuccessGreen
                            else -> MaterialTheme.colorScheme.primary
                        },
                        onClick = { onOpenSubject(subjectItem.subject) }
                    )
                }
            }

            if (loadedCount < matchingItems.size) {
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
    isInProgress: Boolean,
    isRunning: Boolean,
    statusColor: Color,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.shapes.small
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = subject.take(1).uppercase(Locale.ROOT),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
                Column {
                    Text(text = subject,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    if (isInProgress) {
                        Text(
                            text = stringResource(
                                if (isRunning) R.string.study_in_progress else R.string.study_paused
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isRunning) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = pluralStringResource(
                                R.plurals.subject_session_count,
                                sessionCount,
                                sessionCount
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (!isInProgress || totalMinutes > 0) {
                Text(
                    text = stringResource(R.string.calendar_study_duration, totalMinutes),
                    modifier = Modifier.padding(start = MaterialTheme.spacing.small),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
            Box(
                Modifier
                    .size(9.dp)
                    .background(statusColor, CircleShape)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.open_subject, subject),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = MaterialTheme.spacing.extraSmall)
                    .size(24.dp)
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
