package com.example.myapplication.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.myapplication.R
import com.example.myapplication.ui.theme.spacing

@Composable
fun RegisterStudyScreen(
    modifier: Modifier = Modifier,
    onRegisterStudy: (StudyEntry) -> Unit = {},
    onBack: () -> Unit = {},
    initialSubject: String = ""
) {
    var subject by rememberSaveable {
        mutableStateOf(initialSubject)
    }

    var notes by rememberSaveable {
        mutableStateOf("")
    }

    var duration by rememberSaveable {
        mutableStateOf("")
    }
    var selectedDateMillis by rememberSaveable {
        mutableStateOf(LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val selectedDate = Instant.ofEpochMilli(selectedDateMillis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()))
    val durationMinutes = duration.toIntOrNull()

    if (showDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
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
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }

            Text(
                text = stringResource(R.string.register_new_study),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.extraSmall)
        )

        Text(
            text = stringResource(R.string.register_study_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.extraLarge)
        )

        Text(
            text = stringResource(R.string.study_date),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.small)
        )

        OutlinedTextField(
            value = selectedDate,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = stringResource(R.string.select_study_date)
                    )
                }
            }
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.large)
        )

        Text(
            text = stringResource(R.string.study_prompt),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.small)
        )

        OutlinedTextField(
            value = subject,
            onValueChange = {
                subject = it
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(stringResource(R.string.subject_example))
            },
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.large)
        )

        Text(
            text = stringResource(R.string.learning_prompt),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.small)
        )

        OutlinedTextField(
            value = notes,
            onValueChange = {
                notes = it
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(stringResource(R.string.notes_example))
            },
            minLines = 4
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.large)
        )

        Text(
            text = stringResource(R.string.study_time),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.small)
        )

        OutlinedTextField(
            value = duration,
            onValueChange = {
                duration = it
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(stringResource(R.string.duration_example))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        Spacer(
            modifier = Modifier.height(MaterialTheme.spacing.extraLarge)
        )

        Button(
            onClick = {
                val parsedDuration = durationMinutes
                if (parsedDuration != null && parsedDuration > 0 && subject.isNotBlank()) {
                    onRegisterStudy(
                        StudyEntry(
                            date = Instant.ofEpochMilli(selectedDateMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate(),
                            subject = subject.trim(),
                            description = notes.trim(),
                            durationMinutes = parsedDuration
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = subject.isNotBlank() && durationMinutes != null && durationMinutes > 0
        ) {
            Text(stringResource(R.string.register_study))
        }
    }
}
