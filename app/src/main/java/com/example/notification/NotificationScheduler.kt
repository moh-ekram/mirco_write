package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"
    const val CHANNEL_ID = "microwrite_bookmarks"
    const val CHANNEL_NAME = "Bookmark Review Reminders"
    const val ACTION_TRIGGER = "com.example.microwrite.ACTION_TRIGGER_NOTIFICATION"
    const val NOTIFICATION_ID = 4001
    const val REQUEST_CODE = 4002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Periodic reminders showing your bookmarked lines from model answers"
                enableLights(true)
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun scheduleNextAlarm(context: Context, frequencyMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = System.currentTimeMillis() + (frequencyMinutes * 60 * 1000L).coerceAtLeast(15 * 60 * 1000L)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            Log.d(TAG, "Scheduled next review notification in $frequencyMinutes minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule notification alarm: ${e.message}")
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled notification alarm")
    }

    fun showBookmarkNotification(context: Context, isTest: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = PreferencesRepository(context)
                val lines = prefs.bookmarkedLines.first()
                val mode = prefs.notificationMode.first()
                val lastIdx = prefs.notificationLastIndex.first()

                createNotificationChannel(context)

                val selectedLine = if (lines.isNotEmpty()) {
                    if (mode.equals("RANDOM", ignoreCase = true)) {
                        lines.random()
                    } else {
                        val nextIdx = (lastIdx + 1) % lines.size
                        prefs.setNotificationLastIndex(nextIdx)
                        lines[nextIdx]
                    }
                } else null

                val title = if (selectedLine != null) {
                    selectedLine.sectionTitle.ifBlank { "Bookmark Review" }
                } else {
                    "MicroWrite Review"
                }

                val body = if (selectedLine != null) {
                    selectedLine.lineText
                } else {
                    "Tap lines in any answer to bookmark and review them here!"
                }

                val subText = selectedLine?.lessonTitle ?: "MicroWrite"

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    selectedLine?.let {
                        putExtra("NAV_LESSON_ID", it.lessonId)
                        putExtra("NAV_SECTION_ID", it.sectionId)
                    }
                }

                val contentPendingIntent = PendingIntent.getActivity(
                    context,
                    NOTIFICATION_ID,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setSubText(subText)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(body)
                            .setSummaryText(subText)
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(contentPendingIntent)
                    .build()

                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, notification)

                // If not a manual test trigger, re-arm the next periodic alarm
                if (!isTest) {
                    val enabled = prefs.notificationsEnabled.first()
                    if (enabled) {
                        val freq = prefs.notificationFrequencyMinutes.first()
                        scheduleNextAlarm(context, freq)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing notification: ${e.message}", e)
            }
        }
    }
}
