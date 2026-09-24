package com.alix.tsuki.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_folders")
data class LibraryFolderEntity(
    @PrimaryKey
    val treeUriString: String,
    val displayName: String,
    val dateAdded: Long = System.currentTimeMillis()
)
