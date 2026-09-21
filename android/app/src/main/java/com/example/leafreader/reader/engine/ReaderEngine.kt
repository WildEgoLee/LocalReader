package com.example.leafreader.reader.engine

import com.example.leafreader.core.model.Book
import com.example.leafreader.core.model.BookLocator
import com.example.leafreader.core.model.Chapter

data class SearchResult(
    val chapterTitle: String,
    val snippet: String,
    val locator: BookLocator
)

/**
 * Unified Reader Engine abstraction.
 *
 * The UI layer remains strictly agnostic of underlying book file formats.
 * Concrete engines (TxtReaderEngine, EpubReaderEngine, future PdfReaderEngine)
 * implement this interface to provide uniform navigation, TOC, search, and locator restoration.
 */
interface ReaderEngine {
    suspend fun open(book: Book)
    suspend fun close()
    suspend fun getTableOfContents(): List<Chapter>
    suspend fun restore(locator: BookLocator)
    suspend fun currentLocator(): BookLocator
    suspend fun search(query: String): List<SearchResult>
    suspend fun getCurrentContent(): String
}
