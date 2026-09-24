package com.alix.tsuki.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfRendererManager(
    private val context: Context,
    private val cacheManager: PageCacheManager
) {

    suspend fun getPageCount(uri: Uri): Int = withContext(Dispatchers.IO) {
        openPdfRenderer(uri)?.use { renderer ->
            renderer.pageCount
        } ?: 0
    }

    suspend fun extractCover(mangaId: String, uri: Uri): String? = withContext(Dispatchers.IO) {
        val coverFile = cacheManager.getCoverFile(mangaId)
        if (coverFile.exists() && coverFile.length() > 0) {
            return@withContext coverFile.absolutePath
        }

        renderPage(mangaId, uri, pageIndex = 0, targetFile = coverFile)?.absolutePath
    }

    suspend fun renderPageToCache(
        mangaId: String,
        uri: Uri,
        pageIndex: Int
    ): File? = withContext(Dispatchers.IO) {
        val cachedFile = cacheManager.getCachedPageFile(mangaId, pageIndex)
        if (cachedFile.exists() && cachedFile.length() > 0) {
            return@withContext cachedFile
        }

        renderPage(mangaId, uri, pageIndex, cachedFile)
    }

    private fun renderPage(
        mangaId: String,
        uri: Uri,
        pageIndex: Int,
        targetFile: File
    ): File? {
        val renderer = openPdfRenderer(uri) ?: return null
        return try {
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null

            renderer.openPage(pageIndex).use { page ->
                // Calculate rendering dimensions with 2x scale for sharp text/drawings, capped at 2048px
                val baseWidth = page.width
                val baseHeight = page.height
                val targetWidth = (baseWidth * 2).coerceIn(720, 2048)
                val targetHeight = (targetWidth * baseHeight) / baseWidth

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                // Fill white background before rendering PDF
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                // Cache as JPEG-90 as specified in requirements
                FileOutputStream(targetFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    out.flush()
                }
                bitmap.recycle()
                targetFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                renderer.close()
            } catch (_: Exception) { }
        }
    }

    private fun openPdfRenderer(uri: Uri): PdfRenderer? {
        return try {
            val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                PdfRenderer(pfd)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
