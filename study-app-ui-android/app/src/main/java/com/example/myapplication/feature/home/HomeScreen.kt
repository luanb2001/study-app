package com.example.myapplication.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.myapplication.ui.theme.AttentionAmber
import com.example.myapplication.ui.theme.SuccessGreen
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
    val isInProgress: Boolean = false
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    studyRepository: StudyRepository,
    onStartStudy: () -> Unit = {},
    onScheduleStudy: () -> Unit = {},
    onStartReview: (String) -> Unit = {},
    onStartScheduledStudy: (ScheduledStudy) -> Unit = {},
    onContinuePomodoro: (PomodoroSessionState) -> Unit = {},
    canStartStudy: Boolean = true,
    onSeeProgress: () -> Unit = {}
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
        Column(modifier = Modifier.padding(top = MaterialTheme.spacing.medium)) {
            Text(
                text = stringResource(R.string.home_greeting),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        activePomodoro?.let { session ->
            ActiveSessionHero(
                session = session,
                onContinue = { onContinuePomodoro(session) }
            )
        } ?: StartStudyHero(
            onStartStudy = onStartStudy,
            canStartStudy = canStartStudy && activePomodoro == null
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        val reviews = studyRepository.dueReviews()
        val allScheduledStudies = studyRepository.scheduled()
        val upcomingStudies = upcomingScheduledStudies(allScheduledStudies, activePomodoro)
        HomePlan(
            reviews = reviews,
            scheduledStudies = upcomingStudies,
            activePomodoro = activePomodoro,
            onStartReview = onStartReview,
            onStartScheduledStudy = onStartScheduledStudy,
            onContinuePomodoro = onContinuePomodoro,
            canStartStudy = canStartStudy && activePomodoro == null
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))
        ProgressSummary(
            summary = studyRepository.progressSummary(),
            onSeeProgress = onSeeProgress
        )
        androidx.compose.material3.OutlinedButton(
            onClick = onScheduleStudy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.spacing.medium)
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null)
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
            Text(stringResource(R.string.schedule_study))
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
    }
}

private fun upcomingScheduledStudies(
    scheduledStudies: List<ScheduledStudy>,
    activePomodoro: PomodoroSessionState?,
    today: LocalDate = LocalDate.now()
): List<ScheduledStudy> {
    val activeScheduledStudyId = activePomodoro
        ?.takeIf {
            it.phase != PomodoroPhase.COMPLETED && it.scheduledStudyId.isNotEmpty()
        }
        ?.scheduledStudyId
    val activeScheduledStudyExists =
        scheduledStudies.any { it.id == activeScheduledStudyId }

    return scheduledStudies
        .filter { study ->
            val isActiveStudy = study.id == activeScheduledStudyId
            val duplicatesUnmatchedActiveSession =
                activeScheduledStudyId != null &&
                    !activeScheduledStudyExists &&
                    study.subject.equals(activePomodoro?.subject, ignoreCase = true)
            (!study.date.isBefore(today) || isActiveStudy) &&
                !duplicatesUnmatchedActiveSession
        }
        .sortedWith(compareBy<ScheduledStudy> { it.date }.thenBy { it.subject })
}

@Composable
private fun StartStudyHero(
    onStartStudy: () -> Unit,
    canStartStudy: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                    )
                ),
                MaterialTheme.shapes.large
            )
            .padding(MaterialTheme.spacing.large)
    ) {
        Text(
            text = stringResource(R.string.home_start_prompt),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        Button(
            onClick = onStartStudy,
            enabled = canStartStudy,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
            Text(stringResource(R.string.start_free_study))
        }
    }
}

@Composable
private fun buildHomePlanEntries(
    reviews: List<ReviewSchedule>,
    scheduledStudies: List<ScheduledStudy>,
    activePomodoro: PomodoroSessionState?,
    activeScheduledStudy: ScheduledStudy?,
    hasActiveStudyInPlan: Boolean,
    dateFormatter: DateTimeFormatter
): List<HomePlanEntry> {
    val activeSession = activePomodoro?.takeIf { it.phase != PomodoroPhase.COMPLETED }
    val today = LocalDate.now()
    return buildList {
        activeSession
            ?.takeIf { !hasActiveStudyInPlan }
            ?.let { session ->
                add(
                    HomePlanEntry(
                        key = "active:${session.subject}",
                        title = session.subject,
                        detail = sessionInProgressLabel(session),
                        isOverdue = false,
                        reviewSubject = session.subject,
                        isInProgress = true
                    )
                )
            }

        reviews.forEach { review ->
            val reviewSession = activeSession?.takeIf { session ->
                session.isReview && session.subject.equals(review.subject, ignoreCase = true)
            }
            add(
                HomePlanEntry(
                    key = "review:${review.subject}",
                    title = review.subject,
                    detail = if (reviewSession != null) {
                        sessionInProgressLabel(reviewSession)
                    } else {
                        stringResource(

                            if (review.dueDate.isBefore(today)) {
                                R.string.overdue
                            } else {
                                R.string.review_today
                            }

                        )
                    },
                    isOverdue = reviewSession == null && review.dueDate.isBefore(today),
                    reviewSubject = review.subject,
                    isInProgress = reviewSession != null
                )
            )
        }

        scheduledStudies.forEach { study ->
            val scheduledSession = activeSession?.takeIf {
                activeScheduledStudy?.id == study.id
            }
            add(
                HomePlanEntry(
                    key = "scheduled:${study.id}",
                    title = study.subject,
                    detail = if (scheduledSession != null) {
                        sessionInProgressLabel(scheduledSession)
                    } else {
                        stringResource(
                            R.string.scheduled_study_date_subject,
                            study.date.format(dateFormatter),
                            pluralStringResource(
                                R.plurals.scheduled_study_settings,
                                study.sessionCount,
                                study.sessionCount,
                                study.studyMinutes,
                                study.breakMinutes
                            )
                        )
                    },
                    isOverdue = false,
                    scheduledStudy = study,
                    isInProgress = scheduledSession != null
                )
            )
        }

    }
}

