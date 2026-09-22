# LinkGrab MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Goal
Implement the full LinkGrab MVP v1 per locked Q1–Q5 spec: messy-text/URL intake, metadata preview, video (best ≤1080p mp4) / audio (m4a default, mp3 option) downloads via yt-dlp with queue + foreground service, MediaStore saving, Room history, settings, two-layer auto-update (yt-dlp + app), friendly errors. Prove hard gate: `io.github.junkfood02.youtubedl-android:library:0.18.1` runs yt-dlp's JS runtime on physical device ONLY.

## Architecture
- **Kotlin + Jetpack Compose (M3)**, minSdk 26, compileSdk 36, AGP 8.13.2, Kotlin 2.2.21
- **youtubedl-android 0.18.1** + **ffmpeg 0.18.1** pinned
- **Room** for history DB, **Foreground Service** for download queue
- **MediaStore** for saving to `Movies/LinkGrab` / `Music/LinkGrab`
- **Two-layer auto-update**: yt-dlp auto-update on launch/periodically, app self-update via GitHub Releases
- **Settings**: audio format (m4a/mp3), video quality ceiling, download location, yt-dlp update check
- **Error handling**: friendly UI errors, retry, graceful degradation

## Tech Stack
- **Dependencies**: `libs.youtubedl.library`, `libs.youtubedl.ffmpeg`, `androidx.room:room-ktx`, `androidx.lifecycle:lifecycle-runtime`, `androidx.work:work-runtime-ktx`, `com.google.android.material:material`
- **Format selection**: yt-dlp `-f "bestvideo[height<=1080]+bestaudio/best[height<=1080]"` for video, `bestaudio/best` for audio (then transcode to m4a/mp3 via bundled ffmpeg)
- **R8 keep rules**: proven on physical device (already in proguard-rules.pro)
- **Security**: `allowBackup=false`, `usesCleartextTraffic=false`, only Share activity exported

## Spec: Q1–Q5 Locked Bullets
1. JS-runtime proof on physical device ONLY — never emulator (PASSED)
2. Hosting: GitHub Releases, PUBLIC repo, `https://github.com/<owner>/<repo>/releases/latest/download/version.json`; placeholders `linkgrab.updateRepoOwner=CHANGE_ME` / `linkgrab.updateRepoName=CHANGE_ME` in gradle.properties
3. Agent does NOT generate the keystore; user runs `tools/gen-keys.sh`; `keystore.properties` stays absent; `releaseTest` debug-signed for ALL testing
4. Audio default m4a, mp3 option (bundled ffmpeg)
5. Launcher "LinkGrab", applicationId `dev.personal.linkgrab`

## Global Constraints (from spec)
- URLs passed as separate args (never shell strings)
- Reject non-http(s) schemes
- `allowBackup=false`, `usesCleartextTraffic=false`
- Only the Share activity exported in v1
- R8 keep rules proven on device
- Pinned dependencies
- Signing keys never committed/printed/read aloud

## Plan Structure
This plan is split into 7 tasks, one per MVP feature. Each task is self-contained with testable deliverables. Tasks depend on each other in order — do not skip.

## File Structure (target layout)

```
app/src/main/java/dev/personal/linkgrab/
├── MainActivity.kt            (exists — launcher, hosts HomeScreen)
├── ShareActivity.kt           (exists — share handler)
├── SpikeTestActivity.kt       (exists — spike test, exported=false)
├── data/
│   ├── DownloadManager.kt     (queue + yt-dlp execution, Task 1)
│   ├── FormatSelector.kt      (video/audio format strings, Task 1)
│   ├── SettingsRepository.kt  (DataStore-backed settings, Task 5)
│   ├── HistoryRepository.kt   (Room access, Task 4)
│   └── db/
│       ├── AppDatabase.kt     (Room DB, Task 4)
│       ├── HistoryDao.kt      (Task 4)
│       └── HistoryEntity.kt   (Task 4)
├── download/
│   ├── DownloadService.kt     (foreground service, Task 2)
│   └── MediaStoreSaver.kt     (MediaStore save, Task 3)
├── update/
│   ├── YtDlpUpdater.kt        (yt-dlp self-update, Task 6)
│   ├── AppUpdater.kt          (app self-update, Task 6)
│   └── VersionChecker.kt      (version.json parse, Task 6)
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt      (URL input + metadata preview, Task 1)
│   │   ├── DownloadsScreen.kt (queue + progress, Task 2)
│   │   ├── HistoryScreen.kt   (Room history, Task 4)
│   │   └── SettingsScreen.kt  (settings, Task 5)
│   ├── components/
│   │   ├── MetadataCard.kt    (Task 1)
│   │   ├── DownloadItem.kt    (Task 2)
│   │   └── ErrorBanner.kt     (Task 7)
│   └── theme/
│       └── Theme.kt           (M3 theme, exists)
└── util/
    ├── UrlParser.kt           (URL extraction/validation, Task 1)
    └── ErrorMapper.kt         (friendly error mapping, Task 7)
```

**Shared interfaces (defined once, used across tasks):**

```kotlin
// data/DownloadManager.kt — Task 1
data class DownloadRequest(
    val url: String,
    val type: DownloadType,          // VIDEO or AUDIO
    val audioFormat: AudioFormat,    // M4A or MP3 (ignored for VIDEO)
    val qualityCeiling: Int          // 1080, 720, 480 (max height)
)
enum class DownloadType { VIDEO, AUDIO }
enum class AudioFormat { M4A, MP3 }
enum class DownloadStatus { QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED }
data class DownloadItem(
    val id: Long,
    val url: String,
    val title: String?,
    val type: DownloadType,
    val status: DownloadStatus,
    val progress: Float,             // 0f..1f
    val errorMessage: String?,
    val outputPath: String?,
    val createdAt: Long
)

// data/SettingsRepository.kt — Task 5
data class AppSettings(
    val audioFormat: AudioFormat,    // default M4A
    val qualityCeiling: Int,         // default 1080
    val autoUpdateYtDlp: Boolean,    // default true
    val autoUpdateApp: Boolean,      // default true
    val lastYtDlpUpdateCheck: Long   // epoch ms, 0 = never
)

// data/db/HistoryEntity.kt — Task 4
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String,
    val type: String,                // "VIDEO" | "AUDIO"
    val status: String,              // DownloadStatus.name
    val outputPath: String?,
    val errorMessage: String?,
    val createdAt: Long,
    val completedAt: Long?
)
```

---