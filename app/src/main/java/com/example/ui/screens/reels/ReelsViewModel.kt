package com.example.ui.screens.reels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Lesson
import com.example.data.model.ReelBackgroundTheme
import com.example.data.model.ReelDocument
import com.example.data.model.ReelFilterMode
import com.example.data.model.ReelPage
import com.example.data.model.ReelsSettingsState
import com.example.data.parser.ReelsParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReelsViewModel(application: Application) : AndroidViewModel(application) {

    private val parser = ReelsParser()
    val audioManager = ReelsAudioManager(application)

    private val presets = parser.getPresetDocuments()

    private val _availableDocs = MutableStateFlow<List<ReelDocument>>(presets)
    val availableDocs: StateFlow<List<ReelDocument>> = _availableDocs.asStateFlow()

    private val _document = MutableStateFlow<ReelDocument>(presets.firstOrNull() ?: parser.getDefaultReelDocument())
    val document: StateFlow<ReelDocument> = _document.asStateFlow()

    private val _settings = MutableStateFlow(ReelsSettingsState())
    val settings: StateFlow<ReelsSettingsState> = _settings.asStateFlow()

    private val _filterMode = MutableStateFlow(ReelFilterMode.ALL)
    val filterMode: StateFlow<ReelFilterMode> = _filterMode.asStateFlow()

    private val _seenPageIds = MutableStateFlow<Set<String>>(emptySet())
    val seenPageIds: StateFlow<Set<String>> = _seenPageIds.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _filterQuery = MutableStateFlow("")
    val filterQuery: StateFlow<String> = _filterQuery.asStateFlow()

    private val _isLoadingDoc = MutableStateFlow(false)
    val isLoadingDoc: StateFlow<Boolean> = _isLoadingDoc.asStateFlow()

    private val _docLoadError = MutableStateFlow<String?>(null)
    val docLoadError: StateFlow<String?> = _docLoadError.asStateFlow()

    // Filtered Pages combining Document, FilterMode (ALL / UNSEEN / SEEN), SeenIDs, and Search Query
    val filteredPages: StateFlow<List<ReelPage>> = combine(
        _document,
        _filterMode,
        _seenPageIds,
        _filterQuery
    ) { doc, mode, seenIds, query ->
        val modeFiltered = when (mode) {
            ReelFilterMode.ALL -> doc.pages
            ReelFilterMode.UNSEEN -> doc.pages.filter { it.id !in seenIds }
            ReelFilterMode.SEEN -> doc.pages.filter { it.id in seenIds }
        }

        if (query.isBlank()) {
            modeFiltered
        } else {
            val q = query.trim().lowercase()
            modeFiltered.filter { it.content.lowercase().contains(q) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Counts for UI filter badges
    val totalPagesCount: StateFlow<Int> = combine(_document) { doc ->
        doc[0].pages.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val seenPagesCount: StateFlow<Int> = combine(_document, _seenPageIds) { doc, seen ->
        doc.pages.count { it.id in seen }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unseenPagesCount: StateFlow<Int> = combine(_document, _seenPageIds) { doc, seen ->
        doc.pages.count { it.id !in seen }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var lastPlayedPageIndex: Int = -1

    init {
        // Start background music if enabled
        if (_settings.value.musicEnabled) {
            audioManager.playRandomTrack()
        }
    }

    fun onPageChanged(pageIndex: Int, pageId: String? = null) {
        if (pageId != null) {
            markPageAsSeen(pageId)
        }

        if (pageIndex == lastPlayedPageIndex) return
        lastPlayedPageIndex = pageIndex

        val s = _settings.value
        if (s.musicEnabled && s.randomTrackPerPage) {
            audioManager.playRandomTrack()
        }
    }

    fun markPageAsSeen(pageId: String) {
        if (pageId !in _seenPageIds.value) {
            _seenPageIds.value = _seenPageIds.value + pageId
        }
    }

    fun resetSeenPages() {
        _seenPageIds.value = emptySet()
    }

    fun setFilterMode(mode: ReelFilterMode) {
        _filterMode.value = mode
        _settings.value = _settings.value.copy(filterMode = mode)
    }

    fun toggleFullscreen() {
        val next = !_isFullscreen.value
        _isFullscreen.value = next
        _settings.value = _settings.value.copy(isFullscreen = next)
    }

    fun setFullscreen(fullscreen: Boolean) {
        _isFullscreen.value = fullscreen
        _settings.value = _settings.value.copy(isFullscreen = fullscreen)
    }

    fun selectDocument(doc: ReelDocument) {
        _document.value = doc
        _filterQuery.value = ""
        lastPlayedPageIndex = -1
    }

    fun loadDocumentFromUrl(url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) {
            _docLoadError.value = "Please enter a valid document link"
            return
        }

        viewModelScope.launch {
            _isLoadingDoc.value = true
            _docLoadError.value = null
            val (doc, error) = parser.fetchAndParseDocUrl(cleanUrl)
            _isLoadingDoc.value = false
            if (doc != null && doc.pages.isNotEmpty()) {
                _document.value = doc
                _settings.value = _settings.value.copy(docUrl = cleanUrl)
                // Add to available docs list if not already there
                val current = _availableDocs.value.toMutableList()
                if (current.none { it.title == doc.title }) {
                    current.add(0, doc)
                    _availableDocs.value = current
                }
                lastPlayedPageIndex = -1
            } else {
                _docLoadError.value = error ?: "Failed to extract content from this document"
            }
        }
    }

    fun loadDefaultDocument() {
        val defaultDoc = presets.firstOrNull() ?: parser.getDefaultReelDocument()
        _document.value = defaultDoc
        _docLoadError.value = null
        lastPlayedPageIndex = -1
    }

    fun addLessonsToAvailableDocs(lessons: List<Lesson>) {
        if (lessons.isEmpty()) return
        val current = _availableDocs.value.toMutableList()
        var added = false
        for (lesson in lessons) {
            val title = lesson.title.trim()
            if (title.isNotEmpty() && current.none { it.title == title }) {
                val paragraphs = mutableListOf<String>()
                if (lesson.guidelines.paragraphs.isNotEmpty()) {
                    paragraphs.addAll(lesson.guidelines.paragraphs)
                }
                for (section in lesson.sections) {
                    if (section.guide.paragraphs.isNotEmpty()) {
                        paragraphs.addAll(section.guide.paragraphs)
                    } else if (section.modelAnswer.isNotBlank()) {
                        paragraphs.add(section.modelAnswer)
                    } else if (section.task.isNotBlank()) {
                        paragraphs.add(section.task)
                    }
                }
                if (paragraphs.isNotEmpty()) {
                    val pages = paragraphs.mapIndexed { idx, text ->
                        ReelPage(
                            id = "lesson_${lesson.id}_$idx",
                            pageNumber = idx + 1,
                            content = text,
                            docTitle = title
                        )
                    }
                    current.add(ReelDocument(title = title, pages = pages))
                    added = true
                }
            }
        }
        if (added) {
            _availableDocs.value = current
        }
    }

    fun setFilterQuery(query: String) {
        _filterQuery.value = query
    }

    fun updateSettings(transform: (ReelsSettingsState) -> ReelsSettingsState) {
        val updated = transform(_settings.value)
        val old = _settings.value
        _settings.value = updated

        if (updated.isFullscreen != _isFullscreen.value) {
            _isFullscreen.value = updated.isFullscreen
        }
        if (updated.filterMode != _filterMode.value) {
            _filterMode.value = updated.filterMode
        }

        // Apply audio settings updates
        if (updated.musicEnabled != old.musicEnabled) {
            if (updated.musicEnabled) {
                audioManager.playRandomTrack()
            } else {
                audioManager.stop()
            }
        }
        if (updated.volume != old.volume) {
            audioManager.setVolume(updated.volume)
        }
    }

    fun addDriveAudioTrack(url: String, title: String) {
        audioManager.addDriveTrack(url, title)
    }

    fun removeDriveAudioTrack(id: String) {
        audioManager.removeTrack(id)
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
