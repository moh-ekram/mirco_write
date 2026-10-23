package com.example.data.backup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Lesson
import com.example.data.repository.LessonRepository
import com.example.data.repository.PreferencesRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BackupEnvelope(
    val backupVersion: String = "2.0",
    val app: String = "MicroWrite",
    val createdAt: String,
    val timestamp: Long,
    val isAuto: Boolean,
    val totalLessons: Int,
    val lessons: List<Lesson>
)

object BackupManager {
    private const val TAG = "BackupManager"
    const val BACKUP_DIR_NAME = "MicroWrite_Backups"
    const val ALARM_ACTION = "com.example.microwrite.ACTION_AUTO_BACKUP_2AM"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun getBackupDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, BACKUP_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun performBackup(context: Context, isAuto: Boolean = false): File = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val repo = LessonRepository(db.lessonDao(), db.sectionDao())
        val lessons = repo.allLessons.first()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val now = Date()

        val envelope = BackupEnvelope(
            backupVersion = "2.0",
            app = "MicroWrite",
            createdAt = dateFormat.format(now),
            timestamp = now.time,
            isAuto = isAuto,
            totalLessons = lessons.size,
            lessons = lessons
        )

        val adapter = moshi.adapter(BackupEnvelope::class.java)
        val json = adapter.indent("  ").toJson(envelope)

        val dir = getBackupDirectory(context)
        val prefix = if (isAuto) "auto_backup_2am" else "manual_backup"
        val fileName = "${prefix}_${fileDateFormat.format(now)}.json"
        val backupFile = File(dir, fileName)
        backupFile.writeText(json)

        val prefs = PreferencesRepository(context)
        prefs.setLastBackupInfo(dateFormat.format(now), lessons.size)

        Log.d(TAG, "Backup created: ${backupFile.absolutePath} with ${lessons.size} lessons")
        backupFile
    }

    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    fun scheduleDaily2AmBackup(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AutoBackupReceiver::class.java).apply {
            action = ALARM_ACTION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d(TAG, "Scheduled next 2:00 AM backup for: ${calendar.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule 2 AM alarm: ${e.message}")
        }
    }

    fun listBackups(context: Context): List<File> {
        val dir = getBackupDirectory(context)
        return dir.listFiles { file -> file.isFile && file.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun shareBackupFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "MicroWrite Backup - ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share MicroWrite Backup File"))
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing backup file: ${e.message}")
        }
    }

    suspend fun restoreFromFile(context: Context, file: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val text = file.readText()
            restoreFromJson(context, text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromJson(context: Context, json: String): Result<Int> = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val repo = LessonRepository(db.lessonDao(), db.sectionDao())
        try {
            // First attempt to decode as BackupEnvelope
            val envelopeAdapter = moshi.adapter(BackupEnvelope::class.java)
            val envelope = envelopeAdapter.fromJson(json)
            if (envelope != null && envelope.lessons.isNotEmpty()) {
                for (lesson in envelope.lessons) {
                    repo.saveLesson(lesson)
                }
                return@withContext Result.success(envelope.lessons.size)
            }
        } catch (_: Exception) {
            // Fall through to plain list
        }

        try {
            val listAdapter = moshi.adapter<List<Lesson>>(
                Types.newParameterizedType(List::class.java, Lesson::class.java)
            )
            val list = listAdapter.fromJson(json) ?: return@withContext Result.failure(Exception("Invalid JSON format"))
            for (lesson in list) {
                repo.saveLesson(lesson)
            }
            Result.success(list.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
