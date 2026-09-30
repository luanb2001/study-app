package com.example.myapplication.feature.study

import android.content.Context
import com.example.myapplication.APP_PREFERENCES_NAME
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class ReviewScheduleStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        APP_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): List<ReviewSchedule> {
        val storedJson = preferences.getString(REVIEW_SCHEDULES_KEY, "[]") ?: "[]"
        val schedules = JSONArray(storedJson)
        return (0 until schedules.length()).map { index ->
            val schedule = schedules.getJSONObject(index)
            ReviewSchedule(
                subject = schedule.getString(SUBJECT_KEY),
                dueDate = LocalDate.parse(schedule.getString(DUE_DATE_KEY)),
                intervalIndex = schedule.getInt(INTERVAL_INDEX_KEY)
            )
        }
    }

    fun save(schedules: List<ReviewSchedule>) {
        val schedulesJson = JSONArray().apply {
            schedules.forEach { schedule ->
                put(
                    JSONObject()
                        .put(SUBJECT_KEY, schedule.subject)
                        .put(DUE_DATE_KEY, schedule.dueDate.toString())
                        .put(INTERVAL_INDEX_KEY, schedule.intervalIndex)
                )
            }
        }
        preferences.edit()
            .putString(REVIEW_SCHEDULES_KEY, schedulesJson.toString())
            .apply()
    }

    private companion object {
        const val REVIEW_SCHEDULES_KEY = "review_schedules"
        const val SUBJECT_KEY = "subject"
        const val DUE_DATE_KEY = "dueDate"
        const val INTERVAL_INDEX_KEY = "intervalIndex"
    }
}
