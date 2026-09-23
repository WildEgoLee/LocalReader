package com.example.leafreader.reader.txt

/**
 * Format-agnostic in-memory scan used by both the file stream and the
 * in-memory fallback. Offsets are character offsets inside one chapter.
 */
object TextSearch {

    private val whitespace = Regex("\\s+")

    fun scan(
        content: String,
        query: String,
        chapterId: String,
        chapterTitle: String,
        chapterStartCharOffset: Long,
        totalChars: Long,
        limit: Int
    ): List<TxtSearchResult> {
        if (query.isBlank() || content.isEmpty() || limit <= 0) return emptyList()

        val results = ArrayList<TxtSearchResult>(minOf(limit, 8))
        var from = 0
        val step = query.length.coerceAtLeast(1)
        while (results.size < limit) {
            val found = content.indexOf(query, from, ignoreCase = true)
            if (found < 0) break

            val snippetStart = (found - 18).coerceAtLeast(0)
            val snippetEnd = (found + query.length + 28).coerceAtMost(content.length)
            val raw = content.substring(snippetStart, snippetEnd)
                .replace('\n', ' ')
                .replace(whitespace, " ")
                .trim()
            val prefix = if (snippetStart > 0) "…" else ""
            val suffix = if (snippetEnd < content.length) "…" else ""
            val progress = if (totalChars > 0) {
                ((chapterStartCharOffset + found).toFloat() / totalChars.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            results += TxtSearchResult(
                chapterId = chapterId,
                chapterTitle = chapterTitle,
                charOffsetInChapter = found,
                snippet = prefix + raw + suffix,
                relativeProgress = progress
            )
            from = found + step
        }
        return results
    }
}
