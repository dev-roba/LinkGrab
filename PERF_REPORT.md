# LinkGrab Performance Report

## Baseline

## Fetch-speed upgrade baseline (2026-09-22, releaseTest on 192.168.0.106:5555)

The existing app was measured before implementing the five requested changes. Preview was counted only when the UI exposed metadata fields (`Estimated size`/uploader/duration); a timeout means no usable preview was observed within 120 seconds.

| Site | Cold | Warm | Observation |
|---|---:|---:|---|
| YouTube `9MndPs-o-xM` | timed out at 120 s | timed out at 120 s | UI remained in checking state |
| TikTok `6718335390845095173` | 19 s | 5 s | usable preview appeared |
| Instagram `DLHx_WNoXoY` | timed out at 120 s | timed out at 120 s | no usable preview |

These are the **Before** values for Changes 1–5. No optimization was applied before this matrix.

Phone Wi-Fi throughput: **PENDING direct measurement**. The connected Android shell exposes neither `curl` nor `wget`; the user reports an 8 Mbps line (about 1 MB/s real throughput), which is recorded as context only and is not treated as a measured baseline.

Diagnostic device: Xiaomi `10.131.244.249:5555` (state `device`). The mandated `192.168.0.106:5555` was unavailable; testing used the newly authorized serial.

## Step 1 matrix

### Cold metadata probes (debug build, 2026-09-20)

| Site | URL class | Result within 12 s | Trace evidence |
|---|---|---|---|
| YouTube | public watch URL | no preview completion | `fetchInfo_start`, `engine_init_start/end` at 20:18:54; no `fetchInfo_end` |
| TikTok | public video URL | no preview completion | `fetchInfo_start`, `engine_init_start/end` at 20:19:36; no `fetchInfo_end` |
| Instagram | public post URL | no preview completion | `fetchInfo_start`, `engine_init_start/end` at 20:19:54; no `fetchInfo_end` |

The old 12-second cutoff was invalid. Warm `execute` probes completed for TikTok and YouTube; Instagram did not produce a completion in the observed run. Video/audio downloads, first-byte timing, byte-growth sampling, and ETA accuracy remain **PENDING**.

## Diagnostic measurements (2026-09-21)

### Init and bundled files

On a cleared debug install, `run-as` showed the bundled runtime unpacked under `no_backup/youtubedl-android`: Python directory ~48 MB, yt-dlp ~3.1 MB, and QuickJS/Python assets present. Cold trace: `init_start` → `init_end` **3.79 s**. A second launch initialized in **58 ms**. A later cleared-data extractor run measured **5.10 s** cold init; warm init was 0–2 ms. The prior 0–100 ms readings were warm/background calls, not fresh unpack duration.

### Phase timings

| Phase | Device result | Raw evidence |
|---|---:|---|
| `--version` cold | 3.79 s init + 3.95 s execute | `init_ms=3790`, `execute_end ... exit=0 elapsed_ms=3947`, stdout `2025.11.12` |
| `--version` warm | 58 ms init + 4.12 s execute | `init_ms=58`, `execute_end ... exit=0 elapsed_ms=4123` |
| `--list-extractors` cold | 5.10 s init + 9.58 s execute | `init_ms=5100`, `execute_end ... exit=0 elapsed_ms=9576` |
| `--list-extractors` warm | ~1 ms init + 5.86 s execute | `init_ms=1`, `execute_end ... exit=0 elapsed_ms=5860` |
| TikTok simulate warm | 7.86 s | `execute_end ... exit=0 elapsed_ms=7860`; stdout included `Downloading webpage` and selected format |
| TikTok + `--force-ipv4` | error path around 7–8 s; response was not cleanly returned before the run was replaced | stderr: `impersonation ... no impersonate target`, `Unable to extract webpage video data` |
| TikTok + `--socket-timeout 10` | 9.95 s | `execute_end ... exit=0 elapsed_ms=9951`; same impersonation warning/error text |
| YouTube simulate warm | 12.09 s | `execute_end ... exit=0 elapsed_ms=12085`; stderr warned SABR formats were skipped |
| Instagram equivalent | timed out in the observed run | no completion marker captured; not treated as a proven root cause |

Raw TikTok stderr excerpt:

```text
[debug] Python 3.12.11 (CPython aarch64 64bit)
[debug] JS runtimes: quickjs-2025-04-26
[debug] Loaded 1844 extractors
WARNING: [TikTok] The extractor is attempting impersonation, but no impersonate target is available.
ERROR: [TikTok] 6718335390845095173: Unable to extract webpage video data
```

During the abandoned first 120-second attempt, `ps`/`top -H` showed only `dev.personal.linkgrab` and no Python child, with all app threads at 0.0% CPU. That run also suffered a transient ADB-offline event, so it is evidence of a waiting/frozen app observation—not proof that yt-dlp itself was CPU-bound.

### Network separation

Phone shell results: `ping -c 3 8.8.8.8` succeeded (0% loss, average 34.148 ms); `ping -c 3 www.tiktok.com` resolved and succeeded (0% loss, average 33.420 ms). The debug SpeedProbe was added with separate DNS/TCP/TTFB fields and a 15-second DNS bound, but its first Java HTTPS probe still produced no result within the 120-second observation. Therefore phone HTTPS throughput is **PENDING**, and the requested MBPS field cannot be filled without inventing data.

## Root-cause assessment

**Proven:** the former 12-second observation window was too short. Cold runtime unpacking takes 3.8–5.1 s; extractor import takes 5.9–9.6 s; TikTok takes 7.9–10.0 s and YouTube 12.1 s even through direct `execute`. The old `getInfo` timeout therefore conflated normal startup/extraction latency with a hang.

**Still a hypothesis:** Instagram’s longer stall is network/extractor-specific; the available run did not capture its complete stderr/response. **Not proven:** missing JS runtime (QuickJS is present and yt-dlp reports it), duplicate extraction, throttling, or a UI progress-callback bug.

## Existing changes not justified by this evidence

- Metadata cache reuse: sensible, but no completed before/after extraction pair proves its benefit yet.
- Fail-fast flags (`--socket-timeout`, retries, throttled rate): not justified as the cause of the original stall; TikTok still returned extractor errors with them.
- `-N 4`: untested because no sustained download completed.
- 720p/stream-copy format defaults: untested for the same reason.

No fix is implemented pending approval of this diagnosis.

### ReleaseTest smoke (2026-09-20)

- `assembleReleaseTest` passed with R8 enabled.
- Installed on `192.168.0.106:5555` and verified `get-state=device`.
- Explicit `ACTION_SEND` text/plain with messy YouTube text launched `MainActivity`; UI dump contained `youtu.be/9MndPs-o-xM`.
- Full download/metadata timing remains **PENDING** because extraction did not complete within the debug observation window.

## Step 2 diagnosis before fixes

- **(a) JS/challenge:** inconclusive. No `LinkGrabYtdlp` output was emitted by `getInfo` before timeout, so there is no evidence yet of a missing JS runtime, challenge-provider failure, or 403 throttling.
- **(b) Duplicate extraction:** **addressed in source**. Preview metadata is cached per URL for the session and the download service reuses the cached info JSON when available; device timing proof is pending because extraction did not complete.
- **(c) Startup/self-update:** **not observed**. Trace shows engine initialization completed in about 0–100 ms during each probe; no launch-time yt-dlp update is called by `MainActivity`.
- **(d) 0% UI versus growing bytes:** **inconclusive**. No download reached a first progress callback in this run, and the release install is not debuggable for cache inspection; no byte-growth claim is made.
