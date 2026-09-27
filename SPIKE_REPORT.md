# Spike Report: LinkGrab Phase-1 yt-dlp On-Device Extraction

**Spike name:** LinkGrab Phase-1 yt-dlp on-device extraction validation  
**Date:** 2026-09-19  
**Status:** COMPLETE  
**App:** `dev.personal.linkgrab`, Kotlin + Jetpack Compose, version `0.1.0`, minSdk 26, targetSdk 36, compileSdk 36  
**Library:** `io.github.junkfood02.youtubedl-android:library:0.18.1` + `ffmpeg:0.18.1` (from gradle/libs.versions.toml)  
**Bundled yt-dlp version:** 2025.11.12 (older than 90 days — yt-dlp emits a version warning)

---

## Spike goal

Validate that yt-dlp (via the youtubedl-android library) runs on-device in an Android emulator, and determine which of YouTube, TikTok, and Instagram can be extracted successfully.

---

## Method

- **Harness:** `SpikeTestActivity.kt` (debug-only activity), launched via:
  ```
  adb shell am start -n dev.personal.linkgrab/.SpikeTestActivity \
    --es url "<url>" --es mode info
  ```
- **Result file:** written to `/sdcard/Download/linkgrab_spike/result.txt` on the device
- **`YoutubeDL.init()`:** entirely local — extracts bundled Python + yt-dlp from app resources into `no_backup/youtubedl-android/`, sets `LD_LIBRARY_PATH`, `SSL_CERT_FILE`, `PYTHONHOME`; does **not** perform a network update check (verified by decompiling the library).
- **`init()` blocking:** runs many seconds; must execute on an IO thread (already handled via `ensureYoutubeDL()` in `MainActivity.kt`).
- **`getInfo()`:** executes yt-dlp as a child python process (libpython.so) inside the app process.

---

## Results table

| Platform | URL | Result | Exception / Notes | Wall-clock |
|---|---|---|---|---|
| **YouTube** | `https://www.youtube.com/watch?v=dQw4w9WgXcQ` | **PASS** / `INFO_OK` | Title: `Rick Astley - Never Gonna Give You Up (Official Video) (4K Remaster)` | ~12 min (IP‑pool walking) |
| **TikTok** | `https://www.tiktok.com/@scout2015/video/6718335390845095173` | **FAIL** | `YoutubeDLException`: impersonation unavailable; TLS fingerprint missing | ~40 s |
| **Instagram** | `https://www.instagram.com/reel/C0example/` (placeholder) | **FAIL** | `YoutubeDLException`: no csrf token; DNS resolution `[Errno 7]`; placeholder shortcode | ~7 min (DNS retries) |

---

## Detailed findings

### 1. YouTube — SUCCESS (INFO_OK)
- URL tested: `https://www.youtube.com/watch?v=dQw4w9WgXcQ`
- Result: `INFO_OK`
- Extracted title: `Rick Astley - Never Gonna Give You Up (Official Video) (4K Remaster)`
- Extracted url: `https://www.youtube.com/watch?v=dQw4w9WgXcQ`
- Wall-clock: ~12 minutes (10:26 → 10:38). The delay was **not** a hang — yt-dlp iterates a rotating pool of YouTube CDN IPs (142.251.150–157.4) and several were unreachable from the host, so it retried until it hit a working IP (142.251.154.4).
- **Conclusion:** end-to-end on-device extraction works for YouTube.

### 2. TikTok — ERROR (informative failure)
- URL tested: `https://www.tiktok.com/@scout2015/video/6718335390845095173`
- Result: `ERROR`, exception type `com.yausername.youtubedl_android.YoutubeDLException`
- Message highlights:
  - yt-dlp version warning (2025.11.12 older than 90 days)
  - `WARNING: [TikTok] The extractor is attempting impersonation, but no impersonate target is available`
  - `ERROR: [TikTok] 6718335390845095173: Unable to extract webpage video data`
- **Root cause:** TikTok now requires TLS fingerprint impersonation (curl_cffi) which the bundled yt-dlp build lacks. This is a known yt-dlp limitation, not an app bug.
- Wall-clock: ~40 seconds.

### 3. Instagram — ERROR (informative failure)
- URL tested: `https://www.instagram.com/reel/C0example/` (placeholder shortcode)
- Result: `ERROR`, exception type `YoutubeDLException`
- Message highlights:
  - `WARNING: [Instagram] C0example: No csrf token set by Instagram API`
  - `General metadata extraction failed`
  - `ERROR: [Instagram] C0example: Unable to download webpage: [Errno 7] No address associated with hostname`
