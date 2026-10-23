package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import java.io.File
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BookmarkedLine
import com.example.data.model.Lesson
import com.example.data.model.LessonSection
import com.example.data.model.ParsedLessonPreview
import com.example.data.parser.ResourceParser
import com.example.data.repository.LessonRepository
import com.example.data.repository.PreferencesRepository
import com.example.data.seed.SeedLessons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.os.SystemClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.io.InputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val lessonRepository = LessonRepository(database.lessonDao(), database.sectionDao())
    val preferencesRepository = PreferencesRepository(application)
    private val resourceParser = ResourceParser()

    // Preferences
    val themeMode: StateFlow<String> = preferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val colorPalette: StateFlow<String> = preferencesRepository.colorPalette
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "blue")

    val defaultTimerMinutes: StateFlow<Int> = preferencesRepository.defaultTimerMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val fontScale: StateFlow<Float> = preferencesRepository.fontScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val showWordCount: StateFlow<Boolean> = preferencesRepository.showWordCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val enforceWordLimit: StateFlow<Boolean> = preferencesRepository.enforceWordLimit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // All lessons
    val allLessons: StateFlow<List<Lesson>> = lessonRepository.allLessons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bookmarks
    val bookmarkedLessons: StateFlow<List<Lesson>> = lessonRepository.bookmarkedLessons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedSections: StateFlow<List<LessonSection>> = lessonRepository.bookmarkedSections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Answer Settings & Persistent Bookmarked Lines (App-wide)
    val answerFontSize: StateFlow<Float> = preferencesRepository.answerFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16.0f)

    val answerLineSelectionMode: StateFlow<Boolean> = preferencesRepository.answerLineSelectionMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val bookmarkedLines: StateFlow<List<BookmarkedLine>> = preferencesRepository.bookmarkedLines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search & Filter
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedTypeFilter = MutableStateFlow<String?>(null)

    val filteredLessons: StateFlow<List<Lesson>> = combine(
        allLessons,
        searchQuery,
        selectedCategoryFilter,
        selectedTypeFilter
    ) { lessons, query, category, type ->
        lessons.filter { lesson ->
            val matchesQuery = query.isBlank() ||
                    lesson.title.contains(query, ignoreCase = true) ||
                    (lesson.titleBn != null && lesson.titleBn.contains(query, ignoreCase = true)) ||
                    lesson.description.contains(query, ignoreCase = true) ||
                    lesson.category.contains(query, ignoreCase = true) ||
                    lesson.type.contains(query, ignoreCase = true)

            val matchesCategory = category == null || lesson.category.equals(category, ignoreCase = true)
            val matchesType = type == null || lesson.type.equals(type, ignoreCase = true)

            matchesQuery && matchesCategory && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active studying state
    private val _currentLessonId = MutableStateFlow<String?>(null)
    val currentLessonId = _currentLessonId.asStateFlow()

    private val _currentLesson = MutableStateFlow<Lesson?>(null)
    val currentLesson = _currentLesson.asStateFlow()

    private val _currentSectionId = MutableStateFlow<String?>(null)
    val currentSectionId = _currentSectionId.asStateFlow()

    private val _currentSection = MutableStateFlow<LessonSection?>(null)
    val currentSection = _currentSection.asStateFlow()

    // Import state
    private val _importPreviews = MutableStateFlow<List<ParsedLessonPreview>>(emptyList())
    val importPreviews = _importPreviews.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError = _importError.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    val savedGoogleDocsUrl: StateFlow<String> = preferencesRepository.savedGoogleDocsUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "https://docs.google.com/document/d/1RPum0ZlpUdk5v2P4dfIgg1FhFDP6FiOyIMqoTIuTMdw/edit?usp=sharing")

    private val _docCatalog = MutableStateFlow<List<ParsedLessonPreview>>(emptyList())
    val docCatalog: StateFlow<List<ParsedLessonPreview>> = _docCatalog.asStateFlow()

    private val _isCatalogLoading = MutableStateFlow(false)
    val isCatalogLoading: StateFlow<Boolean> = _isCatalogLoading.asStateFlow()

    val lastBackupTime: StateFlow<String?> = preferencesRepository.lastBackupTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastBackupCount: StateFlow<Int> = preferencesRepository.lastBackupCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val autoBackupEnabled: StateFlow<Boolean> = preferencesRepository.autoBackupEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        // Pre-populate catalog so doc lessons are always listed right away
        val defaultDocParsed = resourceParser.parseContent(DEFAULT_DOC_TEXT)
        if (defaultDocParsed.isNotEmpty()) {
            _docCatalog.value = defaultDocParsed
        }

        // Seed initial lessons if first launch
        viewModelScope.launch {
            val isSeeded = preferencesRepository.isSeedLoaded.first()
            if (!isSeeded) {
                lessonRepository.saveLesson(SeedLessons.nplLesson)
                lessonRepository.saveLesson(SeedLessons.automationLesson)
                preferencesRepository.setSeedLoaded(true)
            }
        }

        // Auto-load catalog from saved Google Docs URL
        viewModelScope.launch {
            val savedUrl = preferencesRepository.savedGoogleDocsUrl.first()
            if (savedUrl.isNotBlank()) {
                _isCatalogLoading.value = true
                val (previews, _) = resourceParser.fetchAndParseGoogleDocs(savedUrl)
                if (previews.isNotEmpty()) {
                    _docCatalog.value = previews
                    if (_importPreviews.value.isEmpty()) {
                        _importPreviews.value = previews
                    }
                }
                _isCatalogLoading.value = false
            }
        }
    }

    fun openLesson(lessonId: String) {
        _currentLessonId.value = lessonId
        viewModelScope.launch {
            lessonRepository.updateLastOpened(lessonId)
            preferencesRepository.setLastOpened(lessonId)
            val lesson = lessonRepository.getLessonDirect(lessonId)
            _currentLesson.value = lesson
        }
    }

    fun openSection(lessonId: String, sectionId: String) {
        _currentLessonId.value = lessonId
        _currentSectionId.value = sectionId
        viewModelScope.launch {
            preferencesRepository.setLastOpened(lessonId, sectionId)
            val lesson = lessonRepository.getLessonDirect(lessonId)
            _currentLesson.value = lesson
            val sec = lesson?.sections?.find { it.id == sectionId }
            _currentSection.value = sec
        }
    }

    fun continueLesson(lesson: Lesson): LessonSection? {
        // Find first incomplete section, or first section
        return lesson.sections.firstOrNull { !it.isCompleted } ?: lesson.sections.firstOrNull()
    }

    fun saveDraft(sectionId: String, draft: String) {
        // Update local memory immediately for snappy UI
        _currentSection.value = _currentSection.value?.copy(userDraft = draft)
        _currentLesson.value = _currentLesson.value?.let { l ->
            val updatedSections = l.sections.map {
                if (it.id == sectionId) it.copy(userDraft = draft) else it
            }
            l.copy(sections = updatedSections)
        }

        viewModelScope.launch {
            lessonRepository.updateSectionDraft(sectionId, draft)
        }
    }

    fun markSectionDone(sectionId: String) {
        _currentSection.value = _currentSection.value?.copy(isCompleted = true, isSkipped = false)
        _currentLesson.value = _currentLesson.value?.let { l ->
            val updatedSections = l.sections.map {
                if (it.id == sectionId) it.copy(isCompleted = true, isSkipped = false) else it
            }
            l.copy(sections = updatedSections)
        }

        viewModelScope.launch {
            lessonRepository.markSectionDone(sectionId)
        }
    }

    fun markSectionSkipped(sectionId: String) {
        _currentSection.value = _currentSection.value?.copy(isCompleted = false, isSkipped = true)
        _currentLesson.value = _currentLesson.value?.let { l ->
            val updatedSections = l.sections.map {
                if (it.id == sectionId) it.copy(isCompleted = false, isSkipped = true) else it
            }
            l.copy(sections = updatedSections)
        }

        viewModelScope.launch {
            lessonRepository.markSectionSkipped(sectionId)
        }
    }

    fun toggleLessonBookmark(lessonId: String) {
        val current = _currentLesson.value?.isBookmarked ?: false
        _currentLesson.value = _currentLesson.value?.copy(isBookmarked = !current)
        viewModelScope.launch {
            lessonRepository.toggleLessonBookmark(lessonId, current)
        }
    }

    fun toggleSectionBookmark(sectionId: String) {
        val current = _currentSection.value?.isBookmarked ?: false
        _currentSection.value = _currentSection.value?.copy(isBookmarked = !current)
        viewModelScope.launch {
            lessonRepository.toggleSectionBookmark(sectionId, current)
        }
    }

    fun resetLessonProgress(lessonId: String) {
        viewModelScope.launch {
            lessonRepository.resetLessonProgress(lessonId)
            val updated = lessonRepository.getLessonDirect(lessonId)
            _currentLesson.value = updated
        }
    }

    fun clearLessonDrafts(lessonId: String) {
        viewModelScope.launch {
            lessonRepository.clearLessonDrafts(lessonId)
            val updated = lessonRepository.getLessonDirect(lessonId)
            _currentLesson.value = updated
        }
    }

    fun deleteLesson(lessonId: String) {
        viewModelScope.launch {
            lessonRepository.deleteLesson(lessonId)
            if (_currentLessonId.value == lessonId) {
                _currentLesson.value = null
                _currentLessonId.value = null
                _currentSection.value = null
            }
        }
    }

    // --- Import methods ---

    fun parseTextForImport(text: String) {
        _isImporting.value = true
        _importError.value = null
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val previews = resourceParser.parseContent(text)
                if (previews.isEmpty()) {
                    _importError.value = "No lessons found in input text. Please check format."
                } else {
                    _importPreviews.value = previews
                }
            } catch (e: Exception) {
                _importError.value = "Failed to parse text: ${e.localizedMessage}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun parseDocxForImport(inputStream: InputStream) {
        _isImporting.value = true
        _importError.value = null
        viewModelScope.launch {
            try {
                val previews = resourceParser.parseDocx(inputStream)
                if (previews.isEmpty()) {
                    _importError.value = "No valid lessons could be extracted from the DOCX file."
                } else {
                    _importPreviews.value = previews
                }
            } catch (e: Exception) {
                _importError.value = "Error reading DOCX: ${e.localizedMessage}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun fetchGoogleDocsForImport(url: String) {
        val cleanUrl = url.replace("*", ".").trim()
        _isImporting.value = true
        _importError.value = null
        viewModelScope.launch {
            preferencesRepository.setSavedGoogleDocsUrl(cleanUrl)
            val (previews, error) = resourceParser.fetchAndParseGoogleDocs(cleanUrl)
            if (error != null) {
                _importError.value = error
            } else {
                _importPreviews.value = previews
                _docCatalog.value = previews
            }
            _isImporting.value = false
        }
    }

    fun fetchNewspaperArticleForImport(url: String) {
        val cleanUrl = url.trim()
        _isImporting.value = true
        _importError.value = null
        viewModelScope.launch {
            try {
                val (previews, error) = resourceParser.fetchAndParseNewspaperArticle(cleanUrl)
                if (error != null) {
                    _importError.value = error
                } else {
                    _importPreviews.value = previews
                }
            } catch (e: Exception) {
                _importError.value = "Failed to load newspaper article: ${e.localizedMessage}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun autoFormatAndParseNewspaperText(rawText: String) {
        _isImporting.value = true
        _importError.value = null
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val structured = resourceParser.autoFormatNewspaperText(rawText)
                val previews = resourceParser.parseContent(structured)
                if (previews.isEmpty()) {
                    _importError.value = "আর্টিকেল থেকে কোনো সেকশন পাওয়া যায়নি।"
                } else {
                    _importPreviews.value = previews
                }
            } catch (e: Exception) {
                _importError.value = "Error parsing newspaper article: ${e.localizedMessage}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun autoFormatNewspaperText(rawText: String): String {
        return resourceParser.autoFormatNewspaperText(rawText)
    }

    fun togglePreviewSelection(index: Int) {
        val currentList = _importPreviews.value.toMutableList()
        if (index in currentList.indices) {
            val item = currentList[index]
            currentList[index] = item.copy(isSelectedForImport = !item.isSelectedForImport)
            _importPreviews.value = currentList
        }
    }

    fun commitImport(onSuccess: (count: Int) -> Unit) {
        val toSave = _importPreviews.value.filter { it.isSelectedForImport && it.isValid }.map { it.lesson }
        if (toSave.isEmpty()) return

        viewModelScope.launch {
            lessonRepository.saveLessons(toSave)
            _importPreviews.value = emptyList()
            onSuccess(toSave.size)
        }
    }

    fun clearImportPreview() {
        _importPreviews.value = emptyList()
        _importError.value = null
    }

    // --- Settings & Data Management ---

    fun setDefaultTimerMinutes(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.setDefaultTimerMinutes(minutes)
        }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch {
            preferencesRepository.setFontScale(scale)
        }
    }

    fun setShowWordCount(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setShowWordCount(show)
        }
    }

    fun setEnforceWordLimit(enforce: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setEnforceWordLimit(enforce)
        }
    }

    fun setAnswerFontSize(size: Float) {
        viewModelScope.launch {
            preferencesRepository.setAnswerFontSize(size)
        }
    }

    fun setAnswerLineSelectionMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAnswerLineSelectionMode(enabled)
        }
    }

    fun toggleLineBookmark(
        lessonId: String,
        sectionId: String,
        lessonTitle: String,
        sectionTitle: String,
        lineText: String,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val line = BookmarkedLine(
                lessonId = lessonId,
                sectionId = sectionId,
                lessonTitle = lessonTitle,
                sectionTitle = sectionTitle,
                lineText = lineText
            )
            val added = preferencesRepository.toggleBookmarkedLine(line)
            onResult?.invoke(added)
        }
    }

    fun removeBookmarkedLine(lineId: String) {
        viewModelScope.launch {
            preferencesRepository.removeBookmarkedLine(lineId)
        }
    }

    fun clearAllBookmarkedLines() {
        viewModelScope.launch {
            preferencesRepository.clearAllBookmarkedLines()
        }
    }

    suspend fun exportBackupJson(): String {
        return lessonRepository.exportFullBackupJson()
    }

    fun performManualBackup(context: Context, onResult: (Boolean, File?) -> Unit) {
        viewModelScope.launch {
            try {
                val file = com.example.data.backup.BackupManager.performBackup(context, isAuto = false)
                onResult(true, file)
            } catch (e: Exception) {
                onResult(false, null)
            }
        }
    }

    fun restoreBackupFromFile(context: Context, file: File, onResult: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            val res = com.example.data.backup.BackupManager.restoreFromFile(context, file)
            if (res.isSuccess) {
                onResult(true, res.getOrNull() ?: 0)
            } else {
                onResult(false, 0)
            }
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            preferencesRepository.setAutoBackupEnabled(enabled)
            if (enabled) {
                com.example.data.backup.BackupManager.scheduleDaily2AmBackup(context)
            }
        }
    }

    fun importSingleLesson(lesson: Lesson, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            lessonRepository.saveLesson(lesson)
            onComplete()
        }
    }

    fun importAllUnaddedDocLessons(docLessons: List<ParsedLessonPreview>, onComplete: (count: Int) -> Unit) {
        val currentExistingTitles = allLessons.value.map { it.title.trim().lowercase() }.toSet()
        val currentExistingIds = allLessons.value.map { it.id }.toSet()
        val toSave = docLessons
            .map { it.lesson }
            .filter { !currentExistingIds.contains(it.id) && !currentExistingTitles.contains(it.title.trim().lowercase()) }

        if (toSave.isEmpty()) {
            onComplete(0)
            return
        }
        viewModelScope.launch {
            lessonRepository.saveLessons(toSave)
            onComplete(toSave.size)
        }
    }

    fun restoreBackup(json: String, onResult: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            val res = com.example.data.backup.BackupManager.restoreFromJson(getApplication(), json)
            if (res.isSuccess) {
                onResult(true, res.getOrNull() ?: 0)
            } else {
                onResult(false, 0)
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setColorPalette(palette: String) {
        viewModelScope.launch {
            preferencesRepository.setColorPalette(palette)
        }
    }

    // --- Resilient Timer Engine (Drift-free, survives orientation change) ---
    private val _timerRemainingSeconds = MutableStateFlow(0)
    val timerRemainingSeconds = _timerRemainingSeconds.asStateFlow()

    private val _timerTotalSeconds = MutableStateFlow(0)
    val timerTotalSeconds = _timerTotalSeconds.asStateFlow()

    private val _timerIsRunning = MutableStateFlow(false)
    val timerIsRunning = _timerIsRunning.asStateFlow()

    private val _timerIsFinished = MutableStateFlow(false)
    val timerIsFinished = _timerIsFinished.asStateFlow()

    private val _focusLockEnabled = MutableStateFlow(false)
    val focusLockEnabled = _focusLockEnabled.asStateFlow()

    private var timerJob: Job? = null
    private var timerTargetElapsedMillis: Long = 0L

    fun initTimer(durationSeconds: Int) {
        if (!_timerIsRunning.value && (_timerTotalSeconds.value != durationSeconds || _timerRemainingSeconds.value == 0)) {
            _timerTotalSeconds.value = durationSeconds
            _timerRemainingSeconds.value = durationSeconds
            _timerIsFinished.value = false
        }
    }

    fun startTimer() {
        if (_timerRemainingSeconds.value <= 0) return
        _timerIsRunning.value = true
        _timerIsFinished.value = false
        timerTargetElapsedMillis = SystemClock.elapsedRealtime() + (_timerRemainingSeconds.value * 1000L)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerIsRunning.value) {
                val now = SystemClock.elapsedRealtime()
                val remainingMillis = timerTargetElapsedMillis - now
                val remainingSec = (remainingMillis / 1000L).coerceAtLeast(0L).toInt()
                _timerRemainingSeconds.value = remainingSec
                if (remainingSec <= 0) {
                    _timerIsRunning.value = false
                    _timerIsFinished.value = true
                    break
                }
                delay(250L)
            }
        }
    }

    fun pauseTimer() {
        _timerIsRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer(durationSeconds: Int = _timerTotalSeconds.value) {
        _timerIsRunning.value = false
        timerJob?.cancel()
        _timerRemainingSeconds.value = durationSeconds
        _timerTotalSeconds.value = durationSeconds
        _timerIsFinished.value = false
    }

    fun addTimerMinute() {
        val newSec = _timerRemainingSeconds.value + 60
        _timerRemainingSeconds.value = newSec
        _timerTotalSeconds.value = maxOf(_timerTotalSeconds.value, newSec)
        if (_timerIsRunning.value) {
            timerTargetElapsedMillis += 60_000L
        }
        _timerIsFinished.value = false
    }

    fun setFocusLock(enabled: Boolean) {
        _focusLockEnabled.value = enabled
    }

    // --- Undo Action Support ---
    data class LastSectionAction(
        val sectionId: String,
        val wasCompleted: Boolean,
        val wasSkipped: Boolean
    )

    private val _lastSectionAction = MutableStateFlow<LastSectionAction?>(null)
    val lastSectionAction = _lastSectionAction.asStateFlow()

    fun recordSectionAction(sectionId: String, prevCompleted: Boolean, prevSkipped: Boolean) {
        _lastSectionAction.value = LastSectionAction(sectionId, prevCompleted, prevSkipped)
    }

    fun undoLastSectionAction(onUndone: (String) -> Unit = {}) {
        val action = _lastSectionAction.value ?: return
        viewModelScope.launch {
            lessonRepository.setSectionStatus(action.sectionId, action.wasCompleted, action.wasSkipped)
            _currentSection.value = _currentSection.value?.let {
                if (it.id == action.sectionId) it.copy(isCompleted = action.wasCompleted, isSkipped = action.wasSkipped) else it
            }
            _currentLesson.value = _currentLesson.value?.let { l ->
                val updated = l.sections.map {
                    if (it.id == action.sectionId) it.copy(isCompleted = action.wasCompleted, isSkipped = action.wasSkipped) else it
                }
                l.copy(sections = updated)
            }
            _lastSectionAction.value = null
            onUndone(action.sectionId)
        }
    }

    fun deleteAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            lessonRepository.deleteAllData()
            preferencesRepository.clearAll()
            _currentLesson.value = null
            _currentSection.value = null
            onComplete()
        }
    }

    companion object {
        val DEFAULT_DOC_TEXT = """
@Export Diversification of Bangladesh: Challenges, Opportunities and the Path to a Competitive Economy
[Central argument: Bangladesh has achieved remarkable export growth through the Ready-Made Garments industry. Diversification into pharmaceuticals, leather, light engineering, and agro-processing is imperative.]

1. Introduction: Why Export Diversification Matters
[Key idea: Export diversification reduces vulnerability to external shocks and supports sustainable economic growth.]
Exports are a major source of foreign exchange and economic dynamism in developing nations.

2. Sectoral Opportunities & Strategic Realignment
[Key sectors: Leather footwear, pharmaceuticals, IT services, light engineering.]
Targeted incentive restructuring and trade facilitation are pivotal to catalyze private investment.

@জেরেমি বেন্থাম: জীবন, চিন্তাধারা, কর্ম ও আধুনিক সমাজে তাঁর অবদান
[এই আর্টিকেলের উদ্দেশ্য হলো জেরেমি বেন্থামের উপযোগবাদ এবং আধুনিক আইন ও বিচারব্যবস্থায় তাঁর অবদান বিশদভাবে তুলে ধরা।]

১. জেরেমি বেন্থাম কে ছিলেন?
[জেরেমি বেন্থাম ছিলেন অষ্টাদশ শতকের ব্রিটিশ দার্শনিক, আইনবিদ ও সংস্কারক।]
তিনি উপযোগবাদ (Utilitarianism) দর্শনের প্রতিষ্ঠাতা হিসেবে সমধিক পরিচিত।

২. শৈশব, শিক্ষা ও বুদ্ধিবৃত্তিক বিকাশ
[পিতা জেরেমিয়া বেন্থাম ছিলেন আইনজীবী। অল্প বয়সেই তিনি কুইন্স কলেজ, অক্সফোর্ডে ভর্তি হন।]
আইনের প্রাচীন ও জটিল রীতিনীতির বিপরীতে তিনি যুক্তি ও কার্যকারিতার ভিত্তিতে সংস্কারের পক্ষে ছিলেন।

৩. উপযোগবাদ দর্শন: সর্বাধিক মানুষের সর্বাধিক সুখ
[মূল নীতি: Greatest happiness of the greatest number]
বেন্থামের মতে, কোনো কাজের ভালো বা মন্দ বিচার করা উচিত তা কতটুকু আনন্দ বা সুখ সৃষ্টি করে তার ওপর।

@Inflationary Pressure and Monetary Policy in Bangladesh
[Macroeconomic overview: Post-pandemic inflation dynamics, exchange rate pressures, and contractionary monetary framework in Bangladesh.]

1. Drivers of Domestic Inflation
[Factors: Import cost pass-through, supply disruptions, and exchange rate depreciation.]
Headline CPI inflation has stayed elevated due to a combination of food and non-food pressures.

2. Monetary Tightening and Financial Stability
[Policy response: Policy repo rate hikes, market-based interest rate corridor, and liquidity absorption.]
Contractionary policy aims to anchor inflation expectations while preserving private investment momentum.
        """.trimIndent()
    }
}
