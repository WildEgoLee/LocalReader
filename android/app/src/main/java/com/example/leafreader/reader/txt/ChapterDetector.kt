package com.example.leafreader.reader.txt

/**
 * High-precision chapter detector using regex rules for Chinese novels and Western text.
 */
object ChapterDetector {

    // Common Chinese chapter patterns
    private val PATTERN_CHINESE_STANDARD = Regex(
        "^[ \\t]*(第[0-9一二两三四五六七八九十百千万]+[章回节卷部篇集]\\s*([^\\n\\r]{0,35}))",
        RegexOption.MULTILINE
    )

    private val PATTERN_SPECIAL_SECTIONS = Regex(
        "^[ \\t]*(序[章言]?|前言|楔子|引子|尾声|后记|番外(?:篇)?\\s*([^\\n\\r]{0,35}))",
        RegexOption.MULTILINE
    )

    private val PATTERN_WESTERN_CHAPTER = Regex(
        "^[ \\t]*(Chapter\\s+\\d+([^\\n\\r]{0,35}))",
        setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
    )

    private val PATTERN_NUMBERED_HEADING = Regex(
        "^[ \\t]*(\\d{1,4}[\\.、\\s]+[^\\n\\r]{1,35})",
        RegexOption.MULTILINE
    )

    private val INVALID_TITLE_ENDINGS = setOf('，', '。', '；', '！', '？', ',', ';', '!')
    private val INVALID_TITLE_STARTS = setOf('“', '"', '‘', '\'', '（', '(', '【')

    data class ChapterMatch(
        val rawTitle: String,
        val cleanTitle: String,
        val lineLength: Int
    )

    /**
     * Evaluates whether a given trimmed text line is a chapter heading.
     */
    fun matchLine(line: String): ChapterMatch? {
        val trimmed = line.trim()
        if (trimmed.length < 2 || trimmed.length > 45) {
            return null
        }

        // Must not start with quote
        if (INVALID_TITLE_STARTS.contains(trimmed.first())) {
            return null
        }

        // Must not end with typical sentence-ending punctuation (unless single short title)
        if (trimmed.length > 10 && INVALID_TITLE_ENDINGS.contains(trimmed.last())) {
            return null
        }

        // Rule 1: Standard Chinese Chapter (第X章)
        PATTERN_CHINESE_STANDARD.find(trimmed)?.let { match ->
            return ChapterMatch(
                rawTitle = trimmed,
                cleanTitle = match.value.trim(),
                lineLength = line.length
            )
        }

        // Rule 2: Special sections (序言、尾声、番外)
        PATTERN_SPECIAL_SECTIONS.find(trimmed)?.let { match ->
            return ChapterMatch(
                rawTitle = trimmed,
                cleanTitle = match.value.trim(),
                lineLength = line.length
            )
        }

        // Rule 3: Western Chapter
        PATTERN_WESTERN_CHAPTER.find(trimmed)?.let { match ->
            return ChapterMatch(
                rawTitle = trimmed,
                cleanTitle = match.value.trim(),
                lineLength = line.length
            )
        }

        // Rule 4: Numbered list headings (e.g. "1. 启程")
        PATTERN_NUMBERED_HEADING.find(trimmed)?.let { match ->
            return ChapterMatch(
                rawTitle = trimmed,
                cleanTitle = match.value.trim(),
                lineLength = line.length
            )
        }

        return null
    }
}
