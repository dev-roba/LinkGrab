package dev.personal.linkgrab.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.personal.linkgrab.LinkGrabApp
import dev.personal.linkgrab.model.AudioFormat
import dev.personal.linkgrab.data.YtDlpUpdater
import kotlinx.coroutines.launch
import androidx.core.content.FileProvider
import android.content.Intent
import java.io.File
import dev.personal.linkgrab.util.DiagnosticsExporter
import android.provider.Settings

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repository = (context.applicationContext as LinkGrabApp).settingsRepository
    val settings by repository.settings.collectAsState(initial = dev.personal.linkgrab.model.AppSettings())
    val scope = rememberCoroutineScope()
    var updateMessage by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium) }
        item { ListItem(headlineContent = { Text("Default audio format") }, supportingContent = { Text(settings.audioFormat.name) }, trailingContent = { TextButton(onClick = { scope.launch { repository.setAudioFormat(if (settings.audioFormat == AudioFormat.M4A) AudioFormat.MP3 else AudioFormat.M4A) } }) { Text("Change") } }) }
        item { ListItem(headlineContent = { Text("Video quality") }, supportingContent = { Text("Best up to ${settings.qualityCeiling}p") }, trailingContent = { TextButton(onClick = { scope.launch { repository.setQuality(if (settings.qualityCeiling == 1080) 720 else 1080) } }) { Text("Change") } }) }
        item { ListItem(headlineContent = { Text("Automatically update yt-dlp") }, trailingContent = { Switch(settings.autoUpdateYtDlp, { scope.launch { repository.setAutoYtDlpUpdates(it) } }) }) }
        item { ListItem(headlineContent = { Text("Update yt-dlp now") }, supportingContent = { Text(updateMessage ?: "Downloads the latest extractor when available") }, trailingContent = { Button(onClick = { scope.launch { updateMessage = when (val result = YtDlpUpdater.update(context)) { YtDlpUpdater.Result.Updated -> "yt-dlp updated"; YtDlpUpdater.Result.Current -> "yt-dlp is already current"; is YtDlpUpdater.Result.Failed -> result.message } } }) { Text("Update") } }) }
        item { ListItem(headlineContent = { Text("Check for app updates") }, supportingContent = { Text("Checks GitHub Releases when configured") }, trailingContent = { Button(onClick = {}) { Text("Check") } }) }
        item { ListItem(headlineContent = { Text("Send this app") }, supportingContent = { Text("Share the LinkGrab APK over Quick Share or Bluetooth") }, trailingContent = { Button(onClick = {
            val shareDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val apk = File(shareDir, "LinkGrab.apk")
            context.applicationInfo.sourceDir.let { File(it).copyTo(apk, overwrite = true) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/vnd.android.package-archive").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Send LinkGrab"))
        }) { Text("Share") } }) }
        item { ListItem(headlineContent = { Text("Export diagnostics") }, supportingContent = { Text("Shares redacted technical details; URLs and personal data are excluded") }, trailingContent = { Button(onClick = { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, DiagnosticsExporter.report(context)), "Share diagnostics")) }) { Text("Export") } }) }
        item { ListItem(headlineContent = { Text("Manage storage") }, supportingContent = { Text("Free space is checked before downloads") }, trailingContent = { TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)) }) { Text("Open") } }) }
    }
}
