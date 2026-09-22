package dev.personal.linkgrab.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp

@Composable
fun UrlInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onPasteFromClipboard: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Paste a link...") },
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
            singleLine = true
        )

        FilledTonalButton(
            onClick = {
                val clipText = clipboardManager.getText()?.toString() ?: ""
                if (clipText.isNotBlank()) {
                    onValueChange(clipText)
                }
                onPasteFromClipboard()
            },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
        }

        FilledTonalButton(
            onClick = onSubmit,
            enabled = value.isNotBlank(),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text("Go")
        }
    }
}