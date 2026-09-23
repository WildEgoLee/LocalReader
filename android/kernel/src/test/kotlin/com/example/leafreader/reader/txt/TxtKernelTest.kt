package com.example.leafreader.reader.txt

import java.io.File
import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TxtKernelTest {

    private val utf8: Charset = Charsets.UTF_8
    private val gb18030: Charset = Charset.forName("GB18030")

    @Test
    fun firstLineChapterDoesNotInventEmptyPreface() {
        val bytes = "第1章 启航\n船出港。\n".toByteArray(utf8)
        val index = TxtKernel.index(bytes, utf8)
        assertEquals(listOf("ch_1"), index.chapters.map { it.id })
        assertEquals("第1章 启航", index.chapters.single().title)
        assertEquals(0L, index.chapters.single().startByte)
        assertEquals(bytes.size.toLong(), index.chapters.single().endByte)
        assertEquals("第1章 启航\n船出港。\n", decodeChapter(bytes, index.chapters.single(), utf8))
    }

    @Test
    fun prefaceKeepsItsOwnChapterBeforeTheFirstHeading() {
        val raw = "港城没有名字。\n第1章 浅滩\n铜灯还亮着。\n"
        val bytes = raw.toByteArray(utf8)
        val index = TxtKernel.index(bytes, utf8)
        assertEquals(listOf("ch_1", "ch_2"), index.chapters.map { it.id })
        assertEquals("序言", index.chapters[0].title)
        assertTrue(index.chapters[1].title.contains("第1章"))
        val headingAt = raw.substring(0, raw.indexOf("第1章")).toByteArray(utf8).size.toLong()
        assertEquals(headingAt, index.chapters[1].startByte)
        assertEquals(index.chapters[0].endByte, index.chapters[1].startByte)
        assertTiles(bytes, index)
    }

    @Test
    fun crlfChineseOffsetsMatchTheFileBytes() {
        val prefix = List(30) { "甲乙丙丁戊己庚辛壬癸" }.joinToString("\r\n") + "\r\n"
        val raw = prefix + "第1章 标记\r\n船灯稳。\r\n"
        val bytes = raw.toByteArray(utf8)
        val index = TxtKernel.index(bytes, utf8)
        val chapter = index.chapters.first { it.title.contains("标记") }
        val markerBytes = "第1章 标记".toByteArray(utf8)
        assertEquals(indexOf(bytes, markerBytes).toLong(), chapter.startByte)
        val text = decodeChapter(bytes, chapter, utf8)
        assertTrue(text.contains("\r\n"))
        assertTrue(text.contains("船灯稳"))
        assertEquals(chapter.charCount, text.length)
        assertTiles(bytes, index)
    }

    @Test
    fun gb18030BoundariesAreNotMidCharacter() {
        val raw = "序在前面。\n第1章 港湾\n测深锤沉进雾港。\n"
        val bytes = raw.toByteArray(gb18030)
        val index = TxtKernel.index(bytes, gb18030)
        val chapter = index.chapters.first { it.title.contains("港湾") }
        val text = decodeChapter(bytes, chapter, gb18030)
        assertFalse(text.contains('\uFFFD'))
        assertTrue(text.contains("测深锤沉进雾港"))
        assertEquals(chapter.charCount, text.length)
        assertEquals(raw.indexOf("第1章").let { raw.substring(0, it).toByteArray(gb18030).size.toLong() }, chapter.startByte)
        assertTiles(bytes, index)
    }

    @Test
    fun syntheticChaptersUseRealLineBoundaries() {
        val line = "甲乙丙丁戊己庚辛。\n"
        val body = line.repeat(20_000) + "MARKER_42\n" + line.repeat(20)
        val bytes = body.toByteArray(utf8)
        assertTrue(body.length > 200_000)
        val index = TxtKernel.index(bytes, utf8)
        assertTrue(index.chapters.size > 1, "expected synthetic split, got ${index.chapters.size}")
        assertTrue(index.chapters.all { it.id.startsWith("ch_") && !it.id.startsWith("synthetic_") })
        assertTiles(bytes, index)
        assertEquals(index.totalChars, index.chapters.sumOf { it.charCount.toLong() })
        index.chapters.drop(1).forEach { chapter ->
            assertEquals('\n'.code.toByte(), bytes[chapter.startByte.toInt() - 1])
        }
        index.chapters.forEach { chapter ->
            val text = decodeChapter(bytes, chapter, utf8)
            assertEquals(chapter.charCount, text.length)
            assertFalse(text.contains('\uFFFD'))
        }
        val hit = TxtKernel.search(bytes, index, "MARKER_42", chunkChars = 64).single()
        val host = index.chapters.first { it.id == hit.chapterId }
        val text = decodeChapter(bytes, host, utf8)
        assertEquals(text.indexOf("MARKER_42"), hit.charOffsetInChapter)
        assertEquals("MARKER_42", text.substring(hit.charOffsetInChapter, hit.charOffsetInChapter + "MARKER_42".length))
    }

    @Test
    fun searchFindsAHitPastTheOld512KbCap() {
        val head = "第1章 长章\n"
        val filler = "甲".repeat(400_000)
        val raw = head + filler + "NEEDLE_AT_END\n"
        val bytes = raw.toByteArray(utf8)
        assertTrue(bytes.size > 512 * 1024)
        val index = TxtKernel.index(bytes, utf8)
        assertEquals(1, index.chapters.size)
        val text = decodeChapter(bytes, index.chapters.single(), utf8)
        val expected = text.indexOf("NEEDLE_AT_END")
        assertTrue(expected > 400_000)
        val hit = TxtKernel.search(bytes, index, "NEEDLE_AT_END", chunkChars = 1024).single()
        assertEquals(expected, hit.charOffsetInChapter)
        assertEquals("ch_1", hit.chapterId)
    }

    @Test
    fun searchHitOnAChunkBoundaryIsNotDroppedOrDuplicated() {
        val raw = "0123456aXyz尾\n第1章 边界\n" + "a".repeat(20) + "aX" + "zzzz\n"
        val bytes = raw.toByteArray(utf8)
        val index = TxtKernel.index(bytes, utf8)
        val hits = TxtKernel.search(bytes, index, "aX", chunkChars = 8)
        val decoded = index.chapters.joinToString("") { decodeChapter(bytes, it, utf8) }
        val expected = mutableListOf<Pair<String, Int>>()
        index.chapters.forEach { chapter ->
            val text = decodeChapter(bytes, chapter, utf8)
            var from = 0
            while (true) {
                val at = text.indexOf("aX", from)
                if (at < 0) break
                expected += chapter.id to at
                from = at + 2
            }
        }
        assertEquals(expected, hits.map { it.chapterId to it.charOffsetInChapter })
        assertTrue(expected.isNotEmpty())
        assertTrue(decoded.contains("aX"))
    }

    @Test
    fun legacyChapterIdsResolveOntoTheStableId() {
        val ids = listOf("ch_1", "ch_2", "ch_3")
        assertEquals("ch_1", TxtKernel.resolveChapterId("c1", ids))
        assertEquals("ch_2", TxtKernel.resolveChapterId("synthetic_2", ids))
        assertEquals("ch_1", TxtKernel.resolveChapterId("", ids))
        assertEquals("ch_1", TxtKernel.resolveChapterId("missing", ids))
        assertEquals("ch_3", TxtKernel.resolveChapterId("ch_3", ids))
    }

    @Test
    fun reflowKeepsTheLogicalCharOffset() {
        val text = buildString {
            append("第1章 锚\n")
            repeat(40) { append("港口的雾一层一层压下来，测深锤还没有到底。\n") }
            append("这里是锚点句子，字号变化之后它仍应停在同一个偏移。\n")
            repeat(10) { append("船灯稳，不急。\n") }
        }
        val needle = "锚点句子"
        val logical = text.indexOf(needle)
        assertTrue(logical > 0)
        val tight = PaginationEngine(charsPerLineEstimated = 8, linesPerPageEstimated = 3).paginate(text)
        val wide = PaginationEngine(charsPerLineEstimated = 40, linesPerPageEstimated = 20).paginate(text)
        var offset = logical
        val tightPage = TxtKernel.pageIndexFor(tight, offset)
        val widePage = TxtKernel.pageIndexFor(wide, offset)
        assertEquals(logical, offset)
        assertTrue(offset in tight[tightPage].startCharOffset until tight[tightPage].endCharOffset)
        assertTrue(offset in wide[widePage].startCharOffset until wide[widePage].endCharOffset)
        assertTrue(tight[tightPage].content.contains(needle))
        assertTrue(wide[widePage].content.contains(needle))
        val back = TxtKernel.pageIndexFor(tight, offset)
        assertEquals(logical, offset)
        assertEquals(tightPage, back)
    }

    @Test
    fun utf8BomIsNotPartOfTheFirstChapter() {
        val body = "第1章 启航\n船。\n".toByteArray(utf8)
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + body
        val index = TxtKernel.index(bytes, utf8)
        assertEquals(3L, index.chapters.single().startByte)
        val text = decodeChapter(bytes, index.chapters.single(), utf8)
        assertFalse(text.contains('\uFEFF'))
        assertTrue(text.startsWith("第1章 启航"))
    }

    @Test
    fun fileSearchAgreesWithTheByteIndex() {
        val raw = "序言在前。\r\n第1章 港\r\n雾里有NEEDLE。\r\n"
        val bytes = raw.toByteArray(utf8)
        val file = File.createTempFile("leaf-txt", ".txt")
        try {
            file.writeBytes(bytes)
            val indexed = TxtKernel.index(file, utf8)
            val fromFile = TxtKernel.search(file, indexed, "NEEDLE").single()
            val fromBytes = TxtKernel.search(bytes, TxtKernel.index(bytes, utf8), "NEEDLE").single()
            assertEquals(fromBytes.chapterId, fromFile.chapterId)
            assertEquals(fromBytes.charOffsetInChapter, fromFile.charOffsetInChapter)
            val host = indexed.chapters.first { it.id == fromFile.chapterId }
            val text = decodeChapter(bytes, host, utf8)
            assertTrue(text.contains("\r\n"))
            assertEquals(text.indexOf("NEEDLE"), fromFile.charOffsetInChapter)
        } finally {
            file.delete()
        }
    }

    private fun decodeChapter(
        bytes: ByteArray,
        chapter: TxtKernel.IndexedChapter,
        charset: Charset
    ): String {
        return TxtKernel.decode(
            bytes.copyOfRange(chapter.startByte.toInt(), chapter.endByte.toInt()),
            charset
        )
    }

    private fun assertTiles(bytes: ByteArray, index: TxtKernel.TxtIndex) {
        assertEquals(index.chapters.first().startByte, bomSkip(bytes, index.charset))
        assertEquals(bytes.size.toLong(), index.chapters.last().endByte)
        index.chapters.zipWithNext { a, b ->
            assertEquals(a.endByte, b.startByte)
        }
        assertEquals(index.totalChars, index.chapters.sumOf { it.charCount.toLong() })
    }

    private fun bomSkip(bytes: ByteArray, charset: Charset): Long {
        if (charset.name().uppercase() == "UTF-8" &&
            bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            return 3L
        }
        return 0L
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray): Int {
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return i
        }
        return -1
    }
}
