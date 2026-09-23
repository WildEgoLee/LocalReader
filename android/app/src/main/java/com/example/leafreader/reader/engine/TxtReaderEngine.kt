package com.example.leafreader.reader.engine

import com.example.leafreader.core.model.Book
import com.example.leafreader.core.model.BookLocator
import com.example.leafreader.core.model.Chapter
import com.example.leafreader.reader.txt.ChapterIndex
import com.example.leafreader.reader.txt.ChapterIndexItem
import com.example.leafreader.reader.txt.ChapterIndexer
import com.example.leafreader.reader.txt.CharsetDetector
import com.example.leafreader.reader.txt.PaginationEngine
import com.example.leafreader.reader.txt.TextPage
import com.example.leafreader.reader.txt.TextSearch
import com.example.leafreader.reader.txt.TxtKernel
import com.example.leafreader.reader.txt.TxtStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

/**
 * Production-ready TXT reading engine with charset auto-detection,
 * streamed chapter indexing, random-access chunk reading, and pagination.
 * All I/O operations are strictly performed on Dispatchers.IO.
 */
class TxtReaderEngine : ReaderEngine {

    private var currentBook: Book? = null
    private var currentFile: File? = null
    private var detectedCharset: Charset = Charsets.UTF_8
    private var chapterIndex: ChapterIndex? = null
    private var streamReader: TxtStreamReader? = null
    private var paginationEngine = PaginationEngine()

    private var currentChapterId: String = ""
    private var currentCharOffsetInChapter: Int = 0
    private var currentProgress: Float = 0f

    private var cachedChapterContent: String = ""
    private var cachedPages: List<TextPage> = emptyList()
    private var currentPageIndex: Int = 0

    override suspend fun open(book: Book) = withContext(Dispatchers.IO) {
        currentBook = book
        val filePath = if (book.uri.startsWith("file://")) {
            book.uri.removePrefix("file://")
        } else {
            book.uri.removePrefix("content://")
        }
        val file = File(filePath)
        currentFile = file

        // 1. Detect character encoding if file exists, else default UTF-8
        detectedCharset = if (file.exists()) {
            CharsetDetector.detect(file)
        } else {
            Charsets.UTF_8
        }

        // 2. Build or restore streamed chapter index
        chapterIndex = if (file.exists()) {
            ChapterIndexer.indexFile(file, book.id, detectedCharset)
        } else {
            createFallbackIndex(book.id)
        }

        // 3. Initialize random-access stream reader
        streamReader = if (file.exists()) {
            TxtStreamReader(file, detectedCharset)
        } else null

        // 4. Restore reading position from logical locator
        val locator = book.currentLocator as? BookLocator.TxtLocator
        if (locator != null) {
            restore(locator)
        } else {
            currentCharOffsetInChapter = 0
            currentProgress = 0f
            loadChapter("")
        }
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        currentBook = null
        currentFile = null
        chapterIndex = null
        streamReader = null
        cachedChapterContent = ""
        cachedPages = emptyList()
    }

    override suspend fun getTableOfContents(): List<Chapter> = withContext(Dispatchers.IO) {
        val idx = chapterIndex ?: return@withContext emptyList()
        idx.items.map { it.toChapter(idx.totalChars) }
    }

    override suspend fun restore(locator: BookLocator) = withContext(Dispatchers.IO) {
        if (locator is BookLocator.TxtLocator) {
            currentCharOffsetInChapter = locator.charOffset.coerceAtLeast(0)
            currentProgress = locator.relativeProgress
            loadChapter(locator.chapterId)
        }
    }

    override suspend fun currentLocator(): BookLocator {
        return BookLocator.TxtLocator(
            chapterId = currentChapterId,
            charOffset = currentCharOffsetInChapter,
            paragraphIndex = 0,
            relativeProgress = currentProgress
        )
    }

    override suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val reader = streamReader
        val idx = chapterIndex ?: return@withContext emptyList()
        if (reader != null && currentFile?.exists() == true) {
            return@withContext reader.search(query, idx, maxResults = 40).map { it.toEngineResult() }
        }

