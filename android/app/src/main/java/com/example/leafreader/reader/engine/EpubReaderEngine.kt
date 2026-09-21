package com.example.leafreader.reader.engine

import com.example.leafreader.core.model.Book
import com.example.leafreader.core.model.BookLocator
import com.example.leafreader.core.model.Chapter

/**
 * EPUB reading engine implementation.
 * Wraps the Readium Kotlin Toolkit Navigator and Publication APIs.
 */
class EpubReaderEngine : ReaderEngine {

    private var currentBook: Book? = null
    private var currentLocator: BookLocator.EpubLocator = BookLocator.EpubLocator(
        href = "part1_chapter1.xhtml",
        relativeProgress = 0f
    )

    private val mockChapters = listOf(
        Chapter("e1", "第一部 乱纪元 · 科学边界", 1, BookLocator.EpubLocator("part1_chapter1.xhtml", relativeProgress = 0.1f), 4500),
        Chapter("e2", "第一部 幽灵倒计时", 2, BookLocator.EpubLocator("part1_chapter2.xhtml", relativeProgress = 0.25f), 5200),
        Chapter("e3", "第二部 黑暗森林 · 智子工程", 3, BookLocator.EpubLocator("part2_chapter1.xhtml", relativeProgress = 0.55f), 6100),
        Chapter("e4", "第三部 死神永生 · 掩体纪元", 4, BookLocator.EpubLocator("part3_chapter1.xhtml", relativeProgress = 0.89f), 7800)
    )

    override suspend fun open(book: Book) {
        currentBook = book
        if (book.currentLocator is BookLocator.EpubLocator) {
            currentLocator = book.currentLocator
        }
    }

    override suspend fun close() {
        currentBook = null
    }

    override suspend fun getTableOfContents(): List<Chapter> {
        return mockChapters
    }

    override suspend fun restore(locator: BookLocator) {
        if (locator is BookLocator.EpubLocator) {
            currentLocator = locator
        }
    }

    override suspend fun currentLocator(): BookLocator {
        return currentLocator
    }

    override suspend fun search(query: String): List<SearchResult> {
        return listOf(
            SearchResult("第一部 乱纪元", "...物理学从来就没有存在过，将来的物理学也不会存在...", currentLocator)
        )
    }

    override suspend fun nextPage(): Boolean {
        val currentIdx = mockChapters.indexOfFirst { (it.locator as? BookLocator.EpubLocator)?.href == currentLocator.href }
        if (currentIdx != -1 && currentIdx < mockChapters.size - 1) {
            val next = mockChapters[currentIdx + 1]
            currentLocator = next.locator as BookLocator.EpubLocator
            return true
        }
        return false
    }

    override suspend fun previousPage(): Boolean {
        val currentIdx = mockChapters.indexOfFirst { (it.locator as? BookLocator.EpubLocator)?.href == currentLocator.href }
        if (currentIdx > 0) {
            val prev = mockChapters[currentIdx - 1]
            currentLocator = prev.locator as BookLocator.EpubLocator
            return true
        }
        return false
    }

    override suspend fun getCurrentContent(): String {
        return """
            在文化大革命的狂潮中，红岸基地在偏远险峻的大兴安岭雷达峰秘密落成。
            
            巨大的抛物面天线仿佛一只冷漠的巨眼，恒久注视着浩瀚无垠的星空。冰冷的电波穿透对流层，向着深邃的银河系边缘激射而去。
            
            叶文洁静静站在天线基座下的寒风中，双手插在厚重的军大衣口袋里。
            
            她仰望着头顶那片在零下三十度严寒中闪烁得格外清晰的繁星。那些光芒跨越了数十甚至数百个光年才抵达她的眼眸，古老、静谧，却又隐隐透着令人窒息的残酷法则。
            
            “宇宙这么大，真的只有我们吗？”
            
            身后的控制室内，监听仪表盘的指示灯规律闪烁，绿色的荧光在波形显示器上划出单调的水平线条。那是一片永恒的死寂。
            
            直到那一天，红色警戒灯无声地亮起。
            
            红岸基地的超级计算机正在以超乎寻常的算力，解译着一段来自太阳方向的三体恒星系统信号。
            
            那是三条反复出现的警告：
            “不要回答！不要回答！！不要回答！！！”
        """.trimIndent()
    }
}
