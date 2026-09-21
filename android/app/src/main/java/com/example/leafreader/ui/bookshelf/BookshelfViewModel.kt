package com.example.leafreader.ui.bookshelf

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.leafreader.core.model.Book
import com.example.leafreader.core.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BookshelfViewModel(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookshelfUiState(isLoading = true))
    val uiState: StateFlow<BookshelfUiState> = _uiState.asStateFlow()

    init {
        loadBooks()
    }

    private fun loadBooks() {
        viewModelScope.launch {
            combine(
                bookRepository.observeAllBooks(),
                bookRepository.observeRecentBooks()
            ) { all, recents ->
                Pair(all, recents)
            }.collect { (all, recents) ->
                _uiState.update { state ->
                    state.copy(
                        allBooks = all,
                        recentBooks = recents,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleViewMode() {
        _uiState.update {
            it.copy(
                viewMode = if (it.viewMode == BookshelfViewMode.GRID) {
                    BookshelfViewMode.LIST
                } else {
                    BookshelfViewMode.GRID
                }
            )
        }
    }

    /**
     * Imports a book from SAF document picker URI into private sandbox storage.
     */
    fun importBookFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = bookRepository.importBookFromUri(context, uri)
            result.onSuccess { imported ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "成功导入《${imported.title}》(${imported.format})"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userMessage = "导入失败: ${error.localizedMessage ?: "未知错误"}"
                    )
                }
            }
        }
    }

    fun deleteBook(id: Long) {
        viewModelScope.launch {
            bookRepository.deleteBook(id)
            _uiState.update { it.copy(userMessage = "书籍已从书架移除") }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
