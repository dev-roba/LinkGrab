package dev.personal.linkgrab.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.personal.linkgrab.LinkGrabApp
import dev.personal.linkgrab.data.UrlParser
import dev.personal.linkgrab.data.MediaInfoFetcher
import dev.personal.linkgrab.data.MediaInfoSizer
import dev.personal.linkgrab.model.DownloadStatus
import dev.personal.linkgrab.model.DownloadType
import dev.personal.linkgrab.service.DownloadService
import dev.personal.linkgrab.service.DownloadStore
import dev.personal.linkgrab.service.DownloadLaterScheduler
import dev.personal.linkgrab.ui.components.MetadataCard
import dev.personal.linkgrab.ui.components.UrlInputBar
import dev.personal.linkgrab.ui.theme.Sticker
import dev.personal.linkgrab.ui.theme.StickerButton
import dev.personal.linkgrab.ui.theme.StickerChip
import dev.personal.linkgrab.ui.theme.StickerPill
import dev.personal.linkgrab.ui.theme.PopPill
import dev.personal.linkgrab.ui.theme.sticker
import dev.personal.linkgrab.util.ErrorMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    else -> "%.0f KB".format(bytes / 1_000.0)
}

private data class Preview(val title: String, val thumbnail: String?, val duration: Int, val uploader: String?, val videoSizeBytes: Long?, val audioSizeBytes: Long?)

