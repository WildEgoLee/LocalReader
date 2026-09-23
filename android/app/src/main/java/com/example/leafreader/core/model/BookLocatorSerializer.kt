package com.example.leafreader.core.model

import org.json.JSONObject

/**
 * Reliable JSON serializer and deserializer for polymorphic BookLocator models.
 * Completely replaces ad-hoc string concatenation and regex parsing.
 */
object BookLocatorSerializer {

    private const val KEY_TYPE = "type"
    private const val TYPE_TXT = "TXT"
    private const val TYPE_EPUB = "EPUB"

    // TXT fields
    private const val KEY_CHAPTER_ID = "chapterId"
    private const val KEY_CHAR_OFFSET = "charOffset"
    private const val KEY_PARAGRAPH_INDEX = "paragraphIndex"

    // EPUB fields
    private const val KEY_HREF = "href"
    private const val KEY_MIME_TYPE = "mimeType"
    private const val KEY_TITLE = "title"
    private const val KEY_CFI = "cfi"

    // Common
    private const val KEY_RELATIVE_PROGRESS = "relativeProgress"

    fun serialize(locator: BookLocator?): String? {
        if (locator == null) return null
        val obj = JSONObject()
        when (locator) {
            is BookLocator.TxtLocator -> {
                obj.put(KEY_TYPE, TYPE_TXT)
                obj.put(KEY_CHAPTER_ID, locator.chapterId)
                obj.put(KEY_CHAR_OFFSET, locator.charOffset)
                obj.put(KEY_PARAGRAPH_INDEX, locator.paragraphIndex)
                obj.put(KEY_RELATIVE_PROGRESS, locator.relativeProgress.toDouble())
            }
            is BookLocator.EpubLocator -> {
                obj.put(KEY_TYPE, TYPE_EPUB)
                obj.put(KEY_HREF, locator.href)
                obj.put(KEY_MIME_TYPE, locator.type)
                locator.title?.let { obj.put(KEY_TITLE, it) }
                locator.cfi?.let { obj.put(KEY_CFI, it) }
                obj.put(KEY_RELATIVE_PROGRESS, locator.relativeProgress.toDouble())
            }
        }
        return obj.toString()
    }

    fun deserialize(json: String?): BookLocator? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val obj = JSONObject(json)
            when (obj.optString(KEY_TYPE)) {
                TYPE_TXT -> {
                    BookLocator.TxtLocator(
                        chapterId = obj.optString(KEY_CHAPTER_ID, ""),
                        charOffset = obj.optInt(KEY_CHAR_OFFSET, 0),
                        paragraphIndex = obj.optInt(KEY_PARAGRAPH_INDEX, 0),
                        relativeProgress = obj.optDouble(KEY_RELATIVE_PROGRESS, 0.0).toFloat()
                    )
                }
                TYPE_EPUB -> {
                    BookLocator.EpubLocator(
                        href = obj.optString(KEY_HREF, ""),
                        type = obj.optString(KEY_MIME_TYPE, "application/xhtml+xml"),
                        title = if (obj.has(KEY_TITLE)) obj.getString(KEY_TITLE) else null,
                        cfi = if (obj.has(KEY_CFI)) obj.getString(KEY_CFI) else null,
                        relativeProgress = obj.optDouble(KEY_RELATIVE_PROGRESS, 0.0).toFloat()
                    )
                }
                else -> null
            }
        }.getOrNull()
    }
}
