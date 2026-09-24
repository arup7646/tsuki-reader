package com.alix.tsuki.data.model

enum class ReadingDirection(val label: String) {
    LTR("Left to Right"),
    RTL("Right to Left (Manga)"),
    VERTICAL("Vertical (Webtoon)")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class PageCacheLimit(val label: String, val maxBytes: Long) {
    SMALL("Small (100 MB)", 100L * 1024 * 1024),
    MEDIUM("Medium (250 MB)", 250L * 1024 * 1024),
    LARGE("Large (500 MB)", 500L * 1024 * 1024)
}
