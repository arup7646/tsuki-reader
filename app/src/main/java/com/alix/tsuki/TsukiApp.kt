package com.alix.tsuki

import android.app.Application
import com.alix.tsuki.data.local.TsukiDatabase
import com.alix.tsuki.data.local.preferences.TsukiPreferences
import com.alix.tsuki.data.repository.MangaRepository
import com.alix.tsuki.data.storage.ArchiveReader
import com.alix.tsuki.data.storage.PageCacheManager
import com.alix.tsuki.data.storage.PdfRendererManager
import com.alix.tsuki.data.storage.SafScanner

class TsukiApp : Application() {

    lateinit var database: TsukiDatabase
        private set

    lateinit var preferences: TsukiPreferences
        private set

    lateinit var cacheManager: PageCacheManager
        private set

    lateinit var pdfManager: PdfRendererManager
        private set

    lateinit var archiveReader: ArchiveReader
        private set

    lateinit var safScanner: SafScanner
        private set

    lateinit var repository: MangaRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = TsukiDatabase.getInstance(this)
        preferences = TsukiPreferences(this)
        cacheManager = PageCacheManager(this)
        pdfManager = PdfRendererManager(this, cacheManager)
        archiveReader = ArchiveReader(this, cacheManager)
        safScanner = SafScanner(this, cacheManager)

        repository = MangaRepository(
            context = this,
            mangaDao = database.mangaDao(),
            chapterDao = database.chapterDao(),
            folderDao = database.libraryFolderDao(),
            cacheManager = cacheManager,
            safScanner = safScanner,
            pdfManager = pdfManager,
            archiveReader = archiveReader
        )
    }

    companion object {
        lateinit var instance: TsukiApp
            private set
    }
}
