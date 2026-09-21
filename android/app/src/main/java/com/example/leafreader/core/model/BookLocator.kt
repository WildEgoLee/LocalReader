package com.example.leafreader.core.model

/**
 * Logical position representation decoupled from physical pagination.
 *
 * Screen dimensions, font sizing, line spacing, and device rotation change page
 * counts constantly; saving raw page numbers causes drift and broken states.
 * Locators store structural and logical character offsets instead.
 */
sealed interface BookLocator {
    val relativeProgress: Float

    /**
     * TXT logical locator based on chapter id and character offsets.
     */
    data class TxtLocator(
        val chapterId: String,
        val charOffset: Int,
        val paragraphIndex: Int = 0,
        override val relativeProgress: Float = 0f
    ) : BookLocator

    /**
     * EPUB locator compliant with Readium Publication/Locator specification.
     */
    data class EpubLocator(
        val href: String,
        val type: String = "application/xhtml+xml",
        val title: String? = null,
        val cfi: String? = null,
        override val relativeProgress: Float = 0f
    ) : BookLocator
}
