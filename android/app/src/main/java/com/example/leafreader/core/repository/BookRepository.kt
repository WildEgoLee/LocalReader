package com.example.leafreader.core.repository

import android.content.Context
import android.net.Uri
import com.example.leafreader.core.database.BookDao
import com.example.leafreader.core.database.BookEntity
import com.example.leafreader.core.model.Book
import com.example.leafreader.core.model.BookFormat
import com.example.leafreader.core.model.BookLocator
import com.example.leafreader.core.model.BookLocatorSerializer
import com.example.leafreader.core.storage.BookFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

interface BookRepository {
    fun observeAllBooks(): Flow<List<Book>>
    fun observeRecentBooks(): Flow<List<Book>>
    suspend fun getBookById(id: Long): Book?
    suspend fun insertBook(book: Book): Long
    suspend fun importBookFromUri(context: Context, uri: Uri): Result<Book>
    suspend fun updateReadingProgress(id: Long, progress: Float, locator: BookLocator?, lastChapter: String?)
    suspend fun deleteBook(id: Long)
}

class BookRepositoryImpl(
    private val bookDao: BookDao
) : BookRepository {

    override fun observeAllBooks(): Flow<List<Book>> {
        return bookDao.observeAllBooks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeRecentBooks(): Flow<List<Book>> {
        return bookDao.observeRecentBooks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBookById(id: Long): Book? = withContext(Dispatchers.IO) {
        bookDao.getBookById(id)?.toDomain()
    }

    override suspend fun insertBook(book: Book): Long = withContext(Dispatchers.IO) {
        bookDao.insertBook(book.toEntity())
    }

    override suspend fun importBookFromUri(context: Context, uri: Uri): Result<Book> = withContext(Dispatchers.IO) {
        val fileManager = BookFileManager(context)
        val importResult = fileManager.importFromUri(uri)

        importResult.mapCatching { info ->
            val initialLocator = if (info.format == BookFormat.TXT) {
                BookLocator.TxtLocator(chapterId = "c1", charOffset = 0, paragraphIndex = 0, relativeProgress = 0f)
            } else {
                BookLocator.EpubLocator(href = "", relativeProgress = 0f)
            }
            val initialLocatorJson = BookLocatorSerializer.serialize(initialLocator)

            val bookEntity = BookEntity(
                id = 0,
                title = info.cleanTitle,
                author = "本地导入",
                format = info.format,
                uri = "file://${info.localFilePath}",
                coverPath = null,
                fileSize = info.fileSize,
                addedAt = System.currentTimeMillis(),
                lastReadAt = null,
                readingProgress = 0f,
                currentLocatorJson = initialLocatorJson,
                lastChapterTitle = "第一章"
            )
            val generatedId = bookDao.insertBook(bookEntity)
            bookEntity.copy(id = generatedId).toDomain()
        }
    }

    override suspend fun updateReadingProgress(
        id: Long,
        progress: Float,
        locator: BookLocator?,
        lastChapter: String?
    ) = withContext(Dispatchers.IO) {
        val locatorJson = BookLocatorSerializer.serialize(locator)
        bookDao.updateReadingProgress(id, progress, locatorJson, lastChapter)
    }

    override suspend fun deleteBook(id: Long) = withContext(Dispatchers.IO) {
        val entity = bookDao.getBookById(id)
        if (entity != null && entity.uri.startsWith("file://")) {
            val path = entity.uri.removePrefix("file://")
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        bookDao.deleteBookById(id)
    }
}

private fun BookEntity.toDomain(): Book {
    val locator: BookLocator? = BookLocatorSerializer.deserialize(currentLocatorJson)
    return Book(
        id = id,
        title = title,
        author = author,
        format = format,
        uri = uri,
        coverPath = coverPath,
        fileSize = fileSize,
        addedAt = addedAt,
        lastReadAt = lastReadAt,
        readingProgress = readingProgress,
        currentLocator = locator,
        lastChapterTitle = lastChapterTitle
    )
}

private fun Book.toEntity(): BookEntity {
    val locatorJson = BookLocatorSerializer.serialize(currentLocator)
    return BookEntity(
        id = id,
        title = title,
        author = author,
        format = format,
        uri = uri,
        coverPath = coverPath,
        fileSize = fileSize,
        addedAt = addedAt,
        lastReadAt = lastReadAt,
        readingProgress = readingProgress,
        currentLocatorJson = locatorJson,
        lastChapterTitle = lastChapterTitle
    )
}
