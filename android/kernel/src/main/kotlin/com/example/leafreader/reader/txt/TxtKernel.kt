package com.example.leafreader.reader.txt

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * TXT indexing and search with no Android dependency.
 *
 * A logical locator is (chapterId, charOffset). charOffset indexes the charset
 * decoding of [IndexedChapter.startByte, endByte), newlines included as stored.
 * Page layout may be recomputed from that offset. It must not replace it.
 */
object TxtKernel {

    const val SYNTHETIC_CHAPTER_CHARS = 8000
    const val SYNTHETIC_MIN_CHARS = 15000
    const val SEARCH_CHUNK_CHARS = 64 * 1024

    data class IndexedChapter(
        val id: String,
        val title: String,
        val orderIndex: Int,
        val startByte: Long,
        val endByte: Long,
        val startChar: Long,
        val charCount: Int
    )

    data class TxtIndex(
        val charset: Charset,
        val totalBytes: Long,
        val totalChars: Long,
        val chapters: List<IndexedChapter>
    )

    data class KernelHit(
        val chapterId: String,
        val chapterTitle: String,
        val charOffsetInChapter: Int,
        val snippet: String,
        val relativeProgress: Float
    )

    fun canonicalChapterId(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val legacy = Regex("""^(?:c|synthetic_)(\d+)$""").matchEntire(trimmed) ?: return trimmed
        return "ch_${legacy.groupValues[1]}"
    }

    fun resolveChapterId(requested: String, ids: List<String>): String? {
        if (ids.isEmpty()) return null
        val canonical = canonicalChapterId(requested)
        if (canonical.isNotEmpty() && canonical in ids) return canonical
        return ids.first()
    }

    /** Presentation only. The logical charOffset is not an output of this function. */
    fun pageIndexFor(pages: List<TextPage>, charOffset: Int): Int {
        return PaginationEngine().findPageIndexForOffset(pages, charOffset)
    }

    fun decode(bytes: ByteArray, charset: Charset): String {
        if (bytes.isEmpty()) return ""
        return newDecoder(charset).decode(ByteBuffer.wrap(bytes)).toString()
    }

    fun index(bytes: ByteArray, charset: Charset): TxtIndex {
        return ByteArrayInputStream(bytes).use { indexStream(it, bytes.size.toLong(), charset) }
    }

    fun index(file: File, charset: Charset): TxtIndex {
        return file.inputStream().buffered().use { indexStream(it, file.length(), charset) }
    }

    fun search(
        bytes: ByteArray,
        index: TxtIndex,
        query: String,
        maxResults: Int = 40,
        chunkChars: Int = SEARCH_CHUNK_CHARS
    ): List<KernelHit> {
        if (query.isBlank() || maxResults <= 0) return emptyList()
        val hits = mutableListOf<KernelHit>()
        for (chapter in index.chapters) {
            if (hits.size >= maxResults) break
            val start = chapter.startByte.toInt()
            val length = (chapter.endByte - chapter.startByte).toInt()
            if (length <= 0 || start < 0 || start + length > bytes.size) continue
            ByteArrayInputStream(bytes, start, length).use { stream ->
                collect(stream, length.toLong(), index, chapter, query, maxResults - hits.size, chunkChars, hits)
            }
        }
        return hits
    }

    fun search(
        file: File,
        index: TxtIndex,
        query: String,
        maxResults: Int = 40,
        chunkChars: Int = SEARCH_CHUNK_CHARS
    ): List<KernelHit> {
        if (query.isBlank() || maxResults <= 0 || !file.exists()) return emptyList()
        val hits = mutableListOf<KernelHit>()
        RandomAccessFile(file, "r").use { raf ->
            for (chapter in index.chapters) {
                if (hits.size >= maxResults) break
                val length = chapter.endByte - chapter.startByte
                if (length <= 0L) continue
                raf.seek(chapter.startByte)
                val stream = BoundedFileStream(raf, length)
                collect(stream, length, index, chapter, query, maxResults - hits.size, chunkChars, hits)
            }
        }
        return hits
    }

