package dev.personal.linkgrab.data

import android.content.Context
import java.io.File

object InstagramCookies {
    private val SAFE_NAME = Regex("[A-Za-z0-9_]+")
    private val UNSAFE_VALUE = Regex("[\\r\\n\\t]")
    private val COOKIE_PREFIX = Regex("^Cookie\\s*:\\s*", RegexOption.IGNORE_CASE)

    /**
     * Converts an Instagram cookie header (the browser "Cookie:" header text, e.g.
     * "sessionid=…; csrftoken=…; ig_did=…") into a Netscape-format cookies.txt body
     * suitable for yt-dlp's --cookies option. Returns an empty string when the input
     * contains no usable cookie tokens.
     */
    fun toNetscape(raw: String, deadlineSeconds: Long = System.currentTimeMillis() / 1000L + 365L * 24 * 3600L): String {
        val cleaned = raw.trim()
            .replaceFirst(COOKIE_PREFIX, "")
            .trim()
            .removeSurrounding("\"")
            .trim()
        if (cleaned.isBlank()) return ""
        val lines = cleaned.split(';').mapNotNull { token ->
            val t = token.trim()
            if (t.isBlank() || !t.contains('=')) return@mapNotNull null
            val name = t.substringBefore('=').trim()
            if (!name.matches(SAFE_NAME)) return@mapNotNull null
            val value = t.substringAfter('=').trim().replace(UNSAFE_VALUE, "")
            if (value.isBlank()) return@mapNotNull null
            val domain = if (name.equals("sessionid", ignoreCase = true)) "#HttpOnly_.instagram.com" else ".instagram.com"
            listOf(domain, "TRUE", "/", "TRUE", deadlineSeconds.toString(), name, value).joinToString("\t")
        }
        if (lines.isEmpty()) return ""
        return ("# Netscape HTTP Cookie File" + "\n" + lines.joinToString("\n")).trimEnd('\r', '\n')
    }

    /**
     * Writes the supplied cookie header to a Netscape cookies.txt in cacheDir and
     * returns the file, or null when the input produces no usable cookies.
     */
    fun cookiesFile(context: Context, raw: String): File? {
        val content = toNetscape(raw)
        if (content.isBlank()) return null
        val file = File(context.cacheDir, "instagram-cookies.txt")
        file.writeText(content)
        return file
    }

    /**
     * Adds --cookies to [request] when [url] is Instagram and stored cookies exist.
     */
    fun applyIfInstagram(url: String, context: Context, request: com.yausername.youtubedl_android.YoutubeDLRequest) {
        if (!FormatSelector.isInstagramUrl(url)) return
        cookiesFile(context, InstagramCookieStore.load(context))?.let { request.addOption("--cookies", it.absolutePath) }
    }
}