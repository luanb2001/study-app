package com.example.myapplication.feature.study.data.sync

import org.json.JSONObject

data class PendingSyncChange(
    val changeId: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val baseVersion: Long,
    val payload: JSONObject?
)

data class AcceptedSyncChange(
    val changeId: String,
    val version: Long
)

data class DownloadedSyncChange(
    val sequence: Long,
    val changeId: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val version: Long,
    val payload: JSONObject?
)

data class PendingSyncConflict(
    val entityType: String,
    val entityId: String,
    val submittedVersion: Long,
    val serverVersion: Long,
    val serverOperation: String,
    val localOperation: String,
    val localPayload: JSONObject?,
    val serverPayload: JSONObject?
)
