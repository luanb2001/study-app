package com.example.myapplication.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.feature.study.ReviewSchedule
import com.example.myapplication.feature.study.ScheduledStudy
import com.example.myapplication.feature.study.StudyRepository
import com.example.myapplication.feature.study.StudyProgressSummary
import com.example.myapplication.feature.pomodoro.PomodoroPhase
import com.example.myapplication.feature.pomodoro.PomodoroSessionState
import com.example.myapplication.feature.pomodoro.PomodoroSessionStore
import com.example.myapplication.ui.theme.spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private data class HomePlanEntry(
    val key: String,
    val title: String,
    val detail: String,
    val isOverdue: Boolean,
    val reviewSubject: String? = null,
    val scheduledStudy: ScheduledStudy? = null,
    val pomodoroSession: PomodoroSessionState? = null
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    studyRepository: StudyRepository,
    onStartStudy: () -> Unit = {},
    onScheduleStudy: () -> Unit = {},
    onStartPomodoro: (String) -> Unit = {},
    onStartScheduledStudy: (ScheduledStudy) -> Unit = {},
    onContinuePomodoro: (PomodoroSessionState) -> Unit = {},
    canStartStudy: Boolean = true
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var activePomodoro by remember {
        mutableStateOf(PomodoroSessionStore.load(context))
    }
    LaunchedEffect(context) {
        while (true) {
            activePomodoro = PomodoroSessionStore.load(context)
            delay(1_000L)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.large),
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        Text(
            text = stringResource(R.string.home_greeting),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

        val reviews = studyRepository.dueReviews()
        val upcomingStudies = studyRepository.scheduled()
            .filter {
                !it.date.isBefore(LocalDate.now()) &&
                    it.id != activePomodoro?.scheduledStudyId
            }
            .sortedWith(compareBy<ScheduledStudy> { it.date }.thenBy { it.subject })
        HomePlan(
            reviews = reviews,
            scheduledStudies = upcomingStudies,
            activePomodoro = activePomodoro,
            onStartPomodoro = onStartPomodoro,
            onStartScheduledStudy = onStartScheduledStudy,
            onContinuePomodoro = onContinuePomodoro
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

        StartStudyActions(
            onStartStudy = onStartStudy,
            onScheduleStudy = onScheduleStudy,
            canStartStudy = canStartStudy && activePomodoro == null
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

        ProgressSummary(summary = studyRepository.progressSummary())

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
    }
}

@Composable
private fun HomePlan(
    reviews: List<ReviewSchedule>,
    scheduledStudies: List<ScheduledStudy>,
    activePomodoro: PomodoroSessionState?,
    onStartPomodoro: (String) -> Unit,
    onStartScheduledStudy: (ScheduledStudy) -> Unit,
    onContinuePomodoro: (PomodoroSessionState) -> Unit
) {
    var selectedEntryKey by rememberSaveable { mutableStateOf<String?>(null) }
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("pt-BR"))
    }
    val entries = buildList {
        activePomodoro?.let { session ->
            add(
                HomePlanEntry(
                    key = "pomodoro",
                    title = session.subject,
                    detail = if (session.phase == PomodoroPhase.COMPLETED) {
                        stringResource(R.string.pomodoro_finish_pending)
                    } else {
                        stringResource(
                            R.string.pomodoro_in_progress,
                            stringResource(
                                if (session.isRunning) {
                                    R.string.pomodoro_active_label
                                } else {
                                    R.string.paused
                                }
                            ),
                            session.currentRemainingSeconds() / 60,
                            session.currentRemainingSeconds() % 60
                        )
                    },
                    isOverdue = false,
                    pomodoroSession = session
                )
            )
        }
        reviews.forEach { review ->
            add(
                HomePlanEntry(
                    key = "review:${review.subject}",
                    title = review.subject,
                    detail = stringResource(
                        if (review.dueDate.isBefore(LocalDate.now())) {
                            R.string.overdue
                        } else {
                            R.string.review_today
                        }
                    ),
                    isOverdue = review.dueDate.isBefore(LocalDate.now()),
                    reviewSubject = review.subject
                )
            )
        }
        scheduledStudies.forEach { study ->
            add(
                HomePlanEntry(
                    key = "scheduled:${study.id}",
                    title = study.subject,
                    detail = stringResource(
                        R.string.scheduled_study_date_subject,
                        study.date.format(dateFormatter),
                        pluralStringResource(
                            R.plurals.scheduled_study_settings,
                            study.sessionCount,
                            study.sessionCount,
                            study.studyMinutes,
                            study.breakMinutes
                        )
                    ),
                    isOverdue = false,
                    scheduledStudy = study
                )
            )
        }
    }
    val selectedEntry = entries.firstOrNull { it.pomodoroSession != null }
        ?: entries.firstOrNull { it.key == selectedEntryKey }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
            Text(
                text = stringResource(R.string.your_plan),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            if (entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.plan_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                entries.forEach { entry ->
                    if (entry.pomodoroSession != null) {
                        ActivePomodoroRow(
                            entry = entry,
                            session = entry.pomodoroSession,
                            selected = entry.key == selectedEntry?.key,
                            onClick = { selectedEntryKey = entry.key }
                        )
                    } else {
                        PlanEntryRow(
                            entry = entry,
                            selected = entry.key == selectedEntry?.key,
                            onClick = { selectedEntryKey = entry.key }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            Button(
                onClick = {
                    selectedEntry?.let { entry ->
                        when {
                            entry.pomodoroSession != null ->
                                onContinuePomodoro(entry.pomodoroSession)
                            entry.reviewSubject != null -> onStartPomodoro(entry.reviewSubject)
                            entry.scheduledStudy != null ->
                                onStartScheduledStudy(entry.scheduledStudy)
                        }
                    }
                },
                enabled = selectedEntry != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        if (selectedEntry?.pomodoroSession != null) {
                            if (selectedEntry.pomodoroSession.phase == PomodoroPhase.COMPLETED) {
                                R.string.finish_study
                            } else {
                                R.string.continue_study
                            }
                        } else {
                            R.string.start_study
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun ActivePomodoroRow(
    entry: HomePlanEntry,
    session: PomodoroSessionState,
    selected: Boolean,
    onClick: () -> Unit
) {
    val phaseLabel = stringResource(
        when (session.phase) {
            PomodoroPhase.STUDY -> R.string.pomodoro_study
            PomodoroPhase.BREAK -> R.string.pomodoro_break
            PomodoroPhase.COMPLETED -> R.string.pomodoro_completed
        }
    )
    val status = when {
        session.phase == PomodoroPhase.COMPLETED ->
            stringResource(R.string.pomodoro_finish_pending)
        !session.isRunning -> stringResource(R.string.paused)
        else -> stringResource(
            R.string.pomodoro_session_count,
            session.currentSession,
            session.sessionCount
        )
    }
    val remainingSeconds = session.currentRemainingSeconds()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = MaterialTheme.shapes.medium
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(MaterialTheme.spacing.large)
    ) {
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
        Text(
            text = if (session.phase == PomodoroPhase.COMPLETED) {
                status
            } else {
                "$phaseLabel • $status"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (session.phase != PomodoroPhase.COMPLETED) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = stringResource(
                    R.string.pomodoro_time_format,
                    remainingSeconds / 60,
                    remainingSeconds % 60
                ),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PlanEntryRow(
    entry: HomePlanEntry,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = MaterialTheme.shapes.medium
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    color = if (entry.isOverdue) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = entry.detail,
                style = MaterialTheme.typography.bodySmall,
                color = if (entry.isOverdue) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun StartStudyActions(
    onStartStudy: () -> Unit,
    onScheduleStudy: () -> Unit,
    canStartStudy: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
            Text(
                text = stringResource(R.string.start_study_question),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            Text(
                text = stringResource(R.string.start_study_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            Button(
                onClick = onStartStudy,
                enabled = canStartStudy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(stringResource(R.string.start_free_study))
            }
            OutlinedButton(
                onClick = onScheduleStudy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text(stringResource(R.string.schedule_study))
            }
        }
    }
}

@Composable
private fun ProgressSummary(summary: StudyProgressSummary) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.progress),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        if (summary.subjectCount == 0) {
            Text(
                text = stringResource(R.string.progress_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                ProgressItem(
                    value = summary.streakDays.toString(),
                    label = stringResource(R.string.streak_days),
                    modifier = Modifier.weight(1f)
                )
                ProgressItem(
                    value = stringResource(R.string.study_hours_value, summary.monthlyStudyHours),
                    label = stringResource(R.string.this_month),
                    modifier = Modifier.weight(1f)
                )
                ProgressItem(
                    value = summary.subjectCount.toString(),
                    label = stringResource(R.string.subject_count),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProgressItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = MaterialTheme.spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
