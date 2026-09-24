package com.alix.tsuki.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alix.tsuki.data.local.entity.MangaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaDao {
    @Query("SELECT * FROM manga ORDER BY title ASC")
    fun getAllManga(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM manga WHERE id = :id LIMIT 1")
    suspend fun getMangaById(id: String): MangaEntity?

    @Query("SELECT * FROM manga WHERE lastReadTimestamp > 0 ORDER BY lastReadTimestamp DESC LIMIT :limit")
    fun getRecentManga(limit: Int = 10): Flow<List<MangaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(manga: MangaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mangaList: List<MangaEntity>)

    @Query("UPDATE manga SET lastReadChapterId = :chapterId, lastReadChapterTitle = :chapterTitle, lastReadPage = :page, lastReadTimestamp = :timestamp WHERE id = :mangaId")
    suspend fun updateReadingProgress(
        mangaId: String,
        chapterId: String,
        chapterTitle: String,
        page: Int,
        timestamp: Long
    )

    @Query("UPDATE manga SET coverPath = :coverPath WHERE id = :mangaId")
    suspend fun updateCover(mangaId: String, coverPath: String)

    @Query("DELETE FROM manga WHERE parentFolderUri = :parentFolderUri")
    suspend fun deleteMangaByParentFolder(parentTreeUri: String)

    @Query("DELETE FROM manga WHERE id = :id")
    suspend fun deleteMangaById(id: String)
}
