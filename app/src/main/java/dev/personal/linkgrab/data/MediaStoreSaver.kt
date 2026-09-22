package dev.personal.linkgrab.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import dev.personal.linkgrab.model.DownloadType
import java.io.File

object MediaStoreSaver {
    fun save(context: Context, source: File, displayName: String, type: DownloadType): Pair<String, String> {
        val video = type == DownloadType.VIDEO
        val collection = if (video) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val mime = if (video) "video/mp4" else if (source.extension.equals("mp3", true)) "audio/mpeg" else "audio/mp4"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, if (video) "Movies/LinkGrab" else "Music/LinkGrab")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = requireNotNull(context.contentResolver.insert(collection, values)) { "Could not create media item" }
        try {
            context.contentResolver.openOutputStream(uri)?.use { output -> source.inputStream().use { it.copyTo(output) } }
                ?: error("Could not open media destination")
            values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            return uri.toString() to mime
        } catch (error: Throwable) {
            context.contentResolver.delete(uri, null, null)
            throw error
        }
    }
}
