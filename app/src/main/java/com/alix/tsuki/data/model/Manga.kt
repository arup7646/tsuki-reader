package com.alix.tsuki.data.model

data class Manga(
    val id: String,
    val title: String,
    val folderUriString: String,
    val parentTreeUri: String,
    val coverPath: String? = null,
    val chapterCount: Int = 0,
    val lastReadChapterId: String? = null,
    val lastReadChapterTitle: String? = null,
    val lastReadPage: Int = 0,
    val lastReadTimestamp: Long = 0L,
    val dateAdded: Long = System.currentTimeMillis()
)
