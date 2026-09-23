package com.example.leafreader.ui.reader

import com.example.leafreader.core.model.Book
import com.example.leafreader.core.model.BookLocator
import com.example.leafreader.core.model.Chapter
import com.example.leafreader.core.model.ReaderSettings
import com.example.leafreader.reader.engine.SearchResult

sealed interface ReaderOverlay {
    data object None : ReaderOverlay
    data object Controls : ReaderOverlay
    data object Toc : ReaderOverlay
    data object Settings : ReaderOverlay
    data object Search : ReaderOverlay
}

data class ReaderUiState(
    val book: Book? = null,
    val currentChapter: Chapter? = null,
    val tableOfContents: List<Chapter> = emptyList(),
    val content: String = "",
    val locator: BookLocator? = null,
    val readingProgress: Float = 0f,
    val pageIndex: Int = 0,
    val pageCount: Int = 1,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val overlay: ReaderOverlay = ReaderOverlay.None,
    val settings: ReaderSettings = ReaderSettings(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
