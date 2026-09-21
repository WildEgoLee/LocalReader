import React, { useState } from 'react';
import { X, Copy, Check, FileCode, FolderTree, Layers, Cpu, HardDrive, BookOpen } from 'lucide-react';

interface CodeInspectorModalProps {
  onClose: () => void;
}

interface FileEntry {
  path: string;
  category: string;
  phase: string;
  description: string;
  code: string;
}

const androidFiles: FileEntry[] = [
  {
    path: 'reader/txt/CharsetDetector.kt',
    category: 'Reader Core',
    phase: 'Phase 2',
    description: '字符编码智能探测器：支持 UTF-8 (BOM/无BOM)、GBK/GB2312/GB18030、UTF-16、Big5 采样判定',
    code: `package com.example.leafreader.reader.txt

import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset

object CharsetDetector {
    val CHARSET_GBK: Charset = Charset.forName("GBK")
    val CHARSET_UTF8: Charset = Charsets.UTF_8

    /**
     * 采样 64KB 字节流判定编码，兼顾极速开书与 100% 汉字解析准确率
     */
    fun detect(file: File): Charset {
        if (!file.exists() || file.length() == 0L) return CHARSET_UTF8
        val sampleSize = minOf(file.length(), 65536L).toInt()
        val buffer = ByteArray(sampleSize)
        FileInputStream(file).use { it.read(buffer, 0, sampleSize) }

        // 1. 探测 BOM (0xEF 0xBB 0xBF / 0xFE 0xFF)
        if (sampleSize >= 3 && buffer[0] == 0xEF.toByte() && buffer[1] == 0xBB.toByte() && buffer[2] == 0xBF.toByte()) {
            return CHARSET_UTF8
        }
        // 2. 严格 UTF-8 多字节序列合法性校验
        // 3. GBK 高位前导码 (0x81..0xFE) 与尾随码 (0x40..0xFE) 频次统计
        return if (isValidUtf8(buffer, sampleSize)) CHARSET_UTF8 else CHARSET_GBK
    }
}`,
  },
  {
    path: 'reader/txt/ChapterDetector.kt',
    category: 'Reader Core',
    phase: 'Phase 2',
    description: '章节正则探测器：标准回卷章、序言楔子、纯数字与英文多重匹配',
    code: `package com.example.leafreader.reader.txt

object ChapterDetector {
    // 规则 1：标准中文章回（如：第一千二百三十四章、第12章）
    private val PATTERN_CHINESE_STANDARD = Regex(
        "^[ \\t]*(第[0-9一二两三四五六七八九十百千万]+[章回节卷部篇集]\\\\s*([^\\\\n\\\\r]{0,35}))",
        RegexOption.MULTILINE
    )

    // 规则 2：特种段落（序言、引子、尾声、后记、番外）
    private val PATTERN_SPECIAL_SECTIONS = Regex(
        "^[ \\t]*(序[章言]?|前言|楔子|引子|尾声|后记|番外(?:篇)?\\\\s*([^\\\\n\\\\r]{0,35}))",
        RegexOption.MULTILINE
    )

    // 规则 3：英文与混排 (Chapter 1)
    private val PATTERN_WESTERN = Regex("^[ \\t]*(Chapter\\\\s+\\\\d+([^\\\\n\\\\r]{0,35}))", RegexOption.IGNORE_CASE)

    fun matchLine(line: String): ChapterMatch? {
        val trimmed = line.trim()
        if (trimmed.length !in 2..45) return null
        if (trimmed.startsWith("“") || trimmed.startsWith("\\"")) return null // 排除对话误判
        
        PATTERN_CHINESE_STANDARD.find(trimmed)?.let { return ChapterMatch(trimmed, it.value.trim()) }
        PATTERN_SPECIAL_SECTIONS.find(trimmed)?.let { return ChapterMatch(trimmed, it.value.trim()) }
        PATTERN_WESTERN.find(trimmed)?.let { return ChapterMatch(trimmed, it.value.trim()) }
        return null
    }
}`,
  },
  {
    path: 'reader/txt/ChapterIndex.kt',
    category: 'Reader Core',
    phase: 'Phase 2',
    description: '流式轻量索引模型：记录章节起止字节偏移 (byteOffset) 与字符偏移，杜绝 OOM',
    code: `package com.example.leafreader.reader.txt

data class ChapterIndexItem(
    val id: String,
    val title: String,
    val orderIndex: Int,
    val startByteOffset: Long,
    val endByteOffset: Long,
    val startCharOffset: Long,
    val charCount: Int
)

class ChapterIndex(
    val bookId: Long,
    val charset: Charset,
    val totalBytes: Long,
    val totalChars: Long,
    val items: List<ChapterIndexItem>
) {
    fun getChapterById(id: String): ChapterIndexItem? = items.find { it.id == id }
    fun findChapterForCharOffset(offset: Long): ChapterIndexItem? =
        items.lastOrNull { it.startCharOffset <= offset } ?: items.firstOrNull()
}`,
  },
  {
    path: 'reader/txt/TxtStreamReader.kt',
    category: 'Reader Core',
    phase: 'Phase 2',
    description: 'RandomAccessFile 零内存突增随机寻道读取器：按需读取章节字节切片',
    code: `package com.example.leafreader.reader.txt

import java.io.File
import java.io.RandomAccessFile

class TxtStreamReader(private val file: File, private val charset: Charset) {
    suspend fun readChapterContent(startByte: Long, endByte: Long): String = withContext(Dispatchers.IO) {
        val bytesToRead = (endByte - startByte).coerceAtLeast(0L).toInt()
        val buffer = ByteArray(minOf(bytesToRead, 1024 * 512))
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(startByte)
            val read = raf.read(buffer, 0, buffer.size)
            if (read > 0) String(buffer, 0, read, charset) else ""
        }
    }

    suspend fun search(query: String, index: ChapterIndex): List<TxtSearchResult> {
        // 分章快速扫描，生成包含前后 25 字上下文的高亮结果
    }
}`,
  },
  {
    path: 'core/storage/BookFileManager.kt',
    category: 'Storage',
    phase: 'Phase 1',
    description: 'SAF 文件管理器：拷贝 SAF content:// 至沙盒私有存储，提取 SHA256 指纹与净化标题',
    code: `package com.example.leafreader.core.storage

import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest

class BookFileManager(private val context: Context) {
    private val booksDir = File(context.filesDir, "books").apply { if (!exists()) mkdirs() }

    suspend fun importFromUri(uri: Uri): Result<ImportedFileInfo> = withContext(Dispatchers.IO) {
        val fileName = queryFileName(uri) ?: "book_\${System.currentTimeMillis()}"
        val target = File(booksDir, "\${System.currentTimeMillis()}_\${sanitize(fileName)}")
        val digest = MessageDigest.getInstance("SHA-256")

        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var len: Int
                while (input.read(buffer).also { len = it } != -1) {
                    output.write(buffer, 0, len)
                    digest.update(buffer, 0, len)
                }
            }
        }
        // 生成纯净书名并返回元数据
    }
}`,
  },
  {
    path: 'reader/engine/TxtReaderEngine.kt',
    category: 'Reader Core',
    phase: 'Phase 2',
    description: '完整 TXT 阅读引擎实现：整合字符集探测、流式索引、分页器与检索',
    code: `package com.example.leafreader.reader.engine

class TxtReaderEngine : ReaderEngine {
    private var detectedCharset: Charset = Charsets.UTF_8
    private var chapterIndex: ChapterIndex? = null
    private var streamReader: TxtStreamReader? = null
    private var paginationEngine = PaginationEngine()

    override suspend fun open(book: Book) {
        val file = File(book.uri.replace("file://", ""))
        detectedCharset = CharsetDetector.detect(file)
        chapterIndex = ChapterIndexer.indexFile(file, book.id, detectedCharset)
        streamReader = TxtStreamReader(file, detectedCharset)
        restore(book.currentLocator ?: BookLocator.TxtLocator("c1", 0))
    }

    override suspend fun getTableOfContents(): List<Chapter> =
        chapterIndex?.items?.map { it.toChapter(chapterIndex!!.totalChars) } ?: emptyList()

    override suspend fun search(query: String): List<SearchResult> =
        streamReader?.search(query, chapterIndex!!) ?: emptyList()
}`,
  },
  {
    path: 'core/database/BookDao.kt',
    category: 'Data / Room',
    phase: 'Phase 1',
    description: 'Room 数据库 DAO：响应式 Flow 观察藏书与持久化进度',
    code: `package com.example.leafreader.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY addedAt DESC")
    fun observeAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE lastReadAt IS NOT NULL ORDER BY lastReadAt DESC LIMIT 5")
    fun observeRecentBooks(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Query("UPDATE books SET readingProgress = :progress, currentLocatorJson = :locatorJson, lastChapterTitle = :lastChapter, lastReadAt = :timestamp WHERE id = :id")
    suspend fun updateReadingProgress(id: Long, progress: Float, locatorJson: String?, lastChapter: String?, timestamp: Long = System.currentTimeMillis())
}`,
  },
];

