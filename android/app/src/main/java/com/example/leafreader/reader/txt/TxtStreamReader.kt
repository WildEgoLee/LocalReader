package com.example.leafreader.reader.txt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset

/**
 * Random-access reader. Chapter bytes are decoded exactly over
 * [startByte, endByte). Search streams the same ranges and does not truncate them.
 */
class TxtStreamReader(
    private val file: File,
    private val charset: Charset
) {
    suspend fun readChapterContent(
        startByte: Long,
        endByte: Long
    ): String = withContext(Dispatchers.IO) {
        if (!file.exists() || startByte < 0L || startByte >= file.length()) {
            return@withContext ""
        }
        val boundedEnd = minOf(endByte, file.length())
        val size = (boundedEnd - startByte).coerceAtLeast(0L)
        if (size == 0L) return@withContext ""
        if (size > Int.MAX_VALUE) error("chapter byte range does not fit in memory")

        val buffer = ByteArray(size.toInt())
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(startByte)
            var off = 0
            while (off < buffer.size) {
                val n = raf.read(buffer, off, buffer.size - off)
                if (n < 0) break
                off += n
            }
            val filled = if (off == buffer.size) buffer else buffer.copyOf(off)
            TxtKernel.decode(filled, charset)
        }
    }

    suspend fun search(
        query: String,
        chapterIndex: ChapterIndex,
        maxResults: Int = 50
    ): List<TxtSearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        TxtKernel.search(file, chapterIndex.asKernelIndex(), query, maxResults).map { hit ->
            TxtSearchResult(
                chapterId = hit.chapterId,
                chapterTitle = hit.chapterTitle,
                charOffsetInChapter = hit.charOffsetInChapter,
                snippet = hit.snippet,
                relativeProgress = hit.relativeProgress
            )
        }
    }
}
