package dev.personal.linkgrab.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
// import androidx.compose.ui.text.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.personal.linkgrab.service.MediaType
import dev.personal.linkgrab.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun MetadataCard(
    title: String,
    thumbnailUrl: String? = null,
    duration: String? = null,
    uploader: String? = null,
    estimatedSizeBytes: Long? = null,
    downloadType: MediaType = MediaType.VIDEO,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Thumbnail placeholder
            if (thumbnailUrl != null) {
                val thumbnail by produceState<Bitmap?>(initialValue = null, thumbnailUrl) {
                    value = withContext(Dispatchers.IO) {
                        runCatching { URL(thumbnailUrl).openStream().use(BitmapFactory::decodeStream) }.getOrNull()
                    }
                }
                val bitmap = thumbnail
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Video thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).padding(bottom = 12.dp)
                    )
                } else {
                    androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 12.dp))
                }
            }

            // Title
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                maxLines = 2
            )
            if (estimatedSizeBytes != null && estimatedSizeBytes > 0) {
                Text("Estimated size: ${formatBytes(estimatedSizeBytes)}", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            }

            // Metadata row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (duration != null) {
                    MetadataChip(
                        icon = Icons.Default.AccessTime,
                        text = duration
                    )
                }
                if (uploader != null) {
                    MetadataChip(
                        icon = Icons.Default.Person,
                        text = uploader
                    )
                }
                // Type indicator
                MetadataChip(
                    icon = when (downloadType) {
                        MediaType.VIDEO -> Icons.Default.Videocam
                        MediaType.AUDIO -> Icons.Default.AudioFile
                    },
                    text = downloadType.name
                )
            }

            // Download button
            androidx.compose.material3.Button(
                onClick = onDownloadClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Download", fontSize = 16.sp)
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    else -> "%.0f KB".format(bytes / 1_000.0)
}

@Composable
private fun MetadataChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
