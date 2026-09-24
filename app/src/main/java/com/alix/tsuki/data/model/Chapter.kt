package com.alix.tsuki.data.model

data class Chapter(
    val id: String,
    val mangaId: String,
    val title: String,
    val uriString: String,
    val format: MangaFormat,
    val pageCount: Int = 0,
    val lastReadPage: Int = 0,
    val orderIndex: Int = 0,
    val lastReadTimestamp: Long = 0L
)
