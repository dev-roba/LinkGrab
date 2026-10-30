package dev.personal.linkgrab.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.personal.linkgrab.service.MediaType
import dev.personal.linkgrab.ui.theme.Sticker
import dev.personal.linkgrab.ui.theme.StickerButton
import dev.personal.linkgrab.ui.theme.sticker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Preview card. The download button is optional: pass [onDownloadClick] to render one inside the card,
 * or leave it null when the screen shows its own DOWNLOAD NOW button (as HomeScreen does).
 */
@Composable
fun MetadataCard(
    title: String,
    thumbnailUrl: String? = null,
    duration: String? = null,
    uploader: String? = null,
    estimatedSizeBytes: Long? = null,
    downloadType: MediaType = MediaType.VIDEO,
    onDownloadClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .sticker(Sticker.White, 16.dp, 4.dp)
            .padding(12.dp)
    ) {
        if (thumbnailUrl != null) {
            val thumbnail by produceState<Bitmap?>(initialValue = null, thumbnailUrl) {
                value = withContext(Dispatchers.IO) {
                    runCatching { URL(thumbnailUrl).openStream().use(BitmapFactory::decodeStream) }.getOrNull()
                }
            }
            val bitmap = thumbnail
            val thumbShape = RoundedCornerShape(12.dp)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Video thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(thumbShape)
                        .border(Sticker.Border, Sticker.Ink, thumbShape)
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Sticker.Ink,
                    trackColor = Sticker.Track
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (estimatedSizeBytes != null && estimatedSizeBytes > 0) {
            Text(
                text = "Estimated size: ${formatBytes(estimatedSizeBytes)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (duration != null) {
                MetadataChip(icon = Icons.Default.AccessTime, text = duration)
            }
            if (uploader != null) {
                MetadataChip(
                    icon = Icons.Default.Person,
                    text = uploader,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            MetadataChip(
                icon = when (downloadType) {
                    MediaType.VIDEO -> Icons.Default.Videocam
                    MediaType.AUDIO -> Icons.Default.AudioFile
                },
                text = downloadType.name
            )
        }

        if (onDownloadClick != null) {
            StickerButton(
                text = "DOWNLOAD",
                onClick = onDownloadClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
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
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Sticker.Ink
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Sticker.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
