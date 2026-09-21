package com.example.leafreader.core.model

/**
 * Unified Table of Contents chapter representation.
 * Both TXT ChapterIndex and EPUB Readium TOC map into this model.
 */
data class Chapter(
    val id: String,
    val title: String,
    val orderIndex: Int,
    val locator: BookLocator,
    val characterCount: Int = 0
)
