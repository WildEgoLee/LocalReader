package com.example.leafreader.reader.txt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset

/**
 * Random-access stream reader for text files.
 * Seeks directly to byte offsets, avoiding reading irrelevant parts of large files.
 */
class TxtStreamReader(
    private val file: File,
    private val charset: Charset
) {
    /**
     * Reads chapter content directly from file by byte boundaries.
     */
    suspend fun readChapterContent(
        startByte: Long,
        endByte: Long,
        maxBytes: Int = 1024 * 512 // 512KB safety cap per single chapter
    ): String = withContext(Dispatchers.IO) {
        if (!file.exists() || startByte >= file.length()) {
            return@withContext ""
        }

        val bytesToRead = minOf((endByte - startByte).coerceAtLeast(0L), maxBytes.toLong()).toInt()
        if (bytesToRead <= 0) return@withContext ""

        val buffer = ByteArray(bytesToRead)
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(startByte)
            val read = raf.read(buffer, 0, bytesToRead)
            if (read > 0) {
                String(buffer, 0, read, charset)
            } else {
                ""
            }
        }
    }

    /**
     * Searches for occurrences of a query string across the file stream.
     * Stops after [maxResults] hits so a common word cannot scan an entire novel into memory.
     */
    suspend fun search(
        query: String,
        chapterIndex: ChapterIndex,
        maxResults: Int = 50
    ): List<TxtSearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val results = mutableListOf<TxtSearchResult>()

        for (chapter in chapterIndex.items) {
            val remaining = maxResults - results.size
            if (remaining <= 0) break
            val content = readChapterContent(chapter.startByteOffset, chapter.endByteOffset)
            results += TextSearch.scan(
                content = content,
                query = query,
                chapterId = chapter.id,
                chapterTitle = chapter.title,
                chapterStartCharOffset = chapter.startCharOffset,
                totalChars = chapterIndex.totalChars,
                limit = remaining
            )
        }

        results
    }
}

data class TxtSearchResult(
    val chapterId: String,
    val chapterTitle: String,
    val charOffsetInChapter: Int,
    val snippet: String,
    val relativeProgress: Float
)
