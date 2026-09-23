package com.example.leafreader.reader.txt

import com.example.leafreader.core.model.BookLocator
import com.example.leafreader.core.model.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

data class ChapterIndexItem(
    val id: String,
    val title: String,
    val orderIndex: Int,
    val startByteOffset: Long,
    val endByteOffset: Long,
    val startCharOffset: Long,
    val charCount: Int
) {
    fun toChapter(totalBookChars: Long): Chapter {
        val relProgress = if (totalBookChars > 0) {
            (startCharOffset.toFloat() / totalBookChars).coerceIn(0f, 1f)
        } else 0f

        return Chapter(
            id = id,
            title = title,
            orderIndex = orderIndex,
            locator = BookLocator.TxtLocator(
                chapterId = id,
                charOffset = 0,
                paragraphIndex = 0,
                relativeProgress = relProgress
            ),
            characterCount = charCount
        )
    }
}

class ChapterIndex(
    val bookId: Long,
    val charset: Charset,
    val totalBytes: Long,
    val totalChars: Long,
    val items: List<ChapterIndexItem>
) {
    fun getChapterById(id: String): ChapterIndexItem? {
        val canonical = TxtKernel.canonicalChapterId(id)
        if (canonical.isEmpty()) return null
        return items.find { it.id == canonical }
    }

    fun getChapterByIndex(index: Int): ChapterIndexItem? = items.getOrNull(index)

    fun findChapterForCharOffset(offset: Long): ChapterIndexItem? {
        return items.lastOrNull { it.startCharOffset <= offset } ?: items.firstOrNull()
    }

    fun asKernelIndex(): TxtKernel.TxtIndex {
        return TxtKernel.TxtIndex(
            charset = charset,
            totalBytes = totalBytes,
            totalChars = totalChars,
            chapters = items.map { item ->
                TxtKernel.IndexedChapter(
                    id = item.id,
                    title = item.title,
                    orderIndex = item.orderIndex,
                    startByte = item.startByteOffset,
                    endByte = item.endByteOffset,
                    startChar = item.startCharOffset,
                    charCount = item.charCount
                )
            }
        )
    }
}

/**
 * Chapter boundaries come from [TxtKernel]: real byte offsets for LF, CRLF,
 * UTF-8 and GB-family encodings. Ids are always `ch_N`.
 */
object ChapterIndexer {

    suspend fun indexFile(
        file: File,
        bookId: Long,
        charset: Charset
    ): ChapterIndex = withContext(Dispatchers.IO) {
        val indexed = TxtKernel.index(file, charset)
        val items = indexed.chapters.map { chapter ->
            ChapterIndexItem(
                id = chapter.id,
                title = chapter.title,
                orderIndex = chapter.orderIndex,
                startByteOffset = chapter.startByte,
                endByteOffset = chapter.endByte,
                startCharOffset = chapter.startChar,
                charCount = chapter.charCount
            )
        }
        ChapterIndex(
            bookId = bookId,
            charset = indexed.charset,
            totalBytes = indexed.totalBytes,
            totalChars = indexed.totalChars,
            items = items
        )
    }
}
