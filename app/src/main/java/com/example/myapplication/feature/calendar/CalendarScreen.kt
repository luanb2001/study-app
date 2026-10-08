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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.myapplication.feature.study.ReviewSchedule
import com.example.myapplication.feature.study.ScheduledStudy
import com.example.myapplication.feature.study.StudyEntry
import com.example.myapplication.feature.study.StudyRepository
import com.example.myapplication.ui.components.ConfirmActionDialog
import com.example.myapplication.ui.theme.AttentionAmber
import com.example.myapplication.ui.theme.SuccessGreen
import com.example.myapplication.ui.theme.spacing
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(
    studyRepository: StudyRepository,
    modifier: Modifier = Modifier,
    onScheduleStudy: () -> Unit = {},
    onDeleteStudy: (StudyEntry) -> Unit,
    onCancelScheduledStudy: (ScheduledStudy) -> Unit,
    onRescheduleReview: (ReviewSchedule, LocalDate) -> Unit
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
    val reviewRecords = studyRepository.reviewSchedules().groupBy { it.dueDate }
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

        CalendarMonthCard(
            month = month,
            selectedDate = selectedDate,
            today = today,
            weekdays = weekdays,
            studyRecords = studyRecords,
            scheduledRecords = scheduledRecords,
            reviewRecords = reviewRecords,
            onPreviousMonth = { changeMonth(-1) },
            onNextMonth = { changeMonth(1) },
            onSelectDate = { selectedDateValue = it.toString() }
        )

        CalendarLegend()

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        CalendarSelectedDayCard(
            selectedDate = selectedDate,
            studies = studyRecords[selectedDate].orEmpty(),
            scheduledStudies = scheduledRecords[selectedDate].orEmpty(),
            reviews = reviewRecords[selectedDate].orEmpty(),
            onDeleteStudy = { studyPendingDeletion = it },
            onCancelScheduledStudy = { schedulePendingCancellation = it },
            onRescheduleReview = onRescheduleReview
        )

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
private fun CalendarSelectedDayCard(
    selectedDate: LocalDate,
    studies: List<StudyEntry>,
    scheduledStudies: List<ScheduledStudy>,
    reviews: List<ReviewSchedule>,
    onDeleteStudy: (StudyEntry) -> Unit,
    onCancelScheduledStudy: (ScheduledStudy) -> Unit,
    onRescheduleReview: (ReviewSchedule, LocalDate) -> Unit
) {
    val locale = Locale.forLanguageTag("pt-BR")
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

            if (studies.isEmpty() && scheduledStudies.isEmpty() && reviews.isEmpty()) {
                Text(
                    text = stringResource(R.string.calendar_no_studies),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {

                studies.forEach { study ->
                    CalendarStudyEntryRow(
                        study = study,
                        onDelete = { onDeleteStudy(study) }
                    )
                }

            }

            if (scheduledStudies.isNotEmpty()) {
                CalendarSectionTitle(
                    title = R.string.scheduled_studies,
                    color = MaterialTheme.colorScheme.secondary
                )

                scheduledStudies.forEach { study ->
                    ScheduledStudyRow(
                        study = study,
                        onCancel = { onCancelScheduledStudy(study) }
                    )
                }

            }

            if (reviews.isNotEmpty()) {
                CalendarSectionTitle(
                    title = R.string.calendar_scheduled_reviews,
                    color = MaterialTheme.colorScheme.primary
                )

                reviews.forEach { review ->
                    ReviewScheduleRow(
                        review = review,
                        onReschedule = { date -> onRescheduleReview(review, date) }
                    )
                }

            }

        }
    }
}

@Composable
private fun CalendarStudyEntryRow(
    study: StudyEntry,
    onDelete: () -> Unit
) {
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
                text = stringResource(R.string.calendar_study_duration, study.durationMinutes),
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
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

@Composable
private fun CalendarSectionTitle(title: Int, color: Color) {
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = color
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
}

@Composable
private fun CalendarMonthCard(
    month: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    weekdays: List<String>,
    studyRecords: Map<LocalDate, List<StudyEntry>>,
    scheduledRecords: Map<LocalDate, List<ScheduledStudy>>,
    reviewRecords: Map<LocalDate, List<ReviewSchedule>>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    val locale = Locale.forLanguageTag("pt-BR")
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
                IconButton(onClick = onPreviousMonth) {
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
                IconButton(onClick = onNextMonth) {
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
                                horizontalDrag > 80f -> onPreviousMonth()
                                horizontalDrag < -80f -> onNextMonth()
                            }
                        }
                    )
                }
            ) {
                AnimatedContent(
                    targetState = month,
                    transitionSpec = {
                        val movingForward = targetState.isAfter(initialState)
                        val enterOffset = if (movingForward) 1 else -1
                        val exitOffset = -enterOffset
                        val enter = slideInHorizontally(tween(300)) { enterOffset * it } +
                            fadeIn(tween(180))
                        val exit = slideOutHorizontally(tween(300)) { exitOffset * it } +
                            fadeOut(tween(180))
                        ContentTransform(enter, exit, sizeTransform = null)
                    },
                    label = "calendar_month_grid"
                ) { animatedMonth ->
                    CalendarMonthGrid(
                        month = animatedMonth,
                        selectedDate = selectedDate,
                        today = today,
                        weekdays = weekdays,
                        studyRecords = studyRecords,
                        scheduledRecords = scheduledRecords,
                        reviewRecords = reviewRecords,
                        onSelectDate = onSelectDate
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    month: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    weekdays: List<String>,
    studyRecords: Map<LocalDate, List<StudyEntry>>,
    scheduledRecords: Map<LocalDate, List<ScheduledStudy>>,
    reviewRecords: Map<LocalDate, List<ReviewSchedule>>,
    onSelectDate: (LocalDate) -> Unit
) {
    val leadingDays = month.dayOfWeek.value - DayOfWeek.MONDAY.value
    val weekCount = (leadingDays + month.lengthOfMonth() + 6) / 7

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

        repeat(weekCount) { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(7) { weekday ->
                    val dayNumber = week * 7 + weekday - leadingDays + 1

                    if (dayNumber in 1..month.lengthOfMonth()) {
                        val date = month.withDayOfMonth(dayNumber)
                        CalendarDay(
                            day = dayNumber,
                            studies = studyRecords[date].orEmpty(),
                            scheduledStudies = scheduledRecords[date].orEmpty(),
                            reviews = reviewRecords[date].orEmpty(),
                            isToday = date == today,
                            isSelected = date == selectedDate,
                            onClick = { onSelectDate(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f).height(54.dp))
                    }

                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun CalendarDay(
    day: Int,
    studies: List<StudyEntry>,
    scheduledStudies: List<ScheduledStudy>,
    reviews: List<ReviewSchedule>,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier
            .height(54.dp)
            .selectable(
                selected = isSelected,
                role = Role.Button,
                onClick = onClick
            )
            .background(
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    reviews.isNotEmpty() ->
                        AttentionAmber.copy(alpha = 0.14f)
                    scheduledStudies.isNotEmpty() ->
                        MaterialTheme.colorScheme.secondaryContainer
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

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (studies.isNotEmpty()) CalendarMarker(SuccessGreen)
            if (reviews.isNotEmpty()) CalendarMarker(AttentionAmber)
            if (scheduledStudies.isNotEmpty()) CalendarMarker(MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun CalendarMarker(color: Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            R.string.calendar_legend_study to MaterialTheme.colorScheme.primary,
            R.string.calendar_legend_review to AttentionAmber,
            R.string.calendar_legend_scheduled to MaterialTheme.colorScheme.secondary
        ).forEachIndexed { index, (label, color) ->
            if (index > 0) Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            CalendarMarker(color)
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReviewScheduleRow(
    review: ReviewSchedule,
    onReschedule: (LocalDate) -> Unit
) {
    var showDatePicker by rememberSaveable(review.subject) { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = review.subject,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = review.dueDate.format(dateFormatter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = { showDatePicker = true }) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = stringResource(
                    R.string.reschedule_review_accessibility,
                    review.subject
                ),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = review.dueDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
            selectableDates = FutureReviewDates
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onReschedule(
                                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            )
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState, title = null)
        }
    }

}

private object FutureReviewDates : androidx.compose.material3.SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate() >= LocalDate.now()

    override fun isSelectableYear(year: Int): Boolean = year >= LocalDate.now().year
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
                color = MaterialTheme.colorScheme.secondary
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
