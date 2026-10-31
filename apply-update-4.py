#!/usr/bin/env python3
"""LinkGrab update 4: rich download notification, clipboard banner, bouncing loader, grab animation.
Run from the repo root (folder with gradlew). Run apply-update-3.py first.

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


# ---------------- DownloadService: progress bar, speed and time left in the notification ----------------
patch(f"{BASE}/service/DownloadService.kt", "DownloadService", [
    ("notification() accepts title and progress",
     "private fun notification(text: String, ongoing: Boolean): android.app.Notification {",
     "private fun notification(text: String, ongoing: Boolean, title: String = \"LinkGrab\", progress: Int? = null, indeterminate: Boolean = false): android.app.Notification {"),
    ("notification builder: title, progress bar, yellow accent, no repeat alerts",
     '.setContentTitle("LinkGrab").setContentText(text).setContentIntent(open).setOngoing(ongoing)',
     ".setContentTitle(title).setContentText(text).setContentIntent(open).setOngoing(ongoing).setOnlyAlertOnce(true).setColor(0xFFFFD93B.toInt()).apply { if (progress != null) setProgress(100, progress, indeterminate) }"),
    ("status-bar icon",
     "setSmallIcon(android.R.drawable.stat_sys_download)",
     "setSmallIcon(R.drawable.ic_stat_linkgrab)"),
    ("helpers: notifyRich and rate",
     "private var lastNotificationMillis = 0L",
     "private var lastNotificationMillis = 0L\n"
     "    private fun notifyRich(title: String, text: String, percent: Int, indeterminate: Boolean) =\n"
     "        (getSystemService(NotificationManager::class.java)).notify(NOTIFICATION_ID, notification(text, true, title, percent, indeterminate))\n"
     "    private fun rate(id: Long): String {\n"
     "        val job = DownloadStore.items.value.firstOrNull { it.id == id } ?: return \"\"\n"
     "        return \"${dev.personal.linkgrab.util.EtaFormatter.speed(job.speedBytesPerSecond)} · ${dev.personal.linkgrab.util.EtaFormatter.duration(job.etaSeconds)} left\"\n"
     "    }"),
    ("notification text shows percent, speed and time left",
     'dev.personal.linkgrab.util.EtaState.ESTIMATING -> "${percent}% · ${dev.personal.linkgrab.util.EtaFormatter.status(snapshot)}"',
     'dev.personal.linkgrab.util.EtaState.ESTIMATING -> "${percent}% · ${rate(id)}"'),
    ("progress updates use the rich notification with the video title",
     "notify(detail, true)",
     'notifyRich(DownloadStore.items.value.firstOrNull { it.id == id }?.title ?: "Downloading", detail, percent, snapshot.state == dev.personal.linkgrab.util.EtaState.CALCULATING)'),
])

# ---------------- HomeScreen ----------------
new_imports = (
    "import dev.personal.linkgrab.ui.components.UrlInputBar\n"
    "import dev.personal.linkgrab.ui.components.ClipboardBanner\n"
    "import dev.personal.linkgrab.ui.components.FetchingArrow\n"
    "import dev.personal.linkgrab.ui.components.SavedSticker\n"
    "import dev.personal.linkgrab.ui.components.rememberNotificationPermissionRequester\n"
    "import androidx.compose.animation.core.animateFloatAsState\n"
    "import androidx.compose.animation.core.tween\n"
    "import androidx.compose.ui.graphics.graphicsLayer"
)

state_old = 'var pillFlash by remember { mutableStateOf<String?>(null) }'
state_new = (
    'var pillFlash by remember { mutableStateOf<String?>(null) }\n'
    '    var grabbing by remember { mutableStateOf(false) }\n'
    '    var clipLink by remember { mutableStateOf<String?>(null) }\n'
    '    var lastClip by remember { mutableStateOf<String?>(null) }\n'
    '    var clipTick by remember { mutableStateOf(0) }\n'
    '    val requestNotifications = rememberNotificationPermissionRequester()\n'
    '    val hostActivity = context as? androidx.activity.ComponentActivity\n'
    '    DisposableEffect(hostActivity) {\n'
    '        val observer = object : androidx.lifecycle.LifecycleEventObserver {\n'
    '            override fun onStateChanged(source: androidx.lifecycle.LifecycleOwner, event: androidx.lifecycle.Lifecycle.Event) {\n'
    '                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) clipTick++\n'
    '            }\n'
    '        }\n'
    '        hostActivity?.lifecycle?.addObserver(observer)\n'
    '        onDispose { hostActivity?.lifecycle?.removeObserver(observer) }\n'
    '    }\n'
    '    LaunchedEffect(clipTick) {\n'
    '        delay(350)\n'
    '        val link = clipboard.getText()?.text?.let { UrlParser.extractUrls(it).firstOrNull() }\n'
    '        if (link != null && link != lastClip && input.isBlank()) clipLink = link\n'
    '    }'
)

banner_new = (
    '        clipLink?.let { link ->\n'
    '            if (input.isBlank()) item {\n'
    '                ClipboardBanner(\n'
    '                    link = link,\n'
    '                    onGrab = { input = link; lastClip = link; clipLink = null },\n'
    '                    onDismiss = { lastClip = link; clipLink = null }\n'
    '                )\n'
    '            }\n'
    '        }\n'
    '        // Input + multi-link actions'
)

grab_old = ("preview?.let { data -> item {\n"
            "            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {")
grab_new = ("preview?.let { data -> item {\n"
            "            val grabProgress by animateFloatAsState(if (grabbing) 1f else 0f, tween(320), label = \"grab\")\n"
            "            Column(\n"
            "                modifier = Modifier.graphicsLayer {\n"
            "                    translationY = grabProgress * 260.dp.toPx()\n"
            "                    alpha = 1f - grabProgress\n"
            "                    scaleX = 1f - 0.08f * grabProgress\n"
            "                    scaleY = 1f - 0.08f * grabProgress\n"
            "                },\n"
            "                verticalArrangement = Arrangement.spacedBy(12.dp)\n"
            "            ) {")

patch(f"{BASE}/ui/home/HomeScreen.kt", "HomeScreen", [
    ("imports", "import dev.personal.linkgrab.ui.components.UrlInputBar", new_imports),
    ("state: grabbing, clipboard banner, notification permission", state_old, state_new),
    ("clipboard is offered by the banner instead of auto-pasted",
     "LaunchedEffect(Unit) { if (input.isBlank()) clipboard.getText()?.text?.let { UrlParser.extractUrls(it).firstOrNull()?.let { input = it } } }",
     '// Clipboard links are offered by the "Link found. GRAB?" banner instead of being pasted automatically.'),
    ("clipboard banner item", "        // Input + multi-link actions", banner_new),
    ("bouncing arrow while fetching",
     "LinearProgressIndicator(Modifier.fillMaxWidth(), color = Sticker.Ink, trackColor = Sticker.Track)",
     "FetchingArrow()"),
    ("fetching subtext",
     'Text("Checking the link…", fontWeight = FontWeight.Bold)',
     'Text("Checking the link. One moment.", fontSize = 13.sp)'),
    ("preview card slides down into the queue", grab_old, grab_new),
    ("DOWNLOAD NOW: ask for notifications once, slide card away, then clear",
     'pillFlash = "GRABBED!"; preview = null; input = ""',
     'pillFlash = "GRABBED!"; requestNotifications(); grabbing = true; scope.launch { delay(340); preview = null; input = ""; grabbing = false }'),
    ("DOWNLOAD NOW disabled during the slide",
     "fontSize = 22.sp",
     "fontSize = 22.sp,\n                    enabled = !grabbing"),
    ("big SAVED! sticker overlay",
     "    if (duplicate) AlertDialog(",
     '    SavedSticker(visible = pillFlash == "SAVED!")\n    if (duplicate) AlertDialog('),
])

print("\nApplied:")
for line in applied:
    print("  +", line)
print("\nSkipped:")
for line in skipped or ["  (none)"]:
    print("  -", line)
print("\nNext: git diff, build, install.")
