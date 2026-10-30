package dev.personal.linkgrab.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.ffmpeg.FFmpeg
import dev.personal.linkgrab.LinkGrabApp
import dev.personal.linkgrab.MainActivity
import dev.personal.linkgrab.R
import dev.personal.linkgrab.data.FormatSelector
import dev.personal.linkgrab.data.MediaStoreSaver
import dev.personal.linkgrab.data.MediaInfoCache
import dev.personal.linkgrab.data.MediaInfoFetcher
import dev.personal.linkgrab.data.InstagramCookies
import dev.personal.linkgrab.data.history.HistoryEntity
import dev.personal.linkgrab.model.AudioFormat
import dev.personal.linkgrab.model.DownloadItem
import dev.personal.linkgrab.model.DownloadStatus
import dev.personal.linkgrab.model.DownloadType
import dev.personal.linkgrab.util.ErrorMapper
import dev.personal.linkgrab.util.PerfTrace
import dev.personal.linkgrab.BuildConfig
import java.io.File
import android.os.StatFs
import android.net.Uri
import dev.personal.linkgrab.data.YtDlpUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeDownloads = AtomicInteger(0)
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            DownloadStore.cancel(this, intent.getLongExtra(EXTRA_ID, -1L)); return START_NOT_STICKY
        } else if (intent?.action == ACTION_RESUME) {
            val existing = DownloadStore.items.value.firstOrNull { it.id == intent.getLongExtra(EXTRA_ID, -1L) } ?: return START_NOT_STICKY
            startForeground(NOTIFICATION_ID, notification("Resuming download…", true)); activeDownloads.incrementAndGet(); scope.launch { download(existing, existing.audioFormat, existing.quality) }
        } else if (intent?.action == ACTION_ENQUEUE) {
            val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
            val type = DownloadType.valueOf(intent.getStringExtra(EXTRA_TYPE) ?: DownloadType.VIDEO.name)
            val audio = AudioFormat.valueOf(intent.getStringExtra(EXTRA_AUDIO) ?: AudioFormat.MP3.name)
            val quality = intent.getIntExtra(EXTRA_QUALITY, 1080)
            val maxSize = intent.getLongExtra(EXTRA_MAX_SIZE, 0L).takeIf { it > 0 }
            startForeground(NOTIFICATION_ID, notification("Preparing download…", true))
            val item = DownloadStore.enqueue(url, type, audio, quality, maxSize)
            if (activeDownloads.compareAndSet(0, 1)) scope.launch { download(item, audio, quality) }
        }
        return START_STICKY
    }
    private suspend fun download(item: DownloadItem, audio: AudioFormat, quality: Int) {
        DownloadStore.update(item.id, DownloadStatus.RUNNING)
        try {
            if ((application as LinkGrabApp).historyRepository.hasDownloaded(item.url, item.type.name)) {
                DownloadStore.update(item.id, DownloadStatus.FAILED, errorMessage = "You already downloaded this ${item.type.name.lowercase()}.")
                return
            }
            PerfTrace.mark("download_request", "id=${item.id}")
            ensureYoutubeDL(applicationContext)
            ensureMediaTools(applicationContext)
            val freeBytes = StatFs(cacheDir.absolutePath).availableBytes
            if (freeBytes < 100L * 1024 * 1024) error("Not enough storage space. Free at least 100 MB and retry.")
            val ytdlp = YoutubeDL.getInstance()
            val info = MediaInfoFetcher.fetch(this, item.url)
            val title = info.title ?: "LinkGrab download"
            DownloadStore.title(item.id, title)
            val cache = File(cacheDir, "downloads").apply { mkdirs() }
            DownloadStore.tempPath(item.id, File(cache, "${item.id}.part").absolutePath)
            val instagram = FormatSelector.isInstagramUrl(item.url)
            val defaultFormat = if (item.type == DownloadType.VIDEO) {
                if (instagram) FormatSelector.instagramVideoFormat() else FormatSelector.videoFormat(quality)
            } else {
                FormatSelector.audioFormat(audio)
            }
            val request = YoutubeDLRequest(item.url)
                .addOption("--socket-timeout", "15")
                .addOption("--retries", "3")
                .addOption("--fragment-retries", "3")
                .addOption("--throttled-rate", "100K")
                .addOption("-N", "4")
                .addOption("--continue")
                // Let yt-dlp choose player client (avoids SABR-only android streams)
                .addOption("-f", item.maxSizeBytes?.let { dev.personal.linkgrab.data.MediaInfoSizer.formatUnder(info, item.type == DownloadType.AUDIO, it) } ?: defaultFormat)
                .addOption("--merge-output-format", "mp4")
                .addOption("-o", File(cache, "%(title)s.%(ext)s").absolutePath)
                .also { InstagramCookies.applyIfInstagram(item.url, this, it) }
            android.util.Log.i("LinkGrab", "download url=${item.url} type=${item.type} instagram=$instagram format=$defaultFormat")
            if (BuildConfig.DEBUG) request.addOption("--verbose")
            if (item.type == DownloadType.AUDIO) {
                request.addOption("--embed-metadata")
                request.addOption("--embed-thumbnail")
                FormatSelector.audioPostProcessorArgs(audio).forEachIndexed { index, option ->
                    if (option.startsWith("--") && index + 1 < FormatSelector.audioPostProcessorArgs(audio).size) request.addOption(option, FormatSelector.audioPostProcessorArgs(audio)[index + 1])
                    else if (option == "-x") request.addOption(option)
                }
            }
            PerfTrace.mark("download_start", "id=${item.id}")
            var firstProgress = true
            val estimator = dev.personal.linkgrab.util.EtaEstimator()
            ytdlp.execute(request, item.id.toString()) { progress, eta, output ->
                if (firstProgress) { firstProgress = false; PerfTrace.mark("first_progress", "id=${item.id}") }
                PerfTrace.ytdlp(output ?: "")
                val parsed = ProgressLine.parse(output.orEmpty(), progress, eta)
                val snapshot = estimator.update(parsed.downloadedBytes, parsed.totalBytes, System.currentTimeMillis())
                DownloadStore.progress(item.id, parsed.progress, snapshot, parsed.speedBytesPerSecond)
                notifyProgress(item.id, snapshot, parsed.progress)
            }
            PerfTrace.mark("download_done", "id=${item.id}")
            val output = cache.listFiles()?.filterNot { it.name.endsWith(".part") }?.maxByOrNull { it.lastModified() } ?: error("Download file was not created")
            val extension = output.extension.ifBlank { if (item.type == DownloadType.VIDEO) "mp4" else audio.name.lowercase() }
            val sizeBytes = output.length()
            val saved = MediaStoreSaver.save(this, output, "$title.$extension", item.type)
            output.delete()
            (application as LinkGrabApp).historyRepository.add(HistoryEntity(url = item.url, title = title, mimeType = saved.second, mediaUri = saved.first, downloadType = item.type.name, sizeBytes = sizeBytes, thumbnailUrl = info.thumbnail))
            DownloadStore.update(item.id, DownloadStatus.COMPLETED, outputPath = saved.first, sizeBytes = sizeBytes)
            notify("Saved $title (${formatBytes(sizeBytes)})", false)
        } catch (error: Throwable) {
            android.util.Log.e("LinkGrab", "download error for url=${item.url}", error)
            PerfTrace.ytdlp("download_error type=${error.javaClass.name} message=${error.message.orEmpty()}")
            val host = runCatching { Uri.parse(item.url).host.orEmpty() }.getOrDefault("")
            if (host.contains("instagram") || host.contains("tiktok")) scope.launch { YtDlpUpdater.update(applicationContext) }
            val friendly = ErrorMapper.map(error, this)
            DownloadStore.update(item.id, DownloadStatus.FAILED, errorMessage = friendly.message)
            notify(friendly.message, false)
        } finally {
            if (activeDownloads.decrementAndGet() == 0) {
                val next = DownloadStore.items.value.firstOrNull { it.status == DownloadStatus.QUEUED }
                if (next != null && activeDownloads.compareAndSet(0, 1)) scope.launch { download(next, next.audioFormat, next.quality) }
                else { stopForeground(STOP_FOREGROUND_DETACH); stopSelf() }
            }
        }
    }
    private fun notify(text: String, ongoing: Boolean) = (getSystemService(NotificationManager::class.java)).notify(NOTIFICATION_ID, notification(text, ongoing))
    private var lastNotificationMillis = 0L
    private fun notifyProgress(id: Long, snapshot: dev.personal.linkgrab.util.EtaSnapshot, progress: Float) {
        val now = System.currentTimeMillis()
        if (now - lastNotificationMillis < 1_000) return
        lastNotificationMillis = now
        val percent = (progress * 100).toInt()
        val detail = when (snapshot.state) {
            dev.personal.linkgrab.util.EtaState.CALCULATING -> "Preparing…"
            dev.personal.linkgrab.util.EtaState.STALLED -> "Stalled — retry"
            dev.personal.linkgrab.util.EtaState.DONE -> "Complete"
            dev.personal.linkgrab.util.EtaState.ESTIMATING -> "${percent}% · ${dev.personal.linkgrab.util.EtaFormatter.status(snapshot)}"
        }
        notify(detail, true)
    }
    private fun notification(text: String, ongoing: Boolean): android.app.Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "LinkGrab downloads", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val cancel = PendingIntent.getService(this, 91, Intent(this, DownloadService::class.java).setAction(ACTION_CANCEL).putExtra(EXTRA_ID, DownloadStore.items.value.firstOrNull { it.status == DownloadStatus.RUNNING }?.id ?: -1L), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("LinkGrab").setContentText(text).setContentIntent(open).setOngoing(ongoing).addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancel).build()
    }
    companion object {
        private const val CHANNEL = "downloads"; private const val NOTIFICATION_ID = 7
        const val ACTION_ENQUEUE = "dev.personal.linkgrab.ENQUEUE"; const val ACTION_RESUME = "dev.personal.linkgrab.RESUME"; const val ACTION_CANCEL = "dev.personal.linkgrab.CANCEL"; const val EXTRA_URL = "url"; const val EXTRA_TYPE = "type"; const val EXTRA_AUDIO = "audio"; const val EXTRA_QUALITY = "quality"; const val EXTRA_MAX_SIZE = "max_size"; const val EXTRA_ID = "id"
        @Volatile private var initialized = false
        @Synchronized private fun ensureYoutubeDL(context: Context) { if (!initialized) { YoutubeDL.getInstance().init(context); initialized = true } }
        private fun ensureMediaTools(context: Context) {
            FFmpeg.getInstance().init(context)
            val nativeDir = File(context.applicationInfo.nativeLibraryDir)
            val ffmpegLibDir = File(context.noBackupFilesDir, "youtubedl-android/packages/ffmpeg/usr/lib")
            val pythonLibDir = File(context.noBackupFilesDir, "youtubedl-android/packages/python/usr/lib")
            val toolsDir = File(context.filesDir, "media-tools").apply { mkdirs() }
            val ffmpeg = File(toolsDir, "ffmpeg")
            val ffprobe = File(toolsDir, "ffprobe")
            val nativePath = nativeDir.absolutePath
            val libraryPath = "$nativePath:${ffmpegLibDir.absolutePath}:${pythonLibDir.absolutePath}"
            ffmpeg.writeText("#!/system/bin/sh\nexport LD_LIBRARY_PATH='$libraryPath':\$LD_LIBRARY_PATH\nexec '$nativePath/libffmpeg.so' \"\$@\"\n")
            ffprobe.writeText("#!/system/bin/sh\nexport LD_LIBRARY_PATH='$libraryPath':\$LD_LIBRARY_PATH\nexec '$nativePath/libffprobe.so' \"\$@\"\n")
            ffmpeg.setExecutable(true, false)
            ffprobe.setExecutable(true, false)
            runCatching {
                val field = YoutubeDL::class.java.getDeclaredField("ffmpegPath")
                field.isAccessible = true
                field.set(null, File(nativeDir, "libffmpeg.so"))
            }
        }
        private fun formatBytes(bytes: Long): String = when {
            bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
            else -> "%.0f KB".format(bytes / 1_000.0)
        }
        fun enqueue(context: Context, url: String, type: DownloadType, audio: AudioFormat, quality: Int, maxSizeBytes: Long? = null) {
            context.startForegroundService(Intent(context, DownloadService::class.java).setAction(ACTION_ENQUEUE).putExtra(EXTRA_URL, url).putExtra(EXTRA_TYPE, type.name).putExtra(EXTRA_AUDIO, audio.name).putExtra(EXTRA_QUALITY, quality).putExtra(EXTRA_MAX_SIZE, maxSizeBytes ?: 0L))
        }
        fun resumeQueued(context: Context) {
            DownloadStore.items.value.firstOrNull { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.RUNNING }?.let { item -> context.startForegroundService(Intent(context, DownloadService::class.java).setAction(ACTION_RESUME).putExtra(EXTRA_ID, item.id)) }
        }
    }
}

