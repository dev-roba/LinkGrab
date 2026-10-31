package dev.personal.linkgrab.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.personal.linkgrab.model.DownloadItem
import dev.personal.linkgrab.model.DownloadStatus
import dev.personal.linkgrab.model.DownloadType
import dev.personal.linkgrab.service.DownloadService
import dev.personal.linkgrab.service.DownloadStore
import dev.personal.linkgrab.ui.theme.Sticker
import dev.personal.linkgrab.ui.theme.StickerButton
import dev.personal.linkgrab.ui.theme.StickerCard
import dev.personal.linkgrab.ui.theme.StickerPill
import dev.personal.linkgrab.util.EtaFormatter

@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    val jobs by DownloadStore.items.collectAsState()
    val queued = jobs.count { it.status == DownloadStatus.QUEUED }
    val remaining = jobs
        .filter { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.RUNNING }
        .sumOf { (it.totalBytes ?: 0L) - it.downloadedBytes }
        .coerceAtLeast(0L)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("QUEUE", fontSize = 44.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StickerPill("$queued waiting")
                StickerPill(if (remaining > 0) "${remaining / 1_000_000} MB left" else "Ready")
            }
        }
        if (jobs.isEmpty()) {
            item {
                StickerCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "NOTHING IN THE QUEUE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    )
                    Text(
                        "Paste a link on Home and tap DOWNLOAD NOW. It will show up here.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        items(jobs, key = { it.id }) { job ->
            QueueCard(
                job = job,
                onPause = { DownloadStore.cancel(context, job.id) },
                onResume = {
                    DownloadService.enqueue(context, job.url, job.type, job.audioFormat, job.quality, job.maxSizeBytes)
                }
            )
        }
    }
}

@Composable
private fun QueueCard(job: DownloadItem, onPause: () -> Unit, onResume: () -> Unit) {
    val pct = (job.progress * 100).toInt().coerceIn(0, 100)
    val running = job.status == DownloadStatus.RUNNING
    val (label, pillBackground, pillText) = when (job.status) {
        DownloadStatus.RUNNING -> Triple("DOWNLOADING", Sticker.Blue, Sticker.OnBlue)
        DownloadStatus.QUEUED -> Triple("WAITING", Sticker.White, Sticker.Ink)
        DownloadStatus.PAUSED -> Triple("PAUSED", Sticker.White, Sticker.Ink)
        DownloadStatus.FAILED -> Triple("FAILED", Sticker.Pink, Sticker.Black)
        DownloadStatus.COMPLETED -> Triple("DONE", Sticker.Yellow, Sticker.Ink)
        else -> Triple(job.status.name, Sticker.White, Sticker.Ink)
    }

    StickerCard(modifier = Modifier.fillMaxWidth()) {
        Column16 {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StickerPill(label, background = pillBackground, textColor = pillText)
                StickerPill(if (job.type == DownloadType.VIDEO) "VIDEO" else "AUDIO")
            }
            Text(
                text = job.title ?: job.url,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
            if (running) {
                Text(
                    text = "$pct%",
                    fontSize = 44.sp,
                    lineHeight = 48.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
                ThickProgress(
                    progress = job.progress.toFloat(),
                    fill = Sticker.Blue,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = detail(job),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            val canPause = job.status == DownloadStatus.RUNNING || job.status == DownloadStatus.QUEUED
            val canResume = job.status == DownloadStatus.PAUSED ||
                job.status == DownloadStatus.FAILED ||
                job.status == DownloadStatus.CANCELLED
            if (canPause || canResume) {
                Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (canPause) {
                        StickerButton(
                            text = "PAUSE",
                            onClick = onPause,
                            container = Sticker.White,
                            content = Sticker.Ink,
                            height = 44.dp,
                            radius = 12.dp,
                            shadowOffset = 3.dp,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (canResume) {
                        StickerButton(
                            text = if (job.status == DownloadStatus.FAILED) "RETRY" else "RESUME",
                            onClick = onResume,
                            container = Sticker.Blue,
                            content = Sticker.OnBlue,
                            height = 44.dp,
                            radius = 12.dp,
                            shadowOffset = 3.dp,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/** Padding wrapper so card content has breathing room inside the sticker border. */
@Composable
private fun Column16(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp), content = content)
}

@Composable
private fun ThickProgress(progress: Float, fill: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .clip(shape)
            .background(Sticker.Track)
            .border(Sticker.Border, Sticker.Ink, shape)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(fill)
        )
    }
}

private fun detail(job: DownloadItem): String = when (job.status) {
    DownloadStatus.RUNNING -> when (job.etaState) {
        "STALLED" -> "Stalled. Tap PAUSE then RESUME to retry."
        "ESTIMATING" -> "${EtaFormatter.speed(job.speedBytesPerSecond)} · ${EtaFormatter.duration(job.etaSeconds)} left"
        else -> "Preparing…"
    }
    DownloadStatus.FAILED -> job.errorMessage ?: "Failed"
    DownloadStatus.PAUSED -> "Paused. Resume when ready."
    DownloadStatus.COMPLETED -> "Saved to ${if (job.type == DownloadType.VIDEO) "Movies" else "Music"}/LinkGrab"
    else -> job.status.name.lowercase().replaceFirstChar { it.titlecase() }
}
