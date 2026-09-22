package dev.personal.linkgrab.model

data class DownloadRequest(
    val url: String,
    val type: DownloadType,
    val audioFormat: AudioFormat = AudioFormat.MP3,
    val qualityCeiling: Int = 1080
)
