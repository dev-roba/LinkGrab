package dev.personal.linkgrab.ui.history

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.personal.linkgrab.LinkGrabApp
import dev.personal.linkgrab.data.history.HistoryEntity
import dev.personal.linkgrab.model.DownloadType
import dev.personal.linkgrab.playback.PlayItem
import dev.personal.linkgrab.playback.PlayerConnection
import dev.personal.linkgrab.ui.player.EqBars
import dev.personal.linkgrab.ui.player.rememberPlayerUi
import dev.personal.linkgrab.service.DownloadService
import dev.personal.linkgrab.ui.theme.Sticker
import dev.personal.linkgrab.ui.theme.StickerButton
import dev.personal.linkgrab.ui.theme.StickerCard
import dev.personal.linkgrab.ui.theme.StickerChip
import dev.personal.linkgrab.ui.theme.StickerPill
import dev.personal.linkgrab.ui.theme.sticker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun HistoryScreen(onOpenPlayer: () -> Unit = {}) {
    val context = LocalContext.current
    val app = context.applicationContext as LinkGrabApp
    val repository = app.historyRepository
    val scope = rememberCoroutineScope()
    val items by repository.history.collectAsState(initial = emptyList())
    var filter by remember { mutableStateOf("ALL") }
    var selected by remember { mutableStateOf<HistoryEntity?>(null) }

    val shown = items.filter { filter == "ALL" || it.downloadType == filter }
    val playerUi = rememberPlayerUi(PlayerConnection.player)
    val playingId = if (playerUi?.isPlaying == true) playerUi.mediaId else null
    val playFrom: (HistoryEntity) -> Unit = { entry ->
        val queue = shown.map { PlayItem(it.mediaUri, it.title, it.thumbnailUrl, it.downloadType == DownloadType.VIDEO.name) }
        PlayerConnection.playQueue(context, queue, shown.indexOfFirst { it.id == entry.id }.coerceAtLeast(0))
        onOpenPlayer()
    }
    val usedBytes = items.sumOf { it.sizeBytes }
    val deviceTotal = remember {
        runCatching { Environment.getExternalStorageDirectory().totalSpace }.getOrDefault(0L)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("LIBRARY", fontSize = 44.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StickerPill("${items.size} saved")
                    StickerPill("${formatBytes(usedBytes)} used")
                }
                if (deviceTotal > 0L) {
                    StorageBar(fraction = (usedBytes.toDouble() / deviceTotal.toDouble()).toFloat())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("ALL", "VIDEO", "AUDIO").forEach { name ->
                        StickerChip(
                            label = name,
                            selected = filter == name,
                            onClick = { filter = name },
                            modifier = Modifier.weight(1f),
                            height = 44.dp,
                            fontSize = 14.sp
                        )
                    }
                }
                Text("Tap to play. Hold for more.", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        if (shown.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                StickerCard {
                    Text(
                        "NO GRABS YET",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    )
                    Text(
                        "Your saved downloads will appear here.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        itemsIndexed(shown, key = { _, entry -> entry.id }) { index, entry ->
            LibraryCard(
                item = entry,
                tilt = if (index % 2 == 0) -1.2f else 1.2f,
                nowPlaying = playingId == entry.mediaUri,
                onPlay = { playFrom(entry) },
                onMore = { selected = entry }
            )
        }
    }

    selected?.let { item ->
        AlertDialog(
            onDismissRequest = { selected = null },
            confirmButton = {},
            containerColor = Sticker.White,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StickerButton(
                        text = "PLAY",
                        onClick = {
                            playFrom(item)
                            selected = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        container = Sticker.Blue,
                        content = Sticker.OnBlue,
                        height = 48.dp,
                        fontSize = 16.sp
                    )
                    StickerButton(
                        text = "SHARE",
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_SEND)
                                        .setType(item.mimeType)
                                        .putExtra(Intent.EXTRA_STREAM, Uri.parse(item.mediaUri))
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                )
                            }
                            selected = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        container = Sticker.White,
                        content = Sticker.Ink,
                        height = 48.dp,
                        fontSize = 16.sp
                    )
                    StickerButton(
                        text = "OPEN IN ANOTHER APP",
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(item.mediaUri))
                                        .setDataAndType(Uri.parse(item.mediaUri), item.mimeType)
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                )
                            }
                            selected = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        container = Sticker.White,
                        content = Sticker.Ink,
                        height = 48.dp,
                        fontSize = 16.sp
                    )
                    StickerButton(
                        text = "DOWNLOAD AGAIN",
                        onClick = {
                            scope.launch {
                                val current = app.settingsRepository.settings.first()
                                // Remove the old entry first so the duplicate check doesn't block the new download.
                                repository.remove(item)
                                DownloadService.enqueue(
                                    context,
                                    item.url,
                                    DownloadType.valueOf(item.downloadType),
                                    current.audioFormat,
                                    current.qualityCeiling
                                )
                            }
                            selected = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        container = Sticker.White,
                        content = Sticker.Ink,
                        height = 48.dp,
                        fontSize = 16.sp
                    )
                    StickerButton(
                        text = "DELETE",
                        onClick = {
                            scope.launch {
                                runCatching { context.contentResolver.delete(Uri.parse(item.mediaUri), null, null) }
                                repository.remove(item)
                            }
                            selected = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        container = Sticker.Pink,
                        content = Sticker.Black,
                        height = 48.dp,
                        fontSize = 16.sp
                    )
                }
            }
        )
    }
}

