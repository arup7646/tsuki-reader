package com.alix.tsuki.data.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PageCacheManager(private val context: Context) {

    private val coversDir: File
        get() = File(context.cacheDir, "covers").apply { if (!exists()) mkdirs() }

    private val pagesDir: File
        get() = File(context.cacheDir, "pages").apply { if (!exists()) mkdirs() }

    fun getCoverFile(mangaId: String): File {
        val sanitized = mangaId.hashCode().toString()
        return File(coversDir, "$sanitized.jpg")
    }

    fun getMangaPageDir(mangaId: String): File {
        val sanitized = mangaId.hashCode().toString()
        return File(pagesDir, sanitized).apply { if (!exists()) mkdirs() }
    }

    fun getCachedPageFile(mangaId: String, pageIndex: Int): File {
        return File(getMangaPageDir(mangaId), "page_%04d.jpg".format(pageIndex))
    }

    suspend fun calculateCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        val coversSize = coversDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val pagesSize = pagesDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        coversSize + pagesSize
    }

    suspend fun clearPageCache(): Unit = withContext(Dispatchers.IO) {
        pagesDir.deleteRecursively()
        pagesDir.mkdirs()
    }

    suspend fun clearAllCache(): Unit = withContext(Dispatchers.IO) {
        coversDir.deleteRecursively()
        pagesDir.deleteRecursively()
        coversDir.mkdirs()
        pagesDir.mkdirs()
    }

    suspend fun pruneIfNeeded(maxBytes: Long) = withContext(Dispatchers.IO) {
        val currentSize = calculateCacheSizeBytes()
        if (currentSize <= maxBytes) return@withContext

        // Delete oldest accessed files in pagesDir until within 80% limit
        val targetSize = (maxBytes * 0.8).toLong()
        val allFiles = pagesDir.walkTopDown().filter { it.isFile }.sortedBy { it.lastModified() }.toList()

        var accumulated = currentSize
        for (file in allFiles) {
            if (accumulated <= targetSize) break
            val length = file.length()
            if (file.delete()) {
                accumulated -= length
            }
        }
    }
}