@Composable
fun HomeScreen(initialUrl: String? = null) {
    val context = LocalContext.current
    val app = context.applicationContext as LinkGrabApp
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val settings by app.settingsRepository.settings.collectAsState(initial = dev.personal.linkgrab.model.AppSettings())
    val jobs by DownloadStore.items.collectAsState()
    var input by remember(initialUrl) { mutableStateOf(initialUrl.orEmpty()) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    var type by remember { mutableStateOf(DownloadType.VIDEO) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var sizeLimitMb by remember { mutableStateOf<Int?>(null) }
    var duplicate by remember { mutableStateOf(false) }
    var showWelcome by remember { mutableStateOf(!context.getSharedPreferences("onboarding", 0).getBoolean("seen", false)) }
    val scheduled by produceState(initialValue = 0) {
        value = withContext(Dispatchers.IO) { androidx.work.WorkManager.getInstance(context).getWorkInfosByTag("linkgrab-scheduled").get().filter { !it.state.isFinished }.size }
    }
    val activeCount = jobs.count { it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.QUEUED }
    var pillFlash by remember { mutableStateOf<String?>(null) }
    val completedCount = jobs.count { it.status == DownloadStatus.COMPLETED }
    var lastCompleted by remember { mutableStateOf(completedCount) }
    LaunchedEffect(completedCount) {
        if (completedCount > lastCompleted) pillFlash = "SAVED!"
        lastCompleted = completedCount
    }
    LaunchedEffect(pillFlash) { if (pillFlash != null) { delay(1800); pillFlash = null } }
    LaunchedEffect(Unit) { if (input.isBlank()) clipboard.getText()?.text?.let { UrlParser.extractUrls(it).firstOrNull()?.let { input = it } } }
    LaunchedEffect(input) {
        val url = UrlParser.extractUrls(input).firstOrNull() ?: UrlParser.normalizeUrl(input)
        if (url != null && preview == null && !loading) {
            delay(300)
            loading = true
            error = null
            scope.launch(Dispatchers.IO) {
                try {
                    val info = MediaInfoFetcher.fetch(context, url)
                    withContext(Dispatchers.Main) {
                        input = url
                        preview = Preview(info.title ?: "Untitled", info.thumbnail, info.duration, info.uploader, MediaInfoSizer.estimate(info, false), MediaInfoSizer.estimate(info, true))
                        loading = false
                    }
                } catch (failure: Throwable) {
                    android.util.Log.w("LinkGrab", "getInfo failed for $url", failure)
                    withContext(Dispatchers.Main) { error = ErrorMapper.map(failure, context).message; loading = false }
                }
            }
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: wordmark + status pill
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("LINKGRAB", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                PopPill(pillFlash ?: if (activeCount > 0) "$activeCount downloading" else "Ready", highlight = pillFlash != null)
            }
        }
        // Headline
        item {
            Column {
                Text("GRAB\nANY\nTHING.", fontSize = 56.sp, lineHeight = 54.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
                Text("Share a link here, or paste one below.", fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
                if (scheduled > 0) Text("$scheduled scheduled download${if (scheduled == 1) "" else "s"} waiting for Wi‑Fi and charging", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            }
        }
        // Input + multi-link actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                UrlInputBar(value = input, onValueChange = { input = it; preview = null }, onPasteFromClipboard = { clipboard.getText()?.text?.let { input = it } }, onSubmit = {
                    val url = UrlParser.extractUrls(input).firstOrNull() ?: UrlParser.normalizeUrl(input)
                    if (url == null) { error = "Paste a public web link. Links must start with http:// or https://."; return@UrlInputBar }
                    input = url; loading = true; error = null
                    scope.launch(Dispatchers.IO) {
                        try {
                            val info = MediaInfoFetcher.fetch(context, url)
                            withContext(Dispatchers.Main) {
                                preview = Preview(info.title ?: "Untitled", info.thumbnail, info.duration, info.uploader, MediaInfoSizer.estimate(info, false), MediaInfoSizer.estimate(info, true))
                                loading = false
                            }
                        } catch (failure: Throwable) {
                            android.util.Log.w("LinkGrab", "getInfo failed for $url", failure)
                            withContext(Dispatchers.Main) { error = ErrorMapper.map(failure, context).message; loading = false }
                        }
                    }
                })
                val allUrls = UrlParser.extractUrls(input)
                if (allUrls.size > 1) {
                    Text("${allUrls.size} links detected", style = MaterialTheme.typography.titleMedium)
                    StickerButton(
                        text = "QUEUE ALL NOW",
                        onClick = { allUrls.forEach { DownloadService.enqueue(context, it, DownloadType.VIDEO, settings.audioFormat, settings.qualityCeiling) }; input = "" },
                        modifier = Modifier.fillMaxWidth()
                    )
                    StickerButton(
                        text = "DOWNLOAD LATER · WI‑FI + CHARGING",
                        onClick = { allUrls.forEach { DownloadLaterScheduler.schedule(context, it, DownloadType.VIDEO, settings.audioFormat, settings.qualityCeiling) }; input = "" },
                        modifier = Modifier.fillMaxWidth(),
                        container = Sticker.White,
                        content = Sticker.Ink,
                        fontSize = 14.sp,
                        maxLines = 2
                    )
                }
            }
        }
        if (loading) item {
            Column(Modifier.fillMaxWidth().sticker(Sticker.White, 16.dp, 4.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Sticker.Ink, trackColor = Sticker.Track)
                Text("Checking the link…", fontWeight = FontWeight.Bold)
            }
        }
        error?.let { message -> item {
            Text(message, Modifier.fillMaxWidth().sticker(Sticker.Pink, 16.dp, 4.dp).padding(16.dp), color = Sticker.Black, fontWeight = FontWeight.Bold)
        } }
        preview?.let { data -> item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetadataCard(
                    title = data.title,
                    thumbnailUrl = data.thumbnail,
                    duration = "%d:%02d".format(data.duration / 60, data.duration % 60),
                    uploader = data.uploader,
                    estimatedSizeBytes = if (type == DownloadType.VIDEO) data.videoSizeBytes else data.audioSizeBytes,
                    downloadType = if (type == DownloadType.VIDEO) dev.personal.linkgrab.service.MediaType.VIDEO else dev.personal.linkgrab.service.MediaType.AUDIO
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StickerChip(label = "VIDEO · ${settings.qualityCeiling}p", selected = type == DownloadType.VIDEO, onClick = { type = DownloadType.VIDEO }, modifier = Modifier.weight(1f), height = 60.dp, fontSize = 16.sp)
                    StickerChip(label = "AUDIO · ${settings.audioFormat.name}", selected = type == DownloadType.AUDIO, onClick = { type = DownloadType.AUDIO }, modifier = Modifier.weight(1f), height = 60.dp, fontSize = 16.sp)
                }
                Text("SIZE LIMIT (OPTIONAL)", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(end = 4.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf<Int?>(null, 30, 50, 100).forEach { limit ->
                        StickerChip(label = limit?.let { "$it MB" } ?: "No limit", selected = sizeLimitMb == limit, onClick = { sizeLimitMb = limit }, selectedColor = Sticker.Pink, selectedContent = Sticker.Black, height = 44.dp, fontSize = 14.sp)
                    }
                }
                StickerButton(
                    text = "DOWNLOAD NOW",
                    onClick = { if (app.historyRepository.hasDownloaded(input, type.name)) duplicate = true else { DownloadService.enqueue(context, input, type, settings.audioFormat, settings.qualityCeiling, sizeLimitMb?.times(1_000_000L)); pillFlash = "GRABBED!"; preview = null; input = "" } },
                    modifier = Modifier.fillMaxWidth(),
                    container = Sticker.Ink,
                    content = Sticker.Yellow,
                    height = 72.dp,
                    radius = 18.dp,
                    shadowOffset = 6.dp,
                    shadowColor = Sticker.Pink,
                    haptic = true,
                    fontSize = 22.sp
                )
                StickerButton(
                    text = "DOWNLOAD LATER · WI‑FI + CHARGING",
                    onClick = { DownloadLaterScheduler.schedule(context, input, type, settings.audioFormat, qualityForLimit(settings.qualityCeiling, sizeLimitMb, data.videoSizeBytes)); preview = null; input = "" },
                    modifier = Modifier.fillMaxWidth(),
                    container = Sticker.White,
                    content = Sticker.Ink,
                    fontSize = 14.sp,
                    maxLines = 2
                )
                Text(
                    if (type == DownloadType.VIDEO) "Saves to Movies/LinkGrab" else "Saves to Music/LinkGrab",
                    Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } }
        if (jobs.isNotEmpty()) {
            item { Text("DOWNLOAD QUEUE", style = MaterialTheme.typography.titleLarge) }
            items(jobs, key = { it.id }) { job ->
                Column(Modifier.fillMaxWidth().sticker(Sticker.White, 16.dp, 4.dp)) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = { Icon(if (job.type == DownloadType.VIDEO) Icons.Default.Videocam else Icons.Default.AudioFile, contentDescription = job.type.name) },
                        headlineContent = { Text(job.title ?: "Preparing…", maxLines = 2) },
                        supportingContent = { Text(queueDetail(job)) },
                        trailingContent = { when (job.status) { DownloadStatus.RUNNING, DownloadStatus.QUEUED -> TextButton(onClick = { DownloadStore.cancel(context, job.id) }) { Text("Pause") }; DownloadStatus.PAUSED, DownloadStatus.FAILED, DownloadStatus.CANCELLED -> TextButton(onClick = { DownloadService.enqueue(context, job.url, job.type, job.audioFormat, job.quality) }) { Text("Resume") }; else -> Text("Saved") } }
                    )
                    if (job.status == DownloadStatus.RUNNING) {
                        LinearProgressIndicator(
                            progress = { job.progress.toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                            color = Sticker.Ink,
                            trackColor = Sticker.Track
                        )
                    }
                }
            }
        }
    }
    if (duplicate) AlertDialog(onDismissRequest = { duplicate = false }, title = { Text("Already downloaded") }, text = { Text("You already downloaded this item. Open the existing copy or download it again?") }, confirmButton = { TextButton({ duplicate = false }) { Text("Open existing") } }, dismissButton = { TextButton({ DownloadService.enqueue(context, input, type, settings.audioFormat, settings.qualityCeiling, sizeLimitMb?.times(1_000_000L)); duplicate = false; preview = null; input = "" }) { Text("Download again") } })
    if (showWelcome) AlertDialog(onDismissRequest = { showWelcome = false; context.getSharedPreferences("onboarding", 0).edit().putBoolean("seen", true).apply() }, title = { Text("Welcome to LinkGrab") }, text = { Text("Share links from your apps, paste a link, or use the clipboard tile. Downloads are saved in Movies/LinkGrab or Music/LinkGrab. Scheduled downloads wait for Wi‑Fi and charging.") }, confirmButton = { Button({ showWelcome = false; context.getSharedPreferences("onboarding", 0).edit().putBoolean("seen", true).apply() }) { Text("Got it") } })
}

private fun qualityForLimit(default: Int, limitMb: Int?, estimatedVideoBytes: Long?): Int {
    if (limitMb == null || estimatedVideoBytes == null || estimatedVideoBytes <= limitMb * 1_000_000L) return default
    return when {
        estimatedVideoBytes <= limitMb * 2_000_000L -> 480
        else -> 360
    }
}

private fun queueDetail(job: dev.personal.linkgrab.model.DownloadItem): String = when (job.status) {
    DownloadStatus.RUNNING -> when (job.etaState) {
        "STALLED" -> "Stalled — retry"
        "ESTIMATING" -> "${formatBytes(job.downloadedBytes)} of ${job.totalBytes?.let(::formatBytes) ?: "unknown size"} · ${dev.personal.linkgrab.util.EtaFormatter.speed(job.speedBytesPerSecond)} · ${dev.personal.linkgrab.util.EtaFormatter.duration(job.etaSeconds)} left"
        else -> "Preparing… ${(job.progress * 100).toInt()}%"
    }
    DownloadStatus.FAILED -> job.errorMessage ?: "Failed"
    DownloadStatus.PAUSED -> "Paused — resume when ready"
    DownloadStatus.COMPLETED -> "Saved to ${if (job.type == DownloadType.VIDEO) "Movies" else "Music"}/LinkGrab · ${formatBytes(job.sizeBytes)}"
    else -> job.status.name.lowercase().replaceFirstChar { it.titlecase() }
}
