#!/usr/bin/env python3
"""LinkGrab design update 3: patches the few lines in existing files. Run from the repo root (folder with gradlew).

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
    for name, old, new in edits:
        count = text.count(old)
        if new in text:
            skipped.append(f"{label}: {name} (already applied)")
        elif count == 1:
            text = text.replace(old, new)
            applied.append(f"{label}: {name}")
        else:
            skipped.append(f"{label}: {name} (expected 1 match, found {count}; edit by hand)")
    with open(full, "w", encoding="utf-8") as f:
        f.write(text)


state_block = (
    "val activeCount = jobs.count { it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.QUEUED }\n"
    "    var pillFlash by remember { mutableStateOf<String?>(null) }\n"
    "    val completedCount = jobs.count { it.status == DownloadStatus.COMPLETED }\n"
    "    var lastCompleted by remember { mutableStateOf(completedCount) }\n"
    "    LaunchedEffect(completedCount) {\n"
    "        if (completedCount > lastCompleted) pillFlash = \"SAVED!\"\n"
    "        lastCompleted = completedCount\n"
    "    }\n"
    "    LaunchedEffect(pillFlash) { if (pillFlash != null) { delay(1800); pillFlash = null } }"
)

patch(f"{BASE}/ui/home/HomeScreen.kt", "HomeScreen", [
    ("import PopPill",
     "import dev.personal.linkgrab.ui.theme.StickerPill",
     "import dev.personal.linkgrab.ui.theme.StickerPill\nimport dev.personal.linkgrab.ui.theme.PopPill"),
    ("state for the GRABBED!/SAVED! pill",
     "val activeCount = jobs.count { it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.QUEUED }",
     state_block),
    ("header pill pops in",
     'StickerPill(if (activeCount > 0) "$activeCount downloading" else "Ready")',
     'PopPill(pillFlash ?: if (activeCount > 0) "$activeCount downloading" else "Ready", highlight = pillFlash != null)'),
    ("haptic buzz on DOWNLOAD NOW",
     "shadowColor = Sticker.Pink,",
     "shadowColor = Sticker.Pink,\n                    haptic = true,"),
    ("GRABBED! flash when you tap DOWNLOAD NOW",
     "sizeLimitMb?.times(1_000_000L)); preview = null;",
     'sizeLimitMb?.times(1_000_000L)); pillFlash = "GRABBED!"; preview = null;'),
    ("dark mode: dark text on pink size chips",
     "selectedColor = Sticker.Pink, selectedContent = Sticker.Ink,",
     "selectedColor = Sticker.Pink, selectedContent = Sticker.Black,"),
    ("dark mode: dark text on pink error card",
     ".sticker(Sticker.Pink, 16.dp, 4.dp).padding(16.dp), color = Sticker.Ink,",
     ".sticker(Sticker.Pink, 16.dp, 4.dp).padding(16.dp), color = Sticker.Black,"),
])

patch(f"{BASE}/LinkGrabApp.kt", "LinkGrabApp", [
    ("load the saved light/dark choice at startup",
     "DownloadService.resumeQueued(this)",
     "DownloadService.resumeQueued(this)\n        dev.personal.linkgrab.ui.theme.ThemePrefs.ensureLoaded(this)"),
])

print("\nApplied:")
for line in applied:
    print("  +", line)
print("\nSkipped:")
for line in skipped or ["  (none)"]:
    print("  -", line)
print("\nNext: git diff, build, install.")
