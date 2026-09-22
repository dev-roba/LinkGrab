package dev.personal.linkgrab.ui.history

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.produceState
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import dev.personal.linkgrab.LinkGrabApp
import dev.personal.linkgrab.model.AudioFormat
import dev.personal.linkgrab.model.DownloadType
import dev.personal.linkgrab.service.DownloadService

@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val repository = (context.applicationContext as LinkGrabApp).historyRepository
    val items by repository.history.collectAsState(initial = emptyList())
    if (items.isEmpty()) { Surface(Modifier.fillMaxSize()) { Text("Your saved downloads will appear here.") }; return }
    LazyColumn(Modifier.fillMaxSize()) { items(items, key = { it.id }) { item ->
        ListItem(leadingContent = { HistoryThumbnail(item.thumbnailUrl, item.downloadType) }, headlineContent = { Text(item.title) }, supportingContent = { Text("${item.downloadType} · Downloads/LinkGrab · ${formatBytes(item.sizeBytes)}") }, trailingContent = {
            TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.mediaUri)).setDataAndType(Uri.parse(item.mediaUri), item.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }) { Text("Open") }
        })
        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_SEND).setType(item.mimeType).putExtra(Intent.EXTRA_STREAM, Uri.parse(item.mediaUri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }) { Text("Share") }
        TextButton(onClick = { DownloadService.enqueue(context, item.url, DownloadType.valueOf(item.downloadType), AudioFormat.M4A, 1080) }) { Text("Download again") }
    } }
}

@Composable
private fun HistoryThumbnail(url: String?, type: String) {
    val bitmap by produceState<Bitmap?>(initialValue = null, url) {
        value = withContext(Dispatchers.IO) { url?.let { runCatching { URL(it).openStream().use(BitmapFactory::decodeStream) }.getOrNull() } }
    }
    if (bitmap != null) Image(bitmap!!.asImageBitmap(), "Thumbnail", Modifier.size(64.dp), contentScale = ContentScale.Crop)
    else Icon(if (type == DownloadType.VIDEO.name) Icons.Default.Videocam else Icons.Default.AudioFile, contentDescription = type, modifier = Modifier.size(40.dp))
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    else -> "%.0f KB".format(bytes / 1_000.0)
}
