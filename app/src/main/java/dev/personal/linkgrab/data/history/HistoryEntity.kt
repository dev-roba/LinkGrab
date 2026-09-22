package dev.personal.linkgrab.data.history

data class HistoryEntity(
    val id: Long = 0,
    val url: String,
    val title: String,
    val mimeType: String,
    val mediaUri: String,
    val downloadType: String,
    val sizeBytes: Long = 0,
    val thumbnailUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
