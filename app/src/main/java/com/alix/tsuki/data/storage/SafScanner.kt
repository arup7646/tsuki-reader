package com.alix.tsuki.data.storage

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import com.alix.tsuki.data.model.Chapter
import com.alix.tsuki.data.model.Manga
import com.alix.tsuki.data.model.MangaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SafScanner(
    private val context: Context,
    private val cacheManager: PageCacheManager
) {

    private val contentResolver: ContentResolver
        get() = context.contentResolver

    data class SafDoc(
        val documentId: String,
        val displayName: String,
        val mimeType: String,
        val lastModified: Long,
        val size: Long,
        val uri: Uri
    ) {
        val isDirectory: Boolean
            get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
    }

    data class ScanResult(
        val mangaList: List<Manga>,
        val chaptersList: List<Chapter>
    )

    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp")

    private fun isImage(doc: SafDoc): Boolean {
        if (doc.mimeType.startsWith("image/")) return true
        val ext = doc.displayName.substringAfterLast('.', "").lowercase()
        return ext in imageExtensions
    }

    private fun isCoverImage(name: String): Boolean {
        val lower = name.lowercase()
        return lower.startsWith("cover") || lower.startsWith("poster") || lower.startsWith("folder")
    }

    /**
     * Ultra-fast folder scan using batch DocumentsContract cursor queries only.
     * Does NOT open files or decode PDFs during the scan loop.
     */
    suspend fun scanTree(treeUri: Uri): ScanResult = withContext(Dispatchers.IO) {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val mangaResult = mutableListOf<Manga>()
        val chaptersResult = mutableListOf<Chapter>()

        val rootChildren = queryChildren(treeUri, rootDocId)
        val rootComicFiles = rootChildren.filter { !it.isDirectory && MangaFormat.fromFileName(it.displayName) != null }
        val rootDirs = rootChildren.filter { it.isDirectory }

        // CASE 1: The selected folder directly contains comic files (e.g. user picked "Chainsaw Man" folder)
        if (rootComicFiles.isNotEmpty()) {
            val rootTitle = getFolderDisplayName(treeUri, rootDocId) ?: "My Comics"
            val mangaId = treeUri.toString()

            val sortedFiles = rootComicFiles.sortedWith(compareBy(naturalOrderComparator) { it.displayName })
            val chapters = sortedFiles.mapIndexed { index, file ->
                val format = MangaFormat.fromFileName(file.displayName) ?: MangaFormat.PDF
                Chapter(
                    id = file.uri.toString(),
                    mangaId = mangaId,
                    title = file.displayName.substringBeforeLast('.'),
                    uriString = file.uri.toString(),
                    format = format,
                    orderIndex = index,
                    pageCount = 0,
                    lastReadPage = 0,
                    lastReadTimestamp = 0L
                )
            }

            mangaResult.add(
                Manga(
                    id = mangaId,
                    title = rootTitle,
                    folderUriString = treeUri.toString(),
                    parentTreeUri = treeUri.toString(),
                    coverPath = null,
                    chapterCount = chapters.size,
                    lastReadChapterId = null,
                    lastReadChapterTitle = null,
                    lastReadPage = 0,
                    lastReadTimestamp = 0L,
                    dateAdded = System.currentTimeMillis()
                )
            )
            chaptersResult.addAll(chapters)
        }

        // CASE 2: The selected folder contains subfolders (e.g. MangaLibrary/ containing Manga1/, Manga2/...)
        for (subDir in rootDirs) {
            val subChildren = queryChildren(treeUri, subDir.documentId)
            val subComicFiles = subChildren.filter { !it.isDirectory && MangaFormat.fromFileName(it.displayName) != null }
            val subImageFiles = subChildren.filter { !it.isDirectory && isImage(it) }
            val nestedDirs = subChildren.filter { it.isDirectory }

            val mangaId = subDir.uri.toString()
            val mangaTitle = subDir.displayName

            if (subComicFiles.isNotEmpty()) {
                // Subfolder contains comic files (PDF/CBZ/CBR)
                val sortedFiles = subComicFiles.sortedWith(compareBy(naturalOrderComparator) { it.displayName })
                val chapters = sortedFiles.mapIndexed { index, file ->
                    val format = MangaFormat.fromFileName(file.displayName) ?: MangaFormat.PDF
                    Chapter(
                        id = file.uri.toString(),
                        mangaId = mangaId,
                        title = file.displayName.substringBeforeLast('.'),
                        uriString = file.uri.toString(),
                        format = format,
                        orderIndex = index,
                        pageCount = 0,
                        lastReadPage = 0,
                        lastReadTimestamp = 0L
                    )
                }

                mangaResult.add(
                    Manga(
                        id = mangaId,
                        title = mangaTitle,
                        folderUriString = subDir.uri.toString(),
                        parentTreeUri = treeUri.toString(),
                        coverPath = null,
                        chapterCount = chapters.size,
                        lastReadChapterId = null,
                        lastReadChapterTitle = null,
                        lastReadPage = 0,
                        lastReadTimestamp = 0L,
                        dateAdded = System.currentTimeMillis()
                    )
                )
                chaptersResult.addAll(chapters)
            } else if (subImageFiles.isNotEmpty()) {
                // Subfolder contains loose images directly -> Treated as a single-chapter Manga
                val chapter = Chapter(
                    id = subDir.uri.toString(),
                    mangaId = mangaId,
                    title = mangaTitle,
                    uriString = subDir.uri.toString(),
                    format = MangaFormat.FOLDER,
                    orderIndex = 0,
                    pageCount = subImageFiles.size,
                    lastReadPage = 0,
                    lastReadTimestamp = 0L
                )

                mangaResult.add(
                    Manga(
                        id = mangaId,
                        title = mangaTitle,
                        folderUriString = subDir.uri.toString(),
                        parentTreeUri = treeUri.toString(),
                        coverPath = null,
                        chapterCount = 1,
                        lastReadChapterId = null,
                        lastReadChapterTitle = null,
                        lastReadPage = 0,
                        lastReadTimestamp = 0L,
                        dateAdded = System.currentTimeMillis()
                    )
                )
                chaptersResult.add(chapter)
            } else if (nestedDirs.isNotEmpty()) {
                // Check 1 level deeper (e.g. Author/Series/...)
                for (nested in nestedDirs) {
                    val nestedChildren = queryChildren(treeUri, nested.documentId)
                    val nestedComics = nestedChildren.filter { !it.isDirectory && MangaFormat.fromFileName(it.displayName) != null }
                    if (nestedComics.isNotEmpty()) {
                        val nId = nested.uri.toString()
                        val nTitle = nested.displayName
                        val sortedFiles = nestedComics.sortedWith(compareBy(naturalOrderComparator) { it.displayName })
                        val chapters = sortedFiles.mapIndexed { index, file ->
                            val format = MangaFormat.fromFileName(file.displayName) ?: MangaFormat.PDF
                            Chapter(
                                id = file.uri.toString(),
                                mangaId = nId,
                                title = file.displayName.substringBeforeLast('.'),
                                uriString = file.uri.toString(),
                                format = format,
                                orderIndex = index,
                                pageCount = 0,
                                lastReadPage = 0,
                                lastReadTimestamp = 0L
                            )
                        }

                        mangaResult.add(
                            Manga(
                                id = nId,
                                title = nTitle,
                                folderUriString = nested.uri.toString(),
                                parentTreeUri = treeUri.toString(),
                                coverPath = null,
                                chapterCount = chapters.size,
                                lastReadChapterId = null,
                                lastReadChapterTitle = null,
                                lastReadPage = 0,
                                lastReadTimestamp = 0L,
                                dateAdded = System.currentTimeMillis()
                            )
                        )
                        chaptersResult.addAll(chapters)
                    }
                }
            }
        }

        ScanResult(mangaResult, chaptersResult)
    }

    /**
     * Batch cursor query for child documents under a tree.
     */
    fun queryChildren(treeUri: Uri, parentDocId: String): List<SafDoc> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE
        )

        val list = mutableListOf<SafDoc>()
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(childrenUri, projection, null, null, null)
            if (cursor != null) {
                val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val modIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val sizeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idIdx)
                    val name = cursor.getString(nameIdx) ?: "Unknown"
                    val mime = cursor.getString(mimeIdx) ?: ""
                    val mod = cursor.getLong(modIdx)
                    val size = cursor.getLong(sizeIdx)
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)

                    list.add(
                        SafDoc(
                            documentId = docId,
                            displayName = name,
                            mimeType = mime,
                            lastModified = mod,
                            size = size,
                            uri = docUri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return list
    }

    private fun getFolderDisplayName(treeUri: Uri, docId: String): String? {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return try {
            contentResolver.query(docUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            } ?: docId.substringAfterLast(':', "Comics")
        } catch (e: Exception) {
            docId.substringAfterLast(':', "Comics")
        }
    }

    val naturalOrderComparator = Comparator<String> { s1, s2 ->
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
}
