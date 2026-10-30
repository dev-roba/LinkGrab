package dev.personal.linkgrab.data

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import dev.personal.linkgrab.ensureYoutubeDL
import dev.personal.linkgrab.util.PerfTrace
import java.io.File

/**
 * Shared metadata fetch: memory/disk cache first, then yt-dlp getInfo.
 * Used by HomeScreen preview and DownloadService so check → download
 * does not run extraction twice.
 */
object MediaInfoFetcher {
    fun fetch(context: Context, url: String): VideoInfo {
        PerfTrace.mark("fetchInfo_start")
        MediaInfoCache.get(context, url)?.let {
            PerfTrace.mark("fetchInfo_end", "cache_hit")
            return it
        }
        ensureYoutubeDL(context)
        val cacheDir = File(context.cacheDir, "ytdlp-cache").apply { mkdirs() }
        val request = YoutubeDLRequest(url)
            .addOption("--cache-dir", cacheDir.absolutePath)
            .addOption("--socket-timeout", "15")
            .addOption("--retries", "2")
        InstagramCookies.applyIfInstagram(url, context, request)
        val info = YoutubeDL.getInstance().getInfo(request)
        MediaInfoCache.put(context, url, info)
        PerfTrace.mark("fetchInfo_end", "network")
        return info
    }
}
