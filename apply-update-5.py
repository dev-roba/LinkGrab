#!/usr/bin/env python3
"""LinkGrab update 5: in-app player. Adds the Media3 libraries and the manifest entries.
Run from the repo root (folder with gradlew). Run apply-update-4.py first.

Each edit only applies if the exact original text is found exactly once; anything else is
skipped and reported, never guessed. Safe to run twice. Commit first, then review with git diff.
"""
import os
import sys

ROOT = os.getcwd()
if not os.path.exists(os.path.join(ROOT, "gradlew")):
    sys.exit("Run this from the LinkGrab repo root (the folder that contains gradlew).")

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


MEDIA3 = "1.10.0"

patch("app/build.gradle.kts", "build.gradle.kts", [
    ("Media3 player and session libraries",
     "implementation(libs.youtubedl.ffmpeg)",
     "implementation(libs.youtubedl.ffmpeg)\n"
     f"    implementation(\"androidx.media3:media3-exoplayer:{MEDIA3}\")\n"
     f"    implementation(\"androidx.media3:media3-session:{MEDIA3}\")"),
])

patch("app/src/main/AndroidManifest.xml", "AndroidManifest", [
    ("permissions for background playback",
     '<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />',
     '<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />\n'
     '    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />\n'
     '    <uses-permission android:name="android.permission.WAKE_LOCK" />'),
    ("MainActivity: picture-in-picture and no restart on rotate",
     'android:name=".MainActivity"',
     'android:name=".MainActivity"\n'
     '            android:supportsPictureInPicture="true"\n'
     '            android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout"'),
    ("playback service",
     'android:foregroundServiceType="dataSync" />',
     'android:foregroundServiceType="dataSync" />\n'
     '        <service\n'
     '            android:name=".playback.PlaybackService"\n'
     '            android:exported="true"\n'
     '            android:foregroundServiceType="mediaPlayback">\n'
     '            <intent-filter>\n'
     '                <action android:name="androidx.media3.session.MediaSessionService" />\n'
     '            </intent-filter>\n'
     '        </service>'),
])

print("\nApplied:")
for line in applied:
    print("  +", line)
print("\nSkipped:")
for line in skipped or ["  (none)"]:
    print("  -", line)
print("\nNext: git diff, build, install.")
