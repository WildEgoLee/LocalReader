package com.example.leafreader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.leafreader.core.datastore.ReaderPreferencesRepository
import com.example.leafreader.core.model.Book
import com.example.leafreader.core.model.BookLocator
import com.example.leafreader.core.model.Chapter
import com.example.leafreader.core.model.ReaderColumnMode
import com.example.leafreader.core.model.ReaderSettings
import com.example.leafreader.core.model.ReaderThemePalette
import com.example.leafreader.core.model.ReadingPageMode
import com.example.leafreader.core.repository.BookRepository
import com.example.leafreader.reader.session.ReaderSession
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

    init {
        viewModelScope.launch {
            preferencesRepository.readerSettingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun openBook(book: Book) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, book = book) }
            try {
                val engine = readerSession.openBook(book)
                val toc = engine.getTableOfContents()
                val locator = engine.currentLocator()
                val matchedChapter = when (locator) {
                    is BookLocator.TxtLocator -> toc.find { it.id == locator.chapterId }
                    is BookLocator.EpubLocator -> toc.find { (it.locator as? BookLocator.EpubLocator)?.href == locator.href }
                } ?: toc.firstOrNull()
                val content = engine.getCurrentContent()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tableOfContents = toc,
                        currentChapter = matchedChapter,
                        content = content,
                        locator = locator,
                        readingProgress = locator.relativeProgress
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
            val moved = engine.previousPage()
            if (moved) {
                syncStateFromEngine(engine)
            }
        }
    }

    fun onNextPage() {
        viewModelScope.launch {
            val engine = readerSession.getActiveEngine() ?: return@launch
            val moved = engine.nextPage()
            if (moved) {
                syncStateFromEngine(engine)
            }
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
        val matchedChapter = when (newLocator) {
            is BookLocator.TxtLocator -> toc.find { it.id == newLocator.chapterId }
            is BookLocator.EpubLocator -> toc.find { (it.locator as? BookLocator.EpubLocator)?.href == newLocator.href }
        } ?: _uiState.value.currentChapter

        _uiState.update {
            it.copy(
                content = newContent,
                locator = newLocator,
                currentChapter = matchedChapter,
                readingProgress = newLocator.relativeProgress
            )
        }
        saveCurrentProgress()
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
        viewModelScope.launch {
            readerSession.closeSession()
        }
    }
}
