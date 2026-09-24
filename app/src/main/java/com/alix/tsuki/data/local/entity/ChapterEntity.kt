package com.alix.tsuki.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.alix.tsuki.data.model.Chapter
import com.alix.tsuki.data.model.MangaFormat

@Entity(
    tableName = "chapters",
    indices = [Index(value = ["mangaId"])]
)
data class ChapterEntity(
    @PrimaryKey
    val id: String,
    val mangaId: String,
    val title: String,
    val uriString: String,
    val format: String,
    val pageCount: Int,
    val lastReadPage: Int,
    val orderIndex: Int,
    val lastReadTimestamp: Long
) {
    fun toDomain(): Chapter {
        return Chapter(
            id = id,
            mangaId = mangaId,
            title = title,
            uriString = uriString,
            format = runCatching { MangaFormat.valueOf(format) }.getOrDefault(MangaFormat.PDF),
            pageCount = pageCount,
            lastReadPage = lastReadPage,
            orderIndex = orderIndex,
            lastReadTimestamp = lastReadTimestamp
        )
    }

    companion object {
        fun fromDomain(chapter: Chapter): ChapterEntity {
            return ChapterEntity(
                id = chapter.id,
                mangaId = chapter.mangaId,
                title = chapter.title,
                uriString = chapter.uriString,
                format = chapter.format.name,
                pageCount = chapter.pageCount,
                lastReadPage = chapter.lastReadPage,
                orderIndex = chapter.orderIndex,
                lastReadTimestamp = chapter.lastReadTimestamp
            )
        }
    }
}
