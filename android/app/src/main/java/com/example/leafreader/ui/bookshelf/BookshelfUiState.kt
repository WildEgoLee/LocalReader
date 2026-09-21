package com.example.leafreader.ui.bookshelf

import com.example.leafreader.core.model.Book

enum class BookshelfViewMode {
    GRID,
    LIST
}

data class BookshelfUiState(
    val recentBooks: List<Book> = emptyList(),
    val allBooks: List<Book> = emptyList(),
    val searchQuery: String = "",
    val viewMode: BookshelfViewMode = BookshelfViewMode.GRID,
    val isLoading: Boolean = false,
    val userMessage: String? = null
)
