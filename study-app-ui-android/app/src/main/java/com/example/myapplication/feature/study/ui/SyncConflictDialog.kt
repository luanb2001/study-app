package com.example.myapplication.feature.study.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.feature.study.data.local.StudyDatabase
import com.example.myapplication.feature.study.data.sync.PendingSyncConflict
import org.json.JSONObject

@Composable
fun SyncConflictDialog(
    conflict: PendingSyncConflict,
    onKeepServer: () -> Unit,
    onKeepLocal: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.sync_conflict_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.sync_conflict_description))
                ConflictVersionCard(
                    title = stringResource(R.string.sync_conflict_server_version),
                    operation = conflict.serverOperation,
                    payload = conflict.serverPayload,
                    entityType = conflict.entityType
                )
                ConflictVersionCard(
                    title = stringResource(R.string.sync_conflict_local_version),
                    operation = conflict.localOperation,
                    payload = conflict.localPayload,
                    entityType = conflict.entityType
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onKeepServer) {
                Text(stringResource(R.string.sync_conflict_keep_server))
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepLocal) {
                Text(stringResource(R.string.sync_conflict_keep_local))
            }
        }
    )
}

@Composable
private fun ConflictVersionCard(
    title: String,
    operation: String,
    payload: JSONObject?,
    entityType: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)

            if (operation == StudyDatabase.DELETE || payload == null) {
                Text(stringResource(R.string.sync_conflict_deleted))
            } else {
                ConflictFields(entityType, payload)
            }

        }
    }
}

@Composable
private fun ConflictFields(entityType: String, payload: JSONObject) {

    val fields = when (entityType) {
        StudyDatabase.STUDY -> listOf(
            stringResource(R.string.sync_field_subject) to payload.optString("subject"),
            stringResource(R.string.sync_field_date) to payload.optString("date"),
            stringResource(R.string.sync_field_summary) to payload.optString("description"),
            stringResource(R.string.sync_field_duration) to payload.optInt("durationMinutes").toString(),
            stringResource(R.string.sync_field_sessions) to payload.optInt("sessionCount").toString()
        )
        StudyDatabase.SCHEDULED_STUDY -> listOf(
            stringResource(R.string.sync_field_subject) to payload.optString("subject"),
            stringResource(R.string.sync_field_date) to payload.optString("date"),
            stringResource(R.string.sync_field_sessions) to payload.optInt("sessionCount").toString(),
            stringResource(R.string.sync_field_study_minutes) to payload.optInt("studyMinutes").toString(),
            stringResource(R.string.sync_field_break_minutes) to payload.optInt("breakMinutes").toString()
        )
        StudyDatabase.REVIEW -> listOf(
            stringResource(R.string.sync_field_subject) to payload.optString("subject"),
            stringResource(R.string.sync_field_due_date) to payload.optString("dueDate"),
            stringResource(R.string.sync_field_interval) to payload.optInt("intervalIndex").toString()
        )
        else -> listOf("" to payload.toString())
    }

    fields.forEach { (label, value) ->
        Text("$label: $value", style = MaterialTheme.typography.bodySmall)
    }
}