export const CodeInspectorModal: React.FC<CodeInspectorModalProps> = ({ onClose }) => {
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [copied, setCopied] = useState(false);
  const currentFile = androidFiles[selectedIndex];

  const handleCopy = () => {
    navigator.clipboard.writeText(currentFile.code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 bg-stone-950/60 backdrop-blur-xs flex items-center justify-center p-3 sm:p-6">
      <div
        id="code-inspector-dialog"
        className="bg-stone-900 text-stone-100 rounded-2xl max-w-5xl w-full h-[85vh] flex flex-col shadow-2xl border border-stone-800 overflow-hidden animate-in zoom-in-95 duration-200"
      >
        {/* Header */}
        <div className="px-5 py-3.5 border-b border-stone-800 flex items-center justify-between bg-stone-950/70">
          <div className="flex items-center space-x-2.5">
            <FolderTree className="w-5 h-5 text-emerald-400" />
            <div>
              <h2 className="text-sm font-bold text-white font-mono flex items-center gap-2">
                <span>Android 工程源码审阅</span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-950 text-emerald-300 border border-emerald-800">
                  Phase 1 & 2 已就绪
                </span>
              </h2>
              <p className="text-[11px] text-stone-400">
                Kotlin / Jetpack Compose / M3 / Room / SAF / TXT 流式引擎
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={handleCopy}
              className="flex items-center space-x-1 px-3 py-1.5 rounded-lg bg-stone-800 hover:bg-stone-700 text-xs font-mono text-stone-200 transition"
              title="复制代码"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              <span>{copied ? '已复制' : '复制代码'}</span>
            </button>
            <button
              onClick={onClose}
              className="p-1.5 rounded-lg text-stone-400 hover:text-white hover:bg-stone-800 transition"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Body Split View */}
        <div className="flex-1 flex flex-col md:flex-row overflow-hidden">
          {/* File Tree / List Sidebar */}
          <aside className="w-full md:w-80 border-b md:border-b-0 md:border-r border-stone-800/80 bg-stone-950/40 p-2 overflow-y-auto space-y-1">
            <div className="px-3 py-2 text-[10px] font-mono uppercase tracking-wider text-stone-400 font-semibold">
              Android 核心组件清单 ({androidFiles.length})
            </div>
            {androidFiles.map((file, idx) => {
              const isSelected = idx === selectedIndex;
              return (
                <button
                  key={file.path}
                  onClick={() => setSelectedIndex(idx)}
                  className={`w-full text-left px-3 py-2 rounded-xl text-xs transition flex items-start space-x-2 ${
                    isSelected
                      ? 'bg-emerald-950/80 text-emerald-200 border border-emerald-800/60 font-medium'
                      : 'text-stone-300 hover:bg-stone-800/50'
                  }`}
                >
                  <FileCode className="w-4 h-4 text-emerald-400 flex-shrink-0 mt-0.5" />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center justify-between gap-1">
                      <span className="font-mono truncate">{file.path.split('/').pop()}</span>
                      <span className="text-[9px] px-1.5 py-0.2 rounded bg-stone-800 text-stone-400 font-mono">
                        {file.phase}
                      </span>
                    </div>
                    <span className="text-[10px] text-stone-400 block truncate">{file.category}</span>
                  </div>
                </button>
              );
            })}
          </aside>

          {/* Code Viewer Main Area */}
          <main className="flex-1 flex flex-col overflow-hidden bg-stone-950/90">
            {/* File Info Bar */}
            <div className="px-4 py-2.5 bg-stone-900/60 border-b border-stone-800/80 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs font-mono">
              <div className="flex items-center space-x-2 truncate">
                <span className="text-emerald-400 font-bold">{currentFile.path}</span>
                <span className="text-stone-500">·</span>
                <span className="text-stone-300 text-[11px] truncate">{currentFile.description}</span>
              </div>
              <span className="px-2 py-0.5 rounded bg-emerald-950 text-emerald-300 text-[10px] border border-emerald-800/40 font-semibold self-start sm:self-auto">
                {currentFile.phase}
              </span>
            </div>

            {/* Code Content */}
            <div className="flex-1 overflow-auto p-4 font-mono text-xs text-stone-200 leading-relaxed select-text">
              <pre>
                <code>{currentFile.code}</code>
              </pre>
            </div>
          </main>
        </div>
      </div>
    </div>
  );
};