    private fun indexStream(input: InputStream, length: Long, charset: Charset): TxtIndex {
        val utf16 = isUtf16(charset)
        val scanner = ByteScanner(input)
        scanner.utf16Le = !charset.name().uppercase().contains("BE")
        skipBom(scanner, charset, utf16)

        val chapters = mutableListOf<IndexedChapter>()
        val syntheticMarks = mutableListOf(Mark(scanner.position, 0L))
        var sawHeading = false
        var sinceMark = 0L
        var order = 1
        var title = "序言"
        var chapterStartByte = scanner.position
        var chapterStartChar = 0L
        var chapterChars = 0L
        var charCursor = 0L

        fun emit(endByte: Long, endChar: Long, chapterTitle: String) {
            val count = (endChar - chapterStartChar).toInt()
            if (count <= 0) return
            chapters += IndexedChapter(
                id = "ch_$order",
                title = chapterTitle,
                orderIndex = order,
                startByte = chapterStartByte,
                endByte = endByte,
                startChar = chapterStartChar,
                charCount = count
            )
            order++
        }

        while (true) {
            val lineStart = scanner.position
            val raw = readLogicalLine(scanner, utf16) ?: break
            if (raw.isEmpty()) break
            val decoded = decode(raw, charset)
            val visible = decoded.trimEnd('\r', '\n')
            val heading = ChapterDetector.matchLine(visible)
            if (heading != null) {
                sawHeading = true
                syntheticMarks.clear()
                if (chapterChars > 0) emit(lineStart, charCursor, title)
                title = heading.cleanTitle
                chapterStartByte = lineStart
                chapterStartChar = charCursor
                chapterChars = 0
                sinceMark = 0
            } else if (!sawHeading && sinceMark >= SYNTHETIC_CHAPTER_CHARS && lineStart != chapterStartByte) {
                syntheticMarks += Mark(lineStart, charCursor)
                sinceMark = 0
            }

            chapterChars += decoded.length.toLong()
            charCursor += decoded.length.toLong()
            if (!sawHeading) sinceMark += decoded.length
        }

        if (!sawHeading) title = "正文"
        if (chapterChars > 0 || chapters.isEmpty()) {
            emit(scanner.position, charCursor, title)
        }

        val finalChapters = if (!sawHeading && charCursor > SYNTHETIC_MIN_CHARS && syntheticMarks.size > 1) {
            chaptersFromMarks(syntheticMarks, scanner.position, charCursor)
        } else {
            chapters
        }.ifEmpty {
            listOf(
                IndexedChapter(
                    id = "ch_1",
                    title = "正文",
                    orderIndex = 1,
                    startByte = scanner.position,
                    endByte = scanner.position,
                    startChar = 0L,
                    charCount = 0
                )
            )
        }

        return TxtIndex(charset, length, charCursor, finalChapters)
    }

    private fun chaptersFromMarks(marks: List<Mark>, endByte: Long, totalChars: Long): List<IndexedChapter> {
        val points = marks.distinctBy { it.byteOffset }.sortedBy { it.byteOffset }
        val chapters = mutableListOf<IndexedChapter>()
        for (i in points.indices) {
            val start = points[i]
            val nextByte = if (i + 1 < points.size) points[i + 1].byteOffset else endByte
            val nextChar = if (i + 1 < points.size) points[i + 1].charOffset else totalChars
            val count = (nextChar - start.charOffset).toInt()
            if (nextByte <= start.byteOffset || count <= 0) continue
            val order = chapters.size + 1
            chapters += IndexedChapter(
                id = "ch_$order",
                title = "第 $order 部分",
                orderIndex = order,
                startByte = start.byteOffset,
                endByte = nextByte,
                startChar = start.charOffset,
                charCount = count
            )
        }
        return chapters
    }

