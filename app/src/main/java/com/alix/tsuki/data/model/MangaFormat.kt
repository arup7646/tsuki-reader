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
    }
}
