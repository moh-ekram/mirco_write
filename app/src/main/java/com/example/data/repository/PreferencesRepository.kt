package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.BookmarkedLine
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "microwrite_preferences")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val DEFAULT_TIMER_MINUTES = intPreferencesKey("default_timer_minutes")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val SHOW_WORD_COUNT = booleanPreferencesKey("show_word_count")
        val ENFORCE_WORD_LIMIT = booleanPreferencesKey("enforce_word_limit")
        val LAST_OPENED_LESSON_ID = stringPreferencesKey("last_opened_lesson_id")
        val LAST_OPENED_SECTION_ID = stringPreferencesKey("last_opened_section_id")
        val SEED_LOADED = booleanPreferencesKey("seed_loaded_v1")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
        val COLOR_PALETTE = stringPreferencesKey("color_palette") // "blue", "orange", "green"
        val SAVED_GOOGLE_DOCS_URL = stringPreferencesKey("saved_google_docs_url")
        val LAST_BACKUP_TIME = stringPreferencesKey("last_backup_time")
        val LAST_BACKUP_COUNT = intPreferencesKey("last_backup_count")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val ANSWER_FONT_SIZE = floatPreferencesKey("answer_font_size")
        val ANSWER_LINE_SELECTION_MODE = booleanPreferencesKey("answer_line_selection_mode")
        val BOOKMARKED_LINES_JSON = stringPreferencesKey("bookmarked_lines_json")
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val bookmarkedLinesAdapter by lazy {
        val type = Types.newParameterizedType(List::class.java, BookmarkedLine::class.java)
        moshi.adapter<List<BookmarkedLine>>(type)
    }

    val answerFontSize: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.ANSWER_FONT_SIZE] ?: 16.0f
    }

    val answerLineSelectionMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ANSWER_LINE_SELECTION_MODE] ?: false
    }

    val bookmarkedLines: Flow<List<BookmarkedLine>> = context.dataStore.data.map { prefs ->
        val json = prefs[Keys.BOOKMARKED_LINES_JSON] ?: ""
        if (json.isNotBlank()) {
            try {
                bookmarkedLinesAdapter.fromJson(json) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: "system"
    }

    val colorPalette: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.COLOR_PALETTE] ?: "blue"
    }

    val defaultTimerMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEFAULT_TIMER_MINUTES] ?: 5
    }

    val fontScale: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.FONT_SCALE] ?: 1.0f
    }

    val showWordCount: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_WORD_COUNT] ?: true
    }

    val enforceWordLimit: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ENFORCE_WORD_LIMIT] ?: false
    }

    val lastOpenedLessonId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_OPENED_LESSON_ID]
    }

    val isSeedLoaded: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SEED_LOADED] ?: false
    }

    val savedGoogleDocsUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SAVED_GOOGLE_DOCS_URL] ?: "https://docs.google.com/document/d/1RPum0ZlpUdk5v2P4dfIgg1FhFDP6FiOyIMqoTIuTMdw/edit?usp=sharing"
    }

    val lastBackupTime: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_BACKUP_TIME]
    }

    val lastBackupCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_BACKUP_COUNT] ?: 0
    }

    val autoBackupEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_BACKUP_ENABLED] ?: true
    }

    suspend fun setLastBackupInfo(time: String, count: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_BACKUP_TIME] = time
            prefs[Keys.LAST_BACKUP_COUNT] = count
        }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_BACKUP_ENABLED] = enabled
        }
    }

    suspend fun setSavedGoogleDocsUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SAVED_GOOGLE_DOCS_URL] = url
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    suspend fun setColorPalette(palette: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COLOR_PALETTE] = palette
        }
    }

    suspend fun setDefaultTimerMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_TIMER_MINUTES] = minutes
        }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FONT_SCALE] = scale
        }
    }

    suspend fun setShowWordCount(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_WORD_COUNT] = show
        }
    }

    suspend fun setEnforceWordLimit(enforce: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ENFORCE_WORD_LIMIT] = enforce
        }
    }

    suspend fun setLastOpened(lessonId: String, sectionId: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_OPENED_LESSON_ID] = lessonId
            if (sectionId != null) {
                prefs[Keys.LAST_OPENED_SECTION_ID] = sectionId
            }
        }
    }

    suspend fun setSeedLoaded(loaded: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SEED_LOADED] = loaded
        }
    }

    suspend fun setAnswerFontSize(size: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ANSWER_FONT_SIZE] = size
        }
    }

    suspend fun setAnswerLineSelectionMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ANSWER_LINE_SELECTION_MODE] = enabled
        }
    }

    suspend fun toggleBookmarkedLine(line: BookmarkedLine): Boolean {
        var added = false
        context.dataStore.edit { prefs ->
            val json = prefs[Keys.BOOKMARKED_LINES_JSON] ?: ""
            val list = if (json.isNotBlank()) {
                try {
                    bookmarkedLinesAdapter.fromJson(json)?.toMutableList() ?: mutableListOf()
                } catch (_: Exception) {
                    mutableListOf()
                }
            } else mutableListOf()

            val existingIdx = list.indexOfFirst {
                it.sectionId == line.sectionId && it.lineText.trim() == line.lineText.trim()
            }
            if (existingIdx >= 0) {
                list.removeAt(existingIdx)
                added = false
            } else {
                list.add(0, line)
                added = true
            }
            prefs[Keys.BOOKMARKED_LINES_JSON] = bookmarkedLinesAdapter.toJson(list)
        }
        return added
    }

    suspend fun removeBookmarkedLine(lineId: String) {
        context.dataStore.edit { prefs ->
            val json = prefs[Keys.BOOKMARKED_LINES_JSON] ?: ""
            if (json.isNotBlank()) {
                try {
                    val list = bookmarkedLinesAdapter.fromJson(json)?.toMutableList() ?: mutableListOf()
                    list.removeAll { it.id == lineId }
                    prefs[Keys.BOOKMARKED_LINES_JSON] = bookmarkedLinesAdapter.toJson(list)
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun clearAllBookmarkedLines() {
        context.dataStore.edit { prefs ->
            prefs[Keys.BOOKMARKED_LINES_JSON] = "[]"
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
