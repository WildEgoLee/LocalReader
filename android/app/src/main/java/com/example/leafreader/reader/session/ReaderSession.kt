package com.example.leafreader.reader.session

import com.example.leafreader.core.model.Book
import com.example.leafreader.core.model.BookFormat
import com.example.leafreader.core.model.BookLocator
import com.example.leafreader.core.model.Chapter
import com.example.leafreader.reader.engine.EpubReaderEngine
import com.example.leafreader.reader.engine.ReaderEngine
import com.example.leafreader.reader.engine.TxtReaderEngine

/**
 * Orchestrates reader engine lifecycle, locator progression, and session persistence.
 */
class ReaderSession {

    private var activeEngine: ReaderEngine? = null
    private var currentBook: Book? = null

    suspend fun openBook(book: Book): ReaderEngine {
        currentBook = book
        val engine: ReaderEngine = when (book.format) {
            BookFormat.TXT -> TxtReaderEngine()
            BookFormat.EPUB -> EpubReaderEngine()
        }
        engine.open(book)
        activeEngine = engine
        return engine
    }

    suspend fun closeSession() {
        activeEngine?.close()
        activeEngine = null
        currentBook = null
    }

    fun getActiveEngine(): ReaderEngine? = activeEngine
    fun getCurrentBook(): Book? = currentBook
}
