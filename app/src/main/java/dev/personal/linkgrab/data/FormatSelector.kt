package dev.personal.linkgrab.data

import dev.personal.linkgrab.model.AudioFormat
import dev.personal.linkgrab.model.DownloadType

object FormatSelector {
    /**
     * Returns the yt-dlp format selector string for video downloads.
     * Selects best video up to qualityCeiling height + best audio, merged to mp4.
     */
    fun videoFormat(qualityCeiling: Int): String {
        return "bv*[vcodec^=avc1][height<=$qualityCeiling]+ba[ext=m4a]/b[ext=mp4][height<=$qualityCeiling]"
    }

    /**
     * Returns the yt-dlp format selector string for audio downloads.
     * Selects best audio, then transcodes to the specified format via ffmpeg.
     */
    fun audioFormat(audioFormat: AudioFormat): String {
        return "ba[ext=m4a]/ba"
    }

    /**
     * Returns the ffmpeg post-processor arguments for audio conversion.
     */
    fun audioPostProcessorArgs(audioFormat: AudioFormat): List<String> {
        return when (audioFormat) {
            AudioFormat.M4A -> listOf("-x", "--audio-format", "m4a", "--audio-quality", "0")
            AudioFormat.MP3 -> listOf("-x", "--audio-format", "mp3", "--audio-quality", "0")
        }
    }

    /**
     * Returns the output template for yt-dlp based on download type.
     */
    fun outputTemplate(downloadType: DownloadType): String {
        return when (downloadType) {
            DownloadType.VIDEO -> "%(title)s.%(ext)s"
            DownloadType.AUDIO -> "%(title)s.%(ext)s"
        }
    }
}
