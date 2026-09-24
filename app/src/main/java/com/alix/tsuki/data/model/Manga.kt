package com.alix.tsuki.data.model

data class Manga(
    val id: String,
    val title: String,
    val uriString: String,
    val parentFolderUri: String,
    val format: MangaFormat,
    val coverPath: String? = null,
    val pageCount: Int = 0,
    val lastReadPage: Int = 0,
    val lastReadTimestamp: Long = 0L,
    val dateAdded: Long = System.currentTimeMillis()
)
