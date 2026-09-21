package com.example.leafreader.core.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.leafreader.core.model.BookFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Storage and file manager for local books.
 * Handles SAF content copying, private sandbox caching, and metadata extraction.
 */
class BookFileManager(private val context: Context) {

    private val booksDirectory: File by lazy {
        File(context.filesDir, "books").apply {
            if (!exists()) mkdirs()
        }
    }

    private val indexesDirectory: File by lazy {
        File(context.filesDir, "indexes").apply {
            if (!exists()) mkdirs()
        }
    }

    data class ImportedFileInfo(
        val originalFileName: String,
        val cleanTitle: String,
        val format: BookFormat,
        val localFilePath: String,
        val fileSize: Long,
        val sha256Fingerprint: String
    )

    /**
     * Copies a book from a SAF Uri into app internal private storage.
     * Ensures offline availability even if original file is deleted or SAF permissions expire.
     */
    suspend fun importFromUri(uri: Uri): Result<ImportedFileInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = queryFileName(uri) ?: "未知书目_${System.currentTimeMillis()}"
            val format = detectFormat(fileName)
            val cleanTitle = sanitizeTitle(fileName)

            val targetFile = File(booksDirectory, "${System.currentTimeMillis()}_${cleanFileName(fileName)}")
            val digest = MessageDigest.getInstance("SHA-256")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        digest.update(buffer, 0, bytesRead)
                    }
                }
            } ?: throw IllegalStateException("无法打开文件流: $uri")

            val hashBytes = digest.digest()
            val fingerprint = hashBytes.joinToString("") { "%02x".format(it) }

            ImportedFileInfo(
                originalFileName = fileName,
                cleanTitle = cleanTitle,
                format = format,
                localFilePath = targetFile.absolutePath,
                fileSize = targetFile.length(),
                sha256Fingerprint = fingerprint
            )
        }
    }

    fun getIndexFile(bookId: Long): File {
        return File(indexesDirectory, "$bookId.idx.json")
    }

    suspend fun deleteBookFile(localFilePath: String) = withContext(Dispatchers.IO) {
        val file = File(localFilePath)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun queryFileName(uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        return uri.lastPathSegment
    }

    private fun detectFormat(fileName: String): BookFormat {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".epub") -> BookFormat.EPUB
            else -> BookFormat.TXT
        }
    }

    private fun sanitizeTitle(fileName: String): String {
        return fileName
            .replace(Regex("\\.(txt|epub)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[(精校版?|全本|全集|校对|精编|完本|TXT无错版?)\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\((精校版?|全本|全集|校对|精编|完本|TXT无错版?)\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[_]+"), " ")
            .trim()
    }

    private fun cleanFileName(fileName: String): String {
        return fileName.replace(Regex("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]"), "_")
    }
}