    private fun collect(
        stream: InputStream,
        length: Long,
        index: TxtIndex,
        chapter: IndexedChapter,
        query: String,
        limit: Int,
        chunkChars: Int,
        into: MutableList<KernelHit>
    ) {
        if (limit <= 0) return
        val overlap = (query.length - 1).coerceAtLeast(0)
        val windowChars = maxOf(chunkChars, query.length, 1)
        val buffer = StringBuilder()
        var origin = 0
        var cursor = 0
        var reportedUntil = 0

        fun scan(finalPass: Boolean) {
            while (into.size < limit) {
                val available = buffer.length - cursor
                if (available <= 0) return
                if (!finalPass && available < windowChars) return
                val end = if (finalPass) buffer.length else cursor + windowChars
                val commit = if (finalPass) buffer.length else end - overlap
                if (commit <= cursor) return

                val window = buffer.substring(cursor, end)
                val startLimit = if (finalPass) window.length else commit - cursor
                var from = (reportedUntil - (origin + cursor)).coerceIn(0, window.length)
                while (into.size < limit && from < startLimit) {
                    val found = window.indexOf(query, from, ignoreCase = true)
                    if (found < 0 || found >= startLimit) break
                    val absolute = origin + cursor + found
                    if (absolute >= reportedUntil) {
                        val absoluteInBook = chapter.startChar + absolute
                        val progress = if (index.totalChars > 0) {
                            (absoluteInBook.toFloat() / index.totalChars.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        into += KernelHit(
                            chapterId = chapter.id,
                            chapterTitle = chapter.title,
                            charOffsetInChapter = absolute,
                            snippet = snippetAt(window, found, query),
                            relativeProgress = progress
                        )
                        reportedUntil = absolute + query.length.coerceAtLeast(1)
                    }
                    from = found + query.length.coerceAtLeast(1)
                }
                if (finalPass) return
                cursor = commit
                if (cursor >= 65_536) {
                    buffer.delete(0, cursor)
                    origin += cursor
                    cursor = 0
                }
            }
        }

        for (chunk in decodeChunks(stream, length, index.charset)) {
            if (into.size >= limit) return
            if (chunk.isEmpty()) continue
            buffer.append(chunk)
            scan(finalPass = false)
        }
        scan(finalPass = true)
    }

    private fun snippetAt(window: String, at: Int, query: String): String {
        val start = (at - 18).coerceAtLeast(0)
        val end = (at + query.length + 28).coerceAtMost(window.length)
        val raw = window.substring(start, end).replace('\n', ' ').replace(Regex("\\s+"), " ").trim()
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < window.length) "…" else ""
        return prefix + raw + suffix
    }

    private fun decodeChunks(stream: InputStream, length: Long, charset: Charset): Sequence<String> {
        val decoder = newDecoder(charset)
        val bytes = ByteBuffer.allocate(128 * 1024)
        val chars = CharBuffer.allocate(128 * 1024)
        var remaining = length
        var finished = false
        return sequence {
            while (remaining > 0 && !finished) {
                val want = minOf(bytes.remaining().toLong(), remaining).toInt()
                if (want <= 0) break
                val tmp = ByteArray(want)
                val read = stream.read(tmp)
                if (read <= 0) break
                remaining -= read
                bytes.put(tmp, 0, read)
                bytes.flip()
                val endOfInput = remaining == 0L
                while (true) {
                    val result = decoder.decode(bytes, chars, endOfInput)
                    chars.flip()
                    if (chars.hasRemaining()) yield(chars.toString())
                    chars.clear()
                    if (!result.isOverflow) break
                }
                if (endOfInput) {
                    decoder.flush(chars)
                    chars.flip()
                    if (chars.hasRemaining()) yield(chars.toString())
                    chars.clear()
                    finished = true
                }
                bytes.compact()
            }
            if (!finished) {
                bytes.flip()
                decoder.decode(bytes, chars, true)
                decoder.flush(chars)
                chars.flip()
                if (chars.hasRemaining()) yield(chars.toString())
            }
        }
    }

    private fun newDecoder(charset: Charset) = charset.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)

    private fun isUtf16(charset: Charset): Boolean {
        val name = charset.name().uppercase()
        return name.startsWith("UTF-16")
    }

