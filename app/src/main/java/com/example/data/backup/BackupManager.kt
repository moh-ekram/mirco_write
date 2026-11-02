package com.example.data.backup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.model.BookmarkedLine
import com.example.data.model.Lesson
import com.example.data.repository.LessonRepository
import com.example.data.repository.PreferencesRepository
import com.example.ui.components.cleanDisplayTitle
import com.squareup.moshi.JsonClass
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

@JsonClass(generateAdapter = true)
data class SectionProgress(
    val title: String,
    val order: Int = 1,
    val userDraft: String = "",
    val isCompleted: Boolean = false,
    val isBookmarked: Boolean = false,
    val timerSpentSeconds: Int = 0,
    val lastStudiedAt: Long? = null
)

@JsonClass(generateAdapter = true)
data class LessonProgress(
    val title: String,
    val titleBn: String? = null,
    val isCompleted: Boolean = false,
    val isBookmarked: Boolean = false,
    val lastStudiedAt: Long? = null,
    val sections: List<SectionProgress> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ProgressBackupEnvelope(
    val backupVersion: String = "3.0",
    val backupType: String = "PROGRESS_ONLY",
    val app: String = "MicroWrite",
    val updatedAt: String,
    val timestamp: Long,
    val totalLessonsTracked: Int,
    val lessonsProgress: List<LessonProgress>,
    val bookmarkedLines: List<BookmarkedLine> = emptyList()
)

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
    const val PROGRESS_BACKUP_FILE_NAME = "microwrite_progress_backup.json"
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

    /**
     * Extracts a clean user-facing folder name from a SAF tree URI
     */
    fun getFolderDisplayName(context: Context, treeUriString: String?): String {
        if (treeUriString.isNullOrBlank()) return "Default App Storage"
        return try {
            val uri = Uri.parse(treeUriString)
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val decoded = Uri.decode(docId)
            when {
                decoded.contains(":") -> decoded.substringAfter(":")
                decoded.isNotBlank() -> decoded
                else -> uri.lastPathSegment ?: "Selected Folder"
            }
        } catch (_: Exception) {
            "Selected Folder"
        }
    }

    /**
     * Performs a Progress-Only backup:
     * - Only backs up lesson/article progress (userDraft, isCompleted, isBookmarked, timerSpentSeconds, etc.)
     * - Backs up bookmarked lines
     * - Replaces/overwrites the same file 'microwrite_progress_backup.json' in the selected folder (no duplicates)
     */
    suspend fun performProgressBackup(context: Context, treeUriString: String? = null): File = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val repo = LessonRepository(db.lessonDao(), db.sectionDao())
        val prefs = PreferencesRepository(context)
        val lessons = repo.allLessons.first()
        val lines = prefs.bookmarkedLines.first()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val now = Date()

        val lessonsProgress = lessons.map { lesson ->
            LessonProgress(
                title = lesson.title,
                titleBn = lesson.titleBn,
                isCompleted = lesson.completionPercentage == 100,
                isBookmarked = lesson.isBookmarked,
                lastStudiedAt = lesson.lastOpenedTimestamp,
                sections = lesson.sections.map { sec ->
                    SectionProgress(
                        title = sec.title,
                        order = sec.order,
                        userDraft = sec.userDraft,
                        isCompleted = sec.isCompleted,
                        isBookmarked = sec.isBookmarked,
                        timerSpentSeconds = sec.timerSeconds,
                        lastStudiedAt = sec.lastDraftSavedAt
                    )
                }
            )
        }

        val envelope = ProgressBackupEnvelope(
            backupVersion = "3.0",
            backupType = "PROGRESS_ONLY",
            app = "MicroWrite",
            updatedAt = dateFormat.format(now),
            timestamp = now.time,
            totalLessonsTracked = lessonsProgress.size,
            lessonsProgress = lessonsProgress,
            bookmarkedLines = lines
        )

        val adapter = moshi.adapter(ProgressBackupEnvelope::class.java)
        val json = adapter.indent("  ").toJson(envelope)

        // Always save to default local cache file (replaced on each backup)
        val localDir = getBackupDirectory(context)
        val defaultFile = File(localDir, PROGRESS_BACKUP_FILE_NAME)
        defaultFile.writeText(json)

        // If user selected a custom backup folder via Storage Access Framework:
        if (!treeUriString.isNullOrBlank()) {
            try {
                val treeUri = Uri.parse(treeUriString)
                val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)

                var targetDocUri: Uri? = null
                // Check if file already exists in this folder to replace it
                context.contentResolver.query(
                    childrenUri,
                    arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null, null, null
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    while (cursor.moveToNext()) {
                        val name = if (nameCol >= 0) cursor.getString(nameCol) else ""
                        if (name.equals(PROGRESS_BACKUP_FILE_NAME, ignoreCase = true)) {
                            val docId = cursor.getString(idCol)
                            targetDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            break
                        }
                    }
                }

                // If not found, create it once
                if (targetDocUri == null) {
                    targetDocUri = DocumentsContract.createDocument(
                        context.contentResolver,
                        parentDocUri,
                        "application/json",
                        PROGRESS_BACKUP_FILE_NAME
                    )
                }

                // Overwrite the file contents (truncate mode "wt")
                targetDocUri?.let { uri ->
                    context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                }
                Log.d(TAG, "Replaced progress backup in selected folder: $treeUriString")
            } catch (e: Exception) {
                Log.e(TAG, "Error writing progress backup to selected folder: ${e.message}", e)
            }
        }

        prefs.setLastBackupInfo(dateFormat.format(now), lessonsProgress.size)
        Log.d(TAG, "Progress backup completed with ${lessonsProgress.size} lessons progress and ${lines.size} bookmarked lines")
        defaultFile
    }

    /**
     * Restores progress by detecting lesson title and section title
     */
    suspend fun restoreProgressFromJson(context: Context, json: String): Result<Int> = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val repo = LessonRepository(db.lessonDao(), db.sectionDao())
        val prefs = PreferencesRepository(context)
        val existingLessons = repo.allLessons.first()

        fun normalizeTitle(s: String): String {
            return s.cleanDisplayTitle()
                .trim()
                .lowercase()
                .replace(Regex("[\\s_\\-–—]+"), " ")
        }

        try {
            // Attempt 1: Progress-Only format
            val progressAdapter = moshi.adapter(ProgressBackupEnvelope::class.java)
            val envelope = progressAdapter.fromJson(json)
            if (envelope != null && envelope.lessonsProgress.isNotEmpty()) {
                var restoredCount = 0

                for (lessonProgress in envelope.lessonsProgress) {
                    val normTargetTitle = normalizeTitle(lessonProgress.title)
                    val matchedLesson = existingLessons.find {
                        val norm = normalizeTitle(it.title)
                        norm == normTargetTitle || (it.titleBn != null && normalizeTitle(it.titleBn) == normTargetTitle)
                    }

                    if (matchedLesson != null) {
                        var updatedSections = matchedLesson.sections.toMutableList()
                        var anySectionChanged = false

                        for (secProgress in lessonProgress.sections) {
                            val normSecTitle = normalizeTitle(secProgress.title)
                            val secIdx = updatedSections.indexOfFirst {
                                normalizeTitle(it.title) == normSecTitle || it.order == secProgress.order
                            }
                            if (secIdx >= 0) {
                                val current = updatedSections[secIdx]
                                updatedSections[secIdx] = current.copy(
                                    userDraft = if (secProgress.userDraft.isNotBlank()) secProgress.userDraft else current.userDraft,
                                    isCompleted = secProgress.isCompleted || current.isCompleted,
                                    isBookmarked = secProgress.isBookmarked || current.isBookmarked,
                                    timerSeconds = if (secProgress.timerSpentSeconds > 0) secProgress.timerSpentSeconds else current.timerSeconds,
                                    lastDraftSavedAt = secProgress.lastStudiedAt ?: current.lastDraftSavedAt
                                )
                                anySectionChanged = true
                            }
                        }

                        val updatedLesson = matchedLesson.copy(
                            isBookmarked = lessonProgress.isBookmarked || matchedLesson.isBookmarked,
                            lastOpenedTimestamp = lessonProgress.lastStudiedAt ?: matchedLesson.lastOpenedTimestamp,
                            sections = updatedSections
                        )

                        repo.saveLesson(updatedLesson)
                        restoredCount++
                    }
                }

                // Restore bookmarked lines
                if (envelope.bookmarkedLines.isNotEmpty()) {
                    for (line in envelope.bookmarkedLines) {
                        prefs.toggleBookmarkedLine(line)
                    }
                }

                return@withContext Result.success(restoredCount)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Not a progress-only envelope, checking full backup envelope...")
        }

        try {
            // Attempt 2: Full BackupEnvelope fallback
            val envelopeAdapter = moshi.adapter(BackupEnvelope::class.java)
            val envelope = envelopeAdapter.fromJson(json)
            if (envelope != null && envelope.lessons.isNotEmpty()) {
                var count = 0
                for (backupLesson in envelope.lessons) {
                    val normTitle = normalizeTitle(backupLesson.title)
                    val existing = existingLessons.find { normalizeTitle(it.title) == normTitle }
                    if (existing != null) {
                        // Restore progress into existing structure
                        val merged = existing.copy(
                            isBookmarked = backupLesson.isBookmarked,
                            lastOpenedTimestamp = backupLesson.lastOpenedTimestamp,
                            sections = existing.sections.mapIndexed { idx, sec ->
                                val backupSec = backupLesson.sections.getOrNull(idx)
                                    ?: backupLesson.sections.find { normalizeTitle(it.title) == normalizeTitle(sec.title) }
                                if (backupSec != null) {
                                    sec.copy(
                                        userDraft = if (backupSec.userDraft.isNotBlank()) backupSec.userDraft else sec.userDraft,
                                        isCompleted = backupSec.isCompleted || sec.isCompleted,
                                        isBookmarked = backupSec.isBookmarked || sec.isBookmarked,
                                        timerSeconds = backupSec.timerSeconds,
                                        lastDraftSavedAt = backupSec.lastDraftSavedAt
                                    )
                                } else sec
                            }
                        )
                        repo.saveLesson(merged)
                        count++
                    } else {
                        repo.saveLesson(backupLesson)
                        count++
                    }
                }
                return@withContext Result.success(count)
            }
        } catch (_: Exception) {}

        Result.failure(Exception("Could not detect valid MicroWrite progress backup."))
    }

    suspend fun performBackup(context: Context, isAuto: Boolean = false): File {
        val prefs = PreferencesRepository(context)
        val folderUri = prefs.backupFolderUri.first()
        return performProgressBackup(context, folderUri)
    }

    suspend fun restoreFromJson(context: Context, json: String): Result<Int> {
        return restoreProgressFromJson(context, json)
    }

    suspend fun restoreFromFile(context: Context, file: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val text = file.readText()
            restoreProgressFromJson(context, text)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
}