private data class ParsedProgress(val progress: Float, val downloadedBytes: Long, val totalBytes: Long?, val speedBytesPerSecond: Double)

private object ProgressLine {
    private val totalPattern = Regex("of\\s+([0-9.]+)([KMG]?i?B)")
    private val speedPattern = Regex("at\\s+([0-9.]+)([KMG]?i?B)/s")
    fun parse(line: String, rawProgress: Float, eta: Long): ParsedProgress {
        val progress = (rawProgress / 100f).coerceIn(0f, 1f)
        val totalMatch = totalPattern.find(line)
        val total = totalMatch?.let { bytes(it.groupValues[1].toDouble(), it.groupValues[2]) }
        val downloaded = total?.let { (it * progress).toLong() } ?: 0L
        val speed = speedPattern.find(line)?.let { bytes(it.groupValues[1].toDouble(), it.groupValues[2]) / 1.0 } ?: 0.0
        return ParsedProgress(progress, downloaded, total, speed)
    }
    private fun bytes(value: Double, unit: String): Long {
        val multiplier = when (unit.lowercase()) { "kb", "kib" -> 1_024.0; "mb", "mib" -> 1_048_576.0; "gb", "gib" -> 1_073_741_824.0; else -> 1.0 }
        return (value * multiplier).toLong()
    }
}

