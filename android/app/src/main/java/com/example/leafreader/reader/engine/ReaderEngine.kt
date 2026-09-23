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
 * Logical page window. Index is zero-based inside the current chapter
 * (or spine item, for formats that do not paginate characters).
 */
data class PageCursor(
    val index: Int,
    val count: Int
)

/**
 * Estimated characters that fit one screen. The engine reflows from the
 * saved character offset, so a font or spacing change never drifts the locator.
 */
data class PageLayout(
    val charsPerLine: Int = 24,
    val linesPerPage: Int = 18
)

fun pageLayoutFor(fontSizeSp: Int, lineSpacingMultiplier: Float): PageLayout {
    val size = fontSizeSp.coerceIn(12, 36)
    val charsPerLine = (400 / size).coerceIn(10, 46)
    val linesPerPage = (28f / lineSpacingMultiplier.coerceIn(1f, 2.4f)).toInt().coerceIn(6, 30)
    return PageLayout(charsPerLine, linesPerPage)
}

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
    suspend fun nextPage(): Boolean
    suspend fun previousPage(): Boolean

    fun pageCursor(): PageCursor = PageCursor(0, 1)

    /** Reflow the current chapter. Locator character offset is preserved. */
    suspend fun applyLayout(layout: PageLayout) {}
}
