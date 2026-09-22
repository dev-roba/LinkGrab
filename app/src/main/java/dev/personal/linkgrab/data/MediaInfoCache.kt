package dev.personal.linkgrab.data

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.mapper.VideoInfo
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

object MediaInfoCache {
    private const val MAX_AGE_MILLIS = 30 * 60 * 1000L
    private data class Entry(val info: VideoInfo, val savedAt: Long)
    private val memory = ConcurrentHashMap<String, Entry>()

    fun put(context: Context, url: String, info: VideoInfo) {
        val now = System.currentTimeMillis()
        memory[url] = Entry(info, now)
        runCatching {
            val file = fileFor(context, url)
            file.parentFile?.mkdirs()
            file.writeText(writeJson(info))
            file.setLastModified(now)
        }
    }

    fun get(context: Context, url: String): VideoInfo? {
        val now = System.currentTimeMillis()
        memory[url]?.takeIf { now - it.savedAt <= MAX_AGE_MILLIS }?.let { return it.info }
        val file = fileFor(context, url)
        if (!file.exists() || now - file.lastModified() > MAX_AGE_MILLIS) return null
        return runCatching {
            readJson(file).also { memory[url] = Entry(it, file.lastModified()) }
        }.getOrNull()
    }

    fun fileFor(context: Context, url: String): File = File(File(context.cacheDir, "metadata"), "${sha256(url)}.json")

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun writeJson(info: VideoInfo): String {
        val mapper = YoutubeDL::class.java.getMethod("getObjectMapper").invoke(YoutubeDL.getInstance())
        return mapper.javaClass.getMethod("writeValueAsString", Any::class.java).invoke(mapper, info) as String
    }

    private fun readJson(file: File): VideoInfo {
        val mapper = YoutubeDL::class.java.getMethod("getObjectMapper").invoke(YoutubeDL.getInstance())
        return mapper.javaClass.getMethod("readValue", File::class.java, Class::class.java).invoke(mapper, file, VideoInfo::class.java) as VideoInfo
    }
}
