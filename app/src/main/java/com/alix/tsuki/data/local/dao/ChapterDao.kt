package com.alix.tsuki.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alix.tsuki.data.local.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE mangaId = :mangaId ORDER BY orderIndex ASC")
    fun getChaptersForManga(mangaId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE mangaId = :mangaId ORDER BY orderIndex ASC")
    suspend fun getChaptersForMangaSync(mangaId: String): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    suspend fun getChapterById(id: String): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<ChapterEntity>)

    @Query("UPDATE chapters SET lastReadPage = :page, lastReadTimestamp = :timestamp WHERE id = :chapterId")
    suspend fun updateChapterProgress(chapterId: String, page: Int, timestamp: Long)

    @Query("DELETE FROM chapters WHERE mangaId = :mangaId")
    suspend fun deleteChaptersForManga(mangaId: String)
}
