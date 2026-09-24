package com.alix.tsuki.data.model

data class ReaderPage(
    val index: Int,
    val totalPages: Int,
    val displayName: String,
    val cachedFilePath: String? = null,
    val documentUri: String? = null,
    val archiveEntryName: String? = null,
    val isPdf: Boolean = false
)
