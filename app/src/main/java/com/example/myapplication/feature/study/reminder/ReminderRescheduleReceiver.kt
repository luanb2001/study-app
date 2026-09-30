package com.example.myapplication.feature.study.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReviewReminderScheduler.scheduleDaily(context)
    }
}
