package com.example.leafreader.reader.txt

import com.example.leafreader.core.model.BookLocator
import com.example.leafreader.core.model.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
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
    fun getChapterById(id: String): ChapterIndexItem? = items.find { it.id == id }

    fun getChapterByIndex(index: Int): ChapterIndexItem? = items.getOrNull(index)

    fun findChapterForCharOffset(offset: Long): ChapterIndexItem? {
        return items.lastOrNull { it.startCharOffset <= offset } ?: items.firstOrNull()
    }
}

/**
 * Streamed chapter indexer that scans files without OutOfMemory errors.
 */
object ChapterIndexer {

    suspend fun indexFile(
        file: File,
        bookId: Long,
        charset: Charset
    ): ChapterIndex = withContext(Dispatchers.IO) {
        val items = mutableListOf<ChapterIndexItem>()
        var currentChapterId = 1
        var currentTitle = "序言"
        var currentStartByte = 0L
        var currentStartChar = 0L
        var currentCharsInChapter = 0
        var totalCharsCount = 0L

        var accumulatedByteOffset = 0L
        var accumulatedCharOffset = 0L

        FileInputStream(file).use { fis ->
            val reader = BufferedReader(InputStreamReader(fis, charset), 32768)
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: ""
                val lineChars = currentLine.length + 1 // +1 for newline character
                val lineBytes = (currentLine + "\n").toByteArray(charset).size.toLong()

                val match = ChapterDetector.matchLine(currentLine)
                if (match != null) {
                    // Save previous chapter if it had content
                    if (currentCharsInChapter > 0 || items.isEmpty()) {
                        val endByte = accumulatedByteOffset
                        items.add(
                            ChapterIndexItem(
                                id = "ch_$currentChapterId",
                                title = currentTitle,
                                orderIndex = currentChapterId,
                                startByteOffset = currentStartByte,
                                endByteOffset = endByte,
                                startCharOffset = currentStartChar,
                                charCount = currentCharsInChapter
                            )
                        )
                        currentChapterId++
                    }

                    currentTitle = match.cleanTitle
                    currentStartByte = accumulatedByteOffset
                    currentStartChar = accumulatedCharOffset
                    currentCharsInChapter = 0
                }

                currentCharsInChapter += lineChars
                totalCharsCount += lineChars
                accumulatedByteOffset += lineBytes
                accumulatedCharOffset += lineChars
            }

            // Append final chapter
            if (currentCharsInChapter > 0 || items.isEmpty()) {
                items.add(
                    ChapterIndexItem(
                        id = "ch_$currentChapterId",
                        title = currentTitle,
                        orderIndex = currentChapterId,
                        startByteOffset = currentStartByte,
                        endByteOffset = accumulatedByteOffset,
                        startCharOffset = currentStartChar,
                        charCount = currentCharsInChapter
                    )
                )
            }
        }

        // If no chapters were identified by regex, create synthetic chunks of ~8000 characters
        if (items.size <= 1 && totalCharsCount > 15000) {
            val syntheticItems = createSyntheticChapters(file, charset, totalCharsCount)
            ChapterIndex(bookId, charset, file.length(), totalCharsCount, syntheticItems)
        } else {
            ChapterIndex(bookId, charset, file.length(), totalCharsCount, items)
        }
    }

    private fun createSyntheticChapters(
        file: File,
        charset: Charset,
        totalChars: Long
    ): List<ChapterIndexItem> {
        val chunkChars = 8000
        val totalChapters = ((totalChars + chunkChars - 1) / chunkChars).toInt()
        val list = mutableListOf<ChapterIndexItem>()

        val fileLength = file.length()
        val avgBytesPerChar = if (totalChars > 0) fileLength.toDouble() / totalChars else 2.0

        for (i in 0 until totalChapters) {
            val startChar = i.toLong() * chunkChars
            val endChar = minOf((i + 1).toLong() * chunkChars, totalChars)
            val charLen = (endChar - startChar).toInt()

            val startByte = (startChar * avgBytesPerChar).toLong().coerceIn(0L, fileLength)
            val endByte = (endChar * avgBytesPerChar).toLong().coerceIn(startByte, fileLength)

            list.add(
                ChapterIndexItem(
                    id = "synthetic_${i + 1}",
                    title = "第 ${i + 1} 部分",
                    orderIndex = i + 1,
                    startByteOffset = startByte,
                    endByteOffset = endByte,
                    startCharOffset = startChar,
                    charCount = charLen
                )
            )
        }

        return list
    }
}
