package dev.personal.linkgrab.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yausername.youtubedl_android.YoutubeDL
import dev.personal.linkgrab.LinkGrabApp
import dev.personal.linkgrab.ensureYoutubeDL
import dev.personal.linkgrab.data.UrlParser
import dev.personal.linkgrab.data.MediaInfoCache
import dev.personal.linkgrab.data.MediaInfoSizer
import dev.personal.linkgrab.model.DownloadStatus
import dev.personal.linkgrab.model.DownloadType
import dev.personal.linkgrab.service.DownloadService
import dev.personal.linkgrab.service.DownloadStore
import dev.personal.linkgrab.service.DownloadLaterScheduler
import dev.personal.linkgrab.ui.components.MetadataCard
import dev.personal.linkgrab.ui.components.UrlInputBar
import dev.personal.linkgrab.util.ErrorMapper
import dev.personal.linkgrab.util.PerfTrace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.io.File
import com.yausername.youtubedl_android.YoutubeDLRequest

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
    LaunchedEffect(Unit) { if (input.isBlank()) clipboard.getText()?.text?.let { UrlParser.extractUrls(it).firstOrNull()?.let { input = it } } }
    LaunchedEffect(input) {
        val url = UrlParser.extractUrls(input).firstOrNull() ?: UrlParser.normalizeUrl(input)
        if (url != null && preview == null && !loading) {
            delay(300)
            loading = true
            error = null
            scope.launch(Dispatchers.IO) {
                try { PerfTrace.mark("fetchInfo_start"); ensureYoutubeDL(context); val info = MediaInfoCache.get(context, url) ?: YoutubeDL.getInstance().getInfo(YoutubeDLRequest(url).addOption("--cache-dir", File(context.cacheDir, "ytdlp-cache").absolutePath)).also { MediaInfoCache.put(context, url, it) }; PerfTrace.mark("fetchInfo_end"); withContext(Dispatchers.Main) { input = url; preview = Preview(info.title ?: "Untitled", info.thumbnail, info.duration, info.uploader, MediaInfoSizer.estimate(info, false), MediaInfoSizer.estimate(info, true)); loading = false } }
                catch (failure: Throwable) { withContext(Dispatchers.Main) { error = ErrorMapper.map(failure, context).message; loading = false } }
            }
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("LinkGrab", style = MaterialTheme.typography.headlineMedium); Text("Share a link here, or paste one below."); if (scheduled > 0) Text("$scheduled scheduled download${if (scheduled == 1) "" else "s"} waiting for Wi‑Fi and charging", color = MaterialTheme.colorScheme.primary) }
        item { UrlInputBar(value = input, onValueChange = { input = it; preview = null }, onPasteFromClipboard = { clipboard.getText()?.text?.let { input = it } }, onSubmit = {
            val url = UrlParser.extractUrls(input).firstOrNull() ?: UrlParser.normalizeUrl(input)
            if (url == null) { error = "Paste a public web link. Links must start with http:// or https://."; return@UrlInputBar }
            input = url; loading = true; error = null
            scope.launch(Dispatchers.IO) {
                try { PerfTrace.mark("fetchInfo_start"); ensureYoutubeDL(context); val info = YoutubeDL.getInstance().getInfo(YoutubeDLRequest(url).addOption("--cache-dir", File(context.cacheDir, "ytdlp-cache").absolutePath)); MediaInfoCache.put(context, url, info); PerfTrace.mark("fetchInfo_end"); withContext(Dispatchers.Main) { preview = Preview(info.title ?: "Untitled", info.thumbnail, info.duration, info.uploader, MediaInfoSizer.estimate(info, false), MediaInfoSizer.estimate(info, true)); loading = false } }
                catch (failure: Throwable) { withContext(Dispatchers.Main) { error = ErrorMapper.map(failure, context).message; loading = false } }
            }
        })
            val allUrls = UrlParser.extractUrls(input)
            if (allUrls.size > 1) {
                Text("${allUrls.size} links detected", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { allUrls.forEach { DownloadService.enqueue(context, it, DownloadType.VIDEO, settings.audioFormat, settings.qualityCeiling) }; input = "" }) { Text("Queue all now") }
                OutlinedButton(onClick = { allUrls.forEach { DownloadLaterScheduler.schedule(context, it, DownloadType.VIDEO, settings.audioFormat, settings.qualityCeiling) }; input = "" }) { Text("Download later on Wi‑Fi + charging") }
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Checking the link…") }
        error?.let { message -> item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Text(message, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer) } } }
        preview?.let { data -> item {
            MetadataCard(title = data.title, thumbnailUrl = data.thumbnail, duration = "%d:%02d".format(data.duration / 60, data.duration % 60), uploader = data.uploader, estimatedSizeBytes = if (type == DownloadType.VIDEO) data.videoSizeBytes else data.audioSizeBytes, downloadType = if (type == DownloadType.VIDEO) dev.personal.linkgrab.service.MediaType.VIDEO else dev.personal.linkgrab.service.MediaType.AUDIO, onDownloadClick = { if (app.historyRepository.hasDownloaded(input, type.name)) duplicate = true else { DownloadService.enqueue(context, input, type, settings.audioFormat, settings.qualityCeiling, sizeLimitMb?.times(1_000_000L)); preview = null; input = "" } })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) { FilterChip(selected = type == DownloadType.VIDEO, onClick = { type = DownloadType.VIDEO }, label = { Text("Video · up to ${settings.qualityCeiling}p") }); FilterChip(selected = type == DownloadType.AUDIO, onClick = { type = DownloadType.AUDIO }, label = { Text("Audio · ${settings.audioFormat.name}") }) }
            Text("Size limit (optional)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf<Int?>(null, 30, 50, 100).forEach { limit -> FilterChip(selected = sizeLimitMb == limit, onClick = { sizeLimitMb = limit }, label = { Text(limit?.let { "${it} MB" } ?: "No limit") }) }
            }
            OutlinedButton(onClick = { DownloadLaterScheduler.schedule(context, input, type, settings.audioFormat, qualityForLimit(settings.qualityCeiling, sizeLimitMb, data.videoSizeBytes)); preview = null; input = "" }) { Text("Download later on Wi‑Fi + charging") }
        } }
        if (jobs.isNotEmpty()) { item { Text("Download queue", style = MaterialTheme.typography.titleLarge) }; items(jobs, key = { it.id }) { job -> ListItem(leadingContent = { Icon(if (job.type == DownloadType.VIDEO) Icons.Default.Videocam else Icons.Default.AudioFile, contentDescription = job.type.name) }, headlineContent = { Text(job.title ?: "Preparing…") }, supportingContent = { Text(queueDetail(job)) }, trailingContent = { when (job.status) { DownloadStatus.RUNNING, DownloadStatus.QUEUED -> TextButton(onClick = { DownloadStore.cancel(context, job.id) }) { Text("Pause") }; DownloadStatus.PAUSED, DownloadStatus.FAILED, DownloadStatus.CANCELLED -> TextButton(onClick = { DownloadService.enqueue(context, job.url, job.type, job.audioFormat, job.quality) }) { Text("Resume") }; else -> Text("Saved") } }) } }
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
