# LinkGrab Performance Work

## Step 1 — instrumentation and baseline

- Started on 2026-09-20.
- Test device: Xiaomi 25028RN03A, wireless ADB `192.168.0.106:5555`.
- Device state check: `device`.
- The phone shell has no `curl`, `wget`, or `busybox`; a direct known-file throughput probe could not run from shell. Phone throughput baseline is therefore **PENDING** until measured through the app's download stream.
- Added debug-only `LinkGrabPerf` and sanitized `LinkGrabYtdlp` traces for engine init, metadata fetch, download start/first progress/done, and yt-dlp output.
- Full locked gate passed after instrumentation: `clean assembleDebug testDebugUnitTest lintDebug`.
- Debug APK installed on the wireless phone after removing the old package to recover storage; `get-state` returned `device`.
- Cold YouTube, TikTok, and Instagram metadata probes all exceeded 12 seconds after engine initialization; details are in `PERF_REPORT.md`.

- Added cache reuse, serialized downloads, ETA estimation, progress parsing, fail-fast yt-dlp options, and 720p stream-copy defaults; the mandatory debug gate still passes.
- ReleaseTest rebuilt with R8 enabled and installed on the required phone. Explicit share-sheet smoke passed: messy shared YouTube text opened Home and displayed the extracted URL.
- Metadata/download timing, throughput, and ETA accuracy remain PENDING because all three cold extractors exceeded the observation window; no unsupported performance claim is made.
- Diagnostic run moved to authorized ADB serial `10.131.244.249:5555`. A debug-only `DebugDiagnosticsActivity` now runs library `execute`, logs separate full stdout/stderr, and provides a bounded Kotlin HTTP/DNS SpeedProbe; it is absent from release manifests.
- Proven timing cause: the former 12-second cutoff included real cold unpack/import cost. Cold init measured 3.79–5.10 s; list-extractors added 5.86–9.58 s; TikTok completed in 7.86–9.95 s and YouTube in 12.09 s.
- Phone ping connectivity succeeded, but HTTPS SpeedProbe did not emit a result within 120 s; throughput remains PENDING. No optimization fix was implemented.
- Audio fix verified on `10.131.244.249:5555`: initialized the bundled FFmpeg/FFprobe runtime before yt-dlp post-processing and made MP3 the default setting.
- MP3 and M4A foreground-service downloads completed from `https://youtu.be/9MndPs-o-xM`; MediaStore rows confirmed `Music/LinkGrab/` with `audio/mpeg` and `audio/mp4` respectively.
- Launcher adaptive icon now uses the supplied `linkgrab_icon.ico` converted to `linkgrab_icon.png`; debug APK installed successfully on the phone.
