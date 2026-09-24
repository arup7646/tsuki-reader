package com.alix.tsuki.data.storage

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import com.alix.tsuki.data.model.Manga
import com.alix.tsuki.data.model.MangaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class SafScanner(
    private val context: Context,
    private val cacheManager: PageCacheManager,
    private val pdfManager: PdfRendererManager,
    private val archiveReader: ArchiveReader
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

    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp")

    private fun isImage(doc: SafDoc): Boolean {
        if (doc.mimeType.startsWith("image/")) return true
        val ext = doc.displayName.substringAfterLast('.', "").lowercase()
        return ext in imageExtensions
    }

    suspend fun scanTree(treeUri: Uri): List<Manga> = withContext(Dispatchers.IO) {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val mangaList = mutableListOf<Manga>()

        scanDirectory(treeUri, rootDocId, mangaList, depth = 0)
        mangaList
    }

    private suspend fun scanDirectory(
        treeUri: Uri,
        parentDocId: String,
        results: MutableList<Manga>,
        depth: Int
    ) {
        if (depth > 4) return // Guard against excessive recursion

        val children = queryChildren(treeUri, parentDocId)
        val imageChildren = children.filter { !it.isDirectory && isImage(it) }
        val dirChildren = children.filter { it.isDirectory }
        val fileChildren = children.filter { !it.isDirectory }

        // 1. Check if this folder itself is a loose image folder (contains images)
        if (imageChildren.isNotEmpty()) {
            val folderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocId)
            val mangaId = folderUri.toString()
            val folderTitle = getDocumentDisplayName(treeUri, parentDocId) ?: "Untitled Comic"

            // Extract cover (first image)
            val sortedImages = imageChildren.sortedWith(compareBy(naturalOrderComparator) { it.displayName })
            val coverPath = extractFolderCover(mangaId, sortedImages.first().uri)

            results.add(
                Manga(
                    id = mangaId,
                    title = folderTitle,
                    uriString = folderUri.toString(),
                    parentFolderUri = treeUri.toString(),
                    format = MangaFormat.FOLDER,
                    coverPath = coverPath,
                    pageCount = sortedImages.size,
                    lastReadPage = 0,
                    lastReadTimestamp = 0L,
                    dateAdded = System.currentTimeMillis()
                )
            )
        }

        // 2. Scan archive / PDF files in this folder
        for (file in fileChildren) {
            val format = MangaFormat.fromFileName(file.displayName) ?: continue
            val docUri = file.uri
            val mangaId = docUri.toString()
            val title = file.displayName.substringBeforeLast('.')

            var pageCount = 0
            var coverPath: String? = null

            try {
                when (format) {
                    MangaFormat.PDF -> {
                        pageCount = pdfManager.getPageCount(docUri)
                        coverPath = pdfManager.extractCover(mangaId, docUri)
                    }
                    MangaFormat.CBZ -> {
                        val entries = archiveReader.getZipEntries(docUri)
                        pageCount = entries.size
                        coverPath = archiveReader.extractCbzCover(mangaId, docUri)
                    }
                    MangaFormat.CBR -> {
                        val entries = archiveReader.getRarEntries(docUri)
                        pageCount = entries.size
                        coverPath = archiveReader.extractCbrCover(mangaId, docUri)
                    }
                    MangaFormat.FOLDER -> Unit
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            results.add(
                Manga(
                    id = mangaId,
                    title = title,
                    uriString = docUri.toString(),
                    parentFolderUri = treeUri.toString(),
                    format = format,
                    coverPath = coverPath,
                    pageCount = pageCount,
                    lastReadPage = 0,
                    lastReadTimestamp = 0L,
                    dateAdded = System.currentTimeMillis()
                )
            )
        }

        // 3. Scan subdirectories
        for (subDir in dirChildren) {
            scanDirectory(treeUri, subDir.documentId, results, depth + 1)
        }
    }

    /**
     * Fast batch query via DocumentsContract cursor.
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

    private fun getDocumentDisplayName(treeUri: Uri, docId: String): String? {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return try {
            contentResolver.query(docUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractFolderCover(mangaId: String, imageUri: Uri): String? {
        val coverFile = cacheManager.getCoverFile(mangaId)
        if (coverFile.exists() && coverFile.length() > 0) return coverFile.absolutePath

        return try {
            contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(coverFile).use { output ->
                    input.copyTo(output)
                }
            }
            coverFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

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
}
