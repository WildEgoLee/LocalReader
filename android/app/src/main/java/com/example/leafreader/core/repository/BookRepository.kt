package com.example.leafreader.core.repository

import android.content.Context
import android.net.Uri
import com.example.leafreader.core.database.BookDao
import com.example.leafreader.core.database.BookEntity
import com.example.leafreader.core.model.Book
import com.example.leafreader.core.model.BookFormat
import com.example.leafreader.core.model.BookLocator
import com.example.leafreader.core.storage.BookFileManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface BookRepository {
    fun observeAllBooks(): Flow<List<Book>>
    fun observeRecentBooks(): Flow<List<Book>>
    suspend fun getBookById(id: Long): Book?
    suspend fun insertBook(book: Book): Long
    suspend fun importBookFromUri(context: Context, uri: Uri): Result<Book>
    suspend fun updateReadingProgress(id: Long, progress: Float, locator: BookLocator?, lastChapter: String?)
    suspend fun deleteBook(id: Long)
    suspend fun seedInitialMockBooksIfEmpty()
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

    override fun getBookById(id: Long): Book? {
        return null
    }

    override suspend fun insertBook(book: Book): Long {
        return bookDao.insertBook(book.toEntity())
    }

    override suspend fun importBookFromUri(context: Context, uri: Uri): Result<Book> {
        val fileManager = BookFileManager(context)
        val importResult = fileManager.importFromUri(uri)

        return importResult.mapCatching { info ->
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
                currentLocatorJson = if (info.format == BookFormat.TXT) {
                    "{\"type\":\"TXT\",\"chapterId\":\"c1\",\"charOffset\":0}"
                } else {
                    "{\"type\":\"EPUB\",\"href\":\"\"}"
                },
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
    ) {
        val locatorJson = when (locator) {
            is BookLocator.TxtLocator -> "{\"type\":\"TXT\",\"chapterId\":\"${locator.chapterId}\",\"charOffset\":${locator.charOffset}}"
            is BookLocator.EpubLocator -> "{\"type\":\"EPUB\",\"href\":\"${locator.href}\"}"
            null -> null
        }
        bookDao.updateReadingProgress(id, progress, locatorJson, lastChapter)
    }

    override suspend fun deleteBook(id: Long) {
        bookDao.deleteBookById(id)
    }

    override suspend fun seedInitialMockBooksIfEmpty() {
        val mocks = listOf(
            BookEntity(
                id = 1,
                title = "诡秘之主",
                author = "爱潜水的乌贼",
                format = BookFormat.TXT,
                uri = "content://mock/lord_of_mysteries.txt",
                coverPath = null,
                fileSize = 14250000L,
                addedAt = System.currentTimeMillis() - 86400000L * 3,
                lastReadAt = System.currentTimeMillis() - 3600000L * 2,
                readingProgress = 0.67f,
                currentLocatorJson = "{\"type\":\"TXT\",\"chapterId\":\"c736\",\"charOffset\":1840}",
                lastChapterTitle = "第 736 章 门后的叹息"
            ),
            BookEntity(
                id = 2,
                title = "道诡异仙",
                author = "狐尾的笔",
                format = BookFormat.TXT,
                uri = "content://mock/dao_gui.txt",
                coverPath = null,
                fileSize = 9800000L,
                addedAt = System.currentTimeMillis() - 86400000L * 7,
                lastReadAt = System.currentTimeMillis() - 86400000L,
                readingProgress = 0.34f,
                currentLocatorJson = "{\"type\":\"TXT\",\"chapterId\":\"c120\",\"charOffset\":450}",
                lastChapterTitle = "第 120 章 迷惘之境"
            ),
            BookEntity(
                id = 3,
                title = "三体全集",
                author = "刘慈欣",
                format = BookFormat.EPUB,
                uri = "content://mock/three_body.epub",
                coverPath = null,
                fileSize = 3200000L,
                addedAt = System.currentTimeMillis() - 86400000L * 15,
                lastReadAt = System.currentTimeMillis() - 86400000L * 4,
                readingProgress = 0.89f,
                currentLocatorJson = "{\"type\":\"EPUB\",\"href\":\"part3_chapter12.xhtml\"}",
                lastChapterTitle = "第三部 死神永生 · 掩体纪元"
            ),
            BookEntity(
                id = 4,
                title = "雪中悍刀行",
                author = "烽火戏诸侯",
                format = BookFormat.TXT,
                uri = "content://mock/xue_zhong.txt",
                coverPath = null,
                fileSize = 16800000L,
                addedAt = System.currentTimeMillis() - 86400000L * 20,
                lastReadAt = null,
                readingProgress = 0.05f,
                currentLocatorJson = "{\"type\":\"TXT\",\"chapterId\":\"c12\",\"charOffset\":100}",
                lastChapterTitle = "第 12 章 凉刀出鞘"
            )
        )
        for (m in mocks) {
            bookDao.insertBook(m)
        }
    }
}

private fun BookEntity.toDomain(): Book {
    val locator: BookLocator? = currentLocatorJson?.let {
        if (it.contains("\"TXT\"")) {
            BookLocator.TxtLocator("c1", 0, 0, readingProgress)
        } else {
            BookLocator.EpubLocator("chapter1.xhtml", relativeProgress = readingProgress)
        }
    }
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
    val locatorJson = when (val loc = currentLocator) {
        is BookLocator.TxtLocator -> "{\"type\":\"TXT\",\"chapterId\":\"${loc.chapterId}\",\"charOffset\":${loc.charOffset}}"
        is BookLocator.EpubLocator -> "{\"type\":\"EPUB\",\"href\":\"${loc.href}\"}"
        null -> null
    }
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
