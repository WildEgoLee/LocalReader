package com.example.leafreader.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.leafreader.core.model.BookFormat

/**
 * Room database entity for stored book metadata.
 * Note: Book text is never stored in SQLite directly; only metadata,
 * indexes, and locator markers are recorded.
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String?,
    val format: BookFormat,
    val uri: String,
    val coverPath: String?,
    val fileSize: Long,
    val addedAt: Long,
    val lastReadAt: Long?,
    val readingProgress: Float,
    val currentLocatorJson: String?,
    val lastChapterTitle: String?
)
