package com.example.leafreader.core.model

/**
 * Domain Book model.
 * Decoupled from Room entity and raw storage formats.
 */
data class Book(
    val id: Long,
    val title: String,
    val author: String?,
    val format: BookFormat,
    val uri: String,
    val coverPath: String? = null,
    val fileSize: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long? = null,
    val readingProgress: Float = 0f,
    val currentLocator: BookLocator? = null,
    val lastChapterTitle: String? = null
)
