package dev.personal.linkgrab.data

import java.lang.reflect.Method

object MediaInfoSizer {
    fun estimate(info: Any, audioOnly: Boolean): Long? {
        val formats = (read(info, "formats") as? Iterable<*>)?.filterNotNull() ?: return direct(info)
        val sizes = formats.mapNotNull { format -> formatSize(format)?.let { format to it } }
        if (sizes.isEmpty()) return direct(info)
        if (audioOnly) {
            return sizes.filter { isAudioOnly(it.first) }.maxByOrNull { it.second }?.second
                ?: sizes.maxByOrNull { it.second }?.second
        }
        val video = sizes.filter { hasVideo(it.first) }.maxByOrNull { it.second }?.second
        val audio = sizes.filter { isAudioOnly(it.first) }.maxByOrNull { it.second }?.second
        return when {
            video != null && audio != null -> video + audio
            video != null -> video
            else -> sizes.maxByOrNull { it.second }?.second
        }
    }
    fun formatUnder(info: Any, audioOnly: Boolean, maxBytes: Long): String? {
        val formats = (read(info, "formats") as? Iterable<*>)?.filterNotNull() ?: return null
        return formats.mapNotNull { format ->
            val size = formatSize(format) ?: return@mapNotNull null
            if (size > maxBytes || (!audioOnly && !hasVideo(format)) || (audioOnly && !isAudioOnly(format))) return@mapNotNull null
            val id = read(format, "formatId") as? String ?: return@mapNotNull null
            id to size
        }.maxByOrNull { it.second }?.first
    }

    private fun direct(info: Any): Long? = sequenceOf("filesizeApprox", "filesize").mapNotNull { read(info, it) as? Number }.map { it.toLong() }.firstOrNull { it > 0 }
    private fun formatSize(format: Any): Long? = sequenceOf("filesize", "filesizeApprox").mapNotNull { read(format, it) as? Number }.map { it.toLong() }.firstOrNull { it > 0 }
    private fun hasVideo(format: Any): Boolean = (read(format, "vcodec") as? String)?.let { it != "none" } ?: false
    private fun isAudioOnly(format: Any): Boolean = (read(format, "acodec") as? String)?.let { it != "none" } == true && !hasVideo(format)
    private fun read(target: Any, name: String): Any? = runCatching {
        val method = target.javaClass.methods.firstOrNull { it.name.equals("get${name.replaceFirstChar(Char::uppercase)}", true) && it.parameterCount == 0 }
        method?.invoke(target) ?: target.javaClass.declaredFields.firstOrNull { it.name.equals(name, true) }?.let { it.isAccessible = true; it.get(target) }
    }.getOrNull()
}
