package dev.personal.linkgrab.model

data class AppSettings(
    val audioFormat: AudioFormat = AudioFormat.MP3,
    val qualityCeiling: Int = 720,
    val autoUpdateYtDlp: Boolean = true,
    val autoUpdateApp: Boolean = true,
    val lastYtDlpUpdateCheck: Long = 0
)
