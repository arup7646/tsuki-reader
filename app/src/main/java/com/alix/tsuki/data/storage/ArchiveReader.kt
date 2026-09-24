package com.alix.tsuki.data.storage

import android.content.Context
import android.net.Uri
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class ArchiveReader(
    private val context: Context,
    private val cacheManager: PageCacheManager
) {

    private val supportedImageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")

    private fun isImageFileName(name: String): Boolean {
        if (name.startsWith("__MACOSX") || name.startsWith(".")) return false
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in supportedImageExtensions
    }

    // Natural alphanumerical comparison (e.g. 1.jpg < 2.jpg < 10.jpg)
    private val naturalOrderComparator = Comparator<String> { s1, s2 ->
        val splitRegex = Regex("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)")
        val chunks1 = s1.split(splitRegex)
        val chunks2 = s2.split(splitRegex)
        val minSize = minOf(chunks1.size, chunks2.size)

        for (i in 0 until minSize) {
            val c1 = chunks1[i]
            val c2 = chunks2[i]
            val num1 = c1.toLongOrNull()
            val num2 = c2.toLongOrNull()

            val diff = if (num1 != null && num2 != null) {
                num1.compareTo(num2)
            } else {
                c1.compareTo(c2, ignoreCase = true)
            }
            if (diff != 0) return@Comparator diff
        }
        chunks1.size.compareTo(chunks2.size)
    }

    // ================= ZIP / CBZ =================

    suspend fun getZipEntries(uri: Uri): List<String> = withContext(Dispatchers.IO) {
        val entryNames = mutableListOf<String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory && isImageFileName(name)) {
                            entryNames.add(name)
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        entryNames.sortedWith(naturalOrderComparator)
    }

    suspend fun extractZipEntry(
        uri: Uri,
        entryName: String,
        targetFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == entryName) {
                            FileOutputStream(targetFile).use { out ->
                                zis.copyTo(out)
                            }
                            return@withContext true
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        false
    }

    // ================= RAR / CBR =================

    suspend fun getRarEntries(uri: Uri): List<String> = withContext(Dispatchers.IO) {
        val entryNames = mutableListOf<String>()
        val tempFile = copyUriToTempFile(uri) ?: return@withContext emptyList()
        try {
            Archive(tempFile).use { archive ->
                for (header in archive.fileHeaders) {
                    val name = header.fileName
                    if (!header.isDirectory && isImageFileName(name)) {
                        entryNames.add(name)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            tempFile.delete()
        }
        entryNames.sortedWith(naturalOrderComparator)
    }

    suspend fun extractRarEntry(
        uri: Uri,
        entryName: String,
        targetFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val tempFile = copyUriToTempFile(uri) ?: return@withContext false
        try {
            Archive(tempFile).use { archive ->
                for (header in archive.fileHeaders) {
                    if (header.fileName == entryName) {
                        FileOutputStream(targetFile).use { out ->
                            archive.extractFile(header, out)
                        }
                        return@withContext true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            tempFile.delete()
        }
        false
    }

    // ================= COVER EXTRACTION =================

    suspend fun extractCbzCover(mangaId: String, uri: Uri): String? = withContext(Dispatchers.IO) {
        val coverFile = cacheManager.getCoverFile(mangaId)
        if (coverFile.exists() && coverFile.length() > 0) return@withContext coverFile.absolutePath

        val entries = getZipEntries(uri)
        if (entries.isNotEmpty()) {
            if (extractZipEntry(uri, entries.first(), coverFile)) {
                return@withContext coverFile.absolutePath
            }
        }
        null
    }

    suspend fun extractCbrCover(mangaId: String, uri: Uri): String? = withContext(Dispatchers.IO) {
        val coverFile = cacheManager.getCoverFile(mangaId)
        if (coverFile.exists() && coverFile.length() > 0) return@withContext coverFile.absolutePath

        val entries = getRarEntries(uri)
        if (entries.isNotEmpty()) {
            if (extractRarEntry(uri, entries.first(), coverFile)) {
                return@withContext coverFile.absolutePath
            }
        }
        null
    }

    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val temp = File.createTempFile("rar_tmp_", ".cbr", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temp).use { output ->
                    input.copyTo(output)
                }
            }
            temp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
