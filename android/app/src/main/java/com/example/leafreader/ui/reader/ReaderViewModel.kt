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
                val currentChapter = toc.firstOrNull()
                val content = engine.getCurrentContent()
                val locator = engine.currentLocator()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tableOfContents = toc,
                        currentChapter = currentChapter,
                        content = content,
                        locator = locator,
                        readingProgress = book.readingProgress
                    )
                }
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
        val currentProgress = _uiState.value.readingProgress
        val newProgress = (currentProgress - 0.05f).coerceAtLeast(0f)
        updateProgress(newProgress)
    }

    fun onNextPage() {
        val currentProgress = _uiState.value.readingProgress
        val newProgress = (currentProgress + 0.05f).coerceAtMost(1f)
        updateProgress(newProgress)
    }

    fun onSelectChapter(chapter: Chapter) {
        viewModelScope.launch {
            val engine = readerSession.getActiveEngine()
            engine?.restore(chapter.locator)
            _uiState.update {
                it.copy(
                    currentChapter = chapter,
                    locator = chapter.locator,
                    readingProgress = chapter.locator.relativeProgress,
                    overlay = ReaderOverlay.None
                )
            }
            saveCurrentProgress()
        }
    }

    fun onProgressSliderChange(progress: Float) {
        updateProgress(progress)
    }

    private fun updateProgress(progress: Float) {
        _uiState.update { it.copy(readingProgress = progress) }
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
