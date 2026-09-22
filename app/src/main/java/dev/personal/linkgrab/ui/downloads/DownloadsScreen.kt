package dev.personal.linkgrab.ui.downloads

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.personal.linkgrab.model.DownloadStatus
import dev.personal.linkgrab.service.DownloadService
import dev.personal.linkgrab.service.DownloadStore

@Composable fun DownloadsScreen() {
    val context = LocalContext.current
    val jobs by DownloadStore.items.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Downloads", style = MaterialTheme.typography.headlineMedium) }
        item { val remaining = jobs.filter { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.RUNNING }.sumOf { (it.totalBytes ?: 0L) - it.downloadedBytes }; Text("${jobs.count { it.status == DownloadStatus.QUEUED }} queued · ${if (remaining > 0) "${remaining / 1_000_000} MB remaining" else "ready"}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(jobs, key = { it.id }) { job ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text(job.title ?: job.url, maxLines = 2)
                Text("${job.status.name.lowercase().replaceFirstChar { it.titlecase() }} · ${(job.progress * 100).toInt()}%")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (job.status == DownloadStatus.RUNNING || job.status == DownloadStatus.QUEUED) TextButton({ DownloadStore.cancel(context, job.id) }) { Text("Pause") }
                    if (job.status == DownloadStatus.PAUSED || job.status == DownloadStatus.FAILED) TextButton({ DownloadService.enqueue(context, job.url, job.type, job.audioFormat, job.quality, job.maxSizeBytes) }) { Text("Resume") }
                    if (job.status == DownloadStatus.FAILED) TextButton({ DownloadService.enqueue(context, job.url, job.type, job.audioFormat, job.quality, job.maxSizeBytes) }) { Text("Retry") }
                }
            } }
        }
    }
}
