package dev.personal.linkgrab.model

data class DownloadItem(
    val id: Long,
    val url: String,
    val title: String?,
    val type: DownloadType,
    val status: DownloadStatus,
    val progress: Float,
    val errorMessage: String?,
    val outputPath: String?,
    val sizeBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val totalBytes: Long? = null,
    val speedBytesPerSecond: Double = 0.0,
    val etaSeconds: Long? = null,
    val etaState: String = "CALCULATING",
    val createdAt: Long
    ,val audioFormat: AudioFormat = AudioFormat.MP3,
    val quality: Int = 720
    ,val maxSizeBytes: Long? = null
    ,val tempPath: String? = null
    ,val retryAt: Long? = null,
    val retryAttempt: Int = 0
)
