#!/usr/bin/env python3
"""Applies the LinkGrab update-2 edits to existing files. Run from the repo root (folder with gradlew).

Each edit only applies if the exact original text is found exactly once; anything else is
skipped and reported, never guessed. Safe to run twice. Commit first, then review with git diff.
"""
import os
import sys

ROOT = os.getcwd()
if not os.path.exists(os.path.join(ROOT, "gradlew")):
    sys.exit("Run this from the LinkGrab repo root (the folder that contains gradlew).")

BASE = "app/src/main/java/dev/personal/linkgrab"
applied, skipped = [], []


def patch(path, label, edits):
    full = os.path.join(ROOT, path)
    if not os.path.exists(full):
        skipped.append(f"{label}: file not found ({path})")
        return
    with open(full, encoding="utf-8") as f:
        text = f.read()
    for name, old, new, replace_all in edits:
        count = text.count(old)
        if new in text:
            skipped.append(f"{label}: {name} (already applied)")
        elif count == 1 or (replace_all and count > 0):
            text = text.replace(old, new)
            applied.append(f"{label}: {name}")
        else:
            skipped.append(f"{label}: {name} (expected 1 match, found {count}; edit by hand)")
    with open(full, "w", encoding="utf-8") as f:
        f.write(text)


E = lambda name, old, new, all_=False: (name, old, new, all_)

patch(f"{BASE}/LinkGrabApp.kt", "LinkGrabApp", [
    E("import worker",
      "import dev.personal.linkgrab.data.YtDlpUpdater",
      "import dev.personal.linkgrab.data.YtDlpUpdater\nimport dev.personal.linkgrab.data.YtDlpUpdateWorker"),
    E("import first",
      "import kotlinx.coroutines.launch",
      "import kotlinx.coroutines.flow.first\nimport kotlinx.coroutines.launch"),
    E("schedule weekly background update",
      "DownloadService.resumeQueued(this)",
      "DownloadService.resumeQueued(this)\n        YtDlpUpdateWorker.schedule(this)"),
    E("daily check on launch now respects the Settings switch",
      "if (System.currentTimeMillis() - lastUpdate >= 24 * 60 * 60 * 1000L) {",
      "if (settingsRepository.settings.first().autoUpdateYtDlp && System.currentTimeMillis() - lastUpdate >= 24 * 60 * 60 * 1000L) {"),
])

engine_read = "withContext(Dispatchers.IO) { runCatching { YoutubeDL.getInstance().version(context.applicationContext) }.getOrNull() }"
patch(f"{BASE}/ui/settings/SettingsScreen.kt", "SettingsScreen", [
    E("imports",
      "import kotlinx.coroutines.launch",
      "import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.launch\nimport kotlinx.coroutines.withContext\nimport com.yausername.youtubedl_android.YoutubeDL"),
    E("engine version state",
      "var updateMessage by remember { mutableStateOf<String?>(null) }",
      "var updateMessage by remember { mutableStateOf<String?>(null) }\n"
      "    var engineVersion by remember { mutableStateOf<String?>(null) }\n"
      "    var updating by remember { mutableStateOf(false) }\n"
      f"    LaunchedEffect(Unit) {{ engineVersion = {engine_read} }}"),
    E("show engine version",
      'Text(updateMessage ?: "Downloads the latest extractor when available")',
      'Text(updateMessage ?: "Engine version: ${engineVersion ?: "unknown"}")'),
    E("update button starts",
      "Button(onClick = { scope.launch { updateMessage = when (val result = YtDlpUpdater.update(context)) {",
      "Button(enabled = !updating, onClick = { scope.launch { updating = true; updateMessage = when (val result = YtDlpUpdater.update(context)) {"),
    E("update button refreshes version",
      'is YtDlpUpdater.Result.Failed -> result.message } } }) { Text("Update") }',
      f'is YtDlpUpdater.Result.Failed -> result.message }}; engineVersion = {engine_read}; updating = false }} }}) {{ Text(if (updating) "Updating…" else "Update") }}'),
])

patch("app/build.gradle.kts", "build.gradle.kts", [
    E("versionCode 2", "versionCode = 1", "versionCode = 2"),
    E("versionName 0.2.0", 'versionName = "0.1.0"', 'versionName = "0.2.0"'),
    E("add 32-bit ARM (armeabi-v7a) to release and releaseTest",
      'abiFilters += listOf("arm64-v8a")',
      'abiFilters += listOf("arm64-v8a", "armeabi-v7a")', True),
    E("releaseTest signs with your release key when keystore.properties exists",
      'signingConfig = signingConfigs.getByName("debug")',
      'signingConfig = if (rootProject.file("keystore.properties").exists()) signingConfigs.getByName("release") else signingConfigs.getByName("debug")'),
])

print("\nApplied:")
for line in applied:
    print("  +", line)
print("\nSkipped:")
for line in skipped or ["  (none)"]:
    print("  -", line)
print("\nNext: git diff, then follow docs/RELEASING.md")
