# Tsuki Reader 🌙

A modern, fast, offline manga and comic reader for Android, built with Jetpack Compose (Material 3) and Room.

[![Build Debug APK](https://github.com/ayankandar2-ux/TsukiReader/actions/workflows/build.yml/badge.svg)](https://github.com/ayankandar2-ux/TsukiReader/actions/workflows/build.yml)

---

## 📱 App Identity & Highlights

- **App Name**: Tsuki Reader
- **Package ID**: `com.alix.tsuki`
- **Min SDK**: 21 (Android 5.0 Lollipop)
- **Target SDK**: 35 (Android 15)
- **100% Offline & Private**: Zero network permissions, no accounts, no analytics, no external tracking.
- **Storage Access Framework (SAF)**: Native folder picking without broad `MANAGE_EXTERNAL_STORAGE` permissions.

---

## 🌟 Supported Formats

| Format | Extension | Engine / Handler |
|---|---|---|
| **PDF** | `.pdf` | Native Android `PdfRenderer` API with high-quality JPEG-90 caching |
| **CBZ / ZIP** | `.cbz`, `.zip` | Fast streaming entry decoding via `java.util.zip` |
| **CBR / RAR** | `.cbr`, `.rar` | Pure Java RAR archive parsing via `junrar` |
| **Loose Image Folders** | Directory containing `.jpg`, `.jpeg`, `.png`, `.webp`, `.bmp` | Direct SAF folder tree scanning |

---

## 🎨 UI & Features

### 1. Library Screen (Home)
- **Fast Batch Folder Scan**: Queries directories via `DocumentsContract` cursor projection (`COLUMN_DOCUMENT_ID`, `COLUMN_DISPLAY_NAME`, `COLUMN_MIME_TYPE`, etc.), avoiding the slow multi-query overhead of `DocumentFile.listFiles()`.
- **Card Grid**: Material 3 cards showing extracted cover image, format badges (CBZ, CBR, PDF, DIR), page count, and reading progress bar.
- **Pull-to-Refresh**: Seamless pull-to-refresh to re-index all added folders.
- **Folder Management**: Long-press any manga card to remove the folder or item from the library.
- **Empty State**: Friendly lunar illustration and "Add Folder" button.

### 2. Reader Screen
- **Immersive Full-Screen**: Auto-hides system status and navigation bars for distraction-free reading. Tap center to toggle controls.
- **Dual Reading Modes**:
  - **Horizontal Page Mode**: Smooth swiping with LTR (Left-to-Right) and RTL (Right-to-Left / Japanese manga style).
  - **Vertical Continuous Mode**: Webtoon style continuous scrolling.
- **Edge Tapping**: Tap left 22% of screen to go to previous page; tap right 22% to advance.
- **Zoom Capabilities**: Double-tap to zoom (2.5x toggle) or fluid pinch-to-zoom + pan.
- **Fast PDF Rendering**: PDF pages are rendered at high resolution and cached as JPEG (quality 90) on disk.
- **Progress Memory**: Automatically saves and restores last-read page in Room database.

### 3. Recents Screen
- Displays the last 10 opened manga, sorted chronologically.
- Shows reading progress (`Page X of Y`), relative timestamp ("2 hours ago"), and progress bar.
- Tap to instantly resume from your exact last-read page.

### 4. Settings Screen
- **Reading Direction**: Choose LTR, RTL, or Vertical as default.
- **Page Cache Limit**: Configure cache size (Small 100MB, Medium 250MB, Large 500MB) with one-tap cache clearing.
- **Material You Theming**: Dynamic theming on Android 12+ (API 31+) with fallback Navy & Soft Purple night palette.
- **Theme Selection**: System Default, Light, or Dark.

---

## 🏗️ Architecture & Tech Stack

- **Architecture**: MVVM + Repository pattern with Clean Architecture principles.
- **UI Framework**: Jetpack Compose with Material 3 (`androidx.compose.material3`).
- **Navigation**: Navigation Compose with animated screen transitions.
- **Database**: Room Database (`androidx.room`) with Kotlin Coroutines Flow.
- **Image Pipeline**: Coil Compose (`io.coil-kt:coil-compose`) with SubcomposeAsyncImage.
- **PDF Engine**: Android `PdfRenderer`.
- **Archive Engine**: `java.util.zip` + `com.github.junrar:junrar`.

---

## 🚀 Building via GitHub Actions

This repository includes a pre-configured GitHub Actions workflow in [`.github/workflows/build.yml`](.github/workflows/build.yml).

Every commit or manual workflow dispatch triggers:
1. JDK 17 environment setup.
2. `./gradlew assembleDebug --stacktrace`.
3. Upload of `app-debug.apk` directly to GitHub Actions artifacts:
   `app/build/outputs/apk/debug/app-debug.apk`.
