package com.alix.tsuki.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.alix.tsuki.data.local.dao.ChapterDao
import com.alix.tsuki.data.local.dao.LibraryFolderDao
import com.alix.tsuki.data.local.dao.MangaDao
import com.alix.tsuki.data.local.entity.ChapterEntity
import com.alix.tsuki.data.local.entity.LibraryFolderEntity
import com.alix.tsuki.data.local.entity.MangaEntity
import com.alix.tsuki.data.model.Chapter
import com.alix.tsuki.data.model.Manga
import com.alix.tsuki.data.model.MangaFormat
import com.alix.tsuki.data.model.ReaderPage
import com.alix.tsuki.data.storage.ArchiveReader
import com.alix.tsuki.data.storage.PageCacheManager
import com.alix.tsuki.data.storage.PdfRendererManager
import com.alix.tsuki.data.storage.SafScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MangaRepository(
    private val context: Context,
    private val mangaDao: MangaDao,
    private val chapterDao: ChapterDao,
    private val folderDao: LibraryFolderDao,
    private val cacheManager: PageCacheManager,
    private val safScanner: SafScanner,
    private val pdfManager: PdfRendererManager,
    private val archiveReader: ArchiveReader,
    private val applicationScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
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

    fun getChapters(mangaId: String): Flow<List<Chapter>> {
        return chapterDao.getChaptersForManga(mangaId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun getChaptersSync(mangaId: String): List<Chapter> = withContext(Dispatchers.IO) {
        chapterDao.getChaptersForMangaSync(mangaId).map { it.toDomain() }
    }

    suspend fun getChapterById(chapterId: String): Chapter? = withContext(Dispatchers.IO) {
        chapterDao.getChapterById(chapterId)?.toDomain()
    }

    suspend fun addFolder(treeUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
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

            val scanResult = safScanner.scanTree(treeUri)
            mangaDao.insertAll(scanResult.mangaList.map { MangaEntity.fromDomain(it) })
            chapterDao.insertAll(scanResult.chaptersList.map { ChapterEntity.fromDomain(it) })

            // Asynchronously generate covers in background so the UI is unblocked
            applicationScope.launch {
                extractCoversForMangaList(scanResult.mangaList, scanResult.chaptersList)
            }

            scanResult.mangaList.size
        }
    }

    suspend fun removeFolder(parentTreeUri: String) = withContext(Dispatchers.IO) {
        folderDao.delete(parentTreeUri)
        mangaDao.deleteMangaByParentFolder(parentTreeUri)
    }

    suspend fun deleteManga(mangaId: String) = withContext(Dispatchers.IO) {
        mangaDao.deleteMangaById(mangaId)
        chapterDao.deleteChaptersForManga(mangaId)
    }

    suspend fun rescanAll(): Unit = withContext(Dispatchers.IO) {
        val folders = folderDao.getFoldersSync()
        for (folder in folders) {
            val treeUri = Uri.parse(folder.treeUriString)
            val scanResult = safScanner.scanTree(treeUri)

            for (manga in scanResult.mangaList) {
                val existing = mangaDao.getMangaById(manga.id)
                val toSave = if (existing != null) {
                    manga.copy(
                        lastReadChapterId = existing.lastReadChapterId,
                        lastReadChapterTitle = existing.lastReadChapterTitle,
                        lastReadPage = existing.lastReadPage,
                        lastReadTimestamp = existing.lastReadTimestamp,
                        coverPath = existing.coverPath ?: manga.coverPath
                    )
                } else {
                    manga
                }
                mangaDao.insertOrUpdate(MangaEntity.fromDomain(toSave))
            }

            chapterDao.insertAll(scanResult.chaptersList.map { ChapterEntity.fromDomain(it) })

            applicationScope.launch {
                extractCoversForMangaList(scanResult.mangaList, scanResult.chaptersList)
            }
        }
    }

    suspend fun updateReadingProgress(
        mangaId: String,
        chapterId: String,
        chapterTitle: String,
        page: Int
    ) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        chapterDao.updateChapterProgress(chapterId, page, timestamp)
        mangaDao.updateReadingProgress(mangaId, chapterId, chapterTitle, page, timestamp)
    }

    suspend fun clearMangaProgress(mangaId: String) = withContext(Dispatchers.IO) {
        mangaDao.clearMangaProgress(mangaId)
    }

    suspend fun loadPagesForChapter(chapter: Chapter): List<ReaderPage> = withContext(Dispatchers.IO) {
        val uri = Uri.parse(chapter.uriString)
        val pages = mutableListOf<ReaderPage>()

        when (chapter.format) {
            MangaFormat.PDF -> {
                val count = pdfManager.getPageCount(uri)
                for (i in 0 until count) {
                    pages.add(
                        ReaderPage(
                            index = i,
                            totalPages = count,
                            displayName = "Page ${i + 1}",
                            documentUri = chapter.uriString,
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
                            documentUri = chapter.uriString
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
                            documentUri = chapter.uriString
                        )
                    )
                }
            }
            MangaFormat.FOLDER -> {
                val parentDocId = DocumentsContract.getDocumentId(uri)
                val treeUri = Uri.parse(chapter.uriString)
                val children = safScanner.queryChildren(treeUri, parentDocId)
                    .filter { !it.isDirectory && (it.mimeType.startsWith("image/") || it.displayName.lowercase().matches(Regex(".*\\.(jpg|jpeg|png|webp|bmp)$"))) }
                    .sortedWith(compareBy(safScanner.naturalOrderComparator) { it.displayName })

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

    suspend fun getPageFile(chapterId: String, page: ReaderPage): File? = withContext(Dispatchers.IO) {
        val cached = cacheManager.getCachedPageFile(chapterId, page.index)
        if (cached.exists() && cached.length() > 0) {
            return@withContext cached
        }

        val uri = Uri.parse(page.documentUri ?: return@withContext null)
        if (page.isPdf) {
            return@withContext pdfManager.renderPageToCache(chapterId, uri, page.index)
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
            // Loose image file from SAF
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

    private suspend fun extractCoversForMangaList(mangaList: List<Manga>, chaptersList: List<Chapter>) {
        val chapterMap = chaptersList.groupBy { it.mangaId }
        for (manga in mangaList) {
            val existing = mangaDao.getMangaById(manga.id)
            if (existing?.coverPath != null && File(existing.coverPath).exists()) {
                continue
            }

            val chapters = chapterMap[manga.id]?.sortedBy { it.orderIndex }
            val firstChapter = chapters?.firstOrNull() ?: continue
            val uri = Uri.parse(firstChapter.uriString)

            val coverPath = when (firstChapter.format) {
                MangaFormat.PDF -> pdfManager.extractCover(manga.id, uri)
                MangaFormat.CBZ -> archiveReader.extractCbzCover(manga.id, uri)
                MangaFormat.CBR -> archiveReader.extractCbrCover(manga.id, uri)
                MangaFormat.FOLDER -> {
                    val coverFile = cacheManager.getCoverFile(manga.id)
                    if (!coverFile.exists()) {
                        val treeUri = Uri.parse(firstChapter.uriString)
                        val docId = DocumentsContract.getDocumentId(treeUri)
                        val imgs = safScanner.queryChildren(treeUri, docId)
                            .filter { !it.isDirectory && it.mimeType.startsWith("image/") }
                            .sortedWith(compareBy(safScanner.naturalOrderComparator) { it.displayName })
                        imgs.firstOrNull()?.let { firstImg ->
                            try {
                                context.contentResolver.openInputStream(firstImg.uri)?.use { input ->
                                    FileOutputStream(coverFile).use { out -> input.copyTo(out) }
                                }
                            } catch (_: Exception) { }
                        }
                    }
                    if (coverFile.exists()) coverFile.absolutePath else null
                }
            }

            if (coverPath != null) {
                mangaDao.updateCover(manga.id, coverPath)
            }
        }
    }

    suspend fun clearPageCache() {
        cacheManager.clearPageCache()
    }

    suspend fun getCacheSizeBytes(): Long {
        return cacheManager.calculateCacheSizeBytes()
    }
}
