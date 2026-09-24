package com.alix.tsuki.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.alix.tsuki.data.model.Manga
import com.alix.tsuki.data.model.MangaFormat

@Entity(tableName = "manga")
data class MangaEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val uriString: String,
    val parentFolderUri: String,
    val format: String,
    val coverPath: String?,
    val pageCount: Int,
    val lastReadPage: Int,
    val lastReadTimestamp: Long,
    val dateAdded: Long
) {
    fun toDomain(): Manga {
        return Manga(
            id = id,
            title = title,
            uriString = uriString,
            parentFolderUri = parentFolderUri,
            format = runCatching { MangaFormat.valueOf(format) }.getOrDefault(MangaFormat.CBZ),
            coverPath = coverPath,
            pageCount = pageCount,
            lastReadPage = lastReadPage,
            lastReadTimestamp = lastReadTimestamp,
            dateAdded = dateAdded
        )
    }

    companion object {
        fun fromDomain(manga: Manga): MangaEntity {
            return MangaEntity(
                id = manga.id,
                title = manga.title,
                uriString = manga.uriString,
                parentFolderUri = manga.parentFolderUri,
                format = manga.format.name,
                coverPath = manga.coverPath,
                pageCount = manga.pageCount,
                lastReadPage = manga.lastReadPage,
                lastReadTimestamp = manga.lastReadTimestamp,
                dateAdded = manga.dateAdded
            )
        }
    }
}
