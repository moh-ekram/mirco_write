package com.example.data.backup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoBackupReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AutoBackupReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "AutoBackupReceiver triggered at 2:00 AM. Action: ${intent.action}")
        val appContext = context.applicationContext

        // Schedule next day's 2:00 AM backup
        BackupManager.scheduleDaily2AmBackup(appContext)

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted. Rescheduled 2:00 AM backup.")
            return
        }

        if (BackupManager.isWifiConnected(appContext)) {
            Log.d(TAG, "Wi-Fi is connected. Running 2:00 AM backup immediately.")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    BackupManager.performBackup(appContext, isAuto = true)
                } catch (e: Exception) {
                    Log.e(TAG, "Auto-backup failed: ${e.message}")
                }
            }
        } else {
            Log.d(TAG, "Wi-Fi not connected at 2:00 AM. Waiting for Wi-Fi connection to backup...")
            waitForWifiAndBackup(appContext)
        }
    }

    private fun waitForWifiAndBackup(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Wi-Fi connected! Executing deferred 2:00 AM backup.")
                try {
                    cm.unregisterNetworkCallback(this)
                } catch (_: Exception) {}

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        BackupManager.performBackup(context, isAuto = true)
                    } catch (e: Exception) {
                        Log.e(TAG, "Deferred auto-backup failed: ${e.message}")
                    }
                }
            }
        }

        try {
            cm.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            Log.e(TAG, "Could not register wifi callback: ${e.message}")
        }
    }
}
