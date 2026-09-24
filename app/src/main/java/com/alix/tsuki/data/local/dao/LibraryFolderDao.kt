package com.alix.tsuki.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.alix.tsuki.data.local.entity.LibraryFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryFolderDao {
    @Query("SELECT * FROM library_folders ORDER BY dateAdded DESC")
    fun getAllFolders(): Flow<List<LibraryFolderEntity>>

    @Query("SELECT * FROM library_folders")
    suspend fun getFoldersSync(): List<LibraryFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: LibraryFolderEntity)

    @Query("DELETE FROM library_folders WHERE treeUriString = :treeUriString")
    suspend fun delete(treeUriString: String)
}
