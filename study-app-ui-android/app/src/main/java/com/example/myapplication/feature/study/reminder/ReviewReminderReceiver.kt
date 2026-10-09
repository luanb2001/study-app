package com.example.myapplication.feature.study.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.feature.study.LocalStudyDataStore
import com.example.myapplication.feature.study.ReviewScheduleStore
import java.time.LocalDate

class ReviewReminderReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        ReviewReminderScheduler.scheduleDaily(context)
        createNotificationChannel(context)

        if (!hasNotificationPermission(context)) return

        val today = LocalDate.now()
        val dueReviews = ReviewScheduleStore(context)
            .load()
            .filter { !it.dueDate.isAfter(today) }
        val scheduledStudies = LocalStudyDataStore(context)
            .loadScheduledStudies()
            .filter { !it.date.isAfter(today) }

        if (dueReviews.isEmpty() && scheduledStudies.isEmpty()) {
            NotificationManagerCompat.from(context).cancel(REMINDER_NOTIFICATION_ID)
            return
        }

        val openAppIntent = PendingIntent.getActivity(
            context,
            OPEN_APP_REQUEST_CODE,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val reviewCount = context.resources.getQuantityString(
            R.plurals.review_reminder_count,
            dueReviews.size,
            dueReviews.size
        )
        val scheduledCount = context.resources.getQuantityString(
            R.plurals.scheduled_study_reminder_count,
            scheduledStudies.size,
            scheduledStudies.size
        )
        val summary = listOfNotNull(
            reviewCount.takeIf { dueReviews.isNotEmpty() },
            scheduledCount.takeIf { scheduledStudies.isNotEmpty() }
        ).joinToString(context.getString(R.string.reminder_summary_separator))
        val notificationLines = buildList {

            if (dueReviews.isNotEmpty()) {
                add(reviewCount)

                dueReviews.map { it.subject }.distinct().forEach {
                    add(context.getString(R.string.review_reminder_subject, it))
                }

            }

            if (scheduledStudies.isNotEmpty()) {
                add(scheduledCount)

                scheduledStudies.map { it.subject }.distinct().forEach {
                    add(context.getString(R.string.scheduled_reminder_subject, it))
                }

            }

        }
        val notification = NotificationCompat.Builder(context, REVIEW_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.review_reminder_title))
            .setContentText(summary)
            .setStyle(
                NotificationCompat.InboxStyle().also { style ->
                    notificationLines.forEach(style::addLine)
                    style.setSummaryText(context.getString(R.string.reminder_tap_to_open))
                }
            )
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context)
            .notify(REMINDER_NOTIFICATION_ID, notification)
    }

    private fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            REVIEW_CHANNEL_ID,
            context.getString(R.string.review_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.review_reminder_channel_description)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private companion object {
        const val REVIEW_CHANNEL_ID = "study_review_reminders"
        const val REMINDER_NOTIFICATION_ID = 7001
        const val OPEN_APP_REQUEST_CODE = 7002
    }
}