@Composable
private fun sessionInProgressLabel(session: PomodoroSessionState): String =
    stringResource(
        R.string.home_session_in_progress,
        session.currentSession,
        session.sessionCount
    )

@Composable
private fun ActiveSessionHero(
    session: PomodoroSessionState,
    onContinue: () -> Unit
) {
    val remainingSeconds = session.currentRemainingSeconds()
    val duration = (if (session.phase == PomodoroPhase.BREAK) {
        session.breakMinutes
    } else {
        session.studyMinutes
    }) * 60
    val progress = (duration - remainingSeconds).coerceIn(0, duration).toFloat() / duration
    val trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
    val foreground = SuccessGreen
    val label = when (session.phase) {
        PomodoroPhase.STUDY -> stringResource(R.string.pomodoro_study)
        PomodoroPhase.BREAK -> stringResource(R.string.pomodoro_break)
        PomodoroPhase.COMPLETED -> stringResource(R.string.pomodoro_completed)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                    )
                ),
                androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
            )
            .clickable(onClick = onContinue)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.active_session_title),
            style = MaterialTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.82f)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(78.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(color = trackColor, style = Stroke(width = 6.dp.toPx()))
                    drawArc(
                        color = foreground,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Icon(
                    imageVector = if (session.phase == PomodoroPhase.COMPLETED) {
                        Icons.Default.CheckCircle
                    } else if (session.isRunning) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.subject,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = if (session.phase == PomodoroPhase.COMPLETED) {
                        stringResource(R.string.pomodoro_finish_pending)
                    } else {
                        "$label • ${session.currentSession}/${session.sessionCount}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
            Text(
                text = if (session.phase == PomodoroPhase.COMPLETED) {
                    stringResource(R.string.finish_study)
                } else {
                    stringResource(
                        R.string.pomodoro_time_format,
                        remainingSeconds / 60,
                        remainingSeconds % 60
                    )
                },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun HomePlan(
    reviews: List<ReviewSchedule>,
    scheduledStudies: List<ScheduledStudy>,
    activePomodoro: PomodoroSessionState?,
    onStartReview: (String) -> Unit,
    onStartScheduledStudy: (ScheduledStudy) -> Unit,
    onContinuePomodoro: (PomodoroSessionState) -> Unit,
    canStartStudy: Boolean
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("pt-BR"))
    }
    val activeReviewInPlan = activePomodoro?.takeIf { it.isReview }?.let { session ->
        reviews.any { it.subject.equals(session.subject, ignoreCase = true) }
    } == true
    val activeScheduledStudyId = activePomodoro
        ?.takeIf {
            it.phase != PomodoroPhase.COMPLETED && it.scheduledStudyId.isNotEmpty()
        }
        ?.scheduledStudyId
    val activeScheduledStudy = scheduledStudies.firstOrNull {
        it.id == activeScheduledStudyId
    }
    val activeScheduledStudyInPlan = activeScheduledStudy != null
    val hasActiveStudyInPlan = activeReviewInPlan || activeScheduledStudyInPlan
    val entries = buildHomePlanEntries(
        reviews = reviews,
        scheduledStudies = scheduledStudies,
        activePomodoro = activePomodoro,
        activeScheduledStudy = activeScheduledStudy,
        hasActiveStudyInPlan = hasActiveStudyInPlan,
        dateFormatter = dateFormatter
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.today_reviews),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.plan_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {

                entries.forEach { entry ->
                    val entryCanBeClicked = if (activePomodoro != null) {
                        entry.isInProgress
                    } else {
                        canStartStudy
                    }
                    PlanEntryRow(
                        entry = entry,
                        enabled = entryCanBeClicked,
                        onClick = {
                            when {
                                entry.isInProgress && activePomodoro != null ->
                                    onContinuePomodoro(activePomodoro)
                                entry.reviewSubject != null ->
                                    onStartReview(entry.reviewSubject)
                                entry.scheduledStudy != null ->
                                    onStartScheduledStudy(entry.scheduledStudy)
                            }
                        }
                    )
                }

            }

        }
    }
}

@Composable
private fun PlanEntryRow(
    entry: HomePlanEntry,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .alpha(if (enabled) 1f else 0.48f)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val statusColor = if (entry.isOverdue) {
            MaterialTheme.colorScheme.tertiary
        } else if (entry.isInProgress) {
            SuccessGreen
        } else if (entry.scheduledStudy != null) {
            MaterialTheme.colorScheme.secondary
        } else if (entry.reviewSubject != null) {
            AttentionAmber
        } else {
            SuccessGreen
        }
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = MaterialTheme.shapes.medium
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (entry.isInProgress) {
                    if (entry.reviewSubject != null) Icons.Default.Pause else Icons.Default.PlayArrow
                } else if (entry.scheduledStudy != null) {
                    Icons.Default.CalendarMonth
                } else {
                    Icons.Default.Replay
                },
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(23.dp)
            )
        }
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
                color = when {
                    entry.isOverdue -> statusColor
                    entry.isInProgress -> statusColor
                    entry.reviewSubject != null -> AttentionAmber
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.open_subject, entry.title),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = MaterialTheme.spacing.small)
                .size(24.dp)
        )
    }
}

@Composable
private fun ProgressSummary(
    summary: StudyProgressSummary,
    onSeeProgress: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_progress_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.view_details),
                modifier = Modifier.clickable(onClick = onSeeProgress),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
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
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(vertical = 14.dp, horizontal = 6.dp),
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
