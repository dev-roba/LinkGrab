package dev.personal.linkgrab.service

import android.content.Context
import dev.personal.linkgrab.data.DownloadManager
import dev.personal.linkgrab.model.DownloadItem
import dev.personal.linkgrab.model.DownloadRequest
import dev.personal.linkgrab.model.DownloadStatus
import dev.personal.linkgrab.model.DownloadType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * DownloadQueueManager - UI-facing wrapper around DownloadManager.
 * Provides a reactive job queue for Compose UI.
 */
class DownloadQueueManager(private val context: Context) {

    private val downloadManager = DownloadManager(context)

    // Expose jobs as StateFlow for Compose
    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val jobs: StateFlow<List<DownloadJob>> = _jobs

    init {
        // Observe downloadManager jobs and convert to UI model
        CoroutineScope(Dispatchers.IO).launch {
            downloadManager.jobs.collect { items ->
                _jobs.value = items.map { item ->
                    DownloadJob(
                        id = item.id.toString(),
                        url = item.url,
                        title = item.title ?: item.url,
                        mediaType = when (item.type) {
                            DownloadType.VIDEO -> MediaType.VIDEO
                            DownloadType.AUDIO -> MediaType.AUDIO
                        },
                        status = when (item.status) {
                            DownloadStatus.QUEUED -> JobStatus.QUEUED
                            DownloadStatus.RUNNING -> JobStatus.RUNNING
                            DownloadStatus.PAUSED -> JobStatus.FAILED
                            DownloadStatus.COMPLETED -> JobStatus.COMPLETED
                            DownloadStatus.FAILED -> JobStatus.FAILED
                            DownloadStatus.CANCELLED -> JobStatus.CANCELLED
                        },
                        progress = (item.progress * 100).toInt(),
                        errorMessage = item.errorMessage
                    )
                }
            }
        }
    }

    /**
     * Enqueues a new download job.
     */
    fun enqueue(job: DownloadJob) {
        val request = DownloadRequest(
            url = job.url,
            type = when (job.mediaType) {
                MediaType.VIDEO -> DownloadType.VIDEO
                MediaType.AUDIO -> DownloadType.AUDIO
            }
        )
        downloadManager.enqueue(request)
    }

    /**
     * Cancels a job by ID.
     */
    fun cancel(jobId: String) {
        jobId.toLongOrNull()?.let { downloadManager.cancel(it) }
    }

    /**
     * Clears completed/failed/cancelled jobs.
     */
    fun clearHistory() {
        downloadManager.clearHistory()
    }

    companion object {
        @Volatile
        private var INSTANCE: DownloadQueueManager? = null

        fun getInstance(context: Context): DownloadQueueManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadQueueManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

/**
 * UI-facing download job model.
 */
data class DownloadJob(
    val id: String,
    val url: String,
    val title: String,
    val mediaType: MediaType,
    val status: JobStatus,
    val progress: Int = 0,
    val errorMessage: String? = null
)

enum class JobStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class MediaType {
    VIDEO,
    AUDIO
}
