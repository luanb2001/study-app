package com.example.myapplication.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.feature.study.StudyEntry
import com.example.myapplication.feature.study.ScheduledStudy
import com.example.myapplication.feature.study.StudyRepository
import com.example.myapplication.ui.components.ConfirmActionDialog
import com.example.myapplication.ui.theme.spacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    studyRepository: StudyRepository,
    modifier: Modifier = Modifier,
    onScheduleStudy: () -> Unit = {},
    onDeleteStudy: (StudyEntry) -> Unit,
    onCancelScheduledStudy: (ScheduledStudy) -> Unit
) {
    val today = LocalDate.now()
    var displayedMonthValue by rememberSaveable {
        mutableStateOf(today.withDayOfMonth(1).toString())
    }
    val month = LocalDate.parse(displayedMonthValue)
    val currentMonth = today.withDayOfMonth(1)
    var selectedDateValue by rememberSaveable { mutableStateOf(today.toString()) }
    var studyPendingDeletion by remember { mutableStateOf<StudyEntry?>(null) }
    var schedulePendingCancellation by remember {
        mutableStateOf<ScheduledStudy?>(null)
    }
    val selectedDate = LocalDate.parse(selectedDateValue)
    val locale = Locale.forLanguageTag("pt-BR")
    val studyRecords = studyRepository.all().groupBy { it.date }
    val scheduledRecords = studyRepository.scheduled().groupBy { it.date }
    val weekdays = listOf(
        stringResource(R.string.calendar_weekday_monday),
        stringResource(R.string.calendar_weekday_tuesday),
        stringResource(R.string.calendar_weekday_wednesday),
        stringResource(R.string.calendar_weekday_thursday),
        stringResource(R.string.calendar_weekday_friday),
        stringResource(R.string.calendar_weekday_saturday),
        stringResource(R.string.calendar_weekday_sunday)
    )
    val changeMonth: (Long) -> Unit = { monthOffset ->
        val updatedMonth = month.plusMonths(monthOffset)
        displayedMonthValue = updatedMonth.toString()
        selectedDateValue = updatedMonth
            .withDayOfMonth(minOf(selectedDate.dayOfMonth, updatedMonth.lengthOfMonth()))
            .toString()
    }

    studyPendingDeletion?.let { study ->
        ConfirmActionDialog(
            titleResource = R.string.delete_study_title,
            messageResource = R.string.delete_study_confirmation,
            confirmResource = R.string.delete,
            onConfirm = {
                onDeleteStudy(study)
                studyPendingDeletion = null
            },
            onDismiss = { studyPendingDeletion = null }
        )
    }

    schedulePendingCancellation?.let { study ->
        ConfirmActionDialog(
            titleResource = R.string.cancel_scheduled_study_title,
            messageResource = R.string.cancel_scheduled_study_confirmation,
            confirmResource = R.string.cancel_schedule,
            onConfirm = {
                onCancelScheduledStudy(study)
                schedulePendingCancellation = null
            },
            onDismiss = { schedulePendingCancellation = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.large)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.calendar_study_entries),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedButton(onClick = onScheduleStudy) {
                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null)
                Text(stringResource(R.string.schedule_study_short))
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { changeMonth(-1) }) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = stringResource(R.string.calendar_previous_month)
                        )
                    }
                    Text(
                        text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
                            .replaceFirstChar { it.titlecase(locale) },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { changeMonth(1) }) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.calendar_next_month)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                Column(
                    modifier = Modifier.pointerInput(month, selectedDate) {
                        var horizontalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { horizontalDrag = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                horizontalDrag += dragAmount
                            },
                            onDragEnd = {
                                when {
                                    horizontalDrag > 80f -> changeMonth(-1)
                                    horizontalDrag < -80f -> changeMonth(1)
                                }
                            }
                        )
                    }
                ) {
                    AnimatedContent(
                        targetState = month,
                        transitionSpec = {
                            val enter = if (targetState.isAfter(initialState)) {
                                slideInHorizontally(tween(300)) { it } + fadeIn(tween(180))
                            } else {
                                slideInHorizontally(tween(300)) { -it } + fadeIn(tween(180))
                            }
                            val exit = if (targetState.isAfter(initialState)) {
                                slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(180))
                            } else {
                                slideOutHorizontally(tween(300)) { it } + fadeOut(tween(180))
                            }
                            ContentTransform(enter, exit, sizeTransform = null)
                        },
                        label = "calendar_month_grid"
                    ) { animatedMonth ->
                        val animatedLeadingDays =
                            animatedMonth.dayOfWeek.value - DayOfWeek.MONDAY.value
                        val animatedWeekCount =
                            (animatedLeadingDays + animatedMonth.lengthOfMonth() + 6) / 7
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                weekdays.forEach { weekday ->
                                    Text(
                                        text = weekday,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                            repeat(animatedWeekCount) { week ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    repeat(7) { weekday ->
                                        val dayNumber =
                                            week * 7 + weekday - animatedLeadingDays + 1
                                        if (dayNumber in 1..animatedMonth.lengthOfMonth()) {
                                            CalendarDay(
                                                day = dayNumber,
                                                studies = studyRecords[
                                                    animatedMonth.withDayOfMonth(dayNumber)
                                                ].orEmpty(),
                                                scheduledStudies = scheduledRecords[
                                                    animatedMonth.withDayOfMonth(dayNumber)
                                                ].orEmpty(),
                                                isToday = animatedMonth.withDayOfMonth(dayNumber) == today,
                                                isSelected = animatedMonth.withDayOfMonth(dayNumber) == selectedDate,
                                                onClick = {
                                                    selectedDateValue =
                                                        animatedMonth.withDayOfMonth(dayNumber).toString()
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f).height(76.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
                Text(
                    text = stringResource(
                        R.string.calendar_selected_day,
                        selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", locale))
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                val selectedStudies = studyRecords[selectedDate].orEmpty()
                val selectedSchedules = scheduledRecords[selectedDate].orEmpty()
                if (selectedStudies.isEmpty() && selectedSchedules.isEmpty()) {
                    Text(
                        text = stringResource(R.string.calendar_no_studies),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    selectedStudies.forEach { study ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = MaterialTheme.spacing.extraSmall),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = MaterialTheme.spacing.small,
                                        vertical = MaterialTheme.spacing.extraSmall
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = study.subject,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(
                                        R.string.calendar_study_duration,
                                        study.durationMinutes
                                    ),
                                    modifier = Modifier.padding(
                                        horizontal = MaterialTheme.spacing.small
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = { studyPendingDeletion = study },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(
                                            R.string.delete_study_accessibility,
                                            study.subject
                                        ),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedSchedules.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    Text(
                        text = stringResource(R.string.scheduled_studies),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                    selectedSchedules.forEach { scheduledStudy ->
                        ScheduledStudyRow(
                            study = scheduledStudy,
                            onCancel = { schedulePendingCancellation = scheduledStudy }
                        )
                    }
                }
            }
        }

        if (month == currentMonth && selectedDate != today) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
            val todayStudies = studyRecords[today].orEmpty()
            Text(
                text = if (todayStudies.isEmpty()) {
                    stringResource(R.string.calendar_today_not_studied)
                } else {
                    stringResource(
                        R.string.calendar_today_studied,
                        todayStudies.joinToString { it.subject }
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (todayStudies.isEmpty()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

@Composable
private fun CalendarDay(
    day: Int,
    studies: List<StudyEntry>,
    scheduledStudies: List<ScheduledStudy>,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier
            .height(76.dp)
            .selectable(
                selected = isSelected,
                role = Role.Button,
                onClick = onClick
            )
            .background(
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    scheduledStudies.isNotEmpty() -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                    studies.isNotEmpty() -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else -> MaterialTheme.colorScheme.surface
                },
                shape = shape
            )
            .then(
                if (isToday || isSelected) {
                    Modifier.border(
                        BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            MaterialTheme.colorScheme.primary
                        ),
                        shape
                    )
                } else {
                    Modifier
                }
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (studies.isNotEmpty() || isToday || isSelected) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            },
            color = if (isToday || isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )

        if (studies.isNotEmpty() || scheduledStudies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (scheduledStudies.isNotEmpty()) {
                    stringResource(R.string.scheduled_day_marker, scheduledStudies.first().subject)
                } else if (studies.size > 1) {
                    "${studies.first().subject} +${studies.size - 1}"
                } else {
                    studies.first().subject
                },
                color = if (scheduledStudies.isNotEmpty()) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                fontSize = 10.sp,
                lineHeight = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ScheduledStudyRow(study: ScheduledStudy, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = study.subject,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = pluralStringResource(
                    R.plurals.scheduled_study_settings,
                    study.sessionCount,
                    study.sessionCount,
                    study.studyMinutes,
                    study.breakMinutes
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onCancel) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(
                    R.string.cancel_scheduled_study_accessibility,
                    study.subject
                ),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