- **Root causes:** (a) guest DNS resolution failed (`[Errno 7] No address associated with hostname` — the emulator's DNS‑over‑TLS path to 10.0.2.3:853 was flaky during the run), and (b) the URL used a placeholder shortcode, so even with working DNS the extraction would likely fail on a nonexistent post.
- Wall-clock: ~7 minutes (10:40 → 10:46), dominated by DNS retries.

---

## Environment findings

- **Emulator:** Android emulator x86_64, API 36, network type "MOBILE NR", eth0 10.0.2.15/24, DNS 10.0.2.3, private DNS "opportunistic".
- **Host‑side egress filtering:** the host can reach some Google IPs on 443 (142.251.157.119 OK, 172.217.112.4 OK, curl used 142.251.154.4) but **not** others (142.251.153.4, 142.251.156.4, 142.251.151.4 FAIL). This is why YouTube extraction took 12 minutes — yt-dlp walked the IP pool.
- **Guest DNS via DoT to 10.0.2.3:853 is flaky:** SYN_SENT hangs are normal and recover.
- **These are environment issues, not app defects.** On a real device with normal connectivity, extraction should be much faster.

---

## Recommendations

1. **Update bundled yt-dlp** to a version newer than 90 days (or accept the warning); consider `--no-update` semantics if auto‑update is undesirable.
2. **TikTok:** yt-dlp needs curl_cffi impersonation support. Evaluate whether youtubedl-android can bundle a build with impersonation, or defer TikTok support until then.
3. **Instagram:** add retry logic for flaky DNS; use real shortcodes in tests.
4. **Add retry/timeout handling** in the app for slow extraction (YouTube already took 12 min).
5. **Re‑test on a real device** — host filtering is emulator‑specific and may not reflect production experience.

---

## JS runtime sections

### J2V8 (V8)
Google V8 bindings for Android; large native binary (~10 MB+ per ABI), fast, mature. Note ABI size cost and JNI complexity. **Recommendation:** avoid for LinkGrab due to binary footprint and integration overhead.

### QuickJS
Tiny (~1 MB), fast startup, embeddable, ES2020 support. Good fit for Android; smaller ecosystem. **Recommendation:** consider as a lightweight option if a small JS engine is needed.

### Rhino
Pure‑Java JS engine, no native code, easy integration, but slow and older ES support. Fine for light scripting. **Recommendation:** keep as a fallback for very simple, non‑performance‑critical scripting.

### GraalJS
High performance, modern ES, but very large native footprint and heavy on Android; generally not recommended for mobile. **Recommendation:** avoid.

### JavaScriptCore
Apple's engine, available via Android bindings (e.g. the `jsc-android` npm package used by React Native). Solid ES support, moderate size. **Recommendation:** evaluate if the app already depends on React Native; otherwise conditional use.

**One‑line recommendation:** LinkGrab should **not adopt a custom JS engine** at this time; any future JS needs can be served by QuickJS for minimal footprint or deferred until a concrete requirement arises.

---

## Open questions / next steps

- Re‑test TikTok with an impersonation‑capable yt‑dlp build (curl_cffi).
- Re‑test Instagram with a real shortcode and stable DNS configuration.
- Validate on a real device (emulator egress filtering is not production‑representative).
- Decide on JS engine adoption (currently none recommended).
- Consider updating bundled yt-dlp version and adding `--no-update` or periodic update checks.

## Physical-device release verification

**Date:** 2026-09-20  
**Device:** Xiaomi 25028RN03A (`serenity_global`), arm64-v8a, Android 15 (API 35)  
**Build:** `releaseTest` (`app-releaseTest.apk`), R8-minified, resource-shrunk, debug-signed

The release-test APK was installed over ADB and launched through `MainActivity`. Entering
`https://www.youtube.com/watch?v=dQw4w9WgXcQ` and selecting **Get Info** returned:

`Rick Astley - Never Gonna Give You Up (Official Video) (4K Remaster)`

This validates on physical arm64 hardware that R8 preserved the youtubedl-android JNI and
Python runtime, `YoutubeDL.init()` completed off the main thread, and metadata extraction
worked end to end. A separate stale public test ID (`BaW_jenozKc`) returned a normal
"video unavailable" extractor error after initialization, rather than a runtime failure.

The final rebuilt APK (`app-releaseTest.apk`, 55,682,967 bytes) was reinstalled on the same
device. An `ACTION_SEND` `text/plain` intent containing surrounding prose and a YouTube URL
opened LinkGrab with the URL populated, and metadata extraction again returned the title and
uploader with Video (up to 1080p) and Audio (M4A) choices visible.

---
*End of report*

---

## On-device re-verification (2026-09-27) — Instagram login wall confirmed

**Device:** Xiaomi 25028RN03A (`serenity_global`), Android 15, arm64-v8a — releaseTest APK
**Network:** Wi-Fi first, then LTE (Safaricom) to bypass the LAN egress filter

Testing the `ba+bv` Instagram format change (`FormatSelector.instagramVideoFormat()` in
`DownloadService`) on real reels produced the definitive root cause for the historical
"Instagram fails (csrf / DNS)" finding:

- On LTE, `www.instagram.com` DNS resolved and the resolved IP (`57.144.138.34:443`) passed
  a TCP connect — the network path was healthy. (On the filtered Wi-Fi, Instagram was
  unreachable and extraction failed fast with a DNS-style error.)
- yt-dlp 2025.11.12 then failed on **two different real reels** (the user's original URL and
  a second public one), both with:
  ```
  WARNING: [Instagram] <id>: Minimum webpage is locked behind the login page. Retrying with embed webpage
  ERROR: [Instagram] <id>: Requested content is not available, rate-limit reached or login required.
          Use --cookies-from-browser or --cookies for the authentication.
  ```
- The app surfaced this as "Instagram is asking for a login or has temporarily limited
  access to this Reel." (ErrorMapper).
- Conclusion: Instagram now rate-limits/blocks **anonymous** reel extraction. This is
  **not** an app defect or a network defect — it is Instagram-side auth. Confirms the spike's
  "Instagram fails (csrf — environment issues)" with the precise mechanism.

### Follow-ups for future work
- Unblocking Instagram downloads requires supplying account cookies to yt-dlp
  (`--cookies` / `--cookies-from-browser`) — not implemented in LinkGrab as of this change.
- A newer yt-dlp build (post-2025.11.12) may rework the Instagram extractor, but the login
  wall is expected to persist for anonymous access.
- Also re-observed: YouTube extraction can hang at "Checking the link…" for minutes on
  carrier LTE (Safaricom throttles some Google CDN IPs) — consistent with the spike's
  host-side 443 egress filtering of Google IP ranges.
- Diagnostic logging added on-device: `Log.i("LinkGrab", "download url=… format=…")` in
  `DownloadService`, `Log.w("LinkGrab", "getInfo failed…")` in `HomeScreen`.
