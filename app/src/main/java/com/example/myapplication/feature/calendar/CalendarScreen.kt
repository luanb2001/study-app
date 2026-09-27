package com.example.myapplication.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.feature.study.StudyRepository
import com.example.myapplication.feature.study.StudyEntry
import com.example.myapplication.ui.theme.spacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    var displayedMonthValue by rememberSaveable {
        mutableStateOf(today.withDayOfMonth(1).toString())
    }
    val month = LocalDate.parse(displayedMonthValue)
    val currentMonth = today.withDayOfMonth(1)
    var selectedDateValue by rememberSaveable { mutableStateOf(today.toString()) }
    val selectedDate = LocalDate.parse(selectedDateValue)
    val locale = Locale.forLanguageTag("pt-BR")
    val monthTitle = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
        .replaceFirstChar { it.titlecase(locale) }
    val studyRecords = StudyRepository.all().groupBy { it.date }
    val weekdays = listOf(
        stringResource(R.string.calendar_weekday_monday),
        stringResource(R.string.calendar_weekday_tuesday),
        stringResource(R.string.calendar_weekday_wednesday),
        stringResource(R.string.calendar_weekday_thursday),
        stringResource(R.string.calendar_weekday_friday),
        stringResource(R.string.calendar_weekday_saturday),
        stringResource(R.string.calendar_weekday_sunday)
    )
    val leadingDays = month.dayOfWeek.value - DayOfWeek.MONDAY.value
    val weekCount = (leadingDays + month.lengthOfMonth() + 6) / 7

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.large)
    ) {
        Text(
            text = stringResource(R.string.calendar_study_entries),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                val previousMonth = month.minusMonths(1)
                displayedMonthValue = previousMonth.toString()
                selectedDateValue = previousMonth
                    .withDayOfMonth(minOf(selectedDate.dayOfMonth, previousMonth.lengthOfMonth()))
                    .toString()
            }) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = stringResource(R.string.calendar_previous_month)
                )
            }
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = {
                val nextMonth = month.plusMonths(1)
                displayedMonthValue = nextMonth.toString()
                selectedDateValue = nextMonth
                    .withDayOfMonth(minOf(selectedDate.dayOfMonth, nextMonth.lengthOfMonth()))
                    .toString()
            }) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.calendar_next_month)
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

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
                        CalendarDay(
                            day = dayNumber,
                            studies = studyRecords[month.withDayOfMonth(dayNumber)].orEmpty(),
                            isToday = month.withDayOfMonth(dayNumber) == today,
                            isSelected = month.withDayOfMonth(dayNumber) == selectedDate,
                            onClick = {
                                selectedDateValue = month.withDayOfMonth(dayNumber).toString()
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

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        Text(
            text = stringResource(
                R.string.calendar_selected_day,
                selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", locale))
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        val selectedStudies = studyRecords[selectedDate].orEmpty()
        if (selectedStudies.isEmpty()) {
            Text(
                text = stringResource(R.string.calendar_no_studies),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            selectedStudies.forEach { study ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MaterialTheme.spacing.extraSmall),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = study.subject,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.calendar_study_duration, study.durationMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

        if (studies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (studies.size > 1) {
                    "${studies.first().subject} +${studies.size - 1}"
                } else {
                    studies.first().subject
                },
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
