package com.example.leafreader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.leafreader.core.datastore.ReaderPreferencesRepository
import com.example.leafreader.core.model.Book
import com.example.leafreader.core.model.BookLocator
import com.example.leafreader.core.model.Chapter
import com.example.leafreader.core.model.ReaderColumnMode
import com.example.leafreader.core.model.ReaderThemePalette
import com.example.leafreader.core.model.ReadingPageMode
import com.example.leafreader.core.repository.BookRepository
import com.example.leafreader.reader.engine.ReaderEngine
import com.example.leafreader.reader.engine.SearchResult
import com.example.leafreader.reader.engine.pageLayoutFor
import com.example.leafreader.reader.session.ReaderSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReaderViewModel(
    private val bookRepository: BookRepository,
    private val preferencesRepository: ReaderPreferencesRepository
) : ViewModel() {

    private val readerSession = ReaderSession()
    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            preferencesRepository.readerSettingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
                val engine = readerSession.getActiveEngine() ?: return@collect
                engine.applyLayout(pageLayoutFor(settings.fontSizeSp, settings.lineSpacingMultiplier))
                syncStateFromEngine(engine)
            }
        }
    }

    fun openBook(book: Book) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    book = book,
                    errorMessage = null,
                    searchQuery = "",
                    searchResults = emptyList()
                )
            }
            try {
                val engine = readerSession.openBook(book)
                val settings = _uiState.value.settings
                engine.applyLayout(pageLayoutFor(settings.fontSizeSp, settings.lineSpacingMultiplier))
                val toc = engine.getTableOfContents()
                val locator = engine.currentLocator()
                val matchedChapter = matchChapter(toc, locator) ?: toc.firstOrNull()
                val content = engine.getCurrentContent()
                val cursor = engine.pageCursor()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tableOfContents = toc,
                        currentChapter = matchedChapter,
                        content = content,
                        locator = locator,
                        readingProgress = locator.relativeProgress,
                        pageIndex = cursor.index,
                        pageCount = cursor.count
                    )
                }
                saveCurrentProgress()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "打开书籍失败: ${e.localizedMessage}") }
            }
        }
    }

    fun toggleControlsOverlay() {
        _uiState.update {
            it.copy(
                overlay = if (it.overlay == ReaderOverlay.None) ReaderOverlay.Controls else ReaderOverlay.None
            )
        }
    }

    fun showOverlay(overlay: ReaderOverlay) {
        _uiState.update { it.copy(overlay = overlay) }
    }

    fun hideOverlay() {
        _uiState.update { it.copy(overlay = ReaderOverlay.None) }
    }

    fun onPreviousPage() {
        viewModelScope.launch {
            val engine = readerSession.getActiveEngine() ?: return@launch
            if (engine.previousPage()) syncStateFromEngine(engine)
        }
    }

    fun onNextPage() {
        viewModelScope.launch {
            val engine = readerSession.getActiveEngine() ?: return@launch
            if (engine.nextPage()) syncStateFromEngine(engine)
        }
    }

    fun onSelectChapter(chapter: Chapter) {
        viewModelScope.launch {
            val engine = readerSession.getActiveEngine() ?: return@launch
            engine.restore(chapter.locator)
            syncStateFromEngine(engine)
            _uiState.update { it.copy(overlay = ReaderOverlay.None) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, overlay = ReaderOverlay.Search) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(220)
            val results = readerSession.search(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun onSelectSearchResult(result: SearchResult) {
        viewModelScope.launch {
            val engine = readerSession.getActiveEngine() ?: return@launch
            engine.restore(result.locator)
            syncStateFromEngine(engine)
            _uiState.update { it.copy(overlay = ReaderOverlay.None) }
        }
    }

    fun onProgressSliderChange(progress: Float) {
        viewModelScope.launch {
            val engine = readerSession.getActiveEngine() ?: return@launch
            val toc = _uiState.value.tableOfContents
            if (toc.isNotEmpty()) {
                val targetChapter = toc.lastOrNull { it.locator.relativeProgress <= progress } ?: toc.first()
                engine.restore(targetChapter.locator)
                syncStateFromEngine(engine)
            } else {
                _uiState.update { it.copy(readingProgress = progress) }
                saveCurrentProgress()
            }
        }
    }

    private suspend fun syncStateFromEngine(engine: ReaderEngine) {
        val newContent = engine.getCurrentContent()
        val newLocator = engine.currentLocator()
        val toc = _uiState.value.tableOfContents
        val matchedChapter = matchChapter(toc, newLocator) ?: _uiState.value.currentChapter
        val cursor = engine.pageCursor()

        _uiState.update {
            it.copy(
                content = newContent,
                locator = newLocator,
                currentChapter = matchedChapter,
                readingProgress = newLocator.relativeProgress,
                pageIndex = cursor.index,
                pageCount = cursor.count
            )
        }
        saveCurrentProgress()
    }

    private fun matchChapter(toc: List<Chapter>, locator: BookLocator): Chapter? {
        return when (locator) {
            is BookLocator.TxtLocator -> toc.find { it.id == locator.chapterId }
            is BookLocator.EpubLocator -> toc.find { (it.locator as? BookLocator.EpubLocator)?.href == locator.href }
        }
    }

    private fun saveCurrentProgress() {
        val book = _uiState.value.book ?: return
        val progress = _uiState.value.readingProgress
        val locator = _uiState.value.locator
        val chapter = _uiState.value.currentChapter?.title

        viewModelScope.launch {
            bookRepository.updateReadingProgress(book.id, progress, locator, chapter)
        }
    }

    fun updateTheme(theme: ReaderThemePalette) {
        viewModelScope.launch { preferencesRepository.updateTheme(theme) }
    }

    fun updateFontSize(sizeSp: Int) {
        viewModelScope.launch { preferencesRepository.updateFontSize(sizeSp) }
    }

    fun updateLineSpacing(spacing: Float) {
        viewModelScope.launch { preferencesRepository.updateLineSpacing(spacing) }
    }

    fun updatePageMode(mode: ReadingPageMode) {
        viewModelScope.launch { preferencesRepository.updatePageMode(mode) }
    }

    fun updateColumnMode(mode: ReaderColumnMode) {
        viewModelScope.launch { preferencesRepository.updateColumnMode(mode) }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
        viewModelScope.launch {
            readerSession.closeSession()
        }
    }
}