@Composable
private fun LibraryCard(
    item: HistoryEntity,
    tilt: Float,
    nowPlaying: Boolean,
    onPlay: () -> Unit,
    onMore: () -> Unit
) {
    val thumbShape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .rotate(tilt)
            .sticker(Sticker.White, 16.dp, 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onPlay() }, onLongPress = { onMore() })
            }
            .semantics(mergeDescendants = true) { contentDescription = "Play ${item.title}" }
            .padding(10.dp)
    ) {
        Box {
            LibraryThumbnail(
                url = item.thumbnailUrl,
                type = item.downloadType,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(thumbShape)
                    .border(Sticker.Border, Sticker.Ink, thumbShape)
            )
            StickerPill(
                text = platformOf(item.url),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp),
                background = Sticker.Pink,
                textColor = Sticker.Black
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(36.dp)
                    .sticker(Sticker.Blue, 18.dp, 2.dp, Sticker.Black),
                contentAlignment = Alignment.Center
            ) {
                if (nowPlaying) {
                    EqBars(playing = true, color = Sticker.OnBlue, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Sticker.OnBlue, modifier = Modifier.size(22.dp))
                }
            }
        }
        Text(
            text = item.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "${item.downloadType} · ${formatBytes(item.sizeBytes)}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LibraryThumbnail(url: String?, type: String, modifier: Modifier) {
    val bitmap by produceState<Bitmap?>(initialValue = null, url) {
        value = withContext(Dispatchers.IO) {
            url?.let { runCatching { URL(it).openStream().use(BitmapFactory::decodeStream) }.getOrNull() }
        }
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(loaded.asImageBitmap(), "Thumbnail", modifier, contentScale = ContentScale.Crop)
    } else {
        val isVideo = type == DownloadType.VIDEO.name
        Box(
            modifier = modifier.background(if (isVideo) Sticker.Blue else Sticker.Pink),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.AudioFile,
                contentDescription = type,
                tint = if (isVideo) Sticker.OnBlue else Sticker.Black,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun StorageBar(fraction: Float) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .clip(shape)
            .background(Sticker.Track)
            .border(Sticker.Border, Sticker.Ink, shape)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                .background(Sticker.Pink)
        )
    }
}

private fun platformOf(url: String): String {
    val host = runCatching { Uri.parse(url).host.orEmpty().lowercase() }.getOrDefault("")
    return when {
        host.isBlank() -> "LINK"
        "tiktok" in host -> "TIKTOK"
        "instagram" in host -> "INSTAGRAM"
        "youtu" in host -> "YOUTUBE"
        "twitter" in host || host == "x.com" || host.endsWith(".x.com") -> "X"
        "facebook" in host || "fb." in host -> "FACEBOOK"
        else -> host.removePrefix("www.").substringBefore('.').uppercase()
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    else -> "%.0f KB".format(bytes / 1_000.0)
}
