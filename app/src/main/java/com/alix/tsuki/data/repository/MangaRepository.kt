package com.alix.tsuki.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.alix.tsuki.data.local.dao.LibraryFolderDao
import com.alix.tsuki.data.local.dao.MangaDao
import com.alix.tsuki.data.local.entity.LibraryFolderEntity
import com.alix.tsuki.data.local.entity.MangaEntity
import com.alix.tsuki.data.model.Manga
import com.alix.tsuki.data.model.MangaFormat
import com.alix.tsuki.data.model.ReaderPage
import com.alix.tsuki.data.storage.ArchiveReader
import com.alix.tsuki.data.storage.PageCacheManager
import com.alix.tsuki.data.storage.PdfRendererManager
import com.alix.tsuki.data.storage.SafScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MangaRepository(
    private val context: Context,
    private val mangaDao: MangaDao,
    private val folderDao: LibraryFolderDao,
    private val cacheManager: PageCacheManager,
    private val safScanner: SafScanner,
    private val pdfManager: PdfRendererManager,
    private val archiveReader: ArchiveReader
) {

    fun getAllManga(): Flow<List<Manga>> {
        return mangaDao.getAllManga().map { list -> list.map { it.toDomain() } }
    }

    fun getRecentManga(limit: Int = 10): Flow<List<Manga>> {
        return mangaDao.getRecentManga(limit).map { list -> list.map { it.toDomain() } }
    }

    suspend fun getMangaById(id: String): Manga? = withContext(Dispatchers.IO) {
        mangaDao.getMangaById(id)?.toDomain()
    }

    suspend fun addFolder(treeUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            // Take persistable permission
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)

            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val displayName = rootDocId.substringAfterLast(':', "Library Folder")

            folderDao.insert(
                LibraryFolderEntity(
                    treeUriString = treeUri.toString(),
                    displayName = displayName,
                    dateAdded = System.currentTimeMillis()
                )
            )

            // Scan folder for manga
            val scannedManga = safScanner.scanTree(treeUri)
            mangaDao.insertAll(scannedManga.map { MangaEntity.fromDomain(it) })
            scannedManga.size
        }
    }

    suspend fun removeFolder(treeUriString: String) = withContext(Dispatchers.IO) {
        folderDao.delete(treeUriString)
        mangaDao.deleteMangaByParentFolder(treeUriString)
    }

    suspend fun deleteManga(mangaId: String) = withContext(Dispatchers.IO) {
        mangaDao.deleteMangaById(mangaId)
    }

    suspend fun rescanAll(): Unit = withContext(Dispatchers.IO) {
        val folders = folderDao.getFoldersSync()
        for (folder in folders) {
            val treeUri = Uri.parse(folder.treeUriString)
            val scanned = safScanner.scanTree(treeUri)

            for (manga in scanned) {
                val existing = mangaDao.getMangaById(manga.id)
                val toSave = if (existing != null) {
                    // Preserve reading progress
                    manga.copy(
                        lastReadPage = existing.lastReadPage,
                        lastReadTimestamp = existing.lastReadTimestamp,
                        coverPath = existing.coverPath ?: manga.coverPath
                    )
                } else {
                    manga
                }
                mangaDao.insertOrUpdate(MangaEntity.fromDomain(toSave))
            }
        }
    }

    suspend fun updateReadingProgress(mangaId: String, page: Int) = withContext(Dispatchers.IO) {
        mangaDao.updateReadingProgress(mangaId, page, System.currentTimeMillis())
    }

    suspend fun loadPages(manga: Manga): List<ReaderPage> = withContext(Dispatchers.IO) {
        val uri = Uri.parse(manga.uriString)
        val pages = mutableListOf<ReaderPage>()

        when (manga.format) {
            MangaFormat.PDF -> {
                val count = manga.pageCount.takeIf { it > 0 } ?: pdfManager.getPageCount(uri)
                for (i in 0 until count) {
                    pages.add(
                        ReaderPage(
                            index = i,
                            totalPages = count,
                            displayName = "Page ${i + 1}",
                            documentUri = manga.uriString,
                            isPdf = true
                        )
                    )
                }
            }
            MangaFormat.CBZ -> {
                val entries = archiveReader.getZipEntries(uri)
                val count = entries.size
                entries.forEachIndexed { i, entryName ->
                    pages.add(
                        ReaderPage(
                            index = i,
                            totalPages = count,
                            displayName = entryName.substringAfterLast('/'),
                            archiveEntryName = entryName,
                            documentUri = manga.uriString
                        )
                    )
                }
            }
            MangaFormat.CBR -> {
                val entries = archiveReader.getRarEntries(uri)
                val count = entries.size
                entries.forEachIndexed { i, entryName ->
                    pages.add(
                        ReaderPage(
                            index = i,
                            totalPages = count,
                            displayName = entryName.substringAfterLast('/'),
                            archiveEntryName = entryName,
                            documentUri = manga.uriString
                        )
                    )
                }
            }
            MangaFormat.FOLDER -> {
                val parentDocId = DocumentsContract.getDocumentId(uri)
                val treeUri = Uri.parse(manga.parentFolderUri)
                val children = safScanner.queryChildren(treeUri, parentDocId)
                    .filter { !it.isDirectory && (it.mimeType.startsWith("image/") || it.displayName.lowercase().matches(Regex(".*\\.(jpg|jpeg|png|webp|bmp)$"))) }
                    .sortedBy { it.displayName }

                val count = children.size
                children.forEachIndexed { i, child ->
                    pages.add(
                        ReaderPage(
                            index = i,
                            totalPages = count,
                            displayName = child.displayName,
                            documentUri = child.uri.toString()
                        )
                    )
                }
            }
        }
        pages
    }

    suspend fun getPageFile(mangaId: String, page: ReaderPage): File? = withContext(Dispatchers.IO) {
        val cached = cacheManager.getCachedPageFile(mangaId, page.index)
        if (cached.exists() && cached.length() > 0) {
            return@withContext cached
        }

        val uri = Uri.parse(page.documentUri ?: return@withContext null)
        if (page.isPdf) {
            return@withContext pdfManager.renderPageToCache(mangaId, uri, page.index)
        }

        if (page.archiveEntryName != null) {
            val format = MangaFormat.fromFileName(uri.toString())
            val extracted = if (format == MangaFormat.CBR) {
                archiveReader.extractRarEntry(uri, page.archiveEntryName, cached)
            } else {
                archiveReader.extractZipEntry(uri, page.archiveEntryName, cached)
            }
            if (extracted) return@withContext cached
        } else {
            // Loose image file from SAF folder
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cached).use { output ->
                        input.copyTo(output)
                    }
                }
                if (cached.exists()) return@withContext cached
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        null
    }

    suspend fun clearPageCache() {
        cacheManager.clearPageCache()
    }

    suspend fun getCacheSizeBytes(): Long {
        return cacheManager.calculateCacheSizeBytes()
    }
}
