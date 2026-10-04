package com.example.myapplication.feature.study

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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R
import com.example.myapplication.ui.components.NumberInputField
import com.example.myapplication.ui.theme.spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RegisterStudiedStudyScreen(
    initialSubject: String = "",
    onBack: () -> Unit,
    onRegisterStudy: (StudyEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var subject by rememberSaveable { mutableStateOf(initialSubject) }
    var summary by rememberSaveable { mutableStateOf("") }
    var sessionsText by rememberSaveable { mutableStateOf("1") }
    var durationText by rememberSaveable { mutableStateOf("") }
    var selectedDateMillis by rememberSaveable {
        mutableStateOf(LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val selectedDate = Instant.ofEpochMilli(selectedDateMillis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
    val sessions = sessionsText.toIntOrNull()
    val duration = durationText.toIntOrNull()
    val studyToRegister = when {
        subject.isBlank() -> null
        summary.isBlank() -> null
        sessions == null || sessions <= 0 -> null
        duration == null || duration !in 1..MAX_TIMER_MINUTES -> null
        selectedDate > LocalDate.now() -> null
        else -> StudyEntry(
            date = selectedDate,
            subject = subject.trim(),
            description = summary.trim(),
            durationMinutes = duration,
            sessionCount = sessions
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis,
            selectableDates = PastOrTodayStudyDates
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.large),
        verticalArrangement = Arrangement.Top
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
            Text(
                text = stringResource(R.string.register_studied_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.study_prompt)) },
            placeholder = { Text(stringResource(R.string.subject_example)) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        OutlinedTextField(
            value = selectedDate.format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
            ),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            label = { Text(stringResource(R.string.study_date)) },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = stringResource(R.string.select_study_date)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        OutlinedTextField(
            value = summary,
            onValueChange = { summary = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.study_summary)) },
            placeholder = { Text(stringResource(R.string.register_study_summary_hint)) },
            minLines = 3
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        NumberInputField(
            value = sessionsText,
            onValueChange = { sessionsText = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.number_of_sessions)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        NumberInputField(
            value = durationText,
            onValueChange = { durationText = it },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.total_study_duration)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        Button(
            onClick = { studyToRegister?.let(onRegisterStudy) },
            modifier = Modifier.fillMaxWidth(),
            enabled = studyToRegister != null
        ) {
            Text(stringResource(R.string.register_study))
        }
    }
}

private object PastOrTodayStudyDates : androidx.compose.material3.SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC)
            .toLocalDate() <= LocalDate.now()

    override fun isSelectableYear(year: Int): Boolean = year <= LocalDate.now().year
}
