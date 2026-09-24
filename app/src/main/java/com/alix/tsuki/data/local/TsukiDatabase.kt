package com.alix.tsuki.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.alix.tsuki.data.local.dao.ChapterDao
import com.alix.tsuki.data.local.dao.LibraryFolderDao
import com.alix.tsuki.data.local.dao.MangaDao
import com.alix.tsuki.data.local.entity.ChapterEntity
import com.alix.tsuki.data.local.entity.LibraryFolderEntity
import com.alix.tsuki.data.local.entity.MangaEntity

@Database(
    entities = [
        MangaEntity::class,
        ChapterEntity::class,
        LibraryFolderEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TsukiDatabase : RoomDatabase() {
    abstract fun mangaDao(): MangaDao
    abstract fun chapterDao(): ChapterDao
    abstract fun libraryFolderDao(): LibraryFolderDao

    companion object {
        @Volatile
        private var INSTANCE: TsukiDatabase? = null

        fun getInstance(context: Context): TsukiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TsukiDatabase::class.java,
                    "tsuki_reader.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
