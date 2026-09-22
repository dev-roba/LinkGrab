# LinkGrab Project Conventions

## Project
- **Name:** LinkGrab — personal sideloaded Android app
- **Stack:** Kotlin + Jetpack Compose, Material 3
- **minSdk:** 26, **targetSdk:** 36, **compileSdk:** 36
- **applicationId:** dev.personal.linkgrab
- **Launcher name:** LinkGrab

## Build types
- **debug** — arm64-v8a + x86_64, no minify, default signing
- **releaseTest** — debug-signed, R8 minify + shrinkResources, arm64-v8a only — **USE THIS FOR ALL TESTING**. Works without keystore.properties.
- **release** — requires keystore.properties via tools/gen-keys.sh; fails clearly if absent

## Key commands
- `./gradlew :app:assembleReleaseTest` — build release-test debug-signed APK
- `./gradlew :app:assembleDebug` — build debug APK
- `./gradlew :app:assembleRelease` — build release APK (needs keystore.properties)
- Install via: `adb install -r app/build/outputs/apk/releaseTest/app-releaseTest-unsigned.apk` (or the properly signed variant)
- adb wireless: `adb connect 192.168.0.106:5555`

## Physical test device
- **Xiaomi 25028RN03A** (serenity_global), **arm64-v8a**, **Android 15 (API 35)**
- adb at **192.168.0.106:5555** (wireless)
- **Emulator was killed — do not use emulator for JS-runtime proof**

## youtubedl-android
- **0.18.1** + **ffmpeg 0.18.1** pinned
- `YoutubeDL.init()` blocks and must run on IO thread (see `ensureYoutubeDL` in MainActivity.kt)
- Bundled yt-dlp **2025.11.12** (older than 90 days — version warning accepted)
- R8 keep rules proven on physical device (see proguard-rules.pro)

## Security requirements
- URLs passed as separate args (never shell strings)
- Reject non-http(s) schemes
- `allowBackup=false`, `usesCleartextTraffic=false`
- Only the Share activity exported in v1
- R8 keep rules proven on device
- Pinned dependencies
- Signing keys never committed/printed/read aloud

## App update
Two layers:
1. **yt-dlp auto-update** on launch, periodically, after extraction failure
2. **App self-update** via signed manifest + SHA-256 + PackageInstaller, hosted on public GitHub Releases
   - Stable URL: `https://github.com/<owner>/<repo>/releases/latest/download/version.json`
   - No token required; repo must be public
   - Owner/repo placeholders in `gradle.properties`: `linkgrab.updateRepoOwner=CHANGE_ME`, `linkgrab.updateRepoName=CHANGE_ME`

## Product decisions
- **Audio default:** m4a (mp3 as second option, transcoded with bundled ffmpeg)
- **Video best up to:** 1080p mp4
- **Downloads saved via:** MediaStore to `Movies/LinkGrab` and `Music/LinkGrab`
- **Room DB** for history
- **Foreground service** for download queue

## Spike findings summary (from SPIKE_REPORT.md)
- YouTube works (INFO_OK)
- TikTok fails (impersonation — known yt-dlp limitation)
- Instagram fails (DNS + csrf — environment issues)
- Host filters 443 egress to some Google IPs
- Guest DNS via DoT flaky
- These are environment issues, not app defects