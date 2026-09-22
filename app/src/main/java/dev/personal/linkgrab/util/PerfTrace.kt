package dev.personal.linkgrab.util

import android.util.Log
import dev.personal.linkgrab.BuildConfig

/** Debug-only timing and progress trace. Never include URLs or personal data. */
object PerfTrace {
    private const val TAG = "LinkGrabPerf"
    private const val YTDLP_TAG = "LinkGrabYtdlp"
    private val enabled = BuildConfig.DEBUG

    fun mark(name: String, value: String? = null) {
        if (!enabled) return
        Log.d(TAG, if (value == null) name else "$name $value")
    }

    fun ytdlp(line: String) {
        if (!enabled) return
        val safe = line.replace(Regex("https?://\\S+"), "<url>")
        if (safe.isEmpty()) return
        safe.chunked(900).forEachIndexed { index, chunk -> Log.d(YTDLP_TAG, "[$index] $chunk") }
    }
}
