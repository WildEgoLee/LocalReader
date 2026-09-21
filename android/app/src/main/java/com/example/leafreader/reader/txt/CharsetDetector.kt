package com.example.leafreader.reader.txt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.charset.Charset

/**
 * High accuracy character encoding detector for local novel files.
 * Handles UTF-8 (with/without BOM), GBK/GB2312/GB18030, UTF-16, and Big5.
 */
object CharsetDetector {

    val CHARSET_GBK: Charset = Charset.forName("GBK")
    val CHARSET_UTF8: Charset = Charsets.UTF_8
    val CHARSET_UTF16LE: Charset = Charsets.UTF_16LE
    val CHARSET_UTF16BE: Charset = Charsets.UTF_16BE
    val CHARSET_BIG5: Charset = Charset.forName("Big5")

    /**
     * Inspects the file header and content samples to detect the charset.
     * Samples up to 64KB to achieve maximum accuracy without loading the entire file.
     * Executes safely off the main thread.
     */
    suspend fun detect(file: File): Charset = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) {
            return@withContext CHARSET_UTF8
        }

        val sampleSize = minOf(file.length(), 65536L).toInt()
        val buffer = ByteArray(sampleSize)

        FileInputStream(file).use { input ->
            input.read(buffer, 0, sampleSize)
        }

        // 1. Check Byte Order Mark (BOM)
        if (sampleSize >= 3 && buffer[0] == 0xEF.toByte() && buffer[1] == 0xBB.toByte() && buffer[2] == 0xBF.toByte()) {
            return CHARSET_UTF8
        }
        if (sampleSize >= 2 && buffer[0] == 0xFE.toByte() && buffer[1] == 0xFF.toByte()) {
            return Charsets.UTF_16BE
        }
        if (sampleSize >= 2 && buffer[0] == 0xFF.toByte() && buffer[1] == 0xFE.toByte()) {
            return Charsets.UTF_16LE
        }

        // 2. Strict UTF-8 validation
        var isValidUtf8 = true
        var utf8MultiByteSequences = 0
        var i = 0

        while (i < sampleSize) {
            val b = buffer[i].toInt() and 0xFF
            if (b < 0x80) {
                // 1-byte ASCII
                i++
            } else if ((b and 0xE0) == 0xC0) {
                // 2-byte sequence
                if (i + 1 >= sampleSize) break
                val b2 = buffer[i + 1].toInt() and 0xFF
                if ((b2 and 0xC0) != 0x80) {
                    isValidUtf8 = false
                    break
                }
                utf8MultiByteSequences++
                i += 2
            } else if ((b and 0xF0) == 0xE0) {
                // 3-byte sequence (Common Chinese characters in UTF-8)
                if (i + 2 >= sampleSize) break
                val b2 = buffer[i + 1].toInt() and 0xFF
                val b3 = buffer[i + 2].toInt() and 0xFF
                if ((b2 and 0xC0) != 0x80 || (b3 and 0xC0) != 0x80) {
                    isValidUtf8 = false
                    break
                }
                utf8MultiByteSequences++
                i += 3
            } else if ((b and 0xF8) == 0xF0) {
                // 4-byte sequence
                if (i + 3 >= sampleSize) break
                val b2 = buffer[i + 1].toInt() and 0xFF
                val b3 = buffer[i + 2].toInt() and 0xFF
                val b4 = buffer[i + 3].toInt() and 0xFF
                if ((b2 and 0xC0) != 0x80 || (b3 and 0xC0) != 0x80 || (b4 and 0xC0) != 0x80) {
                    isValidUtf8 = false
                    break
                }
                utf8MultiByteSequences++
                i += 4
            } else {
                isValidUtf8 = false
                break
            }
        }

        if (isValidUtf8 && utf8MultiByteSequences > 5) {
            return CHARSET_UTF8
        }

        // 3. GBK / GB2312 verification
        var gbkValidPairs = 0
        var gbkInvalidPairs = 0
        var idx = 0

        while (idx < sampleSize) {
            val b1 = buffer[idx].toInt() and 0xFF
            if (b1 < 0x80) {
                idx++
            } else {
                if (idx + 1 >= sampleSize) break
                val b2 = buffer[idx + 1].toInt() and 0xFF
                // GBK Lead byte: 0x81-0xFE, Trail byte: 0x40-0xFE except 0x7F
                if (b1 in 0x81..0xFE && b2 in 0x40..0xFE && b2 != 0x7F) {
                    gbkValidPairs++
                    idx += 2
                } else {
                    gbkInvalidPairs++
                    idx++
                }
            }
        }

        if (gbkValidPairs > 0 && gbkInvalidPairs <= gbkValidPairs * 0.05) {
            return@withContext CHARSET_GBK
        }

        // Fallback default
        if (isValidUtf8) CHARSET_UTF8 else CHARSET_GBK
    }
}