    private fun skipBom(scanner: ByteScanner, charset: Charset, utf16: Boolean) {
        if (!utf16 && charset.name().uppercase() == "UTF-8") {
            val b0 = scanner.read()
            val b1 = scanner.read()
            val b2 = scanner.read()
            if (b0 == 0xEF && b1 == 0xBB && b2 == 0xBF) return
            if (b2 >= 0) scanner.unread(b2)
            if (b1 >= 0) scanner.unread(b1)
            if (b0 >= 0) scanner.unread(b0)
            return
        }
        if (utf16) {
            val b0 = scanner.read()
            val b1 = scanner.read()
            val le = charset.name().uppercase().contains("LE")
            val bom = if (le) b0 == 0xFF && b1 == 0xFE else b0 == 0xFE && b1 == 0xFF
            if (bom) return
            if (b1 >= 0) scanner.unread(b1)
            if (b0 >= 0) scanner.unread(b0)
        }
    }

    private fun readLogicalLine(scanner: ByteScanner, utf16: Boolean): ByteArray? {
        if (utf16) return readUtf16Line(scanner)
        val out = ByteArrayOutputStream(128)
        var any = false
        while (true) {
            val b = scanner.read()
            if (b < 0) break
            any = true
            out.write(b)
            if (b == '\n'.code) break
            if (b == '\r'.code) {
                val next = scanner.read()
                if (next == '\n'.code) out.write(next) else if (next >= 0) scanner.unread(next)
                break
            }
        }
        if (!any) return null
        return out.toByteArray()
    }

    private fun readUtf16Line(scanner: ByteScanner): ByteArray? {
        val le = scanner.utf16Le
        val out = ByteArrayOutputStream(128)
        var any = false
        while (true) {
            val unit = scanner.readUnit(le)
            if (unit < 0) break
            any = true
            writeUnit(out, unit, le)
            if (unit == '\n'.code) break
            if (unit == '\r'.code) {
                val next = scanner.readUnit(le)
                if (next == '\n'.code) writeUnit(out, next, le) else if (next >= 0) scanner.unreadUnit(next, le)
                break
            }
        }
        if (!any) return null
        return out.toByteArray()
    }

    private fun writeUnit(out: ByteArrayOutputStream, unit: Int, littleEndian: Boolean) {
        val lo = unit and 0xFF
        val hi = (unit shr 8) and 0xFF
        if (littleEndian) {
            out.write(lo)
            out.write(hi)
        } else {
            out.write(hi)
            out.write(lo)
        }
    }
}

private data class Mark(val byteOffset: Long, val charOffset: Long)

private class ByteScanner(private val input: InputStream) {
    private val ahead = ArrayDeque<Int>()
    var position: Long = 0L
        private set
    var exhausted: Boolean = false
        private set
    var utf16Le: Boolean = true

    fun read(): Int {
        val value = if (ahead.isNotEmpty()) ahead.removeFirst() else input.read().also { if (it < 0) exhausted = true }
        if (value >= 0) position++
        return value
    }

    fun unread(value: Int) {
        if (value < 0) return
        ahead.addFirst(value)
        position--
        exhausted = false
    }

    fun readUnit(littleEndian: Boolean): Int {
        utf16Le = littleEndian
        val b0 = read()
        if (b0 < 0) return -1
        val b1 = read()
        if (b1 < 0) return -1
        return if (littleEndian) b0 or (b1 shl 8) else (b0 shl 8) or b1
    }

    fun unreadUnit(unit: Int, littleEndian: Boolean) {
        if (unit < 0) return
        if (littleEndian) {
            unread((unit shr 8) and 0xFF)
            unread(unit and 0xFF)
        } else {
            unread(unit and 0xFF)
            unread((unit shr 8) and 0xFF)
        }
    }
}

private class BoundedFileStream(
    private val file: RandomAccessFile,
    private var left: Long
) : InputStream() {
    override fun read(): Int {
        if (left <= 0) return -1
        val value = file.read()
        if (value >= 0) left--
        return value
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (left <= 0) return -1
        val n = file.read(b, off, minOf(len.toLong(), left).toInt())
        if (n > 0) left -= n
        return n
    }
}