object DownloadStore {
    private var nextId = 0L
    private val mutableItems = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = mutableItems
    @Synchronized fun enqueue(url: String, type: DownloadType, audio: AudioFormat = AudioFormat.MP3, quality: Int = 720, maxSizeBytes: Long? = null): DownloadItem {
        val item = DownloadItem(id = ++nextId, url = url, title = null, type = type, status = DownloadStatus.QUEUED, progress = 0f, errorMessage = null, outputPath = null, createdAt = System.currentTimeMillis(), audioFormat = audio, quality = quality, maxSizeBytes = maxSizeBytes); mutableItems.value += item; persist(); return item
    }
    fun update(id: Long, status: DownloadStatus, errorMessage: String? = null, outputPath: String? = null, sizeBytes: Long = 0) { mutableItems.value = mutableItems.value.map { if (it.id == id) it.copy(status = status, progress = if (status == DownloadStatus.COMPLETED) 1f else it.progress, errorMessage = errorMessage, outputPath = outputPath ?: it.outputPath, sizeBytes = if (sizeBytes > 0) sizeBytes else it.sizeBytes, etaState = if (status == DownloadStatus.COMPLETED) "DONE" else it.etaState) else it }; persist() }
    fun progress(id: Long, value: Float, snapshot: dev.personal.linkgrab.util.EtaSnapshot, speedBytesPerSecond: Double) { mutableItems.value = mutableItems.value.map { if (it.id == id) it.copy(progress = value.coerceIn(0f, 1f), downloadedBytes = snapshot.downloadedBytes, totalBytes = snapshot.totalBytes, speedBytesPerSecond = speedBytesPerSecond, etaSeconds = snapshot.etaSeconds, etaState = snapshot.state.name) else it }; persist() }
    fun cancel(context: Context, id: Long) {
        YoutubeDL.getInstance().destroyProcessById(id.toString())
        update(id, DownloadStatus.PAUSED)
    }
    fun title(id: Long, value: String) { mutableItems.value = mutableItems.value.map { if (it.id == id) it.copy(title = value) else it }; persist() }
    fun tempPath(id: Long, value: String) { mutableItems.value = mutableItems.value.map { if (it.id == id) it.copy(tempPath = value) else it }; persist() }
    fun init(context: Context) { prefs = context.getSharedPreferences("download_queue", Context.MODE_PRIVATE); val json = org.json.JSONArray(prefs?.getString("items", "[]") ?: "[]"); val restored = (0 until json.length()).mapNotNull { runCatching { val o=json.getJSONObject(it); DownloadItem(o.getLong("id"),o.getString("url"),o.optString("title").ifBlank{null},DownloadType.valueOf(o.getString("type")),DownloadStatus.valueOf(o.getString("status")),o.optDouble("progress",0.0).toFloat(),o.optString("error").ifBlank{null},o.optString("output").ifBlank{null},o.optLong("size"),o.optLong("downloaded"),if(o.has("total"))o.optLong("total") else null,o.optDouble("speed"),if(o.has("eta"))o.optLong("eta") else null,o.optString("etaState","CALCULATING"),o.optLong("created"),AudioFormat.valueOf(o.optString("audio","MP3")),o.optInt("quality",720),if(o.has("maxSize"))o.optLong("maxSize") else null,o.optString("tempPath").ifBlank{null},if(o.has("retryAt"))o.optLong("retryAt") else null,o.optInt("retryAttempt",0)) }.getOrNull() }; mutableItems.value=restored; nextId=restored.maxOfOrNull{it.id}?:0L }
    private var prefs: android.content.SharedPreferences? = null
    private fun persist() { val json = org.json.JSONArray(); mutableItems.value.forEach { o -> json.put(org.json.JSONObject().apply { put("id",o.id);put("url",o.url);put("title",o.title);put("type",o.type.name);put("status",o.status.name);put("progress",o.progress);put("error",o.errorMessage);put("output",o.outputPath);put("size",o.sizeBytes);put("downloaded",o.downloadedBytes);put("total",o.totalBytes);put("speed",o.speedBytesPerSecond);put("eta",o.etaSeconds);put("etaState",o.etaState);put("created",o.createdAt);put("audio",o.audioFormat.name);put("quality",o.quality);put("maxSize",o.maxSizeBytes);put("tempPath",o.tempPath);put("retryAt",o.retryAt);put("retryAttempt",o.retryAttempt) }) }; prefs?.edit()?.putString("items", json.toString())?.apply() }
}
