package com.example.leafreader.reader.txt

/**
 * Paginates text content into readable pages according to font metrics and viewport dimensions.
 * Decoupled from physical screen pixels to allow consistent logical locator resolution.
 */
data class TextPage(
    val pageIndex: Int,
    val totalPagesInChapter: Int,
    val startCharOffset: Int,
    val endCharOffset: Int,
    val content: String
)

class PaginationEngine(
    private val charsPerLineEstimated: Int = 24,
    private val linesPerPageEstimated: Int = 18
) {
    private val targetCharsPerPage: Int = (charsPerLineEstimated * linesPerPageEstimated).coerceAtLeast(200)

    fun paginate(chapterContent: String): List<TextPage> {
        if (chapterContent.isEmpty()) {
            return listOf(TextPage(0, 1, 0, 0, ""))
        }

        val pages = mutableListOf<TextPage>()
        val paragraphs = chapterContent.split("\n")

        var currentPageText = StringBuilder()
        var pageStartOffset = 0
        var currentOffset = 0
        var pageIndex = 0

        for (p in paragraphs) {
            val pWithNewline = p + "\n"
            val projectedLength = currentPageText.length + pWithNewline.length

            if (projectedLength > targetCharsPerPage && currentPageText.isNotEmpty()) {
                val pageStr = currentPageText.toString()
                pages.add(
                    TextPage(
                        pageIndex = pageIndex++,
                        totalPagesInChapter = 0, // Assigned below
                        startCharOffset = pageStartOffset,
                        endCharOffset = currentOffset,
                        content = pageStr
                    )
                )
                currentPageText = StringBuilder()
                pageStartOffset = currentOffset
            }

            currentPageText.append(pWithNewline)
            currentOffset += pWithNewline.length
        }

        if (currentPageText.isNotEmpty()) {
            pages.add(
                TextPage(
                    pageIndex = pageIndex,
                    totalPagesInChapter = 0,
                    startCharOffset = pageStartOffset,
                    endCharOffset = currentOffset,
                    content = currentPageText.toString()
                )
            )
        }

        val total = pages.size
        return pages.map { it.copy(totalPagesInChapter = total) }
    }

    fun findPageIndexForOffset(pages: List<TextPage>, charOffset: Int): Int {
        if (pages.isEmpty()) return 0
        val found = pages.indexOfFirst { charOffset in it.startCharOffset until it.endCharOffset }
        return if (found != -1) found else 0
    }
}
