package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "NotificationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "NotificationReceiver received action: $action")

        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = PreferencesRepository(appContext)
                val enabled = prefs.notificationsEnabled.first()

                if (!enabled) {
                    Log.d(TAG, "Notifications are disabled. Skipping.")
                    return@launch
                }

                if (action == Intent.ACTION_BOOT_COMPLETED) {
                    val freq = prefs.notificationFrequencyMinutes.first()
                    NotificationScheduler.scheduleNextAlarm(appContext, freq)
                    Log.d(TAG, "Device rebooted. Rescheduled notification alarms.")
                    return@launch
                }

                if (action == NotificationScheduler.ACTION_TRIGGER) {
                    NotificationScheduler.showBookmarkNotification(appContext, isTest = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in NotificationReceiver: ${e.message}")
            }
        }
    }
}
