package com.alix.tsuki.data.model

enum class MangaFormat(val displayName: String, val badgeText: String) {
    CBZ("Comic Book Zip", "CBZ"),
    CBR("Comic Book RAR", "CBR"),
    PDF("PDF Document", "PDF"),
    FOLDER("Image Folder", "DIR");

    companion object {
        fun fromExtension(ext: String): MangaFormat? {
            return when (ext.lowercase()) {
                "cbz", "zip" -> CBZ
                "cbr", "rar" -> CBR
                "pdf" -> PDF
                else -> null
            }
        }

        fun fromFileName(name: String): MangaFormat? {
            val ext = name.substringAfterLast('.', "")
            return fromExtension(ext)
        }

        fun fromMimeOrFileName(mimeType: String?, name: String): MangaFormat? {
            val ext = name.substringAfterLast('.', "")
            val fromExt = fromExtension(ext)
            if (fromExt != null) return fromExt

            val mime = mimeType?.lowercase() ?: return null
            return when {
                mime.contains("pdf") -> PDF
                mime.contains("zip") || mime.contains("cbz") -> CBZ
                mime.contains("rar") || mime.contains("cbr") || mime.contains("x-rar") -> CBR
                else -> null
            }
        }
    }
}
