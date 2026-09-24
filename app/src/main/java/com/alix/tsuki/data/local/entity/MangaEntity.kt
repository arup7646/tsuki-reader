package com.alix.tsuki.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.alix.tsuki.data.model.Manga

@Entity(tableName = "manga")
data class MangaEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val folderUriString: String,
    val parentTreeUri: String,
    val coverPath: String?,
    val chapterCount: Int,
    val lastReadChapterId: String?,
    val lastReadChapterTitle: String?,
    val lastReadPage: Int,
    val lastReadTimestamp: Long,
    val dateAdded: Long
) {
    fun toDomain(): Manga {
        return Manga(
            id = id,
            title = title,
            folderUriString = folderUriString,
            parentTreeUri = parentTreeUri,
            coverPath = coverPath,
            chapterCount = chapterCount,
            lastReadChapterId = lastReadChapterId,
            lastReadChapterTitle = lastReadChapterTitle,
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
                folderUriString = manga.folderUriString,
                parentTreeUri = manga.parentTreeUri,
                coverPath = manga.coverPath,
                chapterCount = manga.chapterCount,
                lastReadChapterId = manga.lastReadChapterId,
                lastReadChapterTitle = manga.lastReadChapterTitle,
                lastReadPage = manga.lastReadPage,
                lastReadTimestamp = manga.lastReadTimestamp,
                dateAdded = manga.dateAdded
            )
        }
    }
}