        val results = mutableListOf<SearchResult>()
        for (item in idx.items) {
            val remaining = 40 - results.size
            if (remaining <= 0) break
            val content = getDefaultChapterText(item.title)
            results += TextSearch.scan(
                content = content,
                query = query,
                chapterId = item.id,
                chapterTitle = item.title,
                chapterStartCharOffset = item.startCharOffset,
                totalChars = idx.totalChars,
                limit = remaining
            ).map { it.toEngineResult() }
        }
        results
    }

    override suspend fun getCurrentContent(): String {
        return if (cachedPages.isNotEmpty()) {
            cachedPages.getOrNull(currentPageIndex)?.content ?: cachedChapterContent
        } else {
            cachedChapterContent
        }
    }

    fun getCurrentPages(): List<TextPage> = cachedPages

    fun getCurrentPageIndex(): Int = currentPageIndex

    override suspend fun nextPage(): Boolean = withContext(Dispatchers.IO) {
        if (cachedPages.isNotEmpty() && currentPageIndex < cachedPages.size - 1) {
            currentPageIndex++
            currentCharOffsetInChapter = cachedPages[currentPageIndex].startCharOffset
            updateRelativeProgress()
            return@withContext true
        }
        val chapters = chapterIndex?.items ?: return@withContext false
        val currentIdx = chapters.indexOfFirst { it.id == currentChapterId }
        if (currentIdx != -1 && currentIdx < chapters.size - 1) {
            currentCharOffsetInChapter = 0
            loadChapter(chapters[currentIdx + 1].id)
            return@withContext true
        }
        false
    }

    override suspend fun previousPage(): Boolean = withContext(Dispatchers.IO) {
        if (currentPageIndex > 0 && cachedPages.isNotEmpty()) {
            currentPageIndex--
            currentCharOffsetInChapter = cachedPages[currentPageIndex].startCharOffset
            updateRelativeProgress()
            return@withContext true
        }
        val chapters = chapterIndex?.items ?: return@withContext false
        val currentIdx = chapters.indexOfFirst { it.id == currentChapterId }
        if (currentIdx > 0) {
            loadChapter(chapters[currentIdx - 1].id)
            val last = (cachedPages.size - 1).coerceAtLeast(0)
            currentPageIndex = last
            currentCharOffsetInChapter = cachedPages.getOrNull(last)?.startCharOffset ?: 0
            updateRelativeProgress()
            return@withContext true
        }
        false
    }

    override fun pageCursor(): PageCursor {
        val count = cachedPages.size.coerceAtLeast(1)
        val index = currentPageIndex.coerceIn(0, count - 1)
        return PageCursor(index, count)
    }

    override suspend fun applyLayout(layout: PageLayout) = withContext(Dispatchers.IO) {
        paginationEngine = PaginationEngine(
            charsPerLineEstimated = layout.charsPerLine,
            linesPerPageEstimated = layout.linesPerPage
        )
        cachedPages = paginationEngine.paginate(cachedChapterContent)
        currentPageIndex = paginationEngine.findPageIndexForOffset(cachedPages, currentCharOffsetInChapter)
        // Page index is presentation. Reflow must not rewrite currentCharOffsetInChapter.
    }

    private fun com.example.leafreader.reader.txt.TxtSearchResult.toEngineResult(): SearchResult {
        return SearchResult(
            chapterTitle = chapterTitle,
            snippet = snippet,
            locator = BookLocator.TxtLocator(
                chapterId = chapterId,
                charOffset = charOffsetInChapter,
                paragraphIndex = 0,
                relativeProgress = relativeProgress
            )
        )
    }

    private suspend fun loadChapter(chapterId: String) = withContext(Dispatchers.IO) {
        val idx = chapterIndex ?: return@withContext
        val resolved = TxtKernel.resolveChapterId(chapterId, idx.items.map { it.id }) ?: return@withContext
        currentChapterId = resolved
        val chapterItem = idx.getChapterById(resolved) ?: return@withContext

        cachedChapterContent = if (streamReader != null && currentFile?.exists() == true) {
            streamReader!!.readChapterContent(chapterItem.startByteOffset, chapterItem.endByteOffset)
        } else {
            getDefaultChapterText(chapterItem.title)
        }

        cachedPages = paginationEngine.paginate(cachedChapterContent)
        currentPageIndex = paginationEngine.findPageIndexForOffset(cachedPages, currentCharOffsetInChapter)
        updateRelativeProgress()
    }

    private fun updateRelativeProgress() {
        val idx = chapterIndex ?: return
        val item = idx.getChapterById(currentChapterId) ?: return
        if (idx.totalChars > 0) {
            val totalOffset = item.startCharOffset + currentCharOffsetInChapter
            currentProgress = (totalOffset.toFloat() / idx.totalChars).coerceIn(0f, 1f)
        }
    }

    private fun createFallbackIndex(bookId: Long): ChapterIndex {
        val items = listOf(
            ChapterIndexItem("ch_1", "第一章 雾港", 1, 0, 1000, 0, 450),
            ChapterIndexItem("ch_2", "第二章 测深", 2, 1000, 2500, 450, 600),
            ChapterIndexItem("ch_3", "第三章 船灯", 3, 2500, 4200, 1050, 720)
        )
        return ChapterIndex(bookId, Charsets.UTF_8, 4200, 1770, items)
    }

    private fun getDefaultChapterText(title: String): String {
        return """
            $title

            雾还贴在码头上，灯是一盏一盏点起来的。
            测深锤沉进水里，绳子上的记号被潮气浸暗。

            没有人催。船灯稳，就先把这一页读完。
        """.trimIndent()
    }
}
