package dev.personal.linkgrab.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.personal.linkgrab.service.JobStatus

@Composable
fun StatusChip(
    status: JobStatus,
    modifier: Modifier = Modifier
) {
    val (label, icon, containerColor) = when (status) {
        JobStatus.QUEUED -> Triple(
            "Queued",
            Icons.Filled.HourglassTop,
            MaterialTheme.colorScheme.secondaryContainer
        )
        JobStatus.RUNNING -> Triple(
            "Downloading",
            Icons.Filled.HourglassTop,
            MaterialTheme.colorScheme.tertiaryContainer
        )
        JobStatus.COMPLETED -> Triple(
            "Done",
            Icons.Filled.CheckCircle,
            MaterialTheme.colorScheme.primaryContainer
        )
        JobStatus.FAILED -> Triple(
            "Failed",
            Icons.Filled.Error,
            MaterialTheme.colorScheme.errorContainer
        )
        JobStatus.CANCELLED -> Triple(
            "Cancelled",
            Icons.Filled.HourglassTop,
            MaterialTheme.colorScheme.surfaceVariant
        )
    }

    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = status.name,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor
        ),
        modifier = modifier
    )
}