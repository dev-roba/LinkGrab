package dev.personal.linkgrab.data

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dev.personal.linkgrab.model.AudioFormat
import dev.personal.linkgrab.model.DownloadItem
import dev.personal.linkgrab.model.DownloadRequest
import dev.personal.linkgrab.model.DownloadStatus
import dev.personal.linkgrab.model.DownloadType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/**
 * DownloadManager handles the download queue and yt-dlp execution.
 * Uses a single-worker channel to serialize downloads.
 */
class DownloadManager(private val context: Context) {

    private val jobCounter = AtomicLong(System.currentTimeMillis())
    private val downloadChannel = Channel<DownloadRequest>(capacity = 100)
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isProcessing = false

    // State exposed to UI
    private val _jobs = MutableStateFlow<List<DownloadItem>>(emptyList())
    val jobs: StateFlow<List<DownloadItem>> = _jobs

    init {
        startProcessor()
    }

    /**
     * Enqueues a new download request.
     */
    fun enqueue(request: DownloadRequest): Long {
        val id = jobCounter.incrementAndGet()
        val item = DownloadItem(
            id = id,
            url = request.url,
            title = null,
            type = request.type,
            status = DownloadStatus.QUEUED,
            progress = 0f,
            errorMessage = null,
            outputPath = null,
            createdAt = System.currentTimeMillis()
        )
        _jobs.value = _jobs.value + item
        scope.launch { downloadChannel.send(request) }
        return id
    }

    /**
     * Starts the background processor that consumes the download channel.
     */
    private fun startProcessor() {
        if (isProcessing) return
        isProcessing = true

        scope.launch {
            for (request in downloadChannel) {
                processDownload(request)
            }
        }
    }

    /**
     * Processes a single download request.
     */
    private fun processDownload(request: DownloadRequest) {
        val id = jobCounter.get()
        updateStatus(id, DownloadStatus.RUNNING, progress = 0f)

        scope.launch {
            try {
                ensureYoutubeDL(context)
                val instance = YoutubeDL.getInstance()

                // First, get metadata for title
                val info = instance.getInfo(request.url)
                val title = info.title ?: request.url
                updateTitle(id, title)

                // Build output path
                val outputDir = getOutputDirectory(request.type)
                outputDir.mkdirs()
                val outputTemplate = FormatSelector.outputTemplate(request.type)
                val outputFile = File(outputDir, outputTemplate)

                // Build yt-dlp request
                val ytRequest = YoutubeDLRequest(request.url)
                    .addOption("-f", FormatSelector.videoFormat(request.qualityCeiling))
                    .addOption("-o", outputFile.absolutePath)

                // Add audio post-processing if audio download
                if (request.type == DownloadType.AUDIO) {
                    FormatSelector.audioPostProcessorArgs(request.audioFormat).forEach { arg ->
                        val parts = arg.split(" ")
                        if (parts.size >= 2) {
                            ytRequest.addOption(parts[0], parts[1])
                        }
                    }
                }

                // Execute download
                instance.execute(ytRequest, null)

                // Find the downloaded file
                val downloadedFiles = outputDir.listFiles()?.filter { it.name.contains(title.take(50)) }?.toList() ?: emptyList()
                val outputPath = downloadedFiles.firstOrNull()?.absolutePath

                updateStatus(id, DownloadStatus.COMPLETED, progress = 1f, outputPath = outputPath)

            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error"
                updateStatus(id, DownloadStatus.FAILED, errorMessage = errorMsg)
            }
        }
    }

    /**
     * Gets the appropriate output directory for the download type.
     */
    private fun getOutputDirectory(type: DownloadType): File {
        val envDir = when (type) {
            DownloadType.VIDEO -> android.os.Environment.DIRECTORY_MOVIES
            DownloadType.AUDIO -> android.os.Environment.DIRECTORY_MUSIC
        }
        return context.getExternalFilesDir(envDir)
            ?: File(context.filesDir, "downloads/${type.name.lowercase()}")
    }

    /**
     * Updates the status of a download item.
     */
    private fun updateStatus(
        id: Long,
        status: DownloadStatus,
        progress: Float = 0f,
        errorMessage: String? = null,
        outputPath: String? = null
    ) {
        _jobs.value = _jobs.value.map { item ->
            if (item.id == id) {
                item.copy(
                    status = status,
                    progress = progress,
                    errorMessage = errorMessage,
                    outputPath = outputPath ?: item.outputPath
                )
            } else item
        }
    }

    /**
     * Updates the title of a download item.
     */
    private fun updateTitle(id: Long, title: String) {
        _jobs.value = _jobs.value.map { item ->
            if (item.id == id) item.copy(title = title) else item
        }
    }

    /**
     * Cancels a download by ID.
     */
    fun cancel(id: Long) {
        _jobs.value = _jobs.value.map { item ->
            if (item.id == id && item.status == DownloadStatus.QUEUED) {
                item.copy(status = DownloadStatus.CANCELLED)
            } else item
        }
    }

    /**
     * Clears completed/failed/cancelled items from the queue.
     */
    fun clearHistory() {
        _jobs.value = _jobs.value.filter { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.RUNNING }
    }

    companion object {
        private var ytdlInitialized = false

        @Synchronized
        internal fun ensureYoutubeDL(context: Context) {
            if (!ytdlInitialized) {
                YoutubeDL.getInstance().init(context.applicationContext)
                ytdlInitialized = true
            }
        }
    }
}